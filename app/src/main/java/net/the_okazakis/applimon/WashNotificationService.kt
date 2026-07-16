package net.the_okazakis.applimon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WashNotificationService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var serviceJob: Job? = null
    private var originalVolume = 0
    private val client = OkHttpClient()

    private val channelId = "WashNotifServiceChannel"
    private val notificationId = 101

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification("バックグラウンドで監視中...")

        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val prefs = PreferenceUtils.getEncryptedPrefs(this@WashNotificationService)
                    val currentToken = prefs.getString("token", "") ?: ""
                    val currentSecret = prefs.getString("secret", "") ?: ""
                    
                    if (currentToken.isNotEmpty() && currentSecret.isNotEmpty()) {
                        // 機器1の監視
                        val id1 = prefs.getString("deviceId1", prefs.getString("deviceId", "")) ?: ""
                        val name1 = prefs.getString("deviceName1", prefs.getString("deviceName", "洗濯機")) ?: "洗濯機"
                        val start1 = prefs.getString("startSpokenPhrase1", prefs.getString("startSpokenPhrase", "動作を開始しました。")) ?: ""
                        val end1 = prefs.getString("spokenPhrase1", prefs.getString("spokenPhrase", "動作が終わりました。")) ?: ""
                        
                        if (id1.isNotEmpty()) {
                            fetchPowerStatus(currentToken, currentSecret, id1, name1, start1, end1, WashingManager.device1)
                        }

                        // 機器2の監視
                        val id2 = prefs.getString("deviceId2", "") ?: ""
                        val name2 = prefs.getString("deviceName2", "乾燥機") ?: "乾燥機"
                        val start2 = prefs.getString("startSpokenPhrase2", "乾燥を開始しました。") ?: ""
                        val end2 = prefs.getString("spokenPhrase2", "乾燥が終わりました。") ?: ""

                        if (id2.isNotEmpty()) {
                            fetchPowerStatus(currentToken, currentSecret, id2, name2, start2, end2, WashingManager.device2)
                        }
                    } else {
                        Log.e("SwitchBotService", "トークンまたはシークレットが設定されていません")
                    }
                } catch (e: Exception) {
                    Log.e("SwitchBotService", "設定読み込みエラー: ${e.message}")
                }
                delay(WashingManager.checkIntervalMs)
            }
        }

        return START_STICKY
    }

    private fun updateNotification(contentText: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AppliMon10")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.JAPANESE
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { restoreOriginalVolume() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { restoreOriginalVolume() }
            })
        }
    }

    private fun fetchPowerStatus(token: String, secret: String, deviceId: String, deviceName: String, startPhrase: String, endPhrase: String, state: DeviceState) {
        try {
            val t = System.currentTimeMillis().toString()
            val nonce = UUID.randomUUID().toString()
            val stringToSign = "$token$t$nonce"

            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val hmacBytes = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
            val sign = Base64.encodeToString(hmacBytes, Base64.NO_WRAP)

            val request = Request.Builder()
                .url("https://api.switch-bot.com/v1.1/devices/$deviceId/status")
                .addHeader("Authorization", token)
                .addHeader("sign", sign)
                .addHeader("nonce", nonce)
                .addHeader("t", t)
                .addHeader("Content-Type", "application/json; charset=utf8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return
                    val jsonObject = JSONObject(responseBody)
                    val body = jsonObject.getJSONObject("body")
                    
                    val powerWatts = when {
                        body.has("watt") -> body.getDouble("watt")
                        body.has("weight") -> body.getDouble("weight")
                        else -> 0.0
                    }

                    Log.e("SwitchBotService", "現在の消費電力($deviceName): $powerWatts W")

                    val currentTimeMillis = System.currentTimeMillis()
                    val currentTimeStr = SimpleDateFormat("HH時mm分", Locale.JAPAN).format(Date())

                    if (powerWatts > 5.0) {
                        if (!state.isWashing) {
                            if (currentTimeMillis - state.lastCompletionTime > WashingManager.cooldownDurationMs) {
                                state.isWashing = true
                                state.startTimeStr = currentTimeStr
                                state.startTimeMillis = currentTimeMillis

                                speak(startPhrase)
                                state.statusText.value = "【 $deviceName: 動作中 】\n\n開始時刻: ${state.startTimeStr}\n経過時間: 0分0秒\n(現在: $powerWatts W)"
                                state.statusColor.value = Color.BLACK
                                updateNotification("$deviceName 動作開始 $currentTimeStr")
                            } else {
                                Log.e("SwitchBotService", "パタパタ運転を検知。ガード期間のため無視します。")
                            }
                        } else {
                            val elapsedMs = currentTimeMillis - state.startTimeMillis
                            val elapsedMinutes = elapsedMs / 60000
                            val elapsedSeconds = (elapsedMs % 60000) / 1000
                            
                            state.statusText.value = "【 $deviceName: 動作中 】\n\n開始時刻: ${state.startTimeStr}\n経過時間: ${elapsedMinutes}分${elapsedSeconds}秒\n(現在: $powerWatts W)"
                            updateNotification("$deviceName 動作中: 約${elapsedMinutes}分経過 ($powerWatts W)")
                        }
                    } else if (powerWatts < 1.0 && state.isWashing) {
                        state.isWashing = false
                        state.lastCompletionTime = currentTimeMillis
                        speak(endPhrase)

                        val totalElapsedMs = currentTimeMillis - state.startTimeMillis
                        val totalElapsedMinutes = totalElapsedMs / 60000
                        val totalElapsedSeconds = (totalElapsedMs % 60000) / 1000

                        state.statusText.value = "【 $deviceName: 完了 】\n\n開始時刻: ${state.startTimeStr}\n終了時刻: $currentTimeStr\n(所要時間: ${totalElapsedMinutes}分${totalElapsedSeconds}秒)"
                        state.statusColor.value = Color.BLUE
                        updateNotification("$deviceName 完了しました ($currentTimeStr)")

                    } else if (!state.isWashing) {
                        val passedTime = currentTimeMillis - state.lastCompletionTime
                        if (passedTime < WashingManager.cooldownDurationMs) {
                            val remainingMin = ((WashingManager.cooldownDurationMs - passedTime) / 60000) + 1
                            state.statusText.value = "【 $deviceName: 完了・ガード中 】\n\n誤検知防止ロック残り: 約$remainingMin 分\n(現在: $powerWatts W)"
                        } else {
                            state.statusText.value = "【 $deviceName: 待機中 】\n\n(現在: $powerWatts W)"
                            state.statusColor.value = Color.BLACK
                            // updateNotification("$deviceName 待機中...") // 2台あると通知が頻繁に書き換わるので待機中は抑制してもいいかも
                        }
                    }
                } else {
                    Log.e("SwitchBotService", "HTTPエラー: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("SwitchBotService", "例外エラー: ${e.message}")
        }
    }

    private fun speak(text: String) {
        if (text.isEmpty()) return
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (maxVolume * (WashingManager.targetVolumePercent / 100.0)).toInt()

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "WashUtterance")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "WashUtterance")
    }

    private fun restoreOriginalVolume() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId, "AppliMon10 監視サービス",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? { return null }
}
