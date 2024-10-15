// RUN_PIPELINE_TILL: SOURCE
// ISSUE: KT-47979

enum class Foo { A, B }

fun test() {
    if (Triple(Foo.A, 1, 2) == Pair("a", "b")) println("Doesn't compile")
    if (Triple(0, 1, 2) == Pair(Foo.A, "a")) println("Doesn't compile")
    if (Triple(0, 1, 2) == Pair("a", "b")) println("Doesn't compile")
    if (Triple(Foo.A, 1, 2) == Pair(Foo.A, "a")) println("Compiles, but why?")

    <!EQUALITY_NOT_APPLICABLE!>Triple(Foo.A, 1, 2) === Pair("a", "b")<!>
    <!EQUALITY_NOT_APPLICABLE!>Triple(0, 1, 2) === Pair(Foo.A, "a")<!>
    <!EQUALITY_NOT_APPLICABLE!>Triple(0, 1, 2) === Pair("a", "b")<!>
    <!EQUALITY_NOT_APPLICABLE_WARNING!>Triple(Foo.A, 1, 2) === Pair(Foo.A, "a")<!>
}
