package net.the_okazakis.applimon

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class DeveloperSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_settings)

        val tokenInput = findViewById<EditText>(R.id.tokenInput)
        val secretInput = findViewById<EditText>(R.id.secretInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val prefs = PreferenceUtils.getEncryptedPrefs(this)
        tokenInput.setText(prefs.getString("token", ""))
        secretInput.setText(prefs.getString("secret", ""))

        saveButton.setOnClickListener {
            val token = tokenInput.text.toString()
            val secret = secretInput.text.toString()

            if (token.isEmpty() || secret.isEmpty()) {
                Toast.makeText(this, "すべての頁E��を�E力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PreferenceUtils.getEncryptedPrefs(this).edit()
                .putString("token", token)
                .putString("secret", secret)
                .apply()

            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
