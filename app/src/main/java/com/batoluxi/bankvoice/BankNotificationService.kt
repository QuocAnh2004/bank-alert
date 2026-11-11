package com.batoluxi.bankvoice

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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
        speaker = Speaker(this) { status -> Prefs.setTts(this, status) }
    }

    override fun onDestroy() {
        connected = false
        speaker.shutdown()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        Log.i(TAG, "Đã kết nối đọc thông báo")
    }

    override fun onListenerDisconnected() {
        connected = false
        Log.w(TAG, "Bị ngắt kết nối → xin kết nối lại")
        NotificationListenerService.requestRebind(ComponentName(this, BankNotificationService::class.java))
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Prefs.getBanks(this)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val full = "$title $text".trim()
        if (full.isEmpty()) return

        val txn = AmountParser.parseTxn(full)
        val bankName = getBankName(sbn.packageName)
        val readOutgoing = Prefs.getReadOutgoing(this)
        val shouldSpeak = txn != null && (txn.incoming || readOutgoing)
        Log.d(TAG, "pkg=${sbn.packageName} | $full | txn=$txn")

        val result = when {
            txn == null -> "không phải giao dịch"
            shouldSpeak -> AmountParser.toSpeech(txn, bankName)
            else -> "tiền ra (đang tắt đọc tiền ra)"
        }
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        Prefs.setLast(this, "[$time] $full\n→ $result")

        if (!shouldSpeak || txn == null) return

        val key = "${sbn.packageName}|$full"
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastTime < 10_000) return
        lastKey = key
        lastTime = now

        speaker.speak(AmountParser.toSpeech(txn, bankName))
    }
            /** "com.mbmobile" → "MB Bank" (tên app hiển thị trên máy) */
        private fun getBankName(packageName: String): String = try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName   // không tìm thấy thì đọc tạm mã app
        }
    
    companion object {
        const val TAG = "BankNoti"

        /** true khi hệ thống đang thực sự gửi thông báo cho app (reset về false khi app bị tắt hẳn). */
        @Volatile
        var connected = false

        /**
         * Ép hệ thống kết nối lại việc đọc thông báo.
         * Mẹo tắt/bật lại component: cần thiết trên Xiaomi sau khi app bị "Xoá tất cả".
         */
        fun forceRebind(ctx: Context) {
            val cn = ComponentName(ctx, BankNotificationService::class.java)
            val pm = ctx.packageManager
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            NotificationListenerService.requestRebind(cn)
        }
    }
}
