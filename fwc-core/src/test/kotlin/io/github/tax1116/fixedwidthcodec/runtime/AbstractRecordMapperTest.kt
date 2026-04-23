package io.github.tax1116.fixedwidthcodec.runtime

import io.github.tax1116.fixedwidthcodec.annotations.Align
import io.github.tax1116.fixedwidthcodec.annotations.MetaField
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for the protected helpers in [AbstractRecordMapper].
 *
 * Uses a package-private subclass to expose the protected API to test assertions.
 * Generated mappers produced by the KSP processor use the same helpers, so
 * correctness here directly reflects correctness of generated code.
 */
class AbstractRecordMapperTest {
    private object AsciiMapper : AbstractRecordMapper() {
        override val charset: Charset = Charsets.US_ASCII

        fun readString(bytes: ByteArray, start: Int, end: Int, pad: Char, align: Align) = bytesToString(bytes, start, end, pad, align)

        fun readLong(bytes: ByteArray, start: Int, end: Int, pad: Char, align: Align) = bytesToLong(bytes, start, end, pad, align)

        fun readInt(bytes: ByteArray, start: Int, end: Int, pad: Char, align: Align) = bytesToInt(bytes, start, end, pad, align)

        fun readDouble(bytes: ByteArray, start: Int, end: Int, pad: Char, align: Align) = bytesToDouble(bytes, start, end, pad, align)

        fun writeString(input: String, length: Int, pad: Char, align: Align, pattern: String = "") =
            serialize(input, length, pad, align, pattern)

        fun writeNumber(input: Number, length: Int, pad: Char, align: Align, pattern: String = "") =
            serialize(input, length, pad, align, pattern)
    }

    // region bytesToString

    @Test
    fun `bytesToString strips trailing padding when LEFT-aligned`() {
        val bytes = "ABC       ".toByteArray(Charsets.US_ASCII)
        assertEquals("ABC", AsciiMapper.readString(bytes, 0, 10, ' ', Align.LEFT))
    }

    @Test
    fun `bytesToString strips leading padding when RIGHT-aligned`() {
        val bytes = "0000000123".toByteArray(Charsets.US_ASCII)
        assertEquals("123", AsciiMapper.readString(bytes, 0, 10, '0', Align.RIGHT))
    }

    @Test
    fun `bytesToString preserves internal padding characters`() {
        val bytes = "A B C     ".toByteArray(Charsets.US_ASCII)
        assertEquals("A B C", AsciiMapper.readString(bytes, 0, 10, ' ', Align.LEFT))
    }

    // endregion

    // region bytesToLong / bytesToInt / bytesToDouble — blank guard

    @Test
    fun `bytesToLong returns 0 when field is blank`() {
        val bytes = "          ".toByteArray(Charsets.US_ASCII)
        assertEquals(0L, AsciiMapper.readLong(bytes, 0, 10, ' ', Align.LEFT))
    }

    @Test
    fun `bytesToInt returns 0 when field is blank`() {
        val bytes = "          ".toByteArray(Charsets.US_ASCII)
        assertEquals(0, AsciiMapper.readInt(bytes, 0, 10, ' ', Align.LEFT))
    }

    @Test
    fun `bytesToDouble returns 0_0 when field is blank`() {
        // Regression: bytesToDouble previously lacked the blank guard and threw
        // NumberFormatException on empty-padded numeric fields.
        val bytes = "          ".toByteArray(Charsets.US_ASCII)
        assertEquals(0.0, AsciiMapper.readDouble(bytes, 0, 10, ' ', Align.LEFT))
    }

    @Test
    fun `bytesToLong parses right-padded integer`() {
        val bytes = "0000012345".toByteArray(Charsets.US_ASCII)
        assertEquals(12345L, AsciiMapper.readLong(bytes, 0, 10, '0', Align.RIGHT))
    }

    // endregion

    // region serialize(String, ...)

    @Test
    fun `serialize pads short string with trailing spaces for LEFT alignment`() {
        val out = AsciiMapper.writeString("ABC", length = 10, pad = ' ', align = Align.LEFT)
        assertEquals("ABC       ", out.toString(Charsets.US_ASCII))
    }

    @Test
    fun `serialize pads short numeric string with leading zeros for RIGHT alignment`() {
        val out = AsciiMapper.writeString("123", length = 10, pad = '0', align = Align.RIGHT)
        assertEquals("0000000123", out.toString(Charsets.US_ASCII))
    }

    @Test
    fun `serialize writes exactly length bytes when input equals length`() {
        val out = AsciiMapper.writeString("0123456789", length = 10, pad = '0', align = Align.RIGHT)
        assertEquals("0123456789", out.toString(Charsets.US_ASCII))
        assertEquals(10, out.size)
    }

    @Test
    fun `serialize throws when input exceeds declared length`() {
        // Regression: previously padSize went negative and padEnd silently no-op'd,
        // producing bytes longer than length and corrupting the fixed-width encoding.
        val error = assertFailsWith<IllegalArgumentException> {
            AsciiMapper.writeString("TOO LONG INPUT", length = 5, pad = ' ', align = Align.LEFT)
        }
        assertEquals(true, error.message?.contains("exceeds declared length"))
    }

    @Test
    fun `serialize with EOL length writes input as-is without padding`() {
        val out = AsciiMapper.writeString("variable trailer", length = MetaField.EOL, pad = ' ', align = Align.LEFT)
        assertEquals("variable trailer", out.toString(Charsets.US_ASCII))
    }

    // endregion

    // region serialize(Number, ...)

    @Test
    fun `serialize Number without pattern formats as plain toString and pads`() {
        val out = AsciiMapper.writeNumber(12345L, length = 10, pad = '0', align = Align.RIGHT)
        assertEquals("0000012345", out.toString(Charsets.US_ASCII))
    }

    @Test
    fun `serialize Number with pattern formats via DecimalFormat and still pads to length`() {
        // Regression: previously the pattern branch returned directly without running
        // the pad/length enforcement, so fields came out shorter than declared.
        val out = AsciiMapper.writeNumber(
            input = 1234.5,
            length = 10,
            pad = ' ',
            align = Align.RIGHT,
            pattern = "0000.00",
        )
        assertEquals("   1234.50", out.toString(Charsets.US_ASCII))
    }

    @Test
    fun `serialize Number with pattern throws when formatted value exceeds length`() {
        // Consistency: the post-format length check fires the same way as the
        // plain-string overload.
        assertFailsWith<IllegalArgumentException> {
            AsciiMapper.writeNumber(
                input = 123456789,
                length = 4,
                pad = '0',
                align = Align.RIGHT,
                pattern = "0000000000",
            )
        }
    }

    // endregion

    // region round-trip

    @Test
    fun `bytesToString of serialize output is the original String (round-trip)`() {
        val original = "ACC-42"
        val encoded = AsciiMapper.writeString(original, length = 10, pad = ' ', align = Align.LEFT)
        val decoded = AsciiMapper.readString(encoded, 0, 10, ' ', Align.LEFT)
        assertEquals(original, decoded)
    }

    @Test
    fun `bytesToLong of serialize output equals the original Long (round-trip)`() {
        val original = 987_654_321L
        val encoded = AsciiMapper.writeNumber(original, length = 15, pad = '0', align = Align.RIGHT)
        val decoded = AsciiMapper.readLong(encoded, 0, 15, '0', Align.RIGHT)
        assertEquals(original, decoded)
    }

    // endregion
}
