// RUN_PIPELINE_TILL: SOURCE
fun foo(c : Collection<String>) = {
    c.filter{
        val s : String? = bar()
        if (s == null) false // here!
        zoo(<!TYPE_MISMATCH!>s<!>)
    }
}

fun bar() : String? = null
fun zoo(s : String) : Boolean = true
