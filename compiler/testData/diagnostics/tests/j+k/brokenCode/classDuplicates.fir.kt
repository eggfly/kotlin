// RUN_PIPELINE_TILL: SOURCE
// FILE: Hello.kt
private class <!CLASSIFIER_REDECLARATION!>Hello<!>()
{
    val a = 4
}

fun test() {
    // no exception is thrown (see KT-3897)
    Hello().a
}

// FILE: Hello.java
public class Hello {}
