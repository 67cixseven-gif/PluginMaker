package com.aliucord.plugins.pluginmaker

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Storage Manager for PluginMaker
 * 
 * Handles persistent storage of:
 *  - Scripts (scripts.json)
 *  - Crash logs (crash_logs.json)
 *  - Settings (settings.json)
 *  - Breadcrumb data (last_run.json)
 */
class StorageManager(private val pluginDir: String) {
    companion object {
        private const val TAG = "StorageManager"
        private const val SCRIPTS_FILE = "scripts.json"
        private const val CRASH_LOGS_FILE = "crash_logs.json"
        private const val SETTINGS_FILE = "settings.json"
        private const val LAST_RUN_FILE = "last_run.json"
    }

    private val gson = Gson()
    private val baseDir = File(pluginDir)

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
            Log.d(TAG, "Created plugin directory: $pluginDir")
        }
    }

    // ================== Scripts Management ==================

    /**
     * Save all scripts to scripts.json
     */
    fun saveScripts(scripts: Map<String, UserScript>) {
        try {
            val file = File(baseDir, SCRIPTS_FILE)
            file.writeText(gson.toJson(scripts))
            Log.d(TAG, "Saved ${scripts.size} scripts to $SCRIPTS_FILE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save scripts", e)
        }
    }

    /**
     * Load all scripts from scripts.json
     */
    fun loadScripts(): Map<String, UserScript> {
        return try {
            val file = File(baseDir, SCRIPTS_FILE)
            if (!file.exists()) return emptyMap()

            val type = com.google.gson.reflect.TypeToken<Map<String, UserScript>>().type
            val scripts: Map<String, UserScript> = gson.fromJson(file.readText(), type)
            Log.d(TAG, "Loaded ${scripts.size} scripts from $SCRIPTS_FILE")
            scripts
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load scripts", e)
            emptyMap()
        }
    }

    /**
     * Get a single script by ID
     */
    fun getScript(scriptId: String): UserScript? {
        return loadScripts()[scriptId]
    }

    /**
     * Delete a script by ID
     */
    fun deleteScript(scriptId: String) {
        try {
            val scripts = loadScripts().toMutableMap()
            scripts.remove(scriptId)
            saveScripts(scripts)
            Log.d(TAG, "Deleted script: $scriptId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete script: $scriptId", e)
        }
    }

    /**
     * Clear all scripts
     */
    fun clearAllScripts() {
        try {
            saveScripts(emptyMap())
            Log.d(TAG, "Cleared all scripts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear scripts", e)
        }
    }

    /**
     * Get count of scripts
     */
    fun getScriptCount(): Int = loadScripts().size

    // ================== Crash Logs Management ==================

    /**
     * Save all crash logs to crash_logs.json
     */
    fun saveCrashLogs(logs: List<CrashLog>) {
        try {
            val file = File(baseDir, CRASH_LOGS_FILE)
            file.writeText(gson.toJson(logs))
            Log.d(TAG, "Saved ${logs.size} crash logs to $CRASH_LOGS_FILE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash logs", e)
        }
    }

    /**
     * Load all crash logs from crash_logs.json
     */
    fun loadCrashLogs(): List<CrashLog> {
        return try {
            val file = File(baseDir, CRASH_LOGS_FILE)
            if (!file.exists()) return emptyList()

            val type = com.google.gson.reflect.TypeToken<List<CrashLog>>().type
            val logs: List<CrashLog> = gson.fromJson(file.readText(), type)
            Log.d(TAG, "Loaded ${logs.size} crash logs from $CRASH_LOGS_FILE")
            logs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load crash logs", e)
            emptyList()
        }
    }

    /**
     * Add a crash log entry
     */
    fun addCrashLog(crashLog: CrashLog) {
        try {
            val logs = loadCrashLogs().toMutableList()
            logs.add(crashLog)
            // Keep only last 100 logs to prevent file growth
            if (logs.size > 100) {
                logs.removeAt(0)
            }
            saveCrashLogs(logs)
            Log.d(TAG, "Added crash log for script: ${crashLog.scriptId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add crash log", e)
        }
    }

    /**
     * Clear all crash logs
     */
    fun clearCrashLogs() {
        try {
            saveCrashLogs(emptyList())
            Log.d(TAG, "Cleared all crash logs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash logs", e)
        }
    }

    /**
     * Get recent crash logs (last N entries)
     */
    fun getRecentCrashLogs(limit: Int = 10): List<CrashLog> {
        return loadCrashLogs().takeLast(limit)
    }

    // ================== Settings Management ==================

    /**
     * Save settings to settings.json
     */
    fun saveSettings(settings: PluginSettings) {
        try {
            val json = JsonObject().apply {
                addProperty("safeMode", settings.safeMode)
                addProperty("autoDisableBrokenScripts", settings.autoDisableBrokenScripts)
                addProperty("enableErrorLogging", settings.enableErrorLogging)
                addProperty("commandsEnabled", settings.commandsEnabled)
            }
            File(baseDir, SETTINGS_FILE).writeText(gson.toJson(json))
            Log.d(TAG, "Settings saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
        }
    }

    /**
     * Load settings from settings.json
     */
    fun loadSettings(): PluginSettings {
        return try {
            val file = File(baseDir, SETTINGS_FILE)
            if (!file.exists()) {
                return PluginSettings()
            }

            val json = gson.fromJson(file.readText(), JsonObject::class.java)
            PluginSettings(
                safeMode = json.get("safeMode")?.asBoolean ?: false,
                autoDisableBrokenScripts = json.get("autoDisableBrokenScripts")?.asBoolean ?: true,
                enableErrorLogging = json.get("enableErrorLogging")?.asBoolean ?: true,
                commandsEnabled = json.get("commandsEnabled")?.asBoolean ?: true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings", e)
            PluginSettings()
        }
    }

    // ================== Breadcrumb Management ==================

    /**
     * Write breadcrumb for crash detection
     */
    fun writeBreadcrumb(scriptId: String) {
        try {
            val json = JsonObject().apply {
                addProperty("scriptId", scriptId)
                addProperty("timestamp", System.currentTimeMillis())
            }
            File(baseDir, LAST_RUN_FILE).writeText(gson.toJson(json))
            Log.d(TAG, "Breadcrumb written for script: $scriptId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write breadcrumb", e)
        }
    }

    /**
     * Read breadcrumb to detect previous crash
     */
    fun readBreadcrumb(): Pair<String, Long>? {
        return try {
            val file = File(baseDir, LAST_RUN_FILE)
            if (!file.exists()) return null

            val json = gson.fromJson(file.readText(), JsonObject::class.java)
            val scriptId = json.get("scriptId")?.asString ?: return null
            val timestamp = json.get("timestamp")?.asLong ?: 0L

            Log.d(TAG, "Breadcrumb read: scriptId=$scriptId, timestamp=$timestamp")
            Pair(scriptId, timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read breadcrumb", e)
            null
        }
    }

    /**
     * Clear breadcrumb after successful execution
     */
    fun clearBreadcrumb() {
        try {
            val file = File(baseDir, LAST_RUN_FILE)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Breadcrumb cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear breadcrumb", e)
        }
    }

    // ================== Utilities ==================

    /**
     * Get plugin directory size in bytes
     */
    fun getDirectorySize(): Long {
        return baseDir.walkTopDown().sumOf { it.length() }
    }

    /**
     * Export all data as JSON
     */
    fun exportAllData(): String {
        return try {
            val json = JsonObject().apply {
                add("scripts", gson.toJsonTree(loadScripts()))
                add("crashLogs", gson.toJsonTree(loadCrashLogs()))
                add("settings", gson.toJsonTree(loadSettings()))
                addProperty("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            }
            gson.toJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export data", e)
            "{\"error\": \"Export failed\"}"
        }
    }
}

/**
 * Settings data class
 */
data class PluginSettings(
    val safeMode: Boolean = false,
    val autoDisableBrokenScripts: Boolean = true,
    val enableErrorLogging: Boolean = true,
    val commandsEnabled: Boolean = true
)
