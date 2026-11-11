package com.batoluxi.bankvoice

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {

    private lateinit var speaker: Speaker
    private lateinit var txtStatus: TextView
    private lateinit var txtLast: TextView

    private data class AppItem(val pkg: String, val label: String) {
        override fun toString() = label
    }

    private val bankHints = listOf(
        "bank", "ngân hàng", "vcb", "vietcom", "bidv", "vietin", "agri", "techcom", "mb",
        "acb", "vpb", "tpb", "sacom", "hdb", "vib", "shb", "ocb", "msb", "seab", "lpb",
        "eximb", "nam a", "bac a", "cake", "timo", "momo", "zalopay", "viettel money"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        speaker = Speaker(this) { status ->
            Prefs.setTts(this, status)
            runOnUiThread { refreshStatus() }
        }
        txtStatus = findViewById(R.id.txtStatus)
        txtLast = findViewById(R.id.txtLast)

        findViewById<Button>(R.id.btnPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestNoBatteryLimit() }
        findViewById<Button>(R.id.btnAppSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Bật 'Tự khởi động' và Tiết kiệm pin → 'Không hạn chế'", Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.btnTestVoice).setOnClickListener {
            speaker.speak(AmountParser.toSpeech(1_250_000))
        }

        val swOutgoing = findViewById<Switch>(R.id.swOutgoing)
        swOutgoing.isChecked = Prefs.getReadOutgoing(this)
        swOutgoing.setOnCheckedChangeListener { _, checked -> Prefs.setReadOutgoing(this, checked) }

        setupBankList()
    }

    private fun setupBankList() {
        val listView = findViewById<ListView>(R.id.listApps)
        val apps = loadLaunchableApps()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, apps)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val selected = Prefs.getBanks(this)
        apps.forEachIndexed { i, app -> listView.setItemChecked(i, app.pkg in selected) }

        listView.setOnItemClickListener { _, _, _, _ ->
            val checked = apps.filterIndexed { i, _ -> listView.isItemChecked(i) }.map { it.pkg }.toSet()
            Prefs.setBanks(this, checked)
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun loadLaunchableApps(): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val items = packageManager.queryIntentActivities(intent, 0)
            .map { AppItem(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.pkg != packageName }
            .distinctBy { it.pkg }
        // App có tên giống ngân hàng / ví điện tử lên đầu
        fun looksLikeBank(a: AppItem): Boolean {
            val s = (a.label + " " + a.pkg).lowercase()
            return bankHints.any { it in s }
        }
        return items.sortedWith(compareBy<AppItem>({ !looksLikeBank(it) }, { it.label.lowercase() }))
    }

    @SuppressLint("BatteryLife")
    private fun requestNoBatteryLimit() {
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "Đã tắt tối ưu pin cho app ✅", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
    }

    override fun onResume() {
        super.onResume()
        // Mở app mà mất kết nối (VD: vừa bị "Xoá tất cả") → ép kết nối lại
        if (isGranted() && !BankNotificationService.connected) {
            BankNotificationService.forceRebind(this)
            txtStatus.postDelayed({ refreshStatus() }, 2000)
        }
        refreshStatus()
    }

    private fun isGranted() =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun refreshStatus() {
        val banks = Prefs.getBanks(this).size
        txtStatus.text = when {
            !isGranted() -> "⚠️ Chưa cấp quyền đọc thông báo (bấm nút 1)"
            banks == 0 -> "⚠️ Chưa chọn app ngân hàng (tick ở danh sách bên dưới)"
            !BankNotificationService.connected -> "🔄 Đang kết nối lại… (chờ vài giây)"
            else -> "✅ Đang lắng nghe $banks app"
        }
        val last = Prefs.getLast(this)
        val tts = Prefs.getTts(this).ifEmpty { "(chưa đọc lần nào)" }
        txtLast.text = (if (last.isEmpty()) "Thông báo gần nhất: (chưa có)" else "Thông báo gần nhất:\n$last") +
            "\n\nGiọng đọc: $tts"
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
