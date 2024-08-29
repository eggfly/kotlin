plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
