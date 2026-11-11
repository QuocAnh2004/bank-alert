package com.batoluxi.bankvoice

/** Một giao dịch: số tiền (đồng) + tiền vào hay tiền ra. */
data class Txn(val amount: Long, val incoming: Boolean)

/**
 * Tách số tiền từ nội dung thông báo ngân hàng.
 *  - "+500,000" / "nhận 200.000đ"   → tiền VÀO
 *  - "-500,000" / "bị trừ 200.000đ" → tiền RA
 */
object AmountParser {

    private const val GROUPED = """\d{1,3}(?:[.,]\d{3})+"""          // 500,000 | 1.250.000
    private const val CURRENCY = """(?:VND|VNĐ|đ|đồng|dong)"""

    // "+500,000" hoặc "+500000 VND" (không bắt nhầm số điện thoại +84...)
    private val PLUS_AMOUNT = Regex(
        """\+\s*($GROUPED)|\+\s*(\d+)\s*$CURRENCY""", RegexOption.IGNORE_CASE
    )
    private val MINUS_AMOUNT = Regex(
        """-\s*($GROUPED)|-\s*(\d+)\s*$CURRENCY""", RegexOption.IGNORE_CASE
    )
    private val AMOUNT_WITH_CURRENCY = Regex(
        """($GROUPED|\d+)\s*$CURRENCY""", RegexOption.IGNORE_CASE
    )
    private val INCOMING_WORDS = listOf(
        "nhận", "nhan duoc", "được cộng", "duoc cong", "tiền vào", "tien vao",
        "ghi có", "ghi co", "credit", "received"
    )
    private val OUTGOING_WORDS = listOf(
        "bị trừ", "bi tru", "trừ tiền", "tru tien", "ghi nợ", "ghi no", "debit",
        "chuyển đi", "chuyen di", "tiền ra", "tien ra"
    )

    fun parseTxn(text: String): Txn? {
        // 1. Dấu +/- trước số tiền là chắc chắn nhất
        PLUS_AMOUNT.find(text)?.let { m -> return toLong(m)?.let { Txn(it, true) } }
        MINUS_AMOUNT.find(text)?.let { m -> return toLong(m)?.let { Txn(it, false) } }

        // 2. Không có dấu → dựa vào từ khoá + số tiền kèm đơn vị
        val lower = text.lowercase()
        val amount = AMOUNT_WITH_CURRENCY.find(text)?.let { toLong(it.groupValues[1]) } ?: return null
        return when {
            INCOMING_WORDS.any { it in lower } -> Txn(amount, true)
            OUTGOING_WORDS.any { it in lower } -> Txn(amount, false)
            else -> null
        }
    }

    /** Chỉ lấy tiền vào (giữ lại cho màn hình test và unit test). */
    fun parse(text: String): Long? = parseTxn(text)?.takeIf { it.incoming }?.amount

    private fun toLong(m: MatchResult): Long? = toLong(m.groupValues[1].ifEmpty { m.groupValues[2] })

    private fun toLong(raw: String): Long? =
        raw.replace(",", "").replace(".", "").toLongOrNull()?.takeIf { it > 0 }

    fun toSpeech(txn: Txn): String =
        if (txn.incoming) toSpeech(txn.amount) else "Đã chuyển đi ${toVietnamese(txn.amount)} đồng"

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
