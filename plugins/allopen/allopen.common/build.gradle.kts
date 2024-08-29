description = "Kotlin NoArg Compiler Plugin (Common)"

plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":core:compiler.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
