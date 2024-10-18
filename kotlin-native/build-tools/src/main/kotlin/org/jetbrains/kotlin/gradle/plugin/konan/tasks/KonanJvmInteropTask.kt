/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.plugin.konan.prepareAsOutput
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.platformManagerProvider
import java.io.File
import javax.inject.Inject

private abstract class KonanJvmInteropAction @Inject constructor(
        val execOperations: ExecOperations
) : WorkAction<KonanJvmInteropAction.Parameters> {
    interface Parameters : WorkParameters {
        val stubGeneratorClasspath: ConfigurableFileCollection
        val systemProperties: MapProperty<String, String>
        val args: ListProperty<String>
        val platformManager: Property<PlatformManager>
    }

    override fun execute() {
        execOperations.javaexec {
            classpath(parameters.stubGeneratorClasspath)
            mainClass.set("org.jetbrains.kotlin.native.interop.gen.jvm.MainKt")
            jvmArgs("-ea")
            systemProperties.putAll(parameters.systemProperties.get())
            environment["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
            environment["PATH"] = (parameters.platformManager.get().hostPlatform.clang.clangPaths + listOf(environment["PATH"])).joinToString(separator = File.pathSeparator)
            args(parameters.args.get())
        }
    }
}

/**
 * A task executing JVM-flavor of cinterop.
 *
 * It generated sources to be compiled separately.
 */
open class KonanJvmInteropTask @Inject constructor(
        objectFactory: ObjectFactory,
        private val workerExecutor: WorkerExecutor,
) : DefaultTask() {
    @get:Input
    val target: Property<String> = objectFactory.property(String::class.java)

    /**
     * .def file for which to produce Kotlin bindings.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY) // def file name determines package name
    val defFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Compiler options for clang.
     *
     * Used by the indexing procedure.
     */
    @get:Input
    val compilerOpts: ListProperty<String> = objectFactory.listProperty(String::class.java)

    /**
     * Directory in which generated Kotlin bindings will be placed.
     */
    @get:OutputDirectory
    val generatedSrcDir: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Directory in which generated C bindings will be placed.
     */
    @get:OutputDirectory
    val temporaryFilesDir: DirectoryProperty = objectFactory.directoryProperty()

    /**
     * Classpath of StubGenerator tool.
     */
    @get:Classpath
    val stubGeneratorClasspath: ConfigurableFileCollection = objectFactory.fileCollection()

    /**
     * Native libraries for StubGenerator tool.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    val stubGeneratorNativeLibraries: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Nested
    protected val platformManagerProvider = objectFactory.platformManagerProvider(project)

    @TaskAction
    fun run() {
        generatedSrcDir.get().asFile.prepareAsOutput()
        temporaryFilesDir.get().asFile.prepareAsOutput()

        val systemProperties = buildMap {
            // Set the konan.home property because we run the cinterop tool not from a distribution jar
            // so it will not be able to determine this path by itself.
            put("konan.home", platformManagerProvider.distribution.get().root.asFile.absolutePath)
            put("java.library.path", stubGeneratorNativeLibraries.files.joinToString(File.pathSeparator) { it.parentFile.absolutePath })
        }

        val args = buildList {
            add("-generated")
            add(generatedSrcDir.get().asFile.absolutePath)
            add("-Xtemporary-files-dir")
            add(temporaryFilesDir.get().asFile.absolutePath)
            add("-flavor")
            add("jvm")
            add("-def")
            add(defFile.get().asFile.absolutePath)
            add("-target")
            add(target.get())
            compilerOpts.get().flatMapTo(this) {
                listOf("-compiler-option", it)
            }
        }

        val workQueue = workerExecutor.noIsolation()
        workQueue.submit(KonanJvmInteropAction::class.java) {
            this.stubGeneratorClasspath.from(this@KonanJvmInteropTask.stubGeneratorClasspath)
            this.systemProperties.set(systemProperties)
            this.args.addAll(args)
        }
    }
}