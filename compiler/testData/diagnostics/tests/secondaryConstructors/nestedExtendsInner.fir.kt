// RUN_PIPELINE_TILL: SOURCE
class A {
    open inner class Inner

    class Nested : Inner {
        <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor()<!>
    }
}
