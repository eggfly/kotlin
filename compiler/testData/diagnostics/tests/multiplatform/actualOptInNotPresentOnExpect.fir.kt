// MODULE: m1-common
// FILE: common.kt
@RequiresOptIn
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class MyOptIn

@RequiresOptIn
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER)
annotation class MyOptIn2

@MyOptIn
expect fun ok_onlyOnExpect()

expect fun onlyOnActual()

@SubclassOptInRequired(MyOptIn::class)
expect open class Ok_subclass_onlyOnExpect

expect open class OnlyOnActual_subclass

@MyOptIn
expect open class Ok_ExpectAllVsActualSubclass

@SubclassOptInRequired(MyOptIn::class)
expect open class ExpectSubclassVsActualAll

@SubclassOptInRequired(MyOptIn::class)
expect open class Ok_subclass_both_sides

@SubclassOptInRequired(MyOptIn::class, MyOptIn2::class)
expect open class Ok_orderNotImportant

expect class ViaTypealias {
    class Inner {
        fun onlyOnActual()
    }
}

expect class OptInOnTypealiasItself

expect class Ok_OptInOnTypealiasedClassNotPropagate

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun ok_onlyOnExpect() {}

<!ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT!>@MyOptIn<!>
actual fun onlyOnActual() {}

actual open class Ok_subclass_onlyOnExpect

<!ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT!>@SubclassOptInRequired(MyOptIn::class)<!>
actual open class OnlyOnActual_subclass

@SubclassOptInRequired(MyOptIn::class)
actual open class Ok_ExpectAllVsActualSubclass

<!ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT!>@MyOptIn<!>
actual open class ExpectSubclassVsActualAll

@SubclassOptInRequired(MyOptIn::class)
actual open class Ok_subclass_both_sides

@SubclassOptInRequired(MyOptIn2::class)
actual open class Ok_orderNotImportant

class ViaTypealiasImpl {
    class Inner {
        @MyOptIn
        fun onlyOnActual() {}
    }
}

<!ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT!>actual typealias ViaTypealias = ViaTypealiasImpl<!>

<!ACTUAL_OPTIN_NOT_PRESENT_ON_EXPECT!>@MyOptIn<!>
actual typealias OptInOnTypealiasItself = OptInOnTypealiasItselfImpl

class OptInOnTypealiasItselfImpl

@OptIn(MyOptIn::class)
actual typealias Ok_OptInOnTypealiasedClassNotPropagate = Ok_OptInOnTypealiasedClassNotPropagateImpl

@MyOptIn
class Ok_OptInOnTypealiasedClassNotPropagateImpl