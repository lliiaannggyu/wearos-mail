package com.tengwear.tengemail.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 应用内更新的语义化版本比较（纯 JVM 逻辑，无需设备）。 */
class UpdateManagerTest {

    @Test
    fun newerVersionsAreDetected() {
        assertTrue(UpdateManager.isNewer("1.0.1", "1.0.0"))
        assertTrue(UpdateManager.isNewer("v1.2.0", "1.1.9"))
        assertTrue(UpdateManager.isNewer("2.0.0", "1.9.9"))
        assertTrue(UpdateManager.isNewer("1.0.10", "1.0.9"))
    }

    @Test
    fun sameOrOlderVersionsAreRejected() {
        assertFalse(UpdateManager.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateManager.isNewer("0.9.9", "1.0.0"))
        assertFalse(UpdateManager.isNewer("", "1.0.0"))
        assertFalse(UpdateManager.isNewer("1.0", "1.0.0"))
    }
}
