// RUN_PIPELINE_TILL: SOURCE
// WITH_EXTRA_CHECKERS
interface A<T> {}
interface B<T> {}
interface C<T> {}
interface D<T> {}

interface Test : A<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>in<!> Int>, B<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>out<!> Int>, C<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>?<!REDUNDANT_NULLABLE!>?<!NULLABLE_SUPERTYPE!>?<!><!>, D<Int> {}
val x = object : A<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>> {}
