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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView1: TextView
    private lateinit var statusTextView2: TextView
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

        // ステータスバーとの重なりを防止（アプリ全体に余白を追加）
        val rootLayout = findViewById<android.widget.LinearLayout>(R.id.mainRootLayout)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ツールバーの設定
        val toolbar = findViewById<Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        statusTextView1 = findViewById(R.id.statusTextView1)
        statusTextView2 = findViewById(R.id.statusTextView2)
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

        // 機器1の状態監視
        lifecycleScope.launch {
            WashingManager.device1.statusText.collect { text ->
                statusTextView1.text = text
            }
        }
        lifecycleScope.launch {
            WashingManager.device1.statusColor.collect { color ->
                statusTextView1.setTextColor(color)
            }
        }

        // 機器2の状態監視
        lifecycleScope.launch {
            WashingManager.device2.statusText.collect { text ->
                statusTextView2.text = text
            }
        }
        lifecycleScope.launch {
            WashingManager.device2.statusColor.collect { color ->
                statusTextView2.setTextColor(color)
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
            WashingManager.device1.lastCompletionTime = 0L
            WashingManager.device1.statusText.value = "【 機器1: 待機中 】\n\n(ロック手動解除)"
            WashingManager.device1.statusColor.value = Color.BLACK

            WashingManager.device2.lastCompletionTime = 0L
            WashingManager.device2.statusText.value = "【 機器2: 待機中 】\n\n(ロック手動解除)"
            WashingManager.device2.statusColor.value = Color.BLACK
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
        updateDeviceNameDisplay()
    }

    private fun updateDeviceNameDisplay() {
        val prefs = PreferenceUtils.getEncryptedPrefs(this)
        val name1 = prefs.getString("deviceName1", prefs.getString("deviceName", "洗濯機"))
        val name2 = prefs.getString("deviceName2", "乾燥機")
        
        deviceNameTextView.text = getString(R.string.current_monitoring_target_two, name1, name2)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "トークン等の入力")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 2, 1, "プラグの入力")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu?.add(0, 3, 2, "使用方法")?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                startActivity(Intent(this, DeveloperSettingsActivity::class.java))
                return true
            }
            2 -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            3 -> {
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
