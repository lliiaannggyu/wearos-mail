package com.tengwear.tengemail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EulaGateTest {

    @Test
    fun `cannot agree before reaching the last item`() {
        assertFalse(EulaGate.canAgree(null, 10))
        assertFalse(EulaGate.canAgree(-1, 10))
        assertFalse(EulaGate.canAgree(0, 10))
        assertFalse(EulaGate.canAgree(8, 10))
    }

    @Test
    fun `can agree once the last item is visible`() {
        assertTrue(EulaGate.canAgree(9, 10))
        assertTrue(EulaGate.canAgree(10, 10))
        assertTrue(EulaGate.canAgree(0, 1))
    }

    @Test
    fun `empty or invalid list never unlocks`() {
        assertFalse(EulaGate.canAgree(0, 0))
        assertFalse(EulaGate.canAgree(null, 0))
        assertFalse(EulaGate.canAgree(5, -1))
    }

    @Test
    fun `versioned acceptance requires re-accept after revision`() {
        assertFalse(EulaGate.isAccepted(0, 1))
        assertTrue(EulaGate.isAccepted(1, 1))
        assertTrue(EulaGate.isAccepted(2, 1))
        assertFalse(EulaGate.isAccepted(1, 2))
        assertFalse(EulaGate.isAccepted(0, 0))
        assertFalse(EulaGate.isAccepted(1, 0))
    }

    @Test
    fun `agreement text is complete and versioned`() {
        assertTrue(EulaText.VERSION > 0)
        assertTrue(EulaText.TITLE.isNotBlank())
        assertTrue(EulaText.SUMMARY.isNotBlank())
        assertTrue(EulaText.SECTIONS.size >= 8)
        EulaText.SECTIONS.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue("协议正文过短: ${it.title}", it.body.length >= 30)
        }
        assertEquals(EulaText.SECTIONS.size, EulaText.SECTIONS.map { it.title }.toSet().size)
    }
}
