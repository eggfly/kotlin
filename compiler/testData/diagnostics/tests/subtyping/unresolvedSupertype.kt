// RUN_PIPELINE_TILL: SOURCE
interface A1 : <!UNRESOLVED_REFERENCE!>B<!>

interface A2 : <!UNRESOLVED_REFERENCE!>B<!><!SUPERTYPE_INITIALIZED_IN_INTERFACE!>()<!>

class A3 : <!UNRESOLVED_REFERENCE!>B<!>, <!UNRESOLVED_REFERENCE!>B<!>

enum class A4 : <!UNRESOLVED_REFERENCE!>B<!>
