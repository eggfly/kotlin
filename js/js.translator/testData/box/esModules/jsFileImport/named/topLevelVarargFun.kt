// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
@file:JsImport("./topLevelVarargFun.mjs")

@JsImport.Name("bar")
external fun foo(vararg arg: String): String

fun box(): String {
    val x = arrayOf("a", "b")
    var r = foo(*x)
    if (r != "(ab)") return "fail1: $r"

    r = foo("c", "d")
    if (r != "(cd)") return "fail2: $r"

    return "OK"
}