package com.tengwear.tengemail

import com.tengwear.tengemail.ui.ChipKind
import com.tengwear.tengemail.ui.MailPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MailPaletteTest {

    private fun lightness(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun saturation(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        return if (max == 0.0) 0.0 else (max - min) / max
    }

    @Test
    fun `tone brightness is monotonic from dark to light`() {
        var prev = -1.0
        for (t in 0..100 step 10) {
            val l = lightness(MailPalette.tone(ChipKind.MAIL, t))
            assertTrue("tone $t should be brighter than previous ($l > $prev)", l >= prev - 1e-6)
            prev = l
        }
    }

    @Test
    fun `tone endpoints are near black and white`() {
        assertTrue(lightness(MailPalette.tone(ChipKind.MAIL, 0)) < 0.02)
        assertTrue(lightness(MailPalette.tone(ChipKind.MAIL, 100)) > 0.98)
    }

    @Test
    fun `button container is dark and low saturation`() {
        ChipKind.values().forEach { kind ->
            val container = MailPalette.container(kind)
            assertTrue(
                "$kind container too bright: ${lightness(container)}",
                lightness(container) < 0.35,
            )
            assertTrue(
                "$kind container too saturated: ${saturation(container)}",
                saturation(container) < 0.45,
            )
        }
    }

    @Test
    fun `onContainer is light enough for contrast`() {
        ChipKind.values().forEach { kind ->
            val on = MailPalette.onContainer(kind)
            assertTrue("$kind onContainer too dark", lightness(on) > 0.7)
            assertTrue(
                "$kind contrast too low",
                lightness(on) - lightness(MailPalette.container(kind)) > 0.4,
            )
        }
    }

    @Test
    fun `different functions get different colors`() {
        val colors = ChipKind.values().map { MailPalette.container(it) }
        assertEquals("each function kind must have a distinct container color",
            colors.size, colors.toSet().size)
        assertNotEquals(MailPalette.container(ChipKind.MAIL), MailPalette.container(ChipKind.SEARCH))
    }

    @Test
    fun `tone clamps out of range`() {
        assertEquals(MailPalette.tone(ChipKind.MAIL, 0), MailPalette.tone(ChipKind.MAIL, -10))
        assertEquals(MailPalette.tone(ChipKind.MAIL, 100), MailPalette.tone(ChipKind.MAIL, 200))
    }
}
