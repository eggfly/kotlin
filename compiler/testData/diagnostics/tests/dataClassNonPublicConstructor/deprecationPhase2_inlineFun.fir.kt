// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility
// WITH_STDLIB
data class PrivateInline <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>private<!> constructor(val value: Int) {
    <!NOTHING_TO_INLINE!>inline<!> fun huh() {
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_ERROR!>copy<!>()
    }
}

data class PublishedApiInline @PublishedApi <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>internal<!> constructor(val value: Int) {
    <!NOTHING_TO_INLINE!>inline<!> fun huh() {
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_ERROR!>copy<!>()
    }
}

data class InternalInline <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_ERROR!>internal<!> constructor(val value: Int) {
    <!NOTHING_TO_INLINE!>inline<!> fun huh() {
        <!NON_PUBLIC_DATA_COPY_CALL_FROM_PUBLIC_INLINE_ERROR!>copy<!>()
    }
}
