package io.github.tax1116.fixedwidthcodec.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class ObjectField(val length: Int)
