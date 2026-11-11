package com.batoluxi.bankvoice

/**
 * Tách số tiền NHẬN VÀO từ nội dung thông báo ngân hàng.
 * Trả về null nếu không phải giao dịch tiền vào (tiền ra, OTP, quảng cáo...).
 */
object AmountParser {

    private const val GROUPED = """\d{1,3}(?:[.,]\d{3})+"""          // 500,000 | 1.250.000
    private const val CURRENCY = """(?:VND|VNĐ|đ|đồng|dong)"""

    // "+500,000" hoặc "+500000 VND" (không bắt nhầm số điện thoại +84...)
    private val PLUS_AMOUNT = Regex(
        """\+\s*($GROUPED)|\+\s*(\d+)\s*$CURRENCY""", RegexOption.IGNORE_CASE
    )
    private val MINUS_AMOUNT = Regex("""-\s*(?:$GROUPED|\d+\s*$CURRENCY)""", RegexOption.IGNORE_CASE)
    private val AMOUNT_WITH_CURRENCY = Regex(
        """($GROUPED|\d+)\s*$CURRENCY""", RegexOption.IGNORE_CASE
    )
    private val INCOMING_WORDS = listOf(
        "nhận", "nhan duoc", "được cộng", "duoc cong", "tiền vào", "tien vao",
        "ghi có", "ghi co", "credit", "received", "biến động số dư +"
    )

    fun parse(text: String): Long? {
        // Cách 1: có dấu "+" trước số tiền → chắc chắn tiền vào
        PLUS_AMOUNT.find(text)?.let { m ->
            val raw = m.groupValues[1].ifEmpty { m.groupValues[2] }
            return toLong(raw)
        }
        // Có dấu "-" trước số tiền → tiền ra, bỏ qua
        if (MINUS_AMOUNT.containsMatchIn(text)) return null

        // Cách 2: có từ khoá "nhận / được cộng / ghi có..." + số tiền kèm đơn vị
        val lower = text.lowercase()
        if (INCOMING_WORDS.any { it in lower }) {
            AMOUNT_WITH_CURRENCY.find(text)?.let { return toLong(it.groupValues[1]) }
        }
        return null
    }

    private fun toLong(raw: String): Long? =
        raw.replace(",", "").replace(".", "").toLongOrNull()?.takeIf { it > 0 }

    /** 1250000 → "Đã nhận 1 triệu 250 nghìn đồng" */
    fun toSpeech(amount: Long): String = "Đã nhận ${toVietnamese(amount)} đồng"

    fun toVietnamese(amount: Long): String {
        val ty = amount / 1_000_000_000
        val trieu = (amount / 1_000_000) % 1000
        val nghin = (amount / 1000) % 1000
        val dong = amount % 1000
        val parts = mutableListOf<String>()
        if (ty > 0) parts += "$ty tỷ"
        if (trieu > 0) parts += "$trieu triệu"
        if (nghin > 0) parts += "$nghin nghìn"
        if (dong > 0) parts += "$dong"
        return if (parts.isEmpty()) "0" else parts.joinToString(" ")
    }
}
