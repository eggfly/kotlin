// RUN_PIPELINE_TILL: SOURCE
// WITH_STDLIB

fun main() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(get(0))
    }
}