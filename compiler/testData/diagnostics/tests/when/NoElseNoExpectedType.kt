// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 9 -> sentence 2
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 1 -> sentence 1
 */

fun foo(x: Int): Any {
    val v = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        2 -> 0
    }
    return v
}
