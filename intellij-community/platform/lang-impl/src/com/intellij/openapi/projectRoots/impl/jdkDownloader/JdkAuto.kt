// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl.jdkDownloader

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.SimpleJavaSdkType.notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.openapi.projectRoots.impl.UnknownSdkTracker
import com.intellij.openapi.roots.ui.configuration.*
import com.intellij.openapi.roots.ui.configuration.SdkDetector.DetectedSdkListener
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.UnknownSdkLookup
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.lang.JavaVersion
import com.intellij.util.text.nullize
import com.intellij.util.xmlb.annotations.XCollection
import org.jetbrains.jps.model.java.JdkVersionDetector
import java.io.File

private class JdkAutoHint: BaseState() {
  val name by string()
  val path by string()
  val version by string()
}

private class JdkAutoHints : BaseState() {
  @get:XCollection
  val jdks by list<JdkAutoHint>()
}

private class JdkAutoHintService(private val project: Project) : SimplePersistentStateComponent<JdkAutoHints>(JdkAutoHints()) {
  override fun loadState(state: JdkAutoHints) {
    super.loadState(state)

    UnknownSdkTracker.getInstance(project).updateUnknownSdks()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) : JdkAutoHintService = project.service()
  }
}

private val LOG = logger<JdkAuto>()

class JdkAuto : UnknownSdkResolver, JdkDownloaderBase {
  override fun supportsResolution(sdkTypeId: SdkTypeId) = notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType().value(sdkTypeId)

  override fun createResolver(project: Project?, indicator: ProgressIndicator): UnknownSdkLookup? {
    if (!Registry.`is`("jdk.auto.setup")) return null
    if (ApplicationManager.getApplication().isUnitTestMode) return null
    return createResolverImpl(project, indicator)
  }

  fun createResolverImpl(project: Project?, indicator: ProgressIndicator): UnknownSdkLookup? {
    val sdkType = SdkType.getAllTypes()
                    .singleOrNull(notSimpleJavaSdkTypeIfAlternativeExistsAndNotDependentSdkType()::value) ?: return null

    return object : UnknownSdkLookup {
      val lazyDownloadModel: List<JdkItem> by lazy {
        indicator.pushState()
        indicator.text = "Downloading JDK list..."
        try {
          JdkListDownloader.getInstance().downloadModelForJdkInstaller(indicator)
        } catch(e: ProcessCanceledException) {
          throw e
        } catch (t: Throwable) {
          LOG.warn("JdkAuto has failed to download the list of available JDKs. " + t.message, t)
          listOf<JdkItem>()
        } finally {
          indicator.popState()
        }
      }

      private fun resolveHint(sdk: UnknownSdk) : JdkAutoHint? {
        if (sdk.sdkType != sdkType) return null

        project ?: return null
        val sdkName = sdk.sdkName ?: return null

        return JdkAutoHintService
          .getInstance(project)
          .state
          .jdks.singleOrNull { it.name.equals(sdkName, ignoreCase = true) }
      }

      private fun parseSdkRequirement(sdk: UnknownSdk): JdkRequirement? {
        val hint = resolveHint(sdk)

        val namePredicate = hint?.version?.trim()?.toLowerCase()?.nullize(true) ?: sdk.sdkName

        return JdkRequirements.parseRequirement(
          namePredicate = namePredicate,
          versionStringPredicate = sdk.sdkVersionStringPredicate,
          homePredicate = sdk.sdkHomePredicate
        )
      }

      private fun resolveHintPath(sdk: UnknownSdk, indicator: ProgressIndicator) :UnknownSdkLocalSdkFix? {
        val hint = resolveHint(sdk)
        val path = hint?.path ?: return null
        indicator.text = "Resolving hint path: $path..."
        if (!File(path).isDirectory) return null

        val version = runCatching {
          sdkType.getVersionString(hint.path)
        }.getOrNull() ?: return null

        return object : UnknownSdkLocalSdkFix {
          override fun getExistingSdkHome(): String = path
          override fun getVersionString(): String = version
          override fun getSuggestedSdkName() = sdkType.suggestSdkName(null, hint.path)
          override fun toString() = "resolved to hint $version, $path"
        }
      }

      override fun proposeDownload(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkDownloadableSdkFix? {
        if (sdk.sdkType != sdkType) return null

        val req = parseSdkRequirement(sdk) ?: return null
        LOG.info("Looking for a possible download for ${sdk.sdkType.presentableName} with name ${sdk}")

        //we select the newest matching version for a possible fix
        val jdkToDownload = lazyDownloadModel
                              .filter { req.matches(it) }
                              .mapNotNull {
                                val v = JavaVersion.tryParse(it.versionString)
                                if (v != null) {
                                  it to v
                                }
                                else null
                              }.maxBy { it.second }
                              ?.first ?: return null

        return object: UnknownSdkDownloadableSdkFix {
          override fun getVersionString() = jdkToDownload.versionString
          override fun getDownloadDescription() = jdkToDownload.fullPresentationText

          override fun createTask(indicator: ProgressIndicator): SdkDownloadTask {
            val jdkInstaller = JdkInstaller.getInstance()
            val homeDir = jdkInstaller.defaultInstallDir(jdkToDownload)
            val request = jdkInstaller.prepareJdkInstallation(jdkToDownload, homeDir)
            return newDownloadTask(request, project)
          }
        }
      }

      val lazyLocalJdks by lazy {
        indicator.text = "Detecting local JDKs..."
        val result = mutableListOf<JavaLocalSdkFix>()

        SdkDetector.getInstance().detectSdks(sdkType, indicator, object : DetectedSdkListener {
          override fun onSdkDetected(type: SdkType, version: String, home: String) {
            val javaVersion = JavaVersion.tryParse(version) ?: return
            val suggestedName = JdkUtil.suggestJdkName(version) ?: return
            result += JavaLocalSdkFix(home, javaVersion, suggestedName)
          }
        })

        result
      }

      override fun proposeLocalFix(sdk: UnknownSdk, indicator: ProgressIndicator): UnknownSdkLocalSdkFix? {
        if (sdk.sdkType != sdkType) return null

        val hintMatch = resolveHintPath(sdk, indicator)
        if (hintMatch != null) return hintMatch

        val req = parseSdkRequirement(sdk) ?: return null
        LOG.info("Looking for a local SDK for ${sdk.sdkType.presentableName} with name ${sdk}")

        fun List<JavaLocalSdkFix>.pickBestMatch() = this.maxBy { it.version }

        return tryUsingExistingSdk(req, sdk.sdkType, indicator).pickBestMatch()
               ?: lazyLocalJdks.filter { req.matches(it) }.pickBestMatch()
      }

      private fun tryUsingExistingSdk(req: JdkRequirement, sdkType: SdkType, indicator: ProgressIndicator): List<JavaLocalSdkFix> {
        indicator.text = "Checking existing SDKs..."

        val result = mutableListOf<JavaLocalSdkFix>()
        for (it in runReadAction { ProjectJdkTable.getInstance().allJdks }) {
          if (it.sdkType != sdkType) continue

          val homeDir = runCatching { it.homePath }.getOrNull() ?: continue
          val versionString = runCatching { it.versionString }.getOrNull() ?: continue
          val version = runCatching { JavaVersion.tryParse(versionString) }.getOrNull() ?: continue
          val suggestedName = runCatching { JdkUtil.suggestJdkName(versionString) }.getOrNull() ?: continue

          if (it !is MockSdk && runCatching { sdkType.isValidSdkHome(it.homePath) }.getOrNull() != true) continue
          if (runCatching { req.matches(it) }.getOrNull() != true) continue

          result += JavaLocalSdkFix(homeDir, version, suggestedName)
        }

        return result
      }
    }
  }

  private class JavaLocalSdkFix(val homeDir: String,
                                val version: JavaVersion,
                                val suggestedName: String) : UnknownSdkLocalSdkFix {
    override fun getExistingSdkHome() = homeDir
    override fun getVersionString() = JdkVersionDetector.formatVersionString(version)
    override fun getSuggestedSdkName() : String = suggestedName
  }
}
