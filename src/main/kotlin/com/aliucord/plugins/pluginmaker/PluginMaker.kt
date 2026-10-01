package com.aliucord.plugins.pluginmaker

import android.content.Context
import android.util.Log
import com.aliucord.Http
import com.aliucord.PluginManager
import com.aliucord.entities.Plugin
import com.aliucord.patcher.PinePatch
import com.aliucord.wrappers.CommandsWrapper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * PluginMaker - Aliucord JavaScript UserScript Manager
 * 
 * A comprehensive plugin that allows users to:
 * - Write and execute custom JavaScript snippets
 * - Manage scripts with crash protection and Safe Mode
 * - View diagnostic logs and error tracking
 * - Control execution via a unified settings dashboard
 */
class PluginMaker : Plugin() {
    companion object {
        private const val TAG = "PluginMaker"
        private const val PLUGIN_DIR = "/sdcard/Aliucord/plugins/PluginMaker"
        private const val SCRIPTS_FILE = "$PLUGIN_DIR/scripts.json"
        private const val CRASH_LOGS_FILE = "$PLUGIN_DIR/crash_logs.json"
        private const val LAST_RUN_FILE = "$PLUGIN_DIR/last_run.json"
        private const val SETTINGS_FILE = "$PLUGIN_DIR/settings.json"
    }

    // Settings state
    private var safeMode = false
    private var autoDisableBrokenScripts = true
    private var enableErrorLogging = true
    private var commandsEnabled = true

    // Data storage
    private val scripts = mutableMapOf<String, UserScript>()
    private val crashLogs = mutableListOf<CrashLog>()
    private val gson = Gson()

    // Settings tab
    override fun settingsTab = SettingsTab(PluginSettings::class.java, SettingsTab.Type.PAGE)

    override fun start(context: Context) {
        Log.d(TAG, "Starting PluginMaker...")
        initializeDirectories()
        loadSettings()
        loadScripts()
        checkBreadcrumb(context)
        registerCommands()
        loadScriptsOnStartup()
        Log.d(TAG, "PluginMaker started successfully")
    }

    override fun stop(context: Context) {
        Log.d(TAG, "Stopping PluginMaker...")
        unregisterCommands()
        saveSettings()
        saveScripts()
        Log.d(TAG, "PluginMaker stopped")
    }

    /**
     * Initialize plugin directories
     */
    private fun initializeDirectories() {
        try {
            val pluginDir = File(PLUGIN_DIR)
            if (!pluginDir.exists()) {
                pluginDir.mkdirs()
                Log.d(TAG, "Created plugin directory: $PLUGIN_DIR")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize directories", e)
        }
    }

    /**
     * Load settings from settings.json
     */
    private fun loadSettings() {
        try {
            val settingsFile = File(SETTINGS_FILE)
            if (settingsFile.exists()) {
                val settingsJson = gson.fromJson(
                    settingsFile.readText(),
                    JsonObject::class.java
                )
                safeMode = settingsJson.get("safeMode")?.asBoolean ?: false
                autoDisableBrokenScripts = settingsJson.get("autoDisableBrokenScripts")?.asBoolean ?: true
                enableErrorLogging = settingsJson.get("enableErrorLogging")?.asBoolean ?: true
                commandsEnabled = settingsJson.get("commandsEnabled")?.asBoolean ?: true
                Log.d(TAG, "Settings loaded: safeMode=$safeMode, autoDisable=$autoDisableBrokenScripts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings", e)
        }
    }

    /**
     * Save settings to settings.json
     */
    private fun saveSettings() {
        try {
            val settingsJson = JsonObject().apply {
                addProperty("safeMode", safeMode)
                addProperty("autoDisableBrokenScripts", autoDisableBrokenScripts)
                addProperty("enableErrorLogging", enableErrorLogging)
                addProperty("commandsEnabled", commandsEnabled)
            }
            File(SETTINGS_FILE).writeText(gson.toJson(settingsJson))
            Log.d(TAG, "Settings saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
        }
    }

    /**
     * Load scripts from scripts.json
     */
    private fun loadScripts() {
        try {
            val scriptsFile = File(SCRIPTS_FILE)
            if (scriptsFile.exists()) {
                val type = object : TypeToken<Map<String, UserScript>>() {}.type
                val loadedScripts: Map<String, UserScript> = gson.fromJson(
                    scriptsFile.readText(),
                    type
                )
                scripts.clear()
                scripts.putAll(loadedScripts)
                Log.d(TAG, "Loaded ${scripts.size} scripts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load scripts", e)
        }
    }

    /**
     * Save scripts to scripts.json
     */
    private fun saveScripts() {
        try {
            File(SCRIPTS_FILE).writeText(gson.toJson(scripts))
            Log.d(TAG, "Scripts saved (${scripts.size} total)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save scripts", e)
        }
    }

    /**
     * Load crash logs from crash_logs.json
     */
    private fun loadCrashLogs() {
        try {
            val logsFile = File(CRASH_LOGS_FILE)
            if (logsFile.exists()) {
                val type = object : TypeToken<List<CrashLog>>() {}.type
                val loadedLogs: List<CrashLog> = gson.fromJson(
                    logsFile.readText(),
                    type
                )
                crashLogs.clear()
                crashLogs.addAll(loadedLogs)
                Log.d(TAG, "Loaded ${crashLogs.size} crash logs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load crash logs", e)
        }
    }

    /**
     * Save crash logs to crash_logs.json
     */
    private fun saveCrashLogs() {
        try {
            File(CRASH_LOGS_FILE).writeText(gson.toJson(crashLogs))
            Log.d(TAG, "Crash logs saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash logs", e)
        }
    }

    /**
     * Check breadcrumb for previous crashes
     */
    private fun checkBreadcrumb(context: Context) {
        try {
            val lastRunFile = File(LAST_RUN_FILE)
            if (lastRunFile.exists()) {
                // A crash was detected on the last run
                val lastRun = gson.fromJson(
                    lastRunFile.readText(),
                    JsonObject::class.java
                )
                val scriptId = lastRun.get("scriptId")?.asString ?: return

                Log.e(TAG, "Crash detected for script: $scriptId")

                if (autoDisableBrokenScripts && scripts.containsKey(scriptId)) {
                    // Automatically disable the crashing script
                    scripts[scriptId]?.enabled = false
                    saveScripts()
                    logCrash(
                        scriptId,
                        "Auto-disabled due to previous crash",
                        "Automatic crash recovery activated"
                    )
                    Log.d(TAG, "Auto-disabled script: $scriptId")
                } else {
                    // Enable Safe Mode globally
                    safeMode = true
                    saveSettings()
                    logCrash(
                        scriptId,
                        "Safe Mode activated",
                        "Safe Mode enabled due to crash"
                    )
                    Log.d(TAG, "Safe Mode activated")
                }

                lastRunFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check breadcrumb", e)
        }
    }

    /**
     * Log a crash event
     */
    private fun logCrash(scriptId: String, errorMessage: String, stackTrace: String) {
        if (!enableErrorLogging) return

        try {
            loadCrashLogs() // Reload to get latest
            val crashLog = CrashLog(
                scriptId = scriptId,
                scriptName = scripts[scriptId]?.name ?: "Unknown",
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                timestamp = System.currentTimeMillis(),
                timestampFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(Date())
            )
            crashLogs.add(crashLog)
            saveCrashLogs()
            Log.d(TAG, "Crash logged for script: $scriptId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash", e)
        }
    }

    /**
     * Write breadcrumb before executing script
     */
    private fun writeBreadcrumb(scriptId: String) {
        try {
            val breadcrumb = JsonObject().apply {
                addProperty("scriptId", scriptId)
                addProperty("timestamp", System.currentTimeMillis())
            }
            File(LAST_RUN_FILE).writeText(gson.toJson(breadcrumb))
            Log.d(TAG, "Breadcrumb written for script: $scriptId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write breadcrumb", e)
        }
    }

    /**
     * Clear breadcrumb after successful execution
     */
    private fun clearBreadcrumb() {
        try {
            File(LAST_RUN_FILE).delete()
            Log.d(TAG, "Breadcrumb cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear breadcrumb", e)
        }
    }

    /**
     * Execute a custom script safely
     */
    private fun executeScript(scriptId: String): Boolean {
        val script = scripts[scriptId] ?: return false

        if (!script.enabled || safeMode) {
            Log.d(TAG, "Script $scriptId is disabled or Safe Mode is active")
            return false
        }

        return try {
            writeBreadcrumb(scriptId)
            // TODO: Implement JavaScript engine execution (Rhino, GraalVM, etc.)
            // This is a placeholder - in production, integrate JS engine
            Log.d(TAG, "Executing script: ${script.name}")
            // Simulate execution
            Thread.sleep(100) // Simulated work
            clearBreadcrumb()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Script execution failed: $scriptId", e)
            if (enableErrorLogging) {
                logCrash(scriptId, e.message ?: "Unknown error", e.stackTraceToString())
            }
            false
        }
    }

    /**
     * Load and execute all enabled scripts on startup
     */
    private fun loadScriptsOnStartup() {
        if (safeMode) {
            Log.d(TAG, "Skipping script loading - Safe Mode is active")
            return
        }

        for ((scriptId, script) in scripts) {
            if (script.enabled) {
                executeScript(scriptId)
            }
        }
    }

    /**
     * Register slash commands
     */
    private fun registerCommands() {
        if (!commandsEnabled) return

        // /makeplugin command
        CommandsWrapper.addCommand(
            "makeplugin",
            "Create a new custom plugin",
            listOf("name", "description", "code")
        ) { args ->
            handleMakePluginCommand(args)
        }

        // /cplugin command
        CommandsWrapper.addCommand(
            "cplugin",
            "View and manage custom plugins",
            emptyList()
        ) { _ ->
            handleCPluginCommand()
        }

        // /smcplugin command
        CommandsWrapper.addCommand(
            "smcplugin",
            "Toggle Safe Mode",
            listOf("state")
        ) { args ->
            handleSafeModeCommand(args)
        }

        // /logscplugin command
        CommandsWrapper.addCommand(
            "logscplugin",
            "View crash logs",
            emptyList()
        ) { _ ->
            handleLogsCommand()
        }

        // /cpluginsettings command
        CommandsWrapper.addCommand(
            "cpluginsettings",
            "Open settings dashboard",
            emptyList()
        ) { _ ->
            handleSettingsCommand()
        }

        Log.d(TAG, "Commands registered")
    }

    /**
     * Unregister slash commands
     */
    private fun unregisterCommands() {
        try {
            CommandsWrapper.removeCommand("makeplugin")
            CommandsWrapper.removeCommand("cplugin")
            CommandsWrapper.removeCommand("smcplugin")
            CommandsWrapper.removeCommand("logscplugin")
            CommandsWrapper.removeCommand("cpluginsettings")
            Log.d(TAG, "Commands unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister commands", e)
        }
    }

    /**
     * Handle /makeplugin command
     */
    private fun handleMakePluginCommand(args: List<String>): String {
        return try {
            if (args.isEmpty()) {
                return "❌ Usage: /makeplugin <name> [description] <code>"
            }

            val name = args.getOrNull(0) ?: return "❌ Script name required"
            val code = args.getOrNull(if (args.size > 2) 2 else 1) ?: return "❌ Script code required"
            val description = if (args.size > 2) args[1] else "No description"

            val scriptId = UUID.randomUUID().toString()
            val script = UserScript(
                id = scriptId,
                name = name,
                description = description,
                code = code,
                enabled = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            scripts[scriptId] = script
            saveScripts()

            "✅ Script '$name' created successfully!\nID: `$scriptId`"
        } catch (e: Exception) {
            Log.e(TAG, "Error in makeplugin command", e)
            "❌ Error creating script: ${e.message}"
        }
    }

    /**
     * Handle /cplugin command
     */
    private fun handleCPluginCommand(): String {
        return try {
            if (scripts.isEmpty()) {
                return "📭 No custom scripts created yet.\nUse `/makeplugin` to create one!"
            }

            val sb = StringBuilder()
            sb.append("📋 **Your Custom Plugins:**\n\n")

            scripts.forEach { (id, script) ->
                val status = if (script.enabled) "✅" else "❌"
                sb.append("$status **${script.name}**\n")
                sb.append("   ID: `$id`\n")
                sb.append("   Description: ${script.description}\n")
                sb.append("   Created: ${SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(script.createdAt))}\n\n")
            }

            sb.append("*Use the settings dashboard to edit, toggle, or delete scripts.*")
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in cplugin command", e)
            "❌ Error: ${e.message}"
        }
    }

    /**
     * Handle /smcplugin command
     */
    private fun handleSafeModeCommand(args: List<String>): String {
        return try {
            val newState = when (args.getOrNull(0)?.lowercase()) {
                "on", "true", "enable", "1" -> true
                "off", "false", "disable", "0" -> false
                else -> !safeMode // Toggle if no valid argument
            }

            safeMode = newState
            saveSettings()

            val status = if (safeMode) "**ON** 🔒" else "**OFF** 🔓"
            val message = if (safeMode) {
                "Custom scripts will NOT execute until Safe Mode is disabled."
            } else {
                "Custom scripts are now active."
            }

            "🛡️ Safe Mode is now $status\n$message"
        } catch (e: Exception) {
            Log.e(TAG, "Error in smcplugin command", e)
            "❌ Error: ${e.message}"
        }
    }

    /**
     * Handle /logscplugin command
     */
    private fun handleLogsCommand(): String {
        return try {
            loadCrashLogs()

            if (crashLogs.isEmpty()) {
                return "✅ No crash logs. Everything is running smoothly!"
            }

            val sb = StringBuilder()
            sb.append("📜 **Crash Logs (Last 10):**\n\n")

            crashLogs.takeLast(10).forEach { log ->
                sb.append("⚠️ **${log.scriptName}** (`${log.scriptId}`)\n")
                sb.append("   Error: ${log.errorMessage}\n")
                sb.append("   Time: ${log.timestampFormatted}\n")
                sb.append("   Stack: \`\`\`\n${log.stackTrace}\n\`\`\`\n\n")
            }

            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in logscplugin command", e)
            "❌ Error: ${e.message}"
        }
    }

    /**
     * Handle /cpluginsettings command
     */
    private fun handleSettingsCommand(): String {
        return "⚙️ Opening settings dashboard...\n(In production: Launch PluginSettings BottomSheet UI)"
    }
}

/**
 * Data class representing a user-created script
 */
data class UserScript(
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    var enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Data class representing a crash log entry
 */
data class CrashLog(
    val scriptId: String,
    val scriptName: String,
    val errorMessage: String,
    val stackTrace: String,
    val timestamp: Long,
    val timestampFormatted: String
)
