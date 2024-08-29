plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishJarsForIde(
    listOf(
        ":kotlin-assignment-compiler-plugin.cli",
        ":kotlin-assignment-compiler-plugin.common",
        ":kotlin-assignment-compiler-plugin.k1",
        ":kotlin-assignment-compiler-plugin.k2"
    )
)
