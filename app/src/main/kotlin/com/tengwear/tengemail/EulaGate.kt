package com.tengwear.tengemail

/**
 * 使用协议「同意」门槛的纯逻辑（无 Android / Compose 依赖，便于 JVM 单测）。
 */
object EulaGate {
    /**
     * 只有协议列表的最后一项已经可见（即用户滑到了最底部）才允许点「同意」。
     *
     * @param lastVisibleIndex 当前可见项中的最大 index，无可见项时为 null
     * @param totalItems 列表总项数
     */
    fun canAgree(lastVisibleIndex: Int?, totalItems: Int): Boolean {
        if (lastVisibleIndex == null || totalItems <= 0) return false
        return lastVisibleIndex >= totalItems - 1
    }

    /**
     * 协议按版本管理：条款修订后提升版本号，已同意旧版本的用户必须重新同意。
     */
    fun isAccepted(acceptedVersion: Int, currentVersion: Int): Boolean =
        currentVersion > 0 && acceptedVersion >= currentVersion
}
