// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
package test

annotation class `__`(val value: String)

@<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>__<!>("") class TestAnn
@`__`("") class TestAnn2