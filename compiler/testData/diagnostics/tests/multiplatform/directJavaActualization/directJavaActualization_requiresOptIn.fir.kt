// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect annotation class Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.RequiresOptIn
@kotlin.jvm.KotlinActual
public @interface Foo {}
