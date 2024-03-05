// DONT_TARGET_EXACT_BACKEND: JS
// MODULE_KIND: AMD
// FILE: a.kt
package foo

@JsImport("lib")
external class A(x: Int) {
    val x: Int

    fun foo(y: Int): Int = definedExternally

    class Nested {
        val y: Int
    }
}

@JsImport("lib")
external object B {
    val x: Int = definedExternally

    fun foo(y: Int): Int = definedExternally
}

@JsImport("lib")
external fun foo(y: Int): Int = definedExternally

@JsImport("lib")
external val bar: Int = definedExternally

// FILE: b.kt
package foo

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    val nested = A.Nested()
    assertEquals(55, nested.y)

    assertEquals(123, B.x)
    assertEquals(265, B.foo(142))

    assertEquals(365, foo(42))
    assertEquals(423, bar)

    return "OK"
}