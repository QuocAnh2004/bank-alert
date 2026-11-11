package com.batoluxi.bankvoice

import android.content.Context

/** Lưu cấu hình + trạng thái để hiển thị trên màn hình. */
object Prefs {
    private const val FILE = "config"
    private const val KEY_BANKS = "bank_packages"
    private const val KEY_LAST = "last_notification"
    private const val KEY_TTS = "tts_status"
    private const val KEY_OUTGOING = "read_outgoing"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getBanks(ctx: Context): Set<String> = sp(ctx).getStringSet(KEY_BANKS, emptySet())!!.toSet()

    fun setBanks(ctx: Context, banks: Set<String>) =
        sp(ctx).edit().putStringSet(KEY_BANKS, banks).apply()

    fun getLast(ctx: Context): String = sp(ctx).getString(KEY_LAST, "") ?: ""

    fun setLast(ctx: Context, value: String) = sp(ctx).edit().putString(KEY_LAST, value).apply()

    fun getReadOutgoing(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_OUTGOING, false)

    fun setReadOutgoing(ctx: Context, value: Boolean) =
        sp(ctx).edit().putBoolean(KEY_OUTGOING, value).apply()

    fun getTts(ctx: Context): String = sp(ctx).getString(KEY_TTS, "") ?: ""

    fun setTts(ctx: Context, value: String) = sp(ctx).edit().putString(KEY_TTS, value).apply()
}
