package net.the_okazakis.applimon

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val deviceNameInput = findViewById<EditText>(R.id.deviceNameInput)
        val deviceIdInput = findViewById<EditText>(R.id.deviceIdInput)
        val startPhraseInput = findViewById<EditText>(R.id.startPhraseInput)
        val phraseInput = findViewById<EditText>(R.id.phraseInput)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val developerSettingsButton = findViewById<Button>(R.id.developerSettingsButton)

        // 暗号化された保存�E容を読み込んで表示
        val prefs = PreferenceUtils.getEncryptedPrefs(this)
        deviceNameInput.setText(prefs.getString("deviceName", "洗濯橁E))
        deviceIdInput.setText(prefs.getString("deviceId", ""))
        startPhraseInput.setText(prefs.getString("startSpokenPhrase", "動作を開始しました、E))
        phraseInput.setText(prefs.getString("spokenPhrase", "動作が終わりました、E))

        saveButton.setOnClickListener {
            val deviceName = deviceNameInput.text.toString()
            val deviceId = deviceIdInput.text.toString()
            val startPhrase = startPhraseInput.text.toString()
            val phrase = phraseInput.text.toString()

            if (deviceName.isEmpty() || deviceId.isEmpty() || startPhrase.isEmpty() || phrase.isEmpty()) {
                Toast.makeText(this, "すべての頁E��を�E力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 暗号化して保孁E
            PreferenceUtils.getEncryptedPrefs(this).edit()
                .putString("deviceName", deviceName)
                .putString("deviceId", deviceId)
                .putString("startSpokenPhrase", startPhrase)
                .putString("spokenPhrase", phrase)
                .apply()

            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
            finish()
        }

        developerSettingsButton.setOnClickListener {
            startActivity(Intent(this, DeveloperSettingsActivity::class.java))
        }
    }
}
