package net.the_okazakis.applimon

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class HelpDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_detail)

        findViewById<Button>(R.id.closeButton).setOnClickListener {
            finish()
        }
    }
}
