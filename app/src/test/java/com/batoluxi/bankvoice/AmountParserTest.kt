package com.batoluxi.bankvoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Câu mẫu là giả định; thêm thông báo thật của ngân hàng bạn vào đây. */
class AmountParserTest {

    @Test fun plus_dauPhay() =
        assertEquals(500_000L, AmountParser.parse("TK 1234 +500,000VND luc 19:45 ND: chuyen tien"))

    @Test fun plus_dauCham() =
        assertEquals(1_250_000L, AmountParser.parse("So du TK 1234 +1.250.000 VND"))

    @Test fun tuKhoaNhan() =
        assertEquals(200_000L, AmountParser.parse("Bạn vừa nhận 200.000đ từ NGUYEN VAN A"))

    @Test fun tienRa() = assertNull(AmountParser.parse("TK 1234 -200,000VND luc 08:00"))

    @Test fun otp() = assertNull(AmountParser.parse("Ma OTP cua quy khach la 123456"))

    @Test fun soDienThoai() = assertNull(AmountParser.parse("Goi +84912345678 de duoc ho tro"))

    @Test fun docTien() =
        assertEquals("1 triệu 250 nghìn", AmountParser.toVietnamese(1_250_000))
}
