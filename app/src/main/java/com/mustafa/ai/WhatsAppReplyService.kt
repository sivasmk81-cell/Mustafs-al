package com.mustafa.whatsappreply

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class WhatsAppReplyService : NotificationListenerService() {

    private val handled = LinkedHashMap<String, Long>()

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

        val now = System.currentTimeMillis()
        handled.entries.removeAll { now - it.value > 60000 }
        val signature = "$packageName|$title|$message"
        if (handled.containsKey(signature)) return

        // Önce işaretle: WhatsApp aynı bildirimi art arda yenilerse çift cevap gitmesin.
        handled[signature] = now

        val action = notification.actions?.firstOrNull { item ->
            item.remoteInputs?.isNotEmpty() == true
        } ?: return

        sendReply(action.actionIntent, action.remoteInputs, buildReply(message))
    }

    private fun buildReply(message: String): String {
        val lower = message.lowercase()
            .replace("?", "")
            .replace("!", "")
            .trim()

        val moneyWords = listOf(
            "para", "borç", "borc", "ödeme", "odeme", "iban", "nakit",
            "havale", "eft", "kredi", "para lazım", "para lazim",
            "gönderir misin", "gonderir misin"
        )
        if (moneyWords.any { lower.contains(it) }) {
            return "Şu an maalesef maddi olarak yardımcı olamıyorum, kusura bakma 🙏"
        }

        return when {
            lower == "merhaba" || lower == "selam" || lower == "selamlar" -> "Merhaba 👋"
            lower.contains("günaydın") || lower.contains("gunaydin") -> "Günaydın 😊"
            lower.contains("iyi akşamlar") || lower.contains("iyi aksamlar") -> "İyi akşamlar 😊"
            lower.contains("iyi geceler") -> "İyi geceler 😊"
            lower.contains("nasılsın") || lower.contains("nasilsin") || lower == "naber" || lower == "ne haber" -> "İyiyim, teşekkür ederim. Sen nasılsın?"
            lower == "iyi" || lower == "iyiyim" || lower.contains("ben de iyiyim") -> "Sevindim 😊"
            lower.contains("teşekkür") || lower.contains("tesekkur") || lower == "sağ ol" || lower == "sag ol" -> "Rica ederim 😊"
            lower == "tamam" || lower == "ok" || lower == "olur" -> "Tamam 👍"
            lower.contains("neredesin") -> "Şu an biraz meşgulüm, sonra konuşalım."
            lower.contains("müsait misin") || lower.contains("musait misin") -> "Şu an pek müsait değilim, birazdan yazarım."
            lower.contains("ara beni") || lower.contains("arar mısın") || lower.contains("arar misin") -> "Tamam, müsait olunca ararım."
            else -> "Mesajını gördüm 👍 Birazdan sana dönüş yaparım."
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
            inputs.forEach { remoteInput -> bundle.putCharSequence(remoteInput.resultKey, replyText) }
            RemoteInput.addResultsToIntent(inputs, intent, bundle)
            pendingIntent.send(this, 0, intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
