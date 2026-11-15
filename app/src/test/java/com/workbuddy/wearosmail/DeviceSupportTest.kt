package com.workbuddy.wearosmail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 覆盖闪退根因的判定逻辑：只有「手表设备」且「Wear 共享库可用」时
 * 才允许挂载 AmbientLifecycleObserver，其余任何组合都必须降级。
 */
class DeviceSupportTest {

    @Test
    fun `手表且共享库可用才启用微光`() {
        assertTrue(DeviceSupport.shouldEnableAmbient(isWatch = true, hasLibrary = true))
    }

    @Test
    fun `普通安卓手机无手表特性时必须降级`() {
        assertFalse(DeviceSupport.shouldEnableAmbient(isWatch = false, hasLibrary = false))
    }

    @Test
    fun `有手表特性但缺共享库时必须降级`() {
        // 国产手表 / 非 Wear OS 系统：正是原来闪退的场景
        assertFalse(DeviceSupport.shouldEnableAmbient(isWatch = true, hasLibrary = false))
    }

    @Test
    fun `共享库存在但不是手表设备时也不启用`() {
        assertFalse(DeviceSupport.shouldEnableAmbient(isWatch = false, hasLibrary = true))
    }
}
