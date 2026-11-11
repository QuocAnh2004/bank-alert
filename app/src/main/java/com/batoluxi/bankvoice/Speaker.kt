package com.batoluxi.bankvoice

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bọc TextToSpeech:
 * - Đọc qua luồng âm thanh BÁO THỨC → vẫn kêu khi đang chạy nền / Media = 0 / chế độ im lặng.
 * - Engine bị hệ thống tắt ngầm → tự khởi động lại và đọc lại câu đó.
 * - Báo trạng thái (đã đọc / lỗi) qua onStatus để hiện lên màn hình.
 */
class Speaker(
    context: Context,
    private val onStatus: (String) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    init {
        create()
    }

    private fun create() {
        ready = false
        tts = TextToSpeech(appContext) { status -> main.post { onInit(status) } }
    }

    private fun onInit(status: Int) {
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            onStatus("❌ Không khởi tạo được giọng đọc (status=$status)")
            tts = null
            return
        }
        t.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        if (t.setLanguage(Locale("vi", "VN")) < 0) {
            onStatus("⚠️ Máy chưa có giọng tiếng Việt")
        }
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onStatus("🗣️ Đang đọc… (${now()})")
            override fun onDone(utteranceId: String?) = onStatus("✅ Đã đọc xong lúc ${now()}")
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onStatus("❌ Lỗi khi đọc lúc ${now()}")
            override fun onError(utteranceId: String?, errorCode: Int) =
                onStatus("❌ Lỗi khi đọc (code=$errorCode) lúc ${now()}")
        })
        ready = true
        val queued = pending.toList()
        pending.clear()
        queued.forEach { speak(it) }
    }

    fun speak(text: String) {
        main.post { speakOnMain(text) }
    }

    private fun speakOnMain(text: String) {
        val t = tts
        if (t == null) {           // lần trước khởi tạo hỏng → tạo lại
            pending.add(text)
            create()
            return
        }
        if (!ready) {              // đang khởi tạo → xếp hàng chờ
            pending.add(text)
            return
        }
        val result = t.speak(text, TextToSpeech.QUEUE_ADD, null, "u${System.nanoTime()}")
        if (result == TextToSpeech.ERROR) {
            // Engine bị hệ thống tắt ngầm → khởi động lại rồi đọc lại
            onStatus("⚠️ Giọng đọc bị ngắt, đang khởi động lại… (${now()})")
            pending.add(text)
            t.shutdown()
            create()
        }
    }

    fun shutdown() {
        main.post {
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    private fun now(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
