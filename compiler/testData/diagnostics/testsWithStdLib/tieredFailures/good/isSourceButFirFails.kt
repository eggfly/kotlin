// FIR_IDENTICAL
// ISSUE: KT-67281
// RUN_PIPELINE_TILL: SOURCE

fun test() {
    <!UNRESOLVED_REFERENCE!>nonExistingFunction<!>()
}
