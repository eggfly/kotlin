// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// LANGUAGE: -ForbidInferringTypeVariablesIntoEmptyIntersection
// RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<T>

class Bar<T> {
    fun <S : T> takeFoo(foo: Foo<in S>) {}
}

interface A

fun <K : L, L : N, N> main() where N: A, N: Number {
    val foo = Foo<K>()
    Bar<String>().<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>takeFoo<!>(foo) // error in 1.3.72, no error in 1.4.31
}
