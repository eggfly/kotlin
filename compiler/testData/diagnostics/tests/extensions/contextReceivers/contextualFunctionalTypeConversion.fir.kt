// RUN_PIPELINE_TILL: SOURCE
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class A
class B

fun expectAB(f: context(A, B) () -> Unit) {
    f(A(), B())
}

fun test() {
    val l: context(B, A) () -> Unit = { }
    expectAB(<!ARGUMENT_TYPE_MISMATCH!>l<!>)
}
