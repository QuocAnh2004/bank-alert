package com.batoluxi.bankvoice

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Hệ thống Android gọi onNotificationPosted() mỗi khi có thông báo mới.
 * Luồng: thông báo → chỉ giữ app ngân hàng đã chọn → AmountParser → Speaker đọc to.
 */
class BankNotificationService : NotificationListenerService() {

    private lateinit var speaker: Speaker

    // Chống đọc trùng: một số app cập nhật lại cùng 1 thông báo nhiều lần
    private var lastKey = ""
    private var lastTime = 0L

    override fun onCreate() {
        super.onCreate()
        speaker = Speaker(this)
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Prefs.getBanks(this)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val full = "$title $text".trim()
        if (full.isEmpty()) return

        val amount = AmountParser.parse(full)
        Log.d(TAG, "pkg=${sbn.packageName} | $full | amount=$amount")
        Prefs.setLast(this, "$full\n→ ${amount?.let { AmountParser.toSpeech(it) } ?: "không phải tiền vào"}")

        if (amount == null) return

        val key = "${sbn.packageName}|$full"
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastTime < 10_000) return
        lastKey = key
        lastTime = now

        speaker.speak(AmountParser.toSpeech(amount))
    }

    companion object {
        const val TAG = "BankNoti"
    }
}
