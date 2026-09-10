package com.mustafa.whatsappreply

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppReplyService : NotificationListenerService() {

    private var lastSignature: String? = null
    private var lastHandledAt: Long = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return

        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val prefs = getSharedPreferences("whatsapp_auto_reply", MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (message.isBlank()) return

        val signature = "$packageName|$title|$message"
        val now = System.currentTimeMillis()
        if (signature == lastSignature && now - lastHandledAt < 15000) return

        val action = notification.actions?.firstOrNull { item ->
            item.remoteInputs?.isNotEmpty() == true
        } ?: return

        val replyText = buildReply(message)
        if (sendReply(action.actionIntent, action.remoteInputs, replyText)) {
            lastSignature = signature
            lastHandledAt = now
        }
    }

    private fun buildReply(message: String): String {
        val lower = message.lowercase()
        val moneyWords = listOf(
            "para", "borç", "borc", "ödeme", "odeme", "iban", "nakit",
            "havale", "eft", "kredi", "para lazım", "para lazim", "yardım et",
            "yardim et", "gönderir misin", "gonderir misin"
        )

        return if (moneyWords.any { lower.contains(it) }) {
            "Şu an maalesef maddi olarak yardımcı olamıyorum, kusura bakma 🙏"
        } else {
            "Mesajını gördüm. Şu an müsait değilim, en kısa zamanda dönüş yapacağım."
        }
    }

    private fun sendReply(
        pendingIntent: PendingIntent,
        remoteInputs: Array<RemoteInput>?,
        replyText: String
    ): Boolean {
        val inputs = remoteInputs ?: return false
        return try {
            val intent = Intent()
            val bundle = Bundle()
            inputs.forEach { remoteInput ->
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(inputs, intent, bundle)
            pendingIntent.send(this, 0, intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
