/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.dependencies.NativeDependenciesExtension
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanJvmInteropTask
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.utils.capitalized

class NamedNativeInteropConfig(
        project: Project,
        private val _name: String,
) : Named {
    override fun getName(): String = _name

    val genTask = project.tasks.register<KonanJvmInteropTask>("gen${name.capitalized}InteropStubs") {
        val nativeDependencies = project.extensions.getByType<NativeDependenciesExtension>()
        dependsOn(nativeDependencies.hostPlatformDependency)
        dependsOn(nativeDependencies.llvmDependency)
        stubGeneratorClasspath.from(project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CONFIGURATION))
        stubGeneratorNativeLibraries.from(project.configurations.getByName(NativeInteropPlugin.INTEROP_STUB_GENERATOR_CPP_RUNTIME_CONFIGURATION))
        generatedSrcDir.convention(project.layout.buildDirectory.dir("nativeInteropStubs/$name/kotlin"))
        temporaryFilesDir.convention(project.layout.buildDirectory.dir("interopTemp"))
        target.set(HostManager.host.name)
    }
}