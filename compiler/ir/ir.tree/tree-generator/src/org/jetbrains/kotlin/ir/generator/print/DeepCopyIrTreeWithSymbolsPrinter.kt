/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.deepCopyTypeRemapperType
import org.jetbrains.kotlin.ir.generator.elementTransformerVoidType
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Implementation
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.obsoleteDescriptorBasedApiAnnotation
import org.jetbrains.kotlin.ir.generator.symbolRemapperType
import org.jetbrains.kotlin.ir.generator.typeRemapperType
import org.jetbrains.kotlin.utils.withIndent

internal class DeepCopyIrTreeWithSymbolsPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element): TypeRef = element

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(elementTransformerVoidType)

    override val optIns: List<ClassRef<*>> = listOf(obsoleteDescriptorBasedApiAnnotation)

    override val implementationKind: ImplementationKind
        get() = ImplementationKind.OpenClass

    private val symbolRemapperParameter = PrimaryConstructorParameter(
        FunctionParameter("symbolRemapper", symbolRemapperType),
        VariableKind.VAL,
        Visibility.PRIVATE
    )

    private val typeRemapperParameter = PrimaryConstructorParameter(
        FunctionParameter("typeRemapper", typeRemapperType.copy(nullable = true), "null"),
        VariableKind.PARAMETER
    )

    private val excludedMethods = setOf(
        IrTree.functionWithLateBinding,
        IrTree.propertyWithLateBinding,
    )

    private val alwaysExcludedConstructorFields = setOf("valueArguments", "typeArguments", "irBuiltins")

    private val excludedConstructorFields = mapOf(
        IrTree.enumConstructorCall to setOf("origin"),
        IrTree.delegatingConstructorCall to setOf("origin"),
        IrTree.errorDeclaration to setOf("origin"),
        IrTree.constantPrimitive to setOf("type"),
        IrTree.constructorCall to setOf("source")
    )

    private val bodyFieldsInConstructor = mapOf(
        IrTree.`when` to setOf("branches"),
        IrTree.catch to setOf("result"),
        IrTree.vararg to setOf("elements"),
        IrTree.constantArray to setOf("elements"),
        IrTree.`try` to setOf("tryResult", "catches", "finallyExpression"),
        IrTree.stringConcatenation to setOf("arguments"),
        IrTree.setField to setOf("receiver", "value"),
        IrTree.getField to setOf("receiver"),
        IrTree.inlinedFunctionBlock to setOf("statements"),
        IrTree.returnableBlock to setOf("statements"),
        IrTree.composite to setOf("statements"),
        IrTree.block to setOf("statements"),
        IrTree.constantObject to setOf("valueArguments", "typeArguments")
    )

    private val alwaysExcludedApplyFields = setOf(
        "originalBeforeInline",
        "attributeOwnerId",
        "metadata",
        "typeParameters",
        "dispatchReceiver",
        "extensionReceiver",
        "origin",
        "correspondingPropertySymbol",
        "module",
    )

    private val excludedApplyFields = mapOf(
        IrTree.constructor to setOf(
            "body",
            "contextReceiverParametersCount",
            "extensionReceiverParameter",
            "dispatchReceiverParameter",
            "returnType"
        ),
        IrTree.simpleFunction to setOf(
            "body",
            "overriddenSymbols",
            "extensionReceiverParameter",
            "dispatchReceiverParameter",
            "returnType"
        ),
        IrTree.script to setOf(
            "annotations",
            "baseClass",
            "providedProperties",
            "resultProperty",
            "earlierScriptsParameter",
            "importedScripts",
            "earlierScripts",
            "targetClass",
            "constructor"
        )
    )

    override val constructorParameters: List<PrimaryConstructorParameter> = listOf(symbolRemapperParameter, typeRemapperParameter)

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        addImport(ArbitraryImportable("org.jetbrains.kotlin.utils", "memoryOptimizedMap"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.types", "IrType"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir", "IrStatement"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir", "IrImplementationDetail"))

        printPropertyDeclaration(
            name = "transformedModule",
            type = IrTree.moduleFragment.copy(nullable = true),
            kind = VariableKind.VAR,
            visibility = Visibility.PRIVATE,
            initializer = "null",
        )
        println()
        printPropertyDeclaration(
            name = "typeRemapper",
            type = typeRemapperType,
            kind = VariableKind.VAL,
            visibility = Visibility.PRIVATE,
            initializer = "${typeRemapperParameter.name} ?: ${deepCopyTypeRemapperType.render()}(${symbolRemapperParameter.name})",
        )
        println()
        println()
        printInitBlock()
        println()
        printUtils()
    }

    private fun ImportCollectingPrinter.printInitBlock() {
        print("init")
        printBlock {
            println("// TODO refactor")
            println("// After removing usages of ${deepCopyTypeRemapperType.simpleName} constructor from compose, the lateinit property `${deepCopyTypeRemapperType.simpleName}.deepCopy`")
            println("// can be refactored to a constructor parameter.")
            print("(this.${typeRemapperParameter.name} as? ${deepCopyTypeRemapperType.simpleName})?.let")
            printBlock {
                println("it.deepCopy = this")
            }
        }
    }

    override fun printMethodsForElement(element: Element) {
        if (element in excludedMethods) return

        printer.run {
            if (element.isRootElement) {
                println()
                printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)
                println(" =")
                withIndent {
                    println("throw IllegalArgumentException(\"Unsupported element type: \$${element.visitorParameterName}\")")
                }
                return
            }

            if (element.implementations.isEmpty()) return

            println()
            if (element.isSubclassOf(IrTree.declaration)) {
                println("@OptIn(IrImplementationDetail::class)")
            }
            printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)

            if (element.isSubclassOf(IrTree.expressionBody)) {
                printExpressionBody(element)
                return
            }

            println(" =")
            withIndent {
                val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                print(implementation.render())
                if (useWithShapeConstructor(element)) {
                    print("WithShape")
                }
                val constructorArguments: List<Field> =
                    implementation.constructorArguments()
                println("(")
                withIndent {
                    for (field in constructorArguments) {
                        print(field.name, " = ")
                        copyField(element, field)
                        println(",")
                    }
                    if (element.isSubclassOf(IrTree.errorDeclaration)) {
                        println("origin = IrDeclarationOrigin.DEFINED,")
                    }
                    if (element.isSubclassOf(IrTree.blockBody)) {
                        println("constructorIndicator = null")
                    }
                    if (useWithShapeConstructor(element)) {
                        printWithShapeExtraArguments(element)
                    }
                }
                val fieldsInApply = implementation.fieldsInBody
                    .filter { it.isMutable }
                    .filter { it.name !in alwaysExcludedApplyFields }
                    .filter { excludedApplyFields[element]?.contains(it.name) != true }
                    .filter { it !in constructorArguments }
                printApply(element, fieldsInApply)
            }
        }
    }

    private fun ImportCollectingPrinter.copyField(element: Element, field: Field) {
        if (field is ListField) {
            print(element.visitorParameterName, ".", field.name, field.call(), "memoryOptimizedMap")
            print(" { ")
            copyValue(element, field, "it")
            print(" }")
        } else {
            copyValue(element, field, element.visitorParameterName, ".", field.name)
            if (element.isSubclassOf(IrTree.property) && field.name in setOf("backingField", "getter", "setter")) {
                print("?.also { it.correspondingPropertySymbol = symbol }")
            }
        }
    }

    private fun ImportCollectingPrinter.copyValue(element: Element, field: Field, vararg valueArgs: Any?) {
        val typeRef = if (field is ListField) {
            field.baseType
        } else {
            field.typeRef
        }
        val safeCall = if (typeRef.nullable) "?." else "."
        if (element.name == "When") {
            print("")
        }
        when {
            typeRef !is ClassOrElementRef -> {
                print(*valueArgs)
            }
            typeRef.isSymbol() -> {
                copySymbolValue(element, field.name, typeRef, *valueArgs)
            }
            typeRef.isSameClassAs(IrTree.loop) -> {
                print("transformedLoops.getOrDefault(", *valueArgs, ", ", *valueArgs, ")")
            }
            typeRef is ElementOrRef<*> -> {
                print(*valueArgs, safeCall, "transform()")
            }
            typeRef.isSameClassAs(irTypeType) -> {
                print(*valueArgs, safeCall, "remapType()")
            }
            typeRef.isSameClassAs(type<ValueClassRepresentation<*>>()) -> {
                addImport(irSimpleTypeType)
                print(*valueArgs, safeCall, "mapUnderlyingType { it.remapType() as IrSimpleType }")
            }
            typeRef is ClassRef<*> -> {
                print(*valueArgs)
            }
        }
    }

    private fun ClassOrElementRef.isSymbol(): Boolean =
        packageName == "org.jetbrains.kotlin.ir.symbols.impl" || packageName == "org.jetbrains.kotlin.ir.symbols"

    private fun ImportCollectingPrinter.copySymbolValue(
        element: Element,
        fieldName: String,
        typeRef: ClassOrElementRef,
        vararg valueArgs: Any?,
    ) {
        val symbolRemapperFunctionPrefix =
            if (fieldName == "symbol" && element.isSubclassOfAny(IrTree.declaration, IrTree.packageFragment, IrTree.returnableBlock)) {
                "getDeclared"
            } else {
                "getReferenced"
            }
        val symbolRemapperFunctionSuffix = typeRef.typeName
            .removePrefix("Ir")
            .removeSuffix("Symbol")
        val symbolRemapperFunction = symbolRemapperFunctionPrefix + symbolRemapperFunctionSuffix
        when {
            element.isSubclassOf(IrTree.inlinedFunctionBlock) -> {
                print(*valueArgs)
            }
            typeRef.nullable -> {
                print(*valueArgs, "?.let(", symbolRemapperParameter.name, "::", symbolRemapperFunction, ")")
            }
            else -> {
                print(symbolRemapperParameter.name, ".", symbolRemapperFunction, "(", *valueArgs, ")")
            }
        }
    }

    fun ClassOrElementRef.isSameClassAs(other: ClassOrElementRef): Boolean =
        packageName == other.packageName && typeName == other.typeName

    private fun Element.isSubclassOfAny(vararg elements: Element) =
        elements.any { isSubclassOf(it) }

    private fun Implementation.constructorArguments(): List<Field> {
        val filteredConstructorFields = fieldsInConstructor
            .filter { it.name !in alwaysExcludedConstructorFields }
            .filter { excludedConstructorFields[element]?.contains(it.name) != true }
        val filteredBodyFields = fieldsInBody
            .filter { bodyFieldsInConstructor[element]?.contains(it.name) == true }
        val allFields = filteredConstructorFields + filteredBodyFields
        return allFields
    }

    private fun useWithShapeConstructor(element: Element): Boolean =
        element.isSubclassOfAny(IrTree.functionAccessExpression, IrTree.functionReference, IrTree.propertyReference)

    private fun ImportCollectingPrinter.printApply(element: Element, applyFields: List<Field>) {
        val isApplyBodyEmpty = element.isSubclassOfAny(
            IrTree.branch, IrTree.syntheticBody, IrTree.catch, IrTree.spreadElement, IrTree.suspendableExpression, IrTree.suspensionPoint
        )
        if (isApplyBodyEmpty) {
            println(")")
        } else {
            print(").apply")
            printBlock {
                if (element.isSubclassOf(IrTree.declaration) && !element.isSubclassOf(IrTree.variable)) {
                    println("with(factory) { declarationCreated() }")
                }
                if (element.isSubclassOf(IrTree.loop)) {
                    println("transformedLoops[", element.visitorParameterName, "] = this")
                }
                applyFields.forEach { field ->
                    print(field.name, " = ")
                    copyField(element, field)
                    println()
                }
                if (element.isSubclassOf(IrTree.typeParametersContainer)) {
                    println("copyTypeParametersFrom(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.declarationContainer)) {
                    println(element.visitorParameterName, ".transformDeclarationsTo(this)")
                }
                if (element.isSubclassOf(IrTree.attributeContainer)) {
                    println("processAttributes(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.memberAccessExpression) && !element.isSubclassOf(IrTree.localDelegatedPropertyReference)) {
                    println("copyRemappedTypeArgumentsFrom(", element.visitorParameterName, ")")
                    println("transformValueArguments(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.function)) {
                    println("transformFunctionChildren(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.simpleFunction)) {
                    addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.symbols", "IrSimpleFunctionSymbol"))
                    print("overriddenSymbols = ${element.visitorParameterName}.overriddenSymbols.memoryOptimizedMap")
                    printBlock { println("${symbolRemapperParameter.name}.getReferencedFunction(it) as IrSimpleFunctionSymbol") }
                }
                if (element.isSubclassOfAny(IrTree.errorCallExpression, IrTree.dynamicOperatorExpression)) {
                    println("${element.visitorParameterName}.arguments.transformTo(arguments)")
                }
                if (element.isSubclassOf(IrTree.file)) {
                    println("module = transformedModule ?: ${element.visitorParameterName}.module")
                }
                if (element.isSubclassOf(IrTree.moduleFragment)) {
                    println("this@DeepCopyIrTreeWithSymbols.transformedModule = this")
                    println("files += ${element.visitorParameterName}.files.transform()")
                    println("this@DeepCopyIrTreeWithSymbols.transformedModule = null")
                }
                if (element.isSubclassOf(IrTree.blockBody)) {
                    println("statements.addAll(${element.visitorParameterName}.statements.memoryOptimizedMap { it.transform() })")
                }
                if (element.isSubclassOf(IrTree.constantPrimitive)) {
                    println("this.type = ${element.visitorParameterName}.type.remapType()")
                }
                if (element.isSubclassOf(IrTree.script)) {
                    printScriptApply(element)
                }
            }
        }
    }

    private fun ImportCollectingPrinter.printExpressionBody(element: Element) {
        printBlock {
            printlnMultiLine(
                """
                val expression = ${element.visitorParameterName}.expression.transform()
                return IrExpressionBodyImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    expression = expression,
                    constructorIndicator = null,
                )
                """
            )
        }
    }

    private fun ImportCollectingPrinter.printWithShapeExtraArguments(element: Element) {
        println("typeArgumentsCount = ${element.visitorParameterName}.typeArgumentsCount,")
        println("hasDispatchReceiver = ${element.visitorParameterName}.targetHasDispatchReceiver,")
        println("hasExtensionReceiver = ${element.visitorParameterName}.targetHasExtensionReceiver,")
        if (!element.isSubclassOf(IrTree.propertyReference)) {
            println("valueArgumentsCount = ${element.visitorParameterName}.valueArgumentsCount,")
            println("contextParameterCount = ${element.visitorParameterName}.targetContextParameterCount,")
        }
    }

    private fun ImportCollectingPrinter.printScriptApply(element: Element) {
        println("${element.visitorParameterName}.statements.mapTo(statements) { it.transform() }")
        println("importedScripts = ${element.visitorParameterName}.importedScripts")
        println("resultProperty = ${element.visitorParameterName}.resultProperty")
        println("earlierScripts = ${element.visitorParameterName}.earlierScripts")
        println("earlierScriptsParameter = ${element.visitorParameterName}.earlierScriptsParameter")
    }

    private fun ImportCollectingPrinter.printUtils() {
        printlnMultiLine(
            """
            protected open fun <D : IrAttributeContainer> D.processAttributes(other: IrAttributeContainer?): D =
                copyAttributes(other)
        
            protected inline fun <reified T : IrElement> T.transform() =
                transform(this@DeepCopyIrTreeWithSymbols, null) as T
        
            protected inline fun <reified T : IrElement> List<T>.transform() =
                memoryOptimizedMap { it.transform() }
        
            protected inline fun <reified T : IrElement> List<T>.transformTo(destination: MutableList<T>) =
                mapTo(destination) { it.transform() }
        
            protected fun <T : IrDeclarationContainer> T.transformDeclarationsTo(destination: T) =
                declarations.transformTo(destination.declarations)
        
            protected fun IrType.remapType() = typeRemapper.remapType(this)
            
            private fun <T : IrFunction> T.transformFunctionChildren(declaration: T): T =
                apply {
                    typeRemapper.withinScope(this) {
                        dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                        extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                        returnType = typeRemapper.remapType(declaration.returnType)
                        valueParameters = declaration.valueParameters.transform()
                        body = declaration.body?.transform()
                    }
                }
        
            protected fun IrMutableAnnotationContainer.transformAnnotations(declaration: IrAnnotationContainer) {
                annotations = declaration.annotations.transform()
            }
        
            private fun copyTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
                declaration.factory.createTypeParameter(
                    startOffset = declaration.startOffset,
                    endOffset = declaration.endOffset,
                    origin = declaration.origin,
                    name = declaration.name,
                    symbol = symbolRemapper.getDeclaredTypeParameter(declaration.symbol),
                    variance = declaration.variance,
                    index = declaration.index,
                    isReified = declaration.isReified,
                ).apply {
                    annotations = declaration.annotations.memoryOptimizedMap { it.transform() }
                }
        
            protected fun IrTypeParametersContainer.copyTypeParametersFrom(other: IrTypeParametersContainer) {
                this.typeParameters = other.typeParameters.memoryOptimizedMap {
                    copyTypeParameter(it)
                }
        
                typeRemapper.withinScope(this) {
                    for ((thisTypeParameter, otherTypeParameter) in this.typeParameters.zip(other.typeParameters)) {
                        thisTypeParameter.superTypes = otherTypeParameter.superTypes.memoryOptimizedMap {
                            typeRemapper.remapType(it)
                        }
                    }
                }
            }
        
            protected fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression<*>) {
                assert(typeArgumentsCount == other.typeArgumentsCount) {
                    "Mismatching type arguments: ${'$'}typeArgumentsCount vs ${'$'}{other.typeArgumentsCount} "
                }
                for (i in 0 until typeArgumentsCount) {
                    putTypeArgument(i, other.getTypeArgument(i)?.remapType())
                }
            }
        
            protected fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
                apply {
                    dispatchReceiver = original.dispatchReceiver?.transform()
                    extensionReceiver = original.extensionReceiver?.transform()
                }
        
            protected fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
                transformReceiverArguments(original)
                for (i in 0 until original.valueArgumentsCount) {
                    putValueArgument(i, original.getValueArgument(i)?.transform())
                }
            }
        
            private val transformedLoops = HashMap<IrLoop, IrLoop>()
            
            override fun visitBody(body: IrBody): IrBody =
                throw IllegalArgumentException("Unsupported body type: ${'$'}body")
                
            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement =
                throw IllegalArgumentException("Unsupported declaration type: ${'$'}declaration")
        
            override fun visitExpression(expression: IrExpression): IrExpression =
                throw IllegalArgumentException("Unsupported expression type: ${'$'}expression")
            """
        )
    }
}