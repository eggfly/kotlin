// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
class Foo<T> {
    @<!NOT_A_CLASS!>T<!>
    fun foo() = 0
}

class Bar<T : Annotation> {
    @<!NOT_A_CLASS!>T<!>
    fun foo() = 0
}
