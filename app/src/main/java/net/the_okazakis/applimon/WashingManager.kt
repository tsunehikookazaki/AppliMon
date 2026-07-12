package net.the_okazakis.applimon

import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

object WashingManager {
    // 設定値
    var checkIntervalMs = 5000L
    var cooldownDurationMs = 1800000L
    var targetVolumePercent = 80

    // 状態管理
    var isWashing = false
    var startTimeStr = ""
    var startTimeMillis = 0L // 経過時間を計算するための正確なスタート時刻
    var lastCompletionTime = 0L

    // 画面へ状態を伝えるためのリアルタイムな流れ（Flow）
    val statusText = MutableStateFlow("待機中")
    val statusColor = MutableStateFlow(Color.BLACK)
}
