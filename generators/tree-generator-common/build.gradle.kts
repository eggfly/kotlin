plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))

    implementation(project(":compiler:util"))
    implementation(project(":generators"))
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
