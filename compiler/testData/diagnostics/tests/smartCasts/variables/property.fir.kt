// RUN_PIPELINE_TILL: SOURCE
class MyClass(var p: String?)

fun bar(s: String): Int {
    return s.length
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return bar(<!ARGUMENT_TYPE_MISMATCH!>m.p<!>)
}
