package net.the_okazakis.applimon

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class HelpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val step1Desc = findViewById<TextView>(R.id.helpStep1Desc)
        val fullText = getString(R.string.help_step1_desc)
        val linkText = "詳しくはこちめE
        
        val spannable = SpannableString(fullText)
        val startIndex = fullText.indexOf(linkText)
        
        if (startIndex != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@HelpActivity, HelpDetailActivity::class.java))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@HelpActivity, android.R.color.holo_blue_dark)
                    ds.isUnderlineText = true
                }
            }
            spannable.setSpan(clickableSpan, startIndex, startIndex + linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            step1Desc.text = spannable
            step1Desc.movementMethod = LinkMovementMethod.getInstance()
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}
