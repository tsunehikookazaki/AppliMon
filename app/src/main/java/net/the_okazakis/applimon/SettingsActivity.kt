package net.the_okazakis.applimon

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.edit

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

        // 暗号化された保存内容を読み込んで表示
        val prefs = PreferenceUtils.getEncryptedPrefs(this)
        deviceNameInput.setText(prefs.getString("deviceName", "洗濯機"))
        deviceIdInput.setText(prefs.getString("deviceId", ""))
        startPhraseInput.setText(prefs.getString("startSpokenPhrase", "動作を開始しました。"))
        phraseInput.setText(prefs.getString("spokenPhrase", "動作が終わりました。"))

        saveButton.setOnClickListener {
            val deviceName = deviceNameInput.text.toString()
            val deviceId = deviceIdInput.text.toString()
            val startPhrase = startPhraseInput.text.toString()
            val phrase = phraseInput.text.toString()

            if (deviceName.isEmpty() || deviceId.isEmpty() || startPhrase.isEmpty() || phrase.isEmpty()) {
                Toast.makeText(this, "すべての項目を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 暗号化して保存
            PreferenceUtils.getEncryptedPrefs(this).edit {
                putString("deviceName", deviceName)
                putString("deviceId", deviceId)
                putString("startSpokenPhrase", startPhrase)
                putString("spokenPhrase", phrase)
            }

            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
            finish()
        }

        developerSettingsButton.setOnClickListener {
            startActivity(Intent(this, DeveloperSettingsActivity::class.java))
        }
    }
}
