/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.declarations.getSealedClassInheritors
import org.jetbrains.kotlin.fir.declarations.utils.isEnumEntry
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.dfa.NegativeTypeStatement
import org.jetbrains.kotlin.fir.resolve.dfa.TypeStatement
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

typealias CFGTypeStatements = Pair<TypeStatement?, NegativeTypeStatement?>

class FirWhenExhaustivenessTransformer(private val bodyResolveComponents: BodyResolveComponents) : FirTransformer<CFGTypeStatements>() {
    companion object {
        private val exhaustivenessCheckers = listOf(
            WhenOnBooleanExhaustivenessChecker,
            WhenOnEnumExhaustivenessChecker,
            WhenOnSealedClassExhaustivenessChecker,
            WhenOnNothingExhaustivenessChecker
        )

        fun computeAllMissingCases(session: FirSession, whenExpression: FirWhenExpression, types: CFGTypeStatements): List<WhenMissingCase> {
            val (positive, negative) = types
            val subjectType = getSubjectType(session, whenExpression, positive)?.minimumBoundIfFlexible(session)
                ?: return ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH.reasons
            return buildList {
                for (type in subjectType.unwrapTypeParameterAndIntersectionTypes(session)) {
                    val checkers = getCheckers(type, session)
                    collectMissingCases(checkers, whenExpression, type, negative, session)
                }
            }
        }

        private fun getSubjectType(session: FirSession, whenExpression: FirWhenExpression, positive: TypeStatement?): ConeKotlinType? {
            val subjectType = whenExpression.subjectVariable?.returnTypeRef?.coneType
                ?: whenExpression.subject?.resolvedType
                ?: return null

            return ConeTypeIntersector
                .intersectTypes(session.typeContext, listOfNotNull(subjectType) + positive?.exactType.orEmpty())
                .fullyExpandedType(session)
        }

        /**
         * The "minimum" bound of a flexible type is defined as the bound type which will be checked for exhaustion
         * to determine if the when-expression is considered sufficiently exhaustive.
         *
         * * For [dynamic types][ConeDynamicType], this is the **upper bound**,
         * because the branches must cover ***all** possible cases.
         *
         * * For all other [ConeFlexibleType]s, this is the **lower bound**,
         * as platform types may be treated as non-null for exhaustive checks.
         */
        private fun ConeKotlinType.minimumBoundIfFlexible(session: FirSession): ConeRigidType {
            return when (this) {
                is ConeDynamicType -> when (session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)) {
                    true -> upperBound // `dynamic` types must be exhaustive based on the upper bound (`Any?`).
                    false -> lowerBound
                }
                is ConeFlexibleType -> lowerBound // All other flexible types may be exhaustive based on the lower bound.
                is ConeRigidType -> this
            }
        }

        private fun ConeKotlinType.unwrapTypeParameterAndIntersectionTypes(session: FirSession): Collection<ConeKotlinType> {
            return when {
                this is ConeIntersectionType -> intersectedTypes
                this is ConeTypeParameterType && session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                    -> buildList {
                    lookupTag.typeParameterSymbol.resolvedBounds.flatMapTo(this) {
                        it.coneType.unwrapTypeParameterAndIntersectionTypes(session)
                    }
                    add(this@unwrapTypeParameterAndIntersectionTypes)
                }
                this is ConeDefinitelyNotNullType && session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21)
                    -> original.unwrapTypeParameterAndIntersectionTypes(session)
                    .map { it.makeConeTypeDefinitelyNotNullOrNotNull(session.typeContext) }
                else -> listOf(this)
            }
        }

        private fun getCheckers(
            subjectType: ConeKotlinType,
            session: FirSession
        ): List<WhenExhaustivenessChecker> {
            return buildList<WhenExhaustivenessChecker> {
                exhaustivenessCheckers.filterTo(this) {
                    it.isApplicable(subjectType, session)
                }
                if (isNotEmpty() && subjectType.isMarkedNullable) {
                    this.add(WhenOnNullableExhaustivenessChecker)
                }
                if (isEmpty()) {
                    // This checker must be the *ONLY* checker when used,
                    // as it reports WhenMissingCase.Unknown when it fails.
                    add(WhenSelfTypeExhaustivenessChecker)
                }
            }
        }

        private fun MutableList<WhenMissingCase>.collectMissingCases(
            checkers: List<WhenExhaustivenessChecker>,
            whenExpression: FirWhenExpression,
            subjectType: ConeKotlinType,
            negative: NegativeTypeStatement?,
            session: FirSession
        ) {
            for (checker in checkers) {
                checker.computeMissingCases(whenExpression, subjectType, negative, session, this)
            }
            if (isEmpty() && whenExpression.branches.isEmpty()) {
                add(WhenMissingCase.Unknown)
            }
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: CFGTypeStatements): E {
        throw IllegalArgumentException("Should not be there")
    }

    /**
     * The synthetic call for the whole [whenExpression] might be not completed yet
     */
    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: CFGTypeStatements): FirStatement {
        processExhaustivenessCheck(whenExpression, data)
        bodyResolveComponents.session.enumWhenTracker?.reportEnumUsageInWhen(
            bodyResolveComponents.file.sourceFile?.path,
            getSubjectType(bodyResolveComponents.session, whenExpression, data.first)?.minimumBoundIfFlexible(bodyResolveComponents.session)
        )
        return whenExpression
    }

    private fun processExhaustivenessCheck(whenExpression: FirWhenExpression, types: CFGTypeStatements) {
        val session = bodyResolveComponents.session
        val (positive, negative) = types
        val subjectType = getSubjectType(session, whenExpression, positive)
        if (subjectType == null) {
            whenExpression.replaceExhaustivenessStatus(
                when {
                    whenExpression.hasElseBranch() -> ExhaustivenessStatus.ProperlyExhaustive
                    else -> ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH
                }
            )
            return
        }

        val minimumBound = subjectType.minimumBoundIfFlexible(session)

        // May not need to calculate the status of the minimum bound if there is an else branch for a platform type subject.
        // In that case, only the upper bound of the platform type needs to be calculated.
        val minimumStatus by lazy { computeExhaustivenessStatus(whenExpression, minimumBound, negative) }

        fun computeUpperBoundStatus(): ExhaustivenessStatus {
            val upperBound = subjectType.upperBoundIfFlexible()
            if (upperBound == minimumBound) return minimumStatus
            return computeExhaustivenessStatus(whenExpression, upperBound, negative)
        }

        val status = when {
            whenExpression.hasElseBranch() -> when {
                // If there is an else branch and the upper-bound is properly exhaustive, the else branch is redundant.
                // Otherwise, the when-expression is properly exhaustive based on the else branch.
                computeUpperBoundStatus() == ExhaustivenessStatus.ProperlyExhaustive -> ExhaustivenessStatus.RedundantlyExhaustive
                else -> ExhaustivenessStatus.ProperlyExhaustive
            }

            else -> minimumStatus
        }

        whenExpression.replaceExhaustivenessStatus(status)
    }

    private fun FirWhenExpression.hasElseBranch(): Boolean {
        return branches.any { it.condition is FirElseIfTrueCondition }
    }

    private fun computeExhaustivenessStatus(whenExpression: FirWhenExpression, subjectType: ConeKotlinType, negative: NegativeTypeStatement?): ExhaustivenessStatus {
        val session = bodyResolveComponents.session
        val approximatedType = session.typeApproximator.approximateToSuperType(
            subjectType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
        ) ?: subjectType

        if (whenExpression.branches.isEmpty() && approximatedType.isNothing) {
            return ExhaustivenessStatus.ExhaustiveAsNothing
        }

        var status: ExhaustivenessStatus = ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH

        val unwrappedIntersectionTypes = approximatedType.unwrapTypeParameterAndIntersectionTypes(session)
        if (isVacuousIntersection(unwrappedIntersectionTypes)) {
            return ExhaustivenessStatus.ProperlyExhaustive
        }

        for (unwrappedSubjectType in unwrappedIntersectionTypes) {
            // `kotlin.Boolean` is always exhaustive despite the fact it could be `expect` (relevant for stdlib K2)
            if (unwrappedSubjectType.toRegularClassSymbol(session)?.isExpect != true ||
                unwrappedSubjectType.classId == StandardClassIds.Boolean
            ) {
                val localStatus = computeStatusForNonIntersectionType(unwrappedSubjectType, negative, session, whenExpression)
                when {
                    localStatus === ExhaustivenessStatus.ProperlyExhaustive -> {
                        status = localStatus
                        break
                    }
                    localStatus !== ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH && status === ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH -> {
                        status = localStatus
                    }
                }
            }
        }

        return status
    }

    private fun computeStatusForNonIntersectionType(
        unwrappedSubjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        whenExpression: FirWhenExpression,
    ): ExhaustivenessStatus {
        val checkers = getCheckers(unwrappedSubjectType, session)
        if (checkers.isEmpty()) {
            return ExhaustivenessStatus.NotExhaustive.NO_ELSE_BRANCH
        }

        val whenMissingCases = mutableListOf<WhenMissingCase>()
        whenMissingCases.collectMissingCases(checkers, whenExpression, unwrappedSubjectType, negative, session)

        return if (whenMissingCases.isEmpty()) {
            ExhaustivenessStatus.ProperlyExhaustive
        } else {
            ExhaustivenessStatus.NotExhaustive(whenMissingCases)
        }
    }

    fun isVacuousIntersection(types: Collection<ConeKotlinType>): Boolean {
        val session = bodyResolveComponents.session
        val enumEntries = types.mapNotNull { type ->
            (type as? ConeClassLikeType)
                ?.toRegularClassSymbol(session)
                ?.takeIf { it.classKind == ClassKind.ENUM_ENTRY || it.classKind == ClassKind.OBJECT }
        }
        return enumEntries.any { a ->
            enumEntries.any { b ->
                a != b && a.getSuperClassSymbolOrAny(session) == b.getSuperClassSymbolOrAny(session)
            }
        }
    }
}

private sealed class WhenExhaustivenessChecker {
    abstract fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean
    abstract fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    )

    protected abstract class AbstractConditionChecker<in D : Any> : FirVisitor<Unit, D>() {
        override fun visitElement(element: FirElement, data: D) {}

        override fun visitWhenExpression(whenExpression: FirWhenExpression, data: D) {
            whenExpression.branches.forEach { it.accept(this, data) }
        }

        override fun visitWhenBranch(whenBranch: FirWhenBranch, data: D) {
            // When conditions with guards do not contribute to exhaustiveness.
            // TODO(KT-63696): enhance exhaustiveness checks to consider guards.
            if (whenBranch.hasGuard) return

            whenBranch.condition.accept(this, data)
        }

        override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression, data: D) {
            if (booleanOperatorExpression.kind == LogicOperationKind.OR) {
                booleanOperatorExpression.acceptChildren(this, data)
            }
        }
    }
}

private sealed class TypeBasedWhenExhaustivenessChecker: WhenExhaustivenessChecker() {
    abstract fun computeMissingCases(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    )

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        computeMissingCases(subjectType, negative, session, destination)
    }
}

private object WhenOnNullableExhaustivenessChecker : TypeBasedWhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.isMarkedOrFlexiblyNullable
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (isNullBranchMissing(negative)) {
            destination.add(WhenMissingCase.NullIsMissing)
        }
    }

    fun isNullBranchMissing(negative: NegativeTypeStatement?): Boolean =
        negative == null || negative.types.none { it.isMarkedNullable }
}

private object WhenOnBooleanExhaustivenessChecker : WhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.classId == StandardClassIds.Boolean
    }

    private class Flags {
        var containsTrue = false
        var containsFalse = false
    }

    override fun computeMissingCases(
        whenExpression: FirWhenExpression,
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        if (session.languageVersionSettings.supportsFeature(LanguageFeature.ImprovedExhaustivenessChecksIn21) &&
            WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(subjectType, negative, session)
        ) {
            return
        }

        val flags = Flags()
        whenExpression.accept(ConditionChecker, flags)
        if (!flags.containsTrue) {
            destination.add(WhenMissingCase.BooleanIsMissing.TrueIsMissing)
        }
        if (!flags.containsFalse) {
            destination.add(WhenMissingCase.BooleanIsMissing.FalseIsMissing)
        }
    }

    private object ConditionChecker : AbstractConditionChecker<Flags>() {
        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: Flags) {
            if (equalityOperatorCall.operation.let { it == FirOperation.EQ || it == FirOperation.IDENTITY }) {
                val argument = equalityOperatorCall.arguments[1]
                if (argument is FirLiteralExpression) {
                    when (argument.value) {
                        true -> data.containsTrue = true
                        false -> data.containsFalse = true
                    }
                }
            }
        }
    }
}

private object WhenOnEnumExhaustivenessChecker : TypeBasedWhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        val symbol = subjectType.toRegularClassSymbol(session) ?: return false
        return symbol.fir.classKind == ClassKind.ENUM_CLASS
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        if (WhenSelfTypeExhaustivenessChecker.isExhaustiveThroughSelfTypeCheck(subjectType, negative, session)) return

        val enumClass = (subjectType.toSymbol(session) as FirRegularClassSymbol).fir
        val notCheckedEntries = enumClass.declarations.mapNotNullTo(mutableSetOf()) { it as? FirEnumEntry }
        notCheckedEntries.removeAll(negative?.entries?.map(FirEnumEntrySymbol::fir)?.toSet() ?: emptySet())
        notCheckedEntries.mapTo(destination) { WhenMissingCase.EnumCheckIsMissing(it.symbol.callableId) }
    }
}

private object WhenOnSealedClassExhaustivenessChecker : TypeBasedWhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.toRegularClassSymbol(session)?.fir?.modality == Modality.SEALED
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        val allSubclasses = subjectType.toSymbol(session)?.collectAllSubclasses(session) ?: return
        val remainingSubclasses = allSubclasses.toMutableSet()
        val reportedNegatives =
            negative?.types?.mapNotNull { it.toSymbol(session) }.orEmpty() + negative?.entries?.mapNotNull { it.fir.symbol }.orEmpty()
        for (negativeSubclass in reportedNegatives) {
            val subclassesOfType = negativeSubclass.collectAllSubclasses(session)
            remainingSubclasses.removeAll(subclassesOfType)
        }
        remainingSubclasses.mapNotNullTo(destination) {
            when (it) {
                is FirClassSymbol<*> -> WhenMissingCase.IsTypeCheckIsMissing(
                    it.classId,
                    it.fir.classKind.isSingleton,
                    it.ownTypeParameterSymbols.size
                )
                is FirVariableSymbol<*> -> WhenMissingCase.EnumCheckIsMissing(it.callableId)
                else -> null
            }
        }
    }

    private fun FirBasedSymbol<*>.collectAllSubclasses(session: FirSession): Set<FirBasedSymbol<*>> {
        return mutableSetOf<FirBasedSymbol<*>>().apply { collectAllSubclassesTo(this, session) }
    }

    private fun FirBasedSymbol<*>.collectAllSubclassesTo(destination: MutableSet<FirBasedSymbol<*>>, session: FirSession) {
        if (this !is FirRegularClassSymbol) {
            destination.add(this)
            return
        }
        when {
            fir.modality == Modality.SEALED -> fir.getSealedClassInheritors(session).forEach {
                val symbol = session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol
                symbol?.collectAllSubclassesTo(destination, session)
            }
            fir.classKind == ClassKind.ENUM_CLASS -> fir.collectEnumEntries().mapTo(destination) { it.symbol }
            else -> destination.add(this)
        }
    }
}

private object WhenOnNothingExhaustivenessChecker : TypeBasedWhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return subjectType.isNullableNothing || subjectType.isNothing
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>
    ) {
        // Nothing has no branches. The null case for `Nothing?` is handled by WhenOnNullableExhaustivenessChecker
    }
}

/**
 * Checks if any branches are of the same type, or a super-type, of the subject. Must be the only checker when used, as
 * the result of the checker is [WhenMissingCase.Unknown] when no matching branch is found.
 */
private data object WhenSelfTypeExhaustivenessChecker : TypeBasedWhenExhaustivenessChecker() {
    override fun isApplicable(subjectType: ConeKotlinType, session: FirSession): Boolean {
        return true
    }

    override fun computeMissingCases(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
        destination: MutableCollection<WhenMissingCase>,
    ) {
        // This checker should only be used when no other missing cases are being reported.
        if (destination.isNotEmpty()) return

        if (!isExhaustiveThroughSelfTypeCheck(subjectType, negative, session)) {
            // If there are no cases that check for self-type or super-type, report an Unknown missing case,
            // since we do not want to suggest this sort of check.
            destination.add(WhenMissingCase.Unknown)
        }
    }

    fun isExhaustiveThroughSelfTypeCheck(
        subjectType: ConeKotlinType,
        negative: NegativeTypeStatement?,
        session: FirSession,
    ): Boolean {
        /**
         * If the subject type is nullable and one of the branches allows for a nullable type, the subject can be converted to a non-null
         * type, so a non-null self-type case is still considered exhaustive.
         *
         * ```
         * // This is exhaustive!
         * when (x as? String) {
         *     is CharSequence -> ...
         *     null -> ...
         * }
         * ```
         */
        if (WhenOnNullableExhaustivenessChecker.isApplicable(subjectType, session) &&
            WhenOnNullableExhaustivenessChecker.isNullBranchMissing(negative)
        ) {
            return false
        }

        // If NullIsMissing was *not* reported, the subject can safely be converted to a not-null type.
        val convertedSubjectType = subjectType.withNullability(nullable = false, typeContext = session.typeContext)

        // If there are no cases that check for self-type or super-type, report an Unknown missing case,
        // since we do not want to suggest this sort of check.
        return negative?.types?.any { convertedSubjectType.isSubtypeOf(it, session) } == true
    }

}
