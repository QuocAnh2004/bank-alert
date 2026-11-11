package com.batoluxi.bankvoice

import android.content.Context

/** Lưu cấu hình: danh sách app ngân hàng được chọn + thông báo gần nhất (để kiểm tra). */
object Prefs {
    private const val FILE = "config"
    private const val KEY_BANKS = "bank_packages"
    private const val KEY_LAST = "last_notification"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getBanks(ctx: Context): Set<String> = sp(ctx).getStringSet(KEY_BANKS, emptySet())!!.toSet()

    fun setBanks(ctx: Context, banks: Set<String>) =
        sp(ctx).edit().putStringSet(KEY_BANKS, banks).apply()

    fun getLast(ctx: Context): String = sp(ctx).getString(KEY_LAST, "") ?: ""

    fun setLast(ctx: Context, value: String) = sp(ctx).edit().putString(KEY_LAST, value).apply()
}
