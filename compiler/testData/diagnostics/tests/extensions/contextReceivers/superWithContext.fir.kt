// RUN_PIPELINE_TILL: SOURCE
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

interface Context {
    fun h() {}
}

open class A {
    open fun f() {}
}

class B : A() {
    override fun f() {}

    context(Context)
    inner class C {
        fun g() {
            super@B.f()
            super<!UNRESOLVED_LABEL!>@Context<!>.h()
        }
    }
}