// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// dummy test of syntax error highlighting in tests

fun get() {
    1 + 2 <!SYNTAX!>2 3 4<!>
}
