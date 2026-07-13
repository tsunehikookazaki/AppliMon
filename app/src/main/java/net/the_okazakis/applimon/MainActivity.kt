package net.the_okazakis.applimon

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var deviceNameTextView: TextView
    private lateinit var sharedPref: SharedPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            startWashingService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        deviceNameTextView = findViewById(R.id.deviceNameTextView)

        val intervalPicker = findViewById<NumberPicker>(R.id.intervalPicker)
        val cooldownPicker = findViewById<NumberPicker>(R.id.cooldownPicker)
        val volumePicker = findViewById<NumberPicker>(R.id.volumePicker)

        val saveSettingsButton = findViewById<Button>(R.id.saveSettingsButton)
        val unlockButton = findViewById<Button>(R.id.unlockButton)
        val exitButton = findViewById<Button>(R.id.exitButton)

        val settingsButton = findViewById<Button>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 機器名の表示を更新する
        updateDeviceNameDisplay()

        intervalPicker.minValue = 1
        intervalPicker.maxValue = 300 

        cooldownPicker.minValue = 1
        cooldownPicker.maxValue = 120 

        volumePicker.minValue = 15
        volumePicker.maxValue = 100   
        volumePicker.wrapSelectorWheel = false

        sharedPref = getSharedPreferences("WashNotifPrefs", MODE_PRIVATE)
        val savedSeconds = sharedPref.getLong("intervalSec", 5L)
        val savedMinutes = sharedPref.getLong("cooldownMin", 10L)
        val savedVolume = sharedPref.getInt("volumePercent", 40)

        WashingManager.checkIntervalMs = savedSeconds * 1000L
        WashingManager.cooldownDurationMs = savedMinutes * 60L * 1000L
        WashingManager.targetVolumePercent = savedVolume

        intervalPicker.value = savedSeconds.toInt()
        cooldownPicker.value = savedMinutes.toInt()
        volumePicker.value = savedVolume

        lifecycleScope.launch {
            WashingManager.statusText.collect { text ->
                statusTextView.text = text
            }
        }
        lifecycleScope.launch {
            WashingManager.statusColor.collect { color ->
                statusTextView.setTextColor(color)
            }
        }

        saveSettingsButton.setOnClickListener {
            val inputSeconds = intervalPicker.value.toLong()
            val inputMinutes = cooldownPicker.value.toLong()
            val boundedVolume = volumePicker.value

            WashingManager.checkIntervalMs = inputSeconds * 1000L
            WashingManager.cooldownDurationMs = inputMinutes * 60L * 1000L
            WashingManager.targetVolumePercent = boundedVolume

            sharedPref.edit {
                putLong("intervalSec", inputSeconds)
                putLong("cooldownMin", inputMinutes)
                putInt("volumePercent", boundedVolume)
            }

            startWashingService()
        }

        unlockButton.setOnClickListener {
            WashingManager.lastCompletionTime = 0L
            WashingManager.statusText.value = "【 待機中 】\n\n(ロック手動解除)"
            WashingManager.statusColor.value = Color.BLACK
        }

        exitButton.setOnClickListener {
            val serviceIntent = Intent(this, WashNotificationService::class.java)
            stopService(serviceIntent)
            finish()
        }

        checkNotificationPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        // 設定から戻ってきた時に機器名表示を更新
        updateDeviceNameDisplay()
    }

    private fun updateDeviceNameDisplay() {
        val prefs = PreferenceUtils.getEncryptedPrefs(this)
        val deviceName = prefs.getString("deviceName", "洗濯機")
        deviceNameTextView.text = getString(R.string.current_monitoring_target, deviceName)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "外部連携設定")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 2, 1, "使用方法")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            2 -> {
                startActivity(Intent(this, HelpActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                startWashingService()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startWashingService()
        }
    }

    private fun startWashingService() {
        val serviceIntent = Intent(this, WashNotificationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
