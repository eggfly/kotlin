// RUN_PIPELINE_TILL: SOURCE
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

public expect abstract class AbstractMutableMap<K, V> : MutableMap<K, V> {
    override val values: MutableCollection<V>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

import java.util.AbstractMap

public actual abstract class AbstractMutableMap<K, V>() : MutableMap<K, V>, AbstractMap<K, V>()
