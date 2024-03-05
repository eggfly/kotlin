// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
@file:JsImport("./externalObject.mjs")
package foo

@JsImport.Name("O")
external object A {
    val x: Int = definedExternally

    fun foo(y: Int): Int = definedExternally
}

fun box(): String {
    assertEquals(23, A.x)
    assertEquals(65, A.foo(42))
    return "OK"
}