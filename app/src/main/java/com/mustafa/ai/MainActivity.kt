package com.mustafa.ai

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var messagesLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 24)
        }

        val title = TextView(this).apply {
            text = "Mustafa AI"
            textSize = 28f
            setPadding(8, 0, 8, 8)
        }

        val subtitle = TextView(this).apply {
            text = "Sohbet asistanın"
            textSize = 15f
            setPadding(8, 0, 8, 20)
        }

        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 8, 0, 8)
        }

        scrollView = ScrollView(this).apply {
            addView(messagesLayout)
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 0)
        }

        input = EditText(this).apply {
            hint = "Mesajını yaz..."
            textSize = 17f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
        }

        val sendButton = Button(this).apply {
            text = "Gönder"
            setOnClickListener { sendMessage() }
        }

        inputRow.addView(
            input,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        inputRow.addView(sendButton)

        root.addView(title)
        root.addView(subtitle)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        root.addView(inputRow)

        setContentView(root)

        addMessage("Mustafa AI", "Merhaba! Ben Mustafa AI. Bana bir şey yaz, sohbet edelim.")
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        addMessage("Sen", text)
        input.setText("")

        val reply = createLocalReply(text)
        addMessage("Mustafa AI", reply)
    }

    private fun addMessage(sender: String, text: String) {
        val item = TextView(this).apply {
            this.text = "$sender:\n$text"
            textSize = 17f
            setPadding(24, 18, 24, 18)
        }
        messagesLayout.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        )
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun createLocalReply(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("merhaba") || lower.contains("selam") ->
                "Merhaba! Buradayım. Sana nasıl yardımcı olabilirim?"
            lower.contains("nasılsın") ->
                "İyiyim, teşekkür ederim. Sen nasılsın?"
            lower.contains("adın") ->
                "Ben Mustafa AI."
            lower.contains("saat") ->
                "Şimdilik canlı saat bilgisine bağlı değilim."
            else ->
                "Mesajını aldım: ‘$message’\n\nBu sürüm sohbet ekranını test ediyor. Bir sonraki adımda beni gerçek çevrim içi yapay zekâ servisine bağlayacağız."
        }
    }
}
