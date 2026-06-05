package moe.chenxy.oppopods.config

import android.content.SharedPreferences
import android.util.Log
import io.github.libxposed.service.XposedService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppConfig(
    val fakeDeviceId: String = ConfigManager.DEFAULT_FAKE_DEVICE_ID,
    val logLevel: Int = ConfigManager.LOG_LEVEL_BASIC
)

object ConfigManager {
    private const val TAG = "OppoPods-Config"
    const val PREFS_NAME = "oppopods_settings"
    const val PREF_KEY_CONFIG_JSON = "config_json"
    const val PREF_KEY_FAKE_DEVICE_ID = "fake_device_id"
    const val PREF_KEY_LOG_LEVEL = "log_level"
    const val DEFAULT_FAKE_DEVICE_ID = "01010607"
    const val LOG_LEVEL_OFF = 0
    const val LOG_LEVEL_BASIC = 1
    const val LOG_LEVEL_DEBUG = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var cachedConfig: AppConfig = AppConfig()

    fun init(prefs: SharedPreferences) {
        val oldConfig = cachedConfig
        cachedConfig = readConfig(prefs, "init")
        logConfigChange("init", oldConfig, cachedConfig)
    }

    fun refreshFromPrefs(prefs: SharedPreferences): AppConfig {
        val oldConfig = cachedConfig
        return readConfig(prefs, "refreshFromPrefs").also {
            cachedConfig = it
            logConfigChange("refreshFromPrefs", oldConfig, it)
        }
    }

    fun current(): AppConfig = cachedConfig

    fun fakeDeviceId(): String = current().fakeDeviceId.normalizedFakeDeviceId()

    fun logLevel(): Int = current().logLevel.coerceIn(LOG_LEVEL_OFF, LOG_LEVEL_DEBUG)

    fun fakeSupport(): String = "${fakeDeviceId()},000000000000000010000000"

    fun updateFakeDeviceId(prefs: SharedPreferences, fakeDeviceId: String) {
        val config = current().copy(fakeDeviceId = fakeDeviceId.normalizedFakeDeviceId())
        save(prefs, config)
    }

    fun updateFakeDeviceId(prefs: SharedPreferences, service: XposedService?, fakeDeviceId: String) {
        val config = current().copy(fakeDeviceId = fakeDeviceId.normalizedFakeDeviceId())
        save(prefs, service, config)
    }

    fun updateLogLevel(prefs: SharedPreferences, service: XposedService?, logLevel: Int) {
        val config = current().copy(logLevel = logLevel.coerceIn(LOG_LEVEL_OFF, LOG_LEVEL_DEBUG))
        save(prefs, service, config)
    }

    fun save(prefs: SharedPreferences, config: AppConfig) {
        val oldConfig = cachedConfig
        val normalized = config.copy(fakeDeviceId = config.fakeDeviceId.normalizedFakeDeviceId())
        cachedConfig = normalized
        writePrefs(prefs, normalized)
        logConfigChange("save", oldConfig, normalized)
    }

    fun save(prefs: SharedPreferences, service: XposedService?, config: AppConfig) {
        val oldConfig = cachedConfig
        val normalized = config.copy(fakeDeviceId = config.fakeDeviceId.normalizedFakeDeviceId())
        cachedConfig = normalized
        writePrefs(prefs, normalized)
        service?.getRemotePreferences(PREFS_NAME)?.let { remotePrefs ->
            writePrefs(remotePrefs, normalized)
            Log.d(TAG, "save remote prefs class=${remotePrefs.javaClass.name} fakeDeviceId=${normalized.fakeDeviceId}")
        } ?: Log.w(TAG, "save remote prefs skipped: LSPosed service is null")
        logConfigChange("save", oldConfig, normalized)
    }

    private fun writePrefs(prefs: SharedPreferences, config: AppConfig) {
        prefs.edit()
            .putString(PREF_KEY_CONFIG_JSON, json.encodeToString(AppConfig.serializer(), config))
            .putString(PREF_KEY_FAKE_DEVICE_ID, config.fakeDeviceId)
            .putInt(PREF_KEY_LOG_LEVEL, config.logLevel)
            .commit()
    }

    private fun readConfig(prefs: SharedPreferences, source: String): AppConfig {
        val directFakeDeviceId = prefs.getString(PREF_KEY_FAKE_DEVICE_ID, null)
        val directLogLevel = prefs.getInt(PREF_KEY_LOG_LEVEL, Int.MIN_VALUE)
        val raw = prefs.getString(PREF_KEY_CONFIG_JSON, null)
        logPrefsSnapshot(source, prefs, directFakeDeviceId, raw)
        val config = raw?.let {
            runCatching { json.decodeFromString(AppConfig.serializer(), it) }.getOrNull()
        } ?: AppConfig()
        if (!directFakeDeviceId.isNullOrBlank()) {
            return config.copy(
                fakeDeviceId = directFakeDeviceId.normalizedFakeDeviceId(),
                logLevel = directLogLevel.takeIf { it != Int.MIN_VALUE } ?: config.logLevel,
            ).normalized()
        }
        return config.copy(
            fakeDeviceId = config.fakeDeviceId.normalizedFakeDeviceId(),
            logLevel = directLogLevel.takeIf { it != Int.MIN_VALUE } ?: config.logLevel,
        ).normalized()
    }

    private fun AppConfig.normalized(): AppConfig = copy(
        fakeDeviceId = fakeDeviceId.normalizedFakeDeviceId(),
        logLevel = logLevel.coerceIn(LOG_LEVEL_OFF, LOG_LEVEL_DEBUG),
    )

    private fun String.normalizedFakeDeviceId(): String = trim().takeIf { it.isNotEmpty() } ?: DEFAULT_FAKE_DEVICE_ID

    private fun logConfigChange(source: String, oldConfig: AppConfig, newConfig: AppConfig) {
        val changes = changedFields(oldConfig, newConfig)
        if (changes.isEmpty()) {
            Log.d(TAG, "$source config unchanged: $newConfig")
        } else {
            Log.d(TAG, "$source config changed: ${changes.joinToString()}")
        }
    }

    private fun logPrefsSnapshot(source: String, prefs: SharedPreferences, directFakeDeviceId: String?, rawConfig: String?) {
        val all = runCatching { prefs.all }.getOrElse { error -> mapOf("<getAllError>" to error.message) }
        Log.d(
            TAG,
            "$source prefs snapshot class=${prefs.javaClass.name} keys=${all.keys.sorted()} " +
                "$PREF_KEY_FAKE_DEVICE_ID=$directFakeDeviceId $PREF_KEY_CONFIG_JSON=$rawConfig all=$all"
        )
    }

    private fun changedFields(oldConfig: AppConfig, newConfig: AppConfig): List<String> {
        return buildList {
            if (oldConfig.fakeDeviceId != newConfig.fakeDeviceId) {
                add("fakeDeviceId=${oldConfig.fakeDeviceId}->${newConfig.fakeDeviceId}")
            }
            if (oldConfig.logLevel != newConfig.logLevel) {
                add("logLevel=${oldConfig.logLevel}->${newConfig.logLevel}")
            }
        }
    }
}
