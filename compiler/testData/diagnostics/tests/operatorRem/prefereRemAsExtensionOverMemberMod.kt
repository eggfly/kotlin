// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// LANGUAGE: -ProhibitOperatorMod
// DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int): Foo = Foo()
}

operator fun Foo.rem(x: Int): Int = 0


fun foo() {
    takeInt(Foo() % 1)
}

fun takeInt(x: Int) {}