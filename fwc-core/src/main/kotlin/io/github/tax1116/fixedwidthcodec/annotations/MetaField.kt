package io.github.tax1116.fixedwidthcodec.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class MetaField(val length: Int, val paddingChar: Char = ' ', val align: Align = Align.LEFT, val pattern: String = "") {
    companion object {
        const val EOL = -1
    }
}
