// RUN_PIPELINE_TILL: SOURCE
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers

open class A
class B
class C: A()

context(A)
fun B.f() {}

fun main() {
    val b = B()

    b.<!NO_CONTEXT_RECEIVER!>f<!>()
    with(A()) {
        b.f()
    }
    with(C()) {
        b.f()
    }
}
