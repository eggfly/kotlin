// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
// CHECK_TYPE

fun test() {
    val x = run f@{
      run ff@ {
        return@ff "2"
      }
      return@f 1
    }
    checkSubtype<Int>(x)
}
