// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
enum class E {
    <!WRONG_MODIFIER_TARGET!>public<!> <!WRONG_MODIFIER_TARGET!>final<!> SUBCLASS {
        fun foo() {}
    },

    <!WRONG_MODIFIER_TARGET!>public<!> PUBLIC,
    <!WRONG_MODIFIER_TARGET!>protected<!> PROTECTED,
    <!WRONG_MODIFIER_TARGET!>private<!> PRIVATE,
    <!WRONG_MODIFIER_TARGET!>internal<!> INTERNAL,

    <!WRONG_MODIFIER_TARGET!>abstract<!> ABSTRACT,
    <!WRONG_MODIFIER_TARGET!>open<!> OPEN,
    <!WRONG_MODIFIER_TARGET!>override<!> OVERRIDE,
    <!WRONG_MODIFIER_TARGET!>final<!> FINAL,

    <!WRONG_MODIFIER_TARGET!>inner<!> INNER,
    <!WRONG_MODIFIER_TARGET!>annotation<!> ANNOTATION,
    <!WRONG_MODIFIER_TARGET!>enum<!> ENUM,
    <!WRONG_MODIFIER_TARGET!>out<!> OUT,
    <!WRONG_MODIFIER_TARGET!>in<!> IN,
    <!WRONG_MODIFIER_TARGET!>vararg<!> VARARG,
    <!WRONG_MODIFIER_TARGET!>reified<!> REIFIED
}
