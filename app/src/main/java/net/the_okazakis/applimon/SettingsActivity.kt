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

        val deviceNameInput1 = findViewById<EditText>(R.id.deviceNameInput1)
        val deviceIdInput1 = findViewById<EditText>(R.id.deviceIdInput1)
        val startPhraseInput1 = findViewById<EditText>(R.id.startPhraseInput1)
        val phraseInput1 = findViewById<EditText>(R.id.phraseInput1)

        val deviceNameInput2 = findViewById<EditText>(R.id.deviceNameInput2)
        val deviceIdInput2 = findViewById<EditText>(R.id.deviceIdInput2)
        val startPhraseInput2 = findViewById<EditText>(R.id.startPhraseInput2)
        val phraseInput2 = findViewById<EditText>(R.id.phraseInput2)

        val saveButton = findViewById<Button>(R.id.saveButton)
        val developerSettingsButton = findViewById<Button>(R.id.developerSettingsButton)

        val prefs = PreferenceUtils.getEncryptedPrefs(this)
        
        // 機器1 (互換性のため古いキーもチェック)
        deviceNameInput1.setText(prefs.getString("deviceName1", prefs.getString("deviceName", "洗濯機")))
        deviceIdInput1.setText(prefs.getString("deviceId1", prefs.getString("deviceId", "")))
        startPhraseInput1.setText(prefs.getString("startSpokenPhrase1", prefs.getString("startSpokenPhrase", "動作を開始しました。")))
        phraseInput1.setText(prefs.getString("spokenPhrase1", prefs.getString("spokenPhrase", "動作が終わりました。")))

        // 機器2
        deviceNameInput2.setText(prefs.getString("deviceName2", "乾燥機"))
        deviceIdInput2.setText(prefs.getString("deviceId2", ""))
        startPhraseInput2.setText(prefs.getString("startSpokenPhrase2", "乾燥を開始しました。"))
        phraseInput2.setText(prefs.getString("spokenPhrase2", "乾燥が終わりました。"))

        saveButton.setOnClickListener {
            val name1 = deviceNameInput1.text.toString()
            val id1 = deviceIdInput1.text.toString()
            val start1 = startPhraseInput1.text.toString()
            val end1 = phraseInput1.text.toString()

            val name2 = deviceNameInput2.text.toString()
            val id2 = deviceIdInput2.text.toString()
            val start2 = startPhraseInput2.text.toString()
            val end2 = phraseInput2.text.toString()

            if (name1.isEmpty() || id1.isEmpty()) {
                Toast.makeText(this, "機器1の設定を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PreferenceUtils.getEncryptedPrefs(this).edit {
                putString("deviceName1", name1)
                putString("deviceId1", id1)
                putString("startSpokenPhrase1", start1)
                putString("spokenPhrase1", end1)
                
                putString("deviceName2", name2)
                putString("deviceId2", id2)
                putString("startSpokenPhrase2", start2)
                putString("spokenPhrase2", end2)
            }

            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
            finish()
        }

        developerSettingsButton.setOnClickListener {
            startActivity(Intent(this, DeveloperSettingsActivity::class.java))
        }
    }
}
