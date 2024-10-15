// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -EQUALITY_NOT_APPLICABLE_WARNING, -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

fun LambdaInCondition(x: Any) {
    when (x) {
        { _ : String -> x == "10" } ("11") <!UNSUPPORTED_FEATURE!>if true<!> -> 1
    }
}

fun LambdaWithIEEInCondition(x: Any) {
    when (x) {
        { _ : String -> if (x == "10") true else false } ("11") <!UNSUPPORTED_FEATURE!>if true<!> -> 1
    }
}

fun LambdaWithWhenExprWithGuardInCondition(x: Any) {
    when (x) {
        { _ : String -> when (x == "10") {
            is Boolean <!UNSUPPORTED_FEATURE!>if x == "10"<!> -> true
            else -> false
        } } ("11") <!UNSUPPORTED_FEATURE!>if true<!> -> 1
    }
}

fun LambdaInGuard(x: Any) {
    when (x) {
        is Int <!UNSUPPORTED_FEATURE!>if { a : String -> x == a } ("11")<!> -> 10
        is Int <!UNSUPPORTED_FEATURE!>if run { x == "10" }<!> -> 20
    }
}

fun LambdaWithIEEInGuard(x: Any) {
    when (x) {
        is Int <!UNSUPPORTED_FEATURE!>if { _ : String -> if (x == "10") true else false } ("11")<!> -> 1
    }
}

fun LambdaWithWhenExprWithGuardInGuard(x: Any) {
    when (x) {
        is Int <!UNSUPPORTED_FEATURE!>if { _ : String -> when (x == "10") {
            is Boolean if x == "10" -> true
            else -> false
        } } ("11")<!> -> 1
    }
}
