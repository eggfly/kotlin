// RUN_PIPELINE_TILL: SOURCE
// SKIP_TXT
class Outer {
    inner class Inner1 {
        <!NESTED_CLASS_NOT_ALLOWED!>object Obj1<!>

        companion <!NESTED_CLASS_NOT_ALLOWED!>object Obj2<!>

        inner class Inner2 {
            <!NESTED_CLASS_NOT_ALLOWED!>object Obj3<!>
        }
    }
}
