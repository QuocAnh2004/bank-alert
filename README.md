# bank-voice-alert 🔊

Android app chạy ngầm. Mỗi khi có thông báo **tiền vào** từ app ngân hàng, app đọc to số tiền bằng tiếng Việt, giống loa báo chuyển khoản ở quầy thu ngân.

**Stack:** Kotlin · `NotificationListenerService` · `TextToSpeech`

```
Thông báo ngân hàng → BankNotificationService → AmountParser (regex) → Speaker (TTS) → 🔊
```

## Build ra file APK
Android Studio → File → Open thư mục này → chờ sync → **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
File nằm ở `app/build/outputs/apk/debug/app-debug.apk`.

## Dùng trên điện thoại
1. Cài APK (cho phép "Cài ứng dụng không rõ nguồn gốc").
2. Mở app, bấm lần lượt nút **1 → 2 → 3**:
   - 1: bật quyền đọc thông báo cho "Loa báo tiền"
   - 2: tắt tối ưu pin
   - 3: bật **Tự khởi động**, Tiết kiệm pin → **Không hạn chế**
3. Tick app ngân hàng / ví trong danh sách (app giống ngân hàng được xếp lên đầu).
4. Bấm 🔊 **Thử giọng đọc**. Không có tiếng → cài *Speech Services by Google* + tải gói tiếng Việt.
5. Mở đa nhiệm, kéo app xuống để **khoá** 🔒.
6. Chuyển thử 1.000đ. Ô "Thông báo gần nhất" cho biết app đã đọc được gì.

## Cách tách số tiền
- Có `+500,000` / `+500.000` / `+500000 VND` → tiền vào
- Có `-` trước số tiền → tiền ra, bỏ qua
- Có từ khoá "nhận / được cộng / ghi có..." + số tiền kèm `VND/đ` → tiền vào
