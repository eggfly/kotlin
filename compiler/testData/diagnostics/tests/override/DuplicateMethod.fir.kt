// RUN_PIPELINE_TILL: SOURCE
interface Some {
    fun test()
}

class SomeImpl : Some  {
    override <!CONFLICTING_OVERLOADS!>fun test()<!> {}
    override <!CONFLICTING_OVERLOADS!>fun test()<!> {}
}