// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
interface Tr
interface G<T>

fun test(tr: Tr) = tr is <!NO_TYPE_ARGUMENTS_ON_RHS!>G<!>