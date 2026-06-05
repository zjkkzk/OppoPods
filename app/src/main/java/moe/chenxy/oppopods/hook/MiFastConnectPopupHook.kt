package moe.chenxy.oppopods.hook

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.Locale

@SuppressLint("MissingPermission")
object MiFastConnectPopupHook : HookContext() {
    private const val TAG = "OppoPods-FastPopup"
    private const val OPPO_COMPANY_ID = 0x079A
    private const val POPUP_COOLDOWN_MS = 30_000L
    private const val DEFAULT_DEVICE_NAME = "OPPO Enco"
    private const val FAKE_FAST_CONNECT_DEVICE_ID = "0201010000"

    private val lastPopupTimeByAddress = linkedMapOf<String, Long>()

    override fun onHook() {
        runCatching {
            hookBefore(
                findMethod(
                    "com.android.bluetooth.ble.app.MiuiFastConnectService",
                    "handleScanCallBack",
                    Int::class.javaPrimitiveType!!,
                    ScanResult::class.java
                )
            ) {
                val context = instance as? Context ?: return@hookBefore
                val scanResult = args.getOrNull(1) as? ScanResult ?: return@hookBefore
                if (!isOppoBle(scanResult)) return@hookBefore
                startFastConnectPopup(context, scanResult)
            }
            Log.d(TAG, "MiuiFastConnectService.handleScanCallBack hook installed")
        }.onFailure {
            Log.w(TAG, "hook handleScanCallBack failed", it)
        }
    }

    private fun isOppoBle(scanResult: ScanResult): Boolean {
        val data = scanResult.scanRecord?.getManufacturerSpecificData(OPPO_COMPANY_ID) ?: return false
        val matched = data.size >= 8 &&
            data[0] == 0x10.toByte() &&
            data[1] == 0x74.toByte() &&
            data[2] == 0x06.toByte()
        if (matched) {
            Log.d(
                TAG,
                "OPPO BLE found address=${scanResult.device?.address} rssi=${scanResult.rssi} mfg=${data.toHexString()}"
            )
        }
        return matched
    }

    private fun startFastConnectPopup(context: Context, scanResult: ScanResult) {
        val device = scanResult.device ?: return
        val address = device.address ?: return
        val now = SystemClock.elapsedRealtime()
        val lastPopupTime = lastPopupTimeByAddress[address] ?: 0L
        if (now - lastPopupTime < POPUP_COOLDOWN_MS) return
        lastPopupTimeByAddress[address] = now

        val deviceName = scanResult.displayName(device)
        val intent = Intent().apply {
            setClassName("com.xiaomi.bluetooth", "com.android.bluetooth.ble.app.MiuiFastConnectActivity")
            action = "com.android.bluetooth.FAST_CONNECT_DEVICE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("android.bluetooth.device.extra.DEVICE", device)
            putExtra("headset_addresses", arrayOf(address, "00:00:00:00:00:00", deviceName))
            putExtra("headset_miui_data", fakeFastConnectAdvData())
            putExtra("headset_adv_row_bytes", scanResult.scanRecord?.bytes ?: ByteArray(0))
            putExtra("headset_extra_data", intArrayOf(scanResult.rssi, 0, 0, 2, 0))
            putExtra("current_a2dp_devices", 0)
            putExtra(FAKE_FAST_CONNECT_DEVICE_ID, false)
            putExtra("oppopods_no_battery", true)
        }

        runCatching {
            context.startActivity(intent)
            Log.i(TAG, "started FastConnect popup address=$address name=$deviceName")
        }.onFailure {
            Log.w(TAG, "start FastConnect popup failed address=$address name=$deviceName", it)
        }
    }

    private fun ScanResult.displayName(device: BluetoothDevice): String {
        return runCatching { device.alias }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: scanRecord?.deviceName?.takeIf { it.isNotBlank() }
            ?: DEFAULT_DEVICE_NAME
    }

    private fun fakeFastConnectAdvData(): ByteArray {
        return ByteArray(24).apply {
            this[0] = 19
            // C6206j2 parses b[1], b[2], b[3], b[4] as deviceId 0201010000.
            this[1] = 0x01
            this[2] = 0x01
            this[3] = 0x00
            this[4] = 0x00
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { String.format(Locale.US, "%02X", it) }
    }
}
