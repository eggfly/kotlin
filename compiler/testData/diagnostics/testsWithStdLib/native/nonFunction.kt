// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
<!WRONG_MODIFIER_TARGET!>external<!> class A

<!WRONG_MODIFIER_TARGET!>external<!> val foo: Int = 23

class B {
    <!WRONG_MODIFIER_TARGET!>external<!> class A

    <!WRONG_MODIFIER_TARGET!>external<!> val foo: Int = 23
}
