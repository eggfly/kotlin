// RUN_PIPELINE_TILL: SOURCE
// RENDER_DIAGNOSTICS_FULL_TEXT

fun String?.repro(): Boolean {
    return this?.let {
        return false
    } == true
}