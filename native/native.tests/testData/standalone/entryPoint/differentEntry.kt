// OUTPUT_DATA_FILE: differentEntry.out
// FIR_IDENTICAL
// ENTRY_POINT: foo

import kotlin.test.*

fun foo() {
    fail()
}

fun foo(args: Array<String>) {
    println("OK")
}

fun bar() {
    fail()
}

fun main(args: Array<String>) {
    fail()
}
