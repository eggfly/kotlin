package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

/**
 * Instance which describes specific runtimes for JS and Wasm targets
 *
 * It encapsulates necessary information about a tool to run application and tests
 */
abstract class EnvSpec<T> {

    /**
     * Specify whether we need to download the tool
     */
    abstract val download: Property<Boolean>

    /**
     * Specify url to add repository from which the tool is going to be downloaded
     *
     * If the property has no value, repository is not added,
     * so this can be used to add your own repository where the tool is located
     */
    abstract val downloadBaseUrl: Property<String>

    /**
     * Specify where the tool is installed
     */
    abstract val installationDirectory: DirectoryProperty

    /**
     * Specify a version of the tool is installed
     */
    abstract val version: Property<String>

    /**
     * Specify a command to run the tool
     */
    abstract val command: Property<String>

    /**
     * Produce  full serializable cache-friendly entity without Gradle Provider API
     */
    abstract fun produceEnv(providerFactory: ProviderFactory): Provider<T>
}
