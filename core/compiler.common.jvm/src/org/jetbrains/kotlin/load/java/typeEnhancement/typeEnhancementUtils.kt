/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

/**
 * Version of [NullabilityQualifier] that distinguishes between NOT_NULL and DEFINITELY_NOT_NULL.
 */
private enum class NullabilityState(val nullability: NullabilityQualifier, val definitelyNotNull: Boolean) {
    FORCE_FLEXIBILITY(NullabilityQualifier.FORCE_FLEXIBILITY, false),
    NULLABLE(NullabilityQualifier.NULLABLE, false),
    NOT_NULL(NullabilityQualifier.NOT_NULL, false),
    DEFINITELY_NOT_NULL(NullabilityQualifier.NOT_NULL, true),
}

private fun JavaTypeQualifiers.toNullabilityState(): NullabilityState? {
    val nullability = nullability ?: return null
    return when (nullability) {
        NullabilityQualifier.FORCE_FLEXIBILITY -> NullabilityState.FORCE_FLEXIBILITY
        NullabilityQualifier.NULLABLE -> NullabilityState.NULLABLE
        NullabilityQualifier.NOT_NULL if !definitelyNotNull -> NullabilityState.NOT_NULL
        NullabilityQualifier.NOT_NULL -> NullabilityState.DEFINITELY_NOT_NULL
    }
}

/**
 * If [isCovariant] is `true`, selects the maximum element (as defined by the natural order) between [this] and [own],
 * unless `supertypeQualifier > own` in which case `null` is returned.
 *
 * Otherwise, returns the single element of `this union own` or `null`.
 */
private fun <T : Comparable<T>> Set<T>.select(own: T?, isCovariant: Boolean): T? {
    if (isCovariant) {
        val supertypeQualifier = maxOrNull()
        //In a case like supertypeQualifier = NOT_NULL, own = NULLABLE, return null
        return if (own != null && supertypeQualifier != null && supertypeQualifier > own) null else own ?: supertypeQualifier
    }

    // isInvariant
    val effectiveSet: Set<T> = own?.let { this + own } ?: this
    // if this set contains exactly one element, it is the qualifier everybody agrees upon,
    // otherwise (no qualifiers, or multiple qualifiers), there's no single such qualifier
    // and all qualifiers are discarded
    return effectiveSet.singleOrNull()
}

private fun Set<NullabilityState>.select(own: NullabilityState?, isCovariant: Boolean): NullabilityState? {
    return if (own == NullabilityState.FORCE_FLEXIBILITY)
        NullabilityState.FORCE_FLEXIBILITY
    else
        select<NullabilityState>(own, isCovariant)
}

private val JavaTypeQualifiers.stateForErrors: NullabilityState?
    get() = if (isNullabilityQualifierForWarning) null else toNullabilityState()

private val JavaTypeQualifiers.stateForWarnings: NullabilityState?
    get() = if (!isNullabilityQualifierForWarning) null else toNullabilityState()

/**
 * Computes the [JavaTypeQualifiers] from the immediate and the overridden qualifiers.
 *
 * Error and warning qualifiers are computed separately, and the stricter of the two is selected.
 *
 * In the case when we have `NOT_NULL` for error and `DEFINITELY_NOT_NULL` for warning, we return `DEFINITELY_NOT_NULL` for warning.
 * This is a bit controversial because as a result, we "lose" the enhancement from `T!` to `T` with error severity.
 */
fun JavaTypeQualifiers.computeQualifiersForOverride(
    superQualifiers: Collection<JavaTypeQualifiers>,
    isCovariant: Boolean,
    isForVarargParameter: Boolean,
    ignoreDeclarationNullabilityAnnotations: Boolean,
): JavaTypeQualifiers {
    val stateForErrors = superQualifiers.mapNotNullTo(mutableSetOf()) { it.stateForErrors }
        .select(stateForErrors, isCovariant)
    val stateForWarnings = superQualifiers.mapNotNullTo(mutableSetOf()) { it.stateForWarnings }
        .select(stateForWarnings, isCovariant)

    val (newNullability, forWarning) = when {
        stateForErrors != null && stateForWarnings != null -> maxOf(stateForErrors, stateForWarnings).let { it to (it != stateForErrors) }
        stateForErrors != null -> stateForErrors to false
        else -> stateForWarnings to (stateForWarnings != null)
    }

    // Vararg value parameters effectively have non-nullable type in Kotlin
    // and having nullable types in Java may lead to impossibility of overriding them in Kotlin
    val realNullability = newNullability?.takeUnless {
        ignoreDeclarationNullabilityAnnotations || (isForVarargParameter && it == NullabilityState.NULLABLE)
    }

    val newMutability = superQualifiers.mapNotNullTo(mutableSetOf()) { it.mutability }
        .select(mutability, isCovariant)

    return JavaTypeQualifiers(
        nullability = realNullability?.nullability,
        definitelyNotNull = realNullability?.definitelyNotNull == true,
        isNullabilityQualifierForWarning = forWarning,
        mutability = newMutability,
    )
}

fun TypeSystemCommonBackendContext.hasEnhancedNullability(type: KotlinTypeMarker): Boolean =
    type.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)
