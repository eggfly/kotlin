// RUN_PIPELINE_TILL: KLIB
fun foo() =
    object {
        <!CONFLICTING_JVM_DECLARATIONS!>val x<!> = 1
        <!CONFLICTING_JVM_DECLARATIONS!>fun getX() = 1<!>
    }
