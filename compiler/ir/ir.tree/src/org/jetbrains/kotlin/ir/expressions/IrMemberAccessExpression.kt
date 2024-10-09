/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFakeOverrideSymbolBase
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.resolveFakeOverrideMaybeAbstractOrFail
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

// This class is not autogenerated to for the sake refactoring IR parameters - see KT-68003.
// However, it must be kept in sync with [org.jetbrains.kotlin.ir.generator.IrTree.memberAccessExpression].
abstract class IrMemberAccessExpression<S : IrSymbol> : IrDeclarationReference() {
    abstract override val symbol: S

    abstract var origin: IrStatementOrigin?

    /**
     * A list of all value arguments.
     *
     * It corresponds 1 to 1 with [IrFunction.parameters], and therefore should have the same size.
     * `null` value usually means that the default value of the corresponding parameter will be used.
     */
    val arguments: ArrayList<IrExpression?> = ArrayList()

    // Those properties indicate the shape of `this.symbol.owner`. They are filled
    // when the element is created, and whenever `symbol` changes. They are used
    // to enable usage of old argument API, which embeds partial information of
    // target's shape, on top of new API, which doesn't.
    // There are two exceptions:
    // - If `symbol` is unbound, they represent the expected shape of the target.
    //   It should become actual when the symbol is bound.
    // - When one assigns `dispatchReceiver` or `extensionReceiver`, then
    //   `targetHas*Receiver` is overridden, i.e. `targetHas*Receiver = *Receiver != null`.
    //   In that case, we assume the call knows better than the declarations itself on whether
    //   it has a particular receiver parameter or not. This is usually because the receiver
    //   has been added/removed to the declaration after the call to it has been created.
    //   In that situation it would not be possible to signal back to all calls that the shape
    //   was changed. Even more, it is possible that a receiver argument is added to a call
    //   slightly before the corresponding receiver parameter is added to a declaration.
    //   In order not to break such code, we assume the shape specified on call is,
    //   or will eventually be right.
    var targetContextParameterCount: Int = -1
        private set
    var targetHasDispatchReceiver: Boolean = false
        private set
    var targetHasExtensionReceiver: Boolean = false
        private set
    private var targetRegularParameterCount: Int = 0

    internal fun initializeTargetShapeExplicitly(
        hasDispatchReceiver: Boolean,
        hasExtensionReceiver: Boolean,
        contextParameterCount: Int,
        regularParameterCount: Int,
        isFromTargetUpdate: Boolean = false,
    ) {
        if (isFromTargetUpdate) {
            require(hasDispatchReceiver == targetHasDispatchReceiver)
            { "New symbol has different shape w.r.t. dispatch receiver" }
            require(hasExtensionReceiver == targetHasExtensionReceiver)
            { "New symbol has different shape w.r.t. extension receiver" }
            require(regularParameterCount + contextParameterCount == targetRegularParameterCount + targetContextParameterCount)
            { "New symbol has different shape w.r.t. value parameter count" }
        }

        targetHasDispatchReceiver = hasDispatchReceiver
        targetHasExtensionReceiver = hasExtensionReceiver
        targetContextParameterCount = contextParameterCount
        targetRegularParameterCount = regularParameterCount

        val allParametersCount =
            (if (targetHasDispatchReceiver) 1 else 0) +
                    targetContextParameterCount +
                    (if (targetHasExtensionReceiver) 1 else 0) +
                    targetRegularParameterCount

        arguments.ensureCapacity(allParametersCount)
        repeat((allParametersCount - arguments.size).coerceAtLeast(0)) {
            arguments += null
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    internal fun initializeTargetShapeFromSymbol(isFromTargetUpdate: Boolean = false) {
        @Suppress("UNCHECKED_CAST")
        val target = (symbol as IrBindableSymbol<*, IrSymbolOwner>).getRealOwner()
        when (target) {
            is IrFunction -> {
                var hasDispatchReceiver = false
                var hasExtensionReceiver = false
                var contextParameterCount = 0
                var regularParameterCount = 0
                for (param in target.parameters) {
                    when (param.kind) {
                        IrParameterKind.DispatchReceiver -> hasDispatchReceiver = true
                        IrParameterKind.ExtensionReceiver -> hasExtensionReceiver = true
                        IrParameterKind.ContextParameter -> contextParameterCount++
                        IrParameterKind.RegularParameter -> regularParameterCount++
                    }
                }

                initializeTargetShapeExplicitly(
                    hasDispatchReceiver,
                    hasExtensionReceiver,
                    contextParameterCount,
                    regularParameterCount,
                    isFromTargetUpdate = isFromTargetUpdate,
                )
            }
            is IrProperty -> {
                val hasDispatchReceiver: Boolean
                val hasExtensionReceiver: Boolean

                val accessor = when (this) {
                    is IrPropertyReference -> (getter ?: setter)?.getRealOwner()
                    is IrLocalDelegatedPropertyReference -> getter.owner
                    else -> error("Unexpected reference to a property from $this")
                }
                if (accessor != null) {
                    hasDispatchReceiver = accessor.dispatchReceiverParameter != null
                    hasExtensionReceiver = accessor.extensionReceiverParameter != null
                } else {
                    val realProperty = target.resolveFakeOverrideMaybeAbstractOrFail()
                    if (realProperty.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                        || realProperty.origin == IrDeclarationOrigin.SYNTHETIC_JAVA_PROPERTY_DELEGATE
                    ) {
                        hasDispatchReceiver = !realProperty.backingField!!.isStatic
                        hasExtensionReceiver = false
                    } else {
                        error("Cannot infer the shape of property $symbol, please specify it explicitly")
                    }
                }

                initializeTargetShapeExplicitly(
                    hasDispatchReceiver,
                    hasExtensionReceiver,
                    0,
                    0,
                    isFromTargetUpdate = isFromTargetUpdate,
                )
            }
        }
    }

    protected fun updateTargetSymbol() {
        initializeTargetShapeFromSymbol(isFromTargetUpdate = true)
    }

    private fun <S : IrBindableSymbol<*, D>, D : IrSymbolOwner> S.getRealOwner(): D {
        var symbol = this
        while (symbol is IrFakeOverrideSymbolBase<*, *, *>) {
            @Suppress("UNCHECKED_CAST")
            symbol = symbol.originalSymbol as S
        }
        return symbol.owner
    }

    /**
     * Number of those arguments that correspond to [IrParameterKind.ContextParameter] and [IrParameterKind.RegularParameter] parameters.
     *
     * ##### This is a deprecated API!
     * Only use [arguments] instead. If you need to know the meaning of the arguments, reach out to the corresponding function's parameters.
     * A drop-in replacement:
     * ```
     * symbol.owner.parameters.count { it.kind == IrParameterKind.RegularParameter || it.kind == IrParameterKind.ContextParameter }
     * ```
     *
     * Details on the API migration: KT-68003
     */
    val valueArgumentsCount: Int
        get() = targetRegularParameterCount + targetContextParameterCount

    /**
     * Argument corresponding to the [IrParameterKind.DispatchReceiver] parameter, if any.
     *
     * ##### This is a deprecated API!
     * Only use [arguments] instead. If you need to know the meaning of the arguments, reach out to the corresponding function's parameters.
     * A drop-in replacement:
     * ```
     * arguments[symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }]
     * ```
     *
     * Details on the API migration: KT-68003
     */
    var dispatchReceiver: IrExpression?
        get() {
            return if (targetHasDispatchReceiver) {
                arguments[0]
            } else {
                null
            }
        }
        set(value) {
            targetHasDispatchReceiver = setReceiverArgument(0, value, targetHasDispatchReceiver)
        }

    fun insertDispatchReceiver(value: IrExpression?) {
        if (targetHasDispatchReceiver) {
            arguments[0] = value
        } else {
            arguments.add(0, value)
            targetHasDispatchReceiver = true
        }
    }

    fun removeDispatchReceiver() {
        if (targetHasDispatchReceiver) {
            arguments.removeAt(0)
            targetHasDispatchReceiver = false
        }
    }

    /**
     * Argument corresponding to the [IrParameterKind.ExtensionReceiver] parameter, if any.
     *
     * ##### This is a deprecated API!
     * Only use [arguments] instead. If you need to know the meaning of the arguments, reach out to the corresponding function's parameters.
     * A drop-in replacement:
     * ```
     * arguments[symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }]
     * ```
     *
     * Details on the API migration: KT-68003
     */
    var extensionReceiver: IrExpression?
        get() {
            return if (targetHasExtensionReceiver) {
                val index = getExtensionReceiverIndex()
                arguments[index]
            } else {
                null
            }
        }
        set(value) {
            targetHasExtensionReceiver = setReceiverArgument(getExtensionReceiverIndex(), value, targetHasExtensionReceiver)
        }

    fun insertExtensionReceiver(value: IrExpression?) {
        val index = getExtensionReceiverIndex()
        if (targetHasExtensionReceiver) {
            arguments[index] = value
        } else {
            arguments.add(index, value)
            targetHasExtensionReceiver = true
        }
    }

    fun removeExtensionReceiver() {
        if (targetHasExtensionReceiver) {
            arguments.removeAt(getExtensionReceiverIndex())
            targetHasExtensionReceiver = false
        }
    }

    private fun getExtensionReceiverIndex(): Int {
        return (if (targetHasDispatchReceiver) 1 else 0) + targetContextParameterCount
    }

    private fun setReceiverArgument(index: Int, value: IrExpression?, targetHasThatReceiverParameter: Boolean): Boolean {
        if (targetHasThatReceiverParameter) {
            if (value != null) {
                arguments[index] = value
                return true
            } else {
                if (arguments[index] != null) {
                    arguments.removeAt(index)
                    return false
                } else {
                    return true
                }
            }
        } else {
            if (value != null) {
                arguments.add(index, value)
                return true
            } else {
                return false
            }
        }
    }

    /**
     * Gets one of arguments that correspond to [IrParameterKind.ContextParameter] or [IrParameterKind.RegularParameter] parameters.
     * This is, the index corresponds to the deprecated [IrFunction.valueParameters] list, and not [IrFunction.parameters], which also includes
     * receiver parameters.
     *
     * ##### This is a deprecated API!
     * Only use [arguments] instead.
     *
     * E.g. for code
     * ```
     * call.getValueArgument(parameter.index)
     * ```
     *
     * the replacement should be
     * ```
     * call.arguments[parameter.indexNew]
     * ```
     * If you need to know the meaning of the arguments, reach out to the corresponding function's parameters.
     *
     * Details on the API migration: KT-68003
     */
    fun getValueArgument(index: Int): IrExpression? {
        val actualIndex = getRealValueArgumentIndex(index)
        checkArgumentSlotAccess("value", actualIndex, this.arguments.size)
        return this.arguments[actualIndex]
    }

    /**
     * Sets one of arguments that correspond to [IrParameterKind.ContextParameter] or [IrParameterKind.RegularParameter] parameters.
     * This is, the index corresponds to the deprecated [IrFunction.valueParameters] list, and not [IrFunction.parameters], which also includes
     * receiver parameters.
     *
     * ##### This is a deprecated API!
     * Only use [arguments] instead.
     *
     * E.g. for code
     * ```
     * call.putValueArgument(parameter.index, ...)
     * ```
     *
     * the replacement should be
     * ```
     * call.arguments[parameter.indexNew] = ...
     * ```
     * If you need to know the meaning of the arguments, reach out to the corresponding function's parameters.
     *
     * Details on the API migration: KT-68003
     */
    fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        val actualIndex = getRealValueArgumentIndex(index)
        checkArgumentSlotAccess("value", actualIndex, this.arguments.size)
        this.arguments[actualIndex] = valueArgument
    }

    private fun getRealValueArgumentIndex(index: Int): Int =
        (if (targetHasDispatchReceiver) 1 else 0) +
                (if (targetHasExtensionReceiver && index >= targetContextParameterCount) 1 else 0) +
                index


    protected abstract val typeArguments: Array<IrType?>

    val typeArgumentsCount: Int
        get() = typeArguments.size

    fun getTypeArgument(index: Int): IrType? {
        checkArgumentSlotAccess("type", index, typeArguments.size)
        return typeArguments[index]
    }

    fun putTypeArgument(index: Int, type: IrType?) {
        checkArgumentSlotAccess("type", index, typeArguments.size)
        typeArguments[index] = type
    }


    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        arguments.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        arguments.transformInPlace(transformer, data)
    }
}