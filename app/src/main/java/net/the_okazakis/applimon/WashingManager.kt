package net.the_okazakis.applimon

import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

class DeviceState(val id: Int) {
    var isWashing = false
    var startTimeStr = ""
    var startTimeMillis = 0L
    var lastCompletionTime = 0L

    val statusText = MutableStateFlow("待機中")
    val statusColor = MutableStateFlow(Color.BLACK)
}

object WashingManager {
    // 設定値
    var checkIntervalMs = 5000L
    var cooldownDurationMs = 600000L
    var targetVolumePercent = 40

    // 状態管理（2台分）
    val device1 = DeviceState(1)
    val device2 = DeviceState(2)
}
