package io.github.tax1116.fixedwidthcodec.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Record(val charset: String = "US-ASCII")
