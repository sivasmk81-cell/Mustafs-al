package com.mustafa.ai

import android.app.AlertDialog
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var messagesLayout: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private val prefs by lazy { getSharedPreferences("mustafa_ai", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 24)
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Mustafa AI"
            textSize = 28f
            setPadding(8, 0, 8, 8)
        }

        val apiButton = Button(this).apply {
            text = "API Ayarla"
            setOnClickListener { showApiKeyDialog() }
        }

        titleRow.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(apiButton)

        val subtitle = TextView(this).apply {
            text = "Gerçek yapay zekâ sohbeti"
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

        sendButton = Button(this).apply {
            text = "Gönder"
            setOnClickListener { sendMessage() }
        }

        inputRow.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        inputRow.addView(sendButton)

        root.addView(titleRow)
        root.addView(subtitle)
        root.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(inputRow)

        setContentView(root)

        addMessage("Mustafa AI", "Merhaba! Gerçek yapay zekâ bağlantısı hazır. Önce sağ üstteki API Ayarla düğmesine dokun.")
    }

    private fun showApiKeyDialog() {
        val field = EditText(this).apply {
            hint = "sk-..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("openai_api_key", ""))
        }

        AlertDialog.Builder(this)
            .setTitle("OpenAI API anahtarı")
            .setMessage("Anahtar yalnızca bu telefonda saklanır. GitHub'a gönderilmez.")
            .setView(field)
            .setPositiveButton("Kaydet") { _, _ ->
                prefs.edit().putString("openai_api_key", field.text.toString().trim()).apply()
                addMessage("Mustafa AI", "API anahtarı kaydedildi. Artık bana bir soru yazabilirsin.")
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun sendMessage() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        val apiKey = prefs.getString("openai_api_key", "").orEmpty()
        if (apiKey.isBlank()) {
            showApiKeyDialog()
            return
        }

        addMessage("Sen", text)
        input.setText("")
        sendButton.isEnabled = false
        addMessage("Mustafa AI", "Düşünüyorum...")

        Thread {
            try {
                val reply = callOpenAI(apiKey, text)
                runOnUiThread {
                    removeLastThinkingMessage()
                    addMessage("Mustafa AI", reply)
                    sendButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    removeLastThinkingMessage()
                    addMessage("Mustafa AI", "Bağlantı hatası: ${e.message ?: "Bilinmeyen hata"}")
                    sendButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun callOpenAI(apiKey: String, message: String): String {
        val url = URL("https://api.openai.com/v1/responses")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject().apply {
            put("model", "gpt-5.6-luna")
            put("store", false)
            put("input", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().put(
                        JSONObject().apply {
                            put("type", "input_text")
                            put("text", message)
                        }
                    ))
                }
            ))
        }

        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (code !in 200..299) {
            val errorMessage = try {
                JSONObject(responseText).optJSONObject("error")?.optString("message")
            } catch (_: Exception) { null }
            throw Exception(errorMessage ?: "HTTP $code")
        }

        val json = JSONObject(responseText)
        val output = json.optJSONArray("output") ?: return "Yanıt alınamadı."
        val parts = mutableListOf<String>()

        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            if (item.optString("type") != "message") continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                if (part.optString("type") == "output_text") {
                    val text = part.optString("text")
                    if (text.isNotBlank()) parts.add(text)
                }
            }
        }

        return parts.joinToString("\n").ifBlank { "Yanıt alınamadı." }
    }

    private fun addMessage(sender: String, text: String) {
        val item = TextView(this).apply {
            this.text = "$sender:\n$text"
            textSize = 17f
            setPadding(24, 18, 24, 18)
        }
        messagesLayout.addView(item, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 6, 0, 6) })
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun removeLastThinkingMessage() {
        if (messagesLayout.childCount > 0) {
            val last = messagesLayout.getChildAt(messagesLayout.childCount - 1) as? TextView
            if (last?.text?.toString()?.contains("Düşünüyorum...") == true) {
                messagesLayout.removeView(last)
            }
        }
    }
}
