// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
package extensionFunctions

<!CONFLICTING_OVERLOADS!>fun Int.qwe(a: Float)<!> = 1

<!CONFLICTING_OVERLOADS!>fun Int.qwe(a: Float)<!> = 2
