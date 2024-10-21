/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extractClassesFromArgument
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

internal object FirActualOptInNotPresentOnExpectChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (declaration !is FirMemberDeclaration) return
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return
        if (!declaration.isActual) return

        val actualSymbol = declaration.symbol
        val expectSymbol = actualSymbol.getSingleMatchedExpectForActualOrNull() ?: return

        checkBase(expectSymbol, actualSymbol, context, reporter, DiagnosticReportingStrategy.OnAnnotation)

        // TODO: commonize logic of traversing typealiases with general annotation checker
        if (actualSymbol is FirTypeAliasSymbol && expectSymbol is FirRegularClassSymbol) {
            val actualClassSymbol = actualSymbol.fullyExpandedClass(context.session) ?: return
            val matchingChecker = context.session.expectActualMatchingContextFactory.create(context.session, context.scopeSession)
            with(matchingChecker) {
                matchAndCheckClassMembersRecursively(
                    expectSymbol,
                    actualClassSymbol,
                    context,
                    reporter,
                    DiagnosticReportingStrategy.OnActualTypealias(actualSymbol)
                )
            }
        }
    }

    private fun checkBase(
        expectSymbol: FirBasedSymbol<*>,
        actualSymbol: FirBasedSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        reportingStrategy: DiagnosticReportingStrategy,
    ) {
        if (expectSymbol !is FirClassLikeSymbol && expectSymbol !is FirCallableSymbol) return
        if (actualSymbol !is FirClassLikeSymbol && actualSymbol !is FirCallableSymbol) return

        val expectAnnotationClassIds = expectSymbol.resolvedAnnotationsWithClassIds.mapNotNull { it.toAnnotationClassId(context.session) }
        checkOptInAnnotationsMatch(expectSymbol, actualSymbol, expectAnnotationClassIds, context, reporter, reportingStrategy)
        checkSubclassOptInRequiredAnnotationsMatch(
            expectSymbol, actualSymbol, expectAnnotationClassIds, context, reporter, reportingStrategy
        )
    }

    private fun FirExpectActualMatchingContext.matchAndCheckClassMembersRecursively(
        expectSymbol: FirRegularClassSymbol,
        actualSymbol: FirRegularClassSymbol,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        reportingStrategy: DiagnosticReportingStrategy.OnActualTypealias,
    ) {
        val actualMembers = actualSymbol.collectAllMembers(isActualDeclaration = true).map { it as FirBasedSymbol<*> }
        for (actualMember in actualMembers) {
            val expectMember = findExpectedMember(expectSymbol, actualSymbol, actualMember) ?: continue
            checkBase(expectMember, actualMember, context, reporter, reportingStrategy)

            if (expectMember is FirRegularClassSymbol && actualMember is FirRegularClassSymbol) {
                matchAndCheckClassMembersRecursively(expectMember, actualMember, context, reporter, reportingStrategy)
            }
        }
    }

    private fun FirExpectActualMatchingContext.findExpectedMember(
        expectClassSymbol: FirRegularClassSymbol,
        actualClassSymbol: FirRegularClassSymbol,
        actualMember: DeclarationSymbolMarker,
    ): FirBasedSymbol<*>? {
        val expectToCompatibilityMap = findPotentialExpectClassMembersForActual(expectClassSymbol, actualClassSymbol, actualMember)
        val result = expectToCompatibilityMap
            .filter { it.value == ExpectActualMatchingCompatibility.MatchedSuccessfully }.keys.singleOrNull()
            // Check also incompatible members if only one is found
            ?: expectToCompatibilityMap.keys.singleOrNull()
        return result?.let { it as FirBasedSymbol<*> }
    }


    private fun checkOptInAnnotationsMatch(
        expectSymbol: FirBasedSymbol<*>,
        actualSymbol: FirBasedSymbol<*>,
        expectAnnotationClassIds: List<ClassId>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        reportingStrategy: DiagnosticReportingStrategy,
    ) {
        for (actualAnnotation in actualSymbol.resolvedAnnotationsWithArguments) {
            val annotationClassSymbol = actualAnnotation.toAnnotationClassLikeSymbol(context.session) ?: continue

            if (annotationClassSymbol.resolvedAnnotationsWithClassIds.hasAnnotation(OptInNames.REQUIRES_OPT_IN_CLASS_ID, context.session)) {
                val actualAnnotationClassId = annotationClassSymbol.classId
                if (actualAnnotationClassId !in expectAnnotationClassIds) {
                    reporter.reportOn(
                        reportingStrategy.getSourceToReport(actualAnnotation),
                        FirErrors.ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT,
                        expectSymbol,
                        actualSymbol,
                        actualAnnotationClassId,
                        false,
                        context,
                    )
                }
            }
        }
    }

    private fun checkSubclassOptInRequiredAnnotationsMatch(
        expectSymbol: FirBasedSymbol<*>,
        actualSymbol: FirBasedSymbol<*>,
        expectAnnotationClassIds: List<ClassId>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        reportingStrategy: DiagnosticReportingStrategy,
    ) {
        val actualSubclassOptInAnnotation = actualSymbol.findSubclassOptInAnnotation(context.session) ?: return
        val actualSubclassOptInArgs = actualSubclassOptInAnnotation.extractSubclassOptInRequiredArgs(context.session)
        val expectSubclassOptInArgs = expectSymbol.extractSubclassOptInRequiredClassIds(context.session)

        for (actualClassId in actualSubclassOptInArgs) {
            // @SubclassOptInRequired(X::class) on actual can be satisfied by either @SubclassOptInRequired(X::class) on actual or by @X itself
            if (actualClassId !in expectSubclassOptInArgs && actualClassId !in expectAnnotationClassIds) {
                reporter.reportOn(
                    reportingStrategy.getSourceToReport(actualSubclassOptInAnnotation),
                    FirErrors.ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT,
                    expectSymbol,
                    actualSymbol,
                    actualClassId,
                    true,
                    context,
                )
            }
        }
    }

    private fun FirAnnotation.extractSubclassOptInRequiredArgs(session: FirSession): List<ClassId> {
        return argumentMapping.mapping.values.singleOrNull()
            ?.extractClassesFromArgument(session).orEmpty()
            .map { it.classId }
    }

    private fun FirBasedSymbol<*>.findSubclassOptInAnnotation(session: FirSession): FirAnnotation? {
        return resolvedAnnotationsWithClassIds.singleOrNull { it.toAnnotationClassId(session) == OptInNames.SUBCLASS_OPT_IN_REQUIRED_CLASS_ID }
    }

    private fun FirBasedSymbol<*>.extractSubclassOptInRequiredClassIds(session: FirSession): List<ClassId> {
        val subclassOptInAnnotation = findSubclassOptInAnnotation(session)
        return subclassOptInAnnotation?.extractSubclassOptInRequiredArgs(session).orEmpty()
    }
}

private sealed class DiagnosticReportingStrategy {

    abstract fun getSourceToReport(problemAnnotation: FirAnnotation): KtSourceElement

    object OnAnnotation : DiagnosticReportingStrategy() {
        override fun getSourceToReport(problemAnnotation: FirAnnotation): KtSourceElement {
            return problemAnnotation.source ?: error("Expected annotation source to be not null, annotation = $problemAnnotation")
        }
    }

    class OnActualTypealias(private val typeAliasSymbol: FirTypeAliasSymbol) : DiagnosticReportingStrategy() {
        override fun getSourceToReport(problemAnnotation: FirAnnotation): KtSourceElement {
            return typeAliasSymbol.source ?: error("Expected type alias source to be not null, type alias = $typeAliasSymbol")
        }
    }
}
