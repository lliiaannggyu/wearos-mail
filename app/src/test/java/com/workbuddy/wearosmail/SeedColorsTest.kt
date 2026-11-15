package com.workbuddy.wearosmail

import com.workbuddy.wearosmail.ui.ChipKind
import com.workbuddy.wearosmail.ui.MailPalette
import com.workbuddy.wearosmail.ui.SeedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedColorsTest {

    private fun lightness(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    @Test
    fun `parseHex accepts with and without hash`() {
        assertEquals(0xFF4285F4L, SeedColors.parseHex("#4285F4"))
        assertEquals(0xFF4285F4L, SeedColors.parseHex("4285F4"))
        assertEquals(0xFF4285F4L, SeedColors.parseHex("  #4285f4  "))
        assertEquals(0xFF000000L, SeedColors.parseHex("#000000"))
        assertEquals(0xFFFFFFFFL, SeedColors.parseHex("FFFFFF"))
    }

    @Test
    fun `parseHex rejects malformed input`() {
        assertNull(SeedColors.parseHex("#12345"))
        assertNull(SeedColors.parseHex("#GGGGGG"))
        assertNull(SeedColors.parseHex(""))
        assertNull(SeedColors.parseHex("#1234567"))
        assertNull(SeedColors.parseHex("not-a-color"))
    }

    @Test
    fun `hex round trips for every preset`() {
        SeedColors.PRESETS.forEach { preset ->
            assertEquals(preset.argb, SeedColors.parseHex(SeedColors.hex(preset.argb)))
        }
    }

    @Test
    fun `official presets are valid and distinct`() {
        assertTrue(SeedColors.PRESETS.size >= 6)
        SeedColors.PRESETS.forEach { assertTrue(it.name, SeedColors.isValid(it.argb)) }
        assertEquals(
            SeedColors.PRESETS.size,
            SeedColors.PRESETS.map { it.argb }.toSet().size,
        )
        assertEquals(SeedColors.DEFAULT, SeedColors.PRESETS.first().argb)
    }

    @Test
    fun `seed validation rejects transparent or out of range values`() {
        assertFalse(SeedColors.isValid(0L))
        assertFalse(SeedColors.isValid(0x00FF0000L))
        assertTrue(SeedColors.isValid(SeedColors.DEFAULT))
        assertTrue(SeedColors.isValid(0xFFFFFFFFL))
    }

    @Test
    fun `custom seed changes mail primary but keeps other function colors`() {
        val custom = MailPalette(0xFF4285F4L)
        assertNotEquals(
            MailPalette.DEFAULT.containerFor(ChipKind.MAIL),
            custom.containerFor(ChipKind.MAIL),
        )
        assertEquals(
            MailPalette.DEFAULT.containerFor(ChipKind.SEARCH),
            custom.containerFor(ChipKind.SEARCH),
        )
        assertEquals(
            MailPalette.DEFAULT.containerFor(ChipKind.COMPOSE),
            custom.containerFor(ChipKind.COMPOSE),
        )
        assertEquals(
            MailPalette.DEFAULT.containerFor(ChipKind.UTILITY),
            custom.containerFor(ChipKind.UTILITY),
        )
    }

    @Test
    fun `custom seed tonal palette is monotonic and dark container stays low saturation`() {
        val p = MailPalette(0xFFEA4335L)
        var prev = -1.0
        for (t in 0..100 step 10) {
            val l = lightness(p.seedTone(t))
            assertTrue("tone $t must not get darker ($l >= $prev)", l >= prev - 1e-6)
            prev = l
        }
        assertTrue(lightness(p.seedTone(0)) < 0.02)
        assertTrue(lightness(p.seedTone(100)) > 0.98)
        assertTrue(lightness(p.containerFor(ChipKind.MAIL)) < 0.35)
    }

    @Test
    fun `tonal pure function is deterministic`() {
        val a = MailPalette.tonal(0xFF34A853L, 0.5f, 30)
        val b = MailPalette.tonal(0xFF34A853L, 0.5f, 30)
        assertEquals(a, b)
        assertNotEquals(a, MailPalette.tonal(0xFF34A853L, 0.5f, 40))
    }
}
