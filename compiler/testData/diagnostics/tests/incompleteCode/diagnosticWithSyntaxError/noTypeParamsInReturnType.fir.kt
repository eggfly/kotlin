// RUN_PIPELINE_TILL: SOURCE
package b

fun <T, R> foo(map: Map<T, R>) : R = throw Exception()

fun <F, G> getMap() : Map<F, G> = throw Exception()

fun bar123() {
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TOO_MANY_ARGUMENTS!>getMap<!>(
<!SYNTAX!><!>}
