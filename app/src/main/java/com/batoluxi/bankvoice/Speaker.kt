package com.batoluxi.bankvoice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Bọc TextToSpeech cho dễ dùng: gọi speak("...") là đọc.
 * TTS khởi tạo bất đồng bộ, nên câu nào gọi trước khi sẵn sàng sẽ được xếp hàng chờ.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pending = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Không khởi tạo được TTS, status=$status")
            return
        }
        val result = tts.setLanguage(Locale("vi", "VN"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Máy chưa có giọng tiếng Việt. Cài 'Speech Services by Google' và tải gói vi-VN.")
        }
        ready = true
        pending.forEach { speakNow(it) }
        pending.clear()
    }

    fun speak(text: String) {
        if (ready) speakNow(text) else pending.add(text)
    }

    private fun speakNow(text: String) {
        // QUEUE_ADD: 2 giao dịch đến liền nhau thì đọc lần lượt, không đè nhau
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "Speaker"
    }
}
