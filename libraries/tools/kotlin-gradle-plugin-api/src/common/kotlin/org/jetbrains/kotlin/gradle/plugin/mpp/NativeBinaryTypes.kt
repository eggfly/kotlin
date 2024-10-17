/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*

/**
 * Enum class representing the build types for a native target in a Kotlin/Native project.
 *
 * @property optimized Indicates whether the build is optimized (e.g., for release builds).
 * @property debuggable Indicates whether the build includes debug information.
 *
 * These are typically used to differentiate between release and debug builds.
 */
enum class NativeBuildType(
    val optimized: Boolean,
    val debuggable: Boolean,
) : Named {
    /**
     * A release build type, intended for production use.
     * Optimized for performance but without debugging symbols.
     */
    RELEASE(true, false),

    /**
     * A debug build type, intended for development and debugging.
     * Not optimized for performance, but includes debugging symbols.
     */
    DEBUG(false, true);

    /**
     * Returns the name of this build type.
     * This is typically used for task names or logs.
     *
     * @return The name of the build type.
     */
    override fun getName(): String = name.toLowerCase(Locale.ENGLISH)

    /** @suppress **/
    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("UNUSED_PARAMETER")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, ReplaceWith(""))
    fun embedBitcode(target: KonanTarget) = BitcodeEmbeddingMode.DISABLE

    /** @suppress **/
    companion object {
        val DEFAULT_BUILD_TYPES = setOf(DEBUG, RELEASE)
    }
}

/**
 * Enum class representing different output types generated by the Kotlin/Native compiler.
 * Each output type (or "kind") defines the format of the binary produced, such as an executable,
 * a dynamic library, or a framework.
 *
 * @property compilerOutputKind The internal representation of the output type used by the Kotlin/Native compiler.
 * @property taskNameClassifier A string used to classify tasks related to this output kind, typically appended to
 * task names to distinguish between tasks producing different types of binaries. For example, "executable" or "static".
 * @property description A human-readable description of the output type, typically shown in logs or error messages,
 * helping developers quickly understand the nature of the binary being generated (e.g., "an executable").
 */
enum class NativeOutputKind(
    val compilerOutputKind: CompilerOutputKind,
    val taskNameClassifier: String,
    val description: String = taskNameClassifier,
) {
    /** Represents an executable output type, such as a standalone program. */
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        "executable",
        description = "an executable"
    ),

    /** Represents a test executable, typically used for running unit or integration tests. */
    TEST(
        CompilerOutputKind.PROGRAM,
        "test",
        description = "a test executable"
    ),

    /** Represents a dynamic (shared) library, which is loaded at runtime by other programs. */
    DYNAMIC(
        CompilerOutputKind.DYNAMIC,
        "shared",
        description = "a dynamic library"
    ),

    /** Represents a static library, which is linked into an executable at compile time. */
    STATIC(
        CompilerOutputKind.STATIC,
        "static",
        description = "a static library"
    ),

    /**
     * Represents a framework output type, used for iOS/macOS applications.
     * For more information see Apple's documentation on [Bundles and frameworks](https://kotl.in/s1yfbb).
     */
    FRAMEWORK(
        CompilerOutputKind.FRAMEWORK,
        "framework",
        description = "a framework"
    ) {
        /** Frameworks are available only for Apple platforms. */
        override fun availableFor(target: KonanTarget) =
            target.family.isAppleFamily
    };

    /**
     * Determines whether this output kind is supported for the specified target platform.
     *
     * By default, all output kinds are available for all platforms.
     *
     * @param target The target platform whose availability is being checked.
     * @return Boolean indicating whether the output kind is available for the given target.
     */
    open fun availableFor(target: KonanTarget) = true
}

/**
 * An enum class representing different modes of embedding LLVM IR bitcode in a binary.
 *
 * Bitcode is used in Apple's platforms to defer part of the compilation process to the
 * device where the application is running. These options allow control over whether
 * bitcode should be included in the binary and how it should be embedded.
 *
 * @property DISABLE No bitcode is embedded in the binary.
 * @property BITCODE Bitcode is embedded as part of the binary, allowing later optimization.
 * @property MARKER Only a placeholder marker is embedded instead of actual bitcode.
 */
enum class BitcodeEmbeddingMode {
    /** Don't embed LLVM IR bitcode. */
    DISABLE,

    /** Embed LLVM IR bitcode as data. */
    BITCODE,

    /** Embed placeholder LLVM IR data as a marker. */
    MARKER,
}

/** @suppress **/
@InternalKotlinGradlePluginApi
const val BITCODE_EMBEDDING_DEPRECATION_MESSAGE =
    "Bitcode embedding is not supported anymore. Configuring it has no effect. The corresponding DSL parameters will be removed in Kotlin 2.2"
