package io.github.tax1116.fixedwidthcodec.processor

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import io.github.tax1116.fixedwidthcodec.processor.FixedWidthRecordMapperProcessor.Companion.RECORD_POSTFIX

internal fun KSType.toRecordMapperClassName(): ClassName =
    ClassName(declaration.packageName.asString(), "${declaration.simpleName.asString()}$RECORD_POSTFIX")

internal fun KSType.isPrimitiveOrString() = when (this.declaration.qualifiedName!!.asString()) {
    String::class.qualifiedName,
    Int::class.qualifiedName,
    Long::class.qualifiedName,
    Double::class.qualifiedName,
    Float::class.qualifiedName,
    Char::class.qualifiedName,
    Byte::class.qualifiedName,
    Short::class.qualifiedName,
    Boolean::class.qualifiedName,
    -> true
    else -> false
}

/**
 * Produces a valid Kotlin char literal (including surrounding quotes) for any char,
 * handling escape sequences and non-printable characters. Used to safely embed user-
 * supplied padding chars into generated source code.
 */
internal fun charLiteral(c: Char): String = when (c) {
    '\\' -> "'\\\\'"
    '\'' -> "'\\''"
    '\n' -> "'\\n'"
    '\t' -> "'\\t'"
    '\r' -> "'\\r'"
    '\b' -> "'\\b'"
    '$' -> "'\\$'"
    else -> if (c.code in 32..126) "'$c'" else "'\\u%04X'".format(c.code)
}
