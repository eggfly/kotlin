// RUN_PIPELINE_TILL: SOURCE
// JAVAC_EXPECTED_FILE
// WITH_EXTRA_CHECKERS

class TestIface(r : Runnable) : Runnable by r {}

class TestObject(o : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>) : <!DELEGATION_NOT_TO_INTERFACE, PLATFORM_CLASS_MAPPED_TO_KOTLIN, PLATFORM_CLASS_MAPPED_TO_KOTLIN, PLATFORM_CLASS_MAPPED_TO_KOTLIN, SUPERTYPE_NOT_INITIALIZED!>Object<!> by o {}
