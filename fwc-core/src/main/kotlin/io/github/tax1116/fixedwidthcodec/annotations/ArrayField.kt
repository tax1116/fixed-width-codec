package io.github.tax1116.fixedwidthcodec.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class ArrayField(
    val type: ArrayFieldType = ArrayFieldType.FIXED,
    val size: Int = 0,
    val sizeField: String = "",
    val elementLength: Int,
    val elementAlign: Align = Align.LEFT,
    val elementPaddingChar: Char = ' ',
    val elementPattern: String = "",
)

enum class ArrayFieldType {
    /** Fixed-size array declared via [ArrayField.size]. */
    FIXED,

    /** Size read at runtime from another property identified by [ArrayField.sizeField]. */
    IN_FIELD,

    /** Size supplied as an extra parameter to the generated deserialize function. */
    PARAMETER,
}
