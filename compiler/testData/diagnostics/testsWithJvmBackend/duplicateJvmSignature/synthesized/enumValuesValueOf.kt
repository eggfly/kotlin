// RUN_PIPELINE_TILL: KLIB
// FIR_IDENTICAL

<!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>enum class A {
    A1,
    A2;

    <!CONFLICTING_JVM_DECLARATIONS!>fun valueOf(s: String): A = valueOf(s)<!>

    fun valueOf() = "OK"

    <!CONFLICTING_JVM_DECLARATIONS!>fun values(): Array<A> = null!!<!>

    fun values(x: String) = x
}<!>
