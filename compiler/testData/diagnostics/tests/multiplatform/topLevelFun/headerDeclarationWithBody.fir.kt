// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR
// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> <!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{METADATA}!>fun foo()<!>

<!EXPECTED_DECLARATION_WITH_BODY!><!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> <!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS{METADATA}!>fun foo()<!><!> {}

<!EXPECTED_DECLARATION_WITH_BODY!><!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> fun bar()<!> {}
