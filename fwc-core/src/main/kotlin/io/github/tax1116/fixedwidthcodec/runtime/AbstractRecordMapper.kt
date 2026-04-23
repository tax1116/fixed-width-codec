package io.github.tax1116.fixedwidthcodec.runtime

import io.github.tax1116.fixedwidthcodec.annotations.Align
import io.github.tax1116.fixedwidthcodec.annotations.MetaField
import java.nio.charset.Charset
import java.text.DecimalFormat

/**
 * Base class for generated fixed-width record mappers.
 *
 * Subclasses are produced at compile time by the KSP processor; application
 * code should not extend this class directly.
 */
abstract class AbstractRecordMapper {
    abstract val charset: Charset

    protected fun bytesToString(bytes: ByteArray, start: Int, end: Int, paddingChar: Char, align: Align): String {
        val slicedValue = bytes.sliceArray(start until end).toString(charset)
        return when (align) {
            Align.LEFT -> slicedValue.trimEnd(paddingChar)
            Align.RIGHT -> slicedValue.trimStart(paddingChar)
        }
    }

    protected fun bytesToLong(bytes: ByteArray, start: Int, end: Int, paddingChar: Char, align: Align): Long =
        bytesToString(bytes, start, end, paddingChar, align)
            .takeIf { it.isNotBlank() }?.toLong() ?: 0L

    protected fun bytesToInt(bytes: ByteArray, start: Int, end: Int, paddingChar: Char, align: Align): Int =
        bytesToString(bytes, start, end, paddingChar, align)
            .takeIf { it.isNotBlank() }?.toInt() ?: 0

    protected fun bytesToDouble(bytes: ByteArray, start: Int, end: Int, paddingChar: Char, align: Align): Double =
        bytesToString(bytes, start, end, paddingChar, align)
            .takeIf { it.isNotBlank() }?.toDouble() ?: 0.0

    protected fun slice(bytes: ByteArray, start: Int, end: Int): ByteArray = bytes.sliceArray(start until end)

    protected fun serialize(input: String, length: Int, paddingChar: Char, align: Align, pattern: String): ByteArray {
        if (length == MetaField.EOL) {
            return input.toByteArray(charset)
        }

        val inputBytes = input.toByteArray(charset)
        if (inputBytes.size > length) {
            throw IllegalArgumentException(
                "Field value exceeds declared length: $length bytes allowed, got ${inputBytes.size} ($input)",
            )
        }

        val padSize = length - inputBytes.size
        return when (align) {
            Align.LEFT -> input + "".padEnd(padSize, paddingChar)
            Align.RIGHT -> "".padStart(padSize, paddingChar) + input
        }.toByteArray(charset)
    }

    protected fun serialize(input: Number, length: Int, paddingChar: Char, align: Align, pattern: String): ByteArray {
        val text = if (pattern.isNotBlank()) DecimalFormat(pattern).format(input) else input.toString()
        return serialize(text, length, paddingChar, align, "")
    }
}
