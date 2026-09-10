package com.mustafa.whatsappreply

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var autoReplyButton: Button
    private val prefs by lazy { getSharedPreferences("whatsapp_auto_reply", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 72, 40, 40)
        }

        val title = TextView(this).apply {
            text = "WhatsApp Otomatik Cevap"
            textSize = 26f
        }

        val info = TextView(this).apply {
            text = "Yeni WhatsApp bildirimlerini okuyup bildirimdeki Yanıtla özelliğiyle otomatik cevap verir."
            textSize = 17f
            setPadding(0, 24, 0, 28)
        }

        statusText = TextView(this).apply {
            textSize = 17f
            setPadding(0, 0, 0, 24)
        }

        val permissionButton = Button(this).apply {
            text = "Bildirim erişimini aç"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        autoReplyButton = Button(this).apply {
            setOnClickListener {
                val newValue = !prefs.getBoolean("enabled", false)
                prefs.edit().putBoolean("enabled", newValue).apply()
                updateStatus()
            }
        }

        val rules = TextView(this).apply {
            text = "Cevap kuralları:\n\n• Para/borç/ödeme isteyenlere: ‘Şu an maalesef maddi olarak yardımcı olamıyorum, kusura bakma 🙏’\n\n• Diğer mesajlara: ‘Mesajını gördüm. Şu an müsait değilim, en kısa zamanda dönüş yapacağım.’"
            textSize = 16f
            setPadding(0, 28, 0, 0)
        }

        root.addView(title)
        root.addView(info)
        root.addView(statusText)
        root.addView(permissionButton)
        root.addView(autoReplyButton)
        root.addView(rules)

        setContentView(root)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val accessEnabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )?.contains(packageName) == true

        val autoReplyEnabled = prefs.getBoolean("enabled", false)

        statusText.text = "Bildirim erişimi: ${if (accessEnabled) "AÇIK" else "KAPALI"}\nOtomatik cevap: ${if (autoReplyEnabled) "AÇIK" else "KAPALI"}"
        autoReplyButton.text = if (autoReplyEnabled) "Otomatik cevabı kapat" else "Otomatik cevabı aç"
    }
}
