// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@DslMarker
annotation class AnnMarker

@AnnMarker
class Inv<T> {
    fun bar() {}
}

fun Inv<*>.foo() {
    bar()
}