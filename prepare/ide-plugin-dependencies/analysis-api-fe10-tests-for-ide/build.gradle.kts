plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

publishTestJarsForIde(listOf(":analysis:analysis-api-fe10"))
