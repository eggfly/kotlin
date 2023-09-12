/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

sealed class TypeArgument(val name: String) {
    abstract val upperBounds: List<TypeRef>
}

class SimpleTypeArgument(name: String, val upperBound: TypeRef?) : TypeArgument(name) {
    override val upperBounds: List<TypeRef> = listOfNotNull(upperBound)

    override fun toString(): String {
        var result = name
        if (upperBound != null) {
            result += " : ${upperBound.typeWithArguments}"
        }
        return result
    }
}

class TypeArgumentWithMultipleUpperBounds(name: String, override val upperBounds: List<TypeRef>) : TypeArgument(name) {
    override fun toString(): String {
        return name
    }
}