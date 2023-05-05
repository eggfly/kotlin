/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("js/js.translator/testData/incremental/invalidation")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class JsIrInvalidationTestGenerated extends AbstractJsIrInvalidationTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.JS_IR, testDataFilePath);
    }

    @TestMetadata("addUpdateRemoveDependentFile")
    public void testAddUpdateRemoveDependentFile() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/addUpdateRemoveDependentFile/");
    }

    @TestMetadata("addUpdateRemoveDependentModule")
    public void testAddUpdateRemoveDependentModule() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/addUpdateRemoveDependentModule/");
    }

    public void testAllFilesPresentInInvalidation() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("js/js.translator/testData/incremental/invalidation"), Pattern.compile("^([^_](.+))$"), null, TargetBackend.JS_IR, false);
    }

    @TestMetadata("breakKlibBinaryCompatibilityWithVariance")
    public void testBreakKlibBinaryCompatibilityWithVariance() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/breakKlibBinaryCompatibilityWithVariance/");
    }

    @TestMetadata("circleExportsUpdate")
    public void testCircleExportsUpdate() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/circleExportsUpdate/");
    }

    @TestMetadata("circleInlineImportsUpdate")
    public void testCircleInlineImportsUpdate() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/circleInlineImportsUpdate/");
    }

    @TestMetadata("class")
    public void testClass() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/class/");
    }

    @TestMetadata("classFunctionsAndFields")
    public void testClassFunctionsAndFields() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/classFunctionsAndFields/");
    }

    @TestMetadata("companionFunction")
    public void testCompanionFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/companionFunction/");
    }

    @TestMetadata("companionInlineFunction")
    public void testCompanionInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/companionInlineFunction/");
    }

    @TestMetadata("companionProperties")
    public void testCompanionProperties() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/companionProperties/");
    }

    @TestMetadata("companionWithStdLibCall")
    public void testCompanionWithStdLibCall() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/companionWithStdLibCall/");
    }

    @TestMetadata("constVals")
    public void testConstVals() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/constVals/");
    }

    @TestMetadata("crossModuleReferences")
    public void testCrossModuleReferences() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/crossModuleReferences/");
    }

    @TestMetadata("eagerInitialization")
    public void testEagerInitialization() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/eagerInitialization/");
    }

    @TestMetadata("enum")
    public void testEnum() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/enum/");
    }

    @TestMetadata("enumsInInlineFunctions")
    public void testEnumsInInlineFunctions() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/enumsInInlineFunctions/");
    }

    @TestMetadata("esModules")
    public void testEsModules() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/esModules/");
    }

    @TestMetadata("exceptionsFromInlineFunction")
    public void testExceptionsFromInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/exceptionsFromInlineFunction/");
    }

    @TestMetadata("exportsThroughInlineFunction")
    public void testExportsThroughInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/exportsThroughInlineFunction/");
    }

    @TestMetadata("fakeOverrideClassFunctionQualifiers")
    public void testFakeOverrideClassFunctionQualifiers() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fakeOverrideClassFunctionQualifiers/");
    }

    @TestMetadata("fakeOverrideInheritance")
    public void testFakeOverrideInheritance() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fakeOverrideInheritance/");
    }

    @TestMetadata("fakeOverrideInlineExtension")
    public void testFakeOverrideInlineExtension() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fakeOverrideInlineExtension/");
    }

    @TestMetadata("fakeOverrideInlineFunction")
    public void testFakeOverrideInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fakeOverrideInlineFunction/");
    }

    @TestMetadata("fakeOverrideInlineProperty")
    public void testFakeOverrideInlineProperty() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fakeOverrideInlineProperty/");
    }

    @TestMetadata("fakeOverrideInterfaceFunctionQualifiers")
    public void testFakeOverrideInterfaceFunctionQualifiers() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fakeOverrideInterfaceFunctionQualifiers/");
    }

    @TestMetadata("fastPath1")
    public void testFastPath1() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fastPath1/");
    }

    @TestMetadata("fastPath2")
    public void testFastPath2() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/fastPath2/");
    }

    @TestMetadata("friendDependency")
    public void testFriendDependency() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/friendDependency/");
    }

    @TestMetadata("functionDefaultParams")
    public void testFunctionDefaultParams() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/functionDefaultParams/");
    }

    @TestMetadata("functionSignature")
    public void testFunctionSignature() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/functionSignature/");
    }

    @TestMetadata("functionTypeInterfaceReflect")
    public void testFunctionTypeInterfaceReflect() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/functionTypeInterfaceReflect/");
    }

    @TestMetadata("functionalInterface")
    public void testFunctionalInterface() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/functionalInterface/");
    }

    @TestMetadata("genericFunctions")
    public void testGenericFunctions() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/genericFunctions/");
    }

    @TestMetadata("genericInlineFunctions")
    public void testGenericInlineFunctions() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/genericInlineFunctions/");
    }

    @TestMetadata("gettersAndSettersInlining")
    public void testGettersAndSettersInlining() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/gettersAndSettersInlining/");
    }

    @TestMetadata("inlineBecomeNonInline")
    public void testInlineBecomeNonInline() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineBecomeNonInline/");
    }

    @TestMetadata("inlineFunctionAnnotations")
    public void testInlineFunctionAnnotations() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineFunctionAnnotations/");
    }

    @TestMetadata("inlineFunctionAsFunctionReference")
    public void testInlineFunctionAsFunctionReference() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineFunctionAsFunctionReference/");
    }

    @TestMetadata("inlineFunctionAsParam")
    public void testInlineFunctionAsParam() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineFunctionAsParam/");
    }

    @TestMetadata("inlineFunctionCircleUsage")
    public void testInlineFunctionCircleUsage() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineFunctionCircleUsage/");
    }

    @TestMetadata("inlineFunctionDefaultParams")
    public void testInlineFunctionDefaultParams() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineFunctionDefaultParams/");
    }

    @TestMetadata("inlineFunctionWithObject")
    public void testInlineFunctionWithObject() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/inlineFunctionWithObject/");
    }

    @TestMetadata("interfaceSuperUsage")
    public void testInterfaceSuperUsage() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/interfaceSuperUsage/");
    }

    @TestMetadata("interfaceWithDefaultParams")
    public void testInterfaceWithDefaultParams() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/interfaceWithDefaultParams/");
    }

    @TestMetadata("jsCode")
    public void testJsCode() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/jsCode/");
    }

    @TestMetadata("jsCodeWithConstString")
    public void testJsCodeWithConstString() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/jsCodeWithConstString/");
    }

    @TestMetadata("jsExport")
    public void testJsExport() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/jsExport/");
    }

    @TestMetadata("jsModuleAnnotation")
    public void testJsModuleAnnotation() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/jsModuleAnnotation/");
    }

    @TestMetadata("languageVersionSettings")
    public void testLanguageVersionSettings() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/languageVersionSettings/");
    }

    @TestMetadata("localInlineFunction")
    public void testLocalInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/localInlineFunction/");
    }

    @TestMetadata("localObjectsLeakThroughInterface")
    public void testLocalObjectsLeakThroughInterface() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/localObjectsLeakThroughInterface/");
    }

    @TestMetadata("mainModuleInvalidation")
    public void testMainModuleInvalidation() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/mainModuleInvalidation/");
    }

    @TestMetadata("moveAndModifyInlineFunction")
    public void testMoveAndModifyInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/moveAndModifyInlineFunction/");
    }

    @TestMetadata("moveExternalDeclarationsBetweenJsModules")
    public void testMoveExternalDeclarationsBetweenJsModules() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/moveExternalDeclarationsBetweenJsModules/");
    }

    @TestMetadata("moveFilesBetweenModules")
    public void testMoveFilesBetweenModules() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/moveFilesBetweenModules/");
    }

    @TestMetadata("moveInlineFunctionBetweenModules")
    public void testMoveInlineFunctionBetweenModules() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/moveInlineFunctionBetweenModules/");
    }

    @TestMetadata("nestedClass")
    public void testNestedClass() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/nestedClass/");
    }

    @TestMetadata("nonInlineBecomeInline")
    public void testNonInlineBecomeInline() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/nonInlineBecomeInline/");
    }

    @TestMetadata("privateDeclarationLeakThroughDefaultParam")
    public void testPrivateDeclarationLeakThroughDefaultParam() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/privateDeclarationLeakThroughDefaultParam/");
    }

    @TestMetadata("privateInlineFunction1")
    public void testPrivateInlineFunction1() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/privateInlineFunction1/");
    }

    @TestMetadata("privateObjectsLeakThroughSealedInterface")
    public void testPrivateObjectsLeakThroughSealedInterface() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/privateObjectsLeakThroughSealedInterface/");
    }

    @TestMetadata("removeFile")
    public void testRemoveFile() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/removeFile/");
    }

    @TestMetadata("removeModule")
    public void testRemoveModule() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/removeModule/");
    }

    @TestMetadata("removeUnusedFile")
    public void testRemoveUnusedFile() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/removeUnusedFile/");
    }

    @TestMetadata("renameFile")
    public void testRenameFile() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/renameFile/");
    }

    @TestMetadata("renameModule")
    public void testRenameModule() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/renameModule/");
    }

    @TestMetadata("simple")
    public void testSimple() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/simple/");
    }

    @TestMetadata("splitJoinModule")
    public void testSplitJoinModule() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/splitJoinModule/");
    }

    @TestMetadata("suspendFunctions")
    public void testSuspendFunctions() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/suspendFunctions/");
    }

    @TestMetadata("suspendInterfaceWithDefaultParams")
    public void testSuspendInterfaceWithDefaultParams() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/suspendInterfaceWithDefaultParams/");
    }

    @TestMetadata("toplevelProperties")
    public void testToplevelProperties() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/toplevelProperties/");
    }

    @TestMetadata("transitiveInlineFunction")
    public void testTransitiveInlineFunction() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/transitiveInlineFunction/");
    }

    @TestMetadata("typeScriptExports")
    public void testTypeScriptExports() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/typeScriptExports/");
    }

    @TestMetadata("unicodeSerializationAndDeserialization")
    public void testUnicodeSerializationAndDeserialization() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/unicodeSerializationAndDeserialization/");
    }

    @TestMetadata("updateExports")
    public void testUpdateExports() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/updateExports/");
    }

    @TestMetadata("updateExportsAndInlineImports")
    public void testUpdateExportsAndInlineImports() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/updateExportsAndInlineImports/");
    }

    @TestMetadata("variance")
    public void testVariance() throws Exception {
        runTest("js/js.translator/testData/incremental/invalidation/variance/");
    }
}
