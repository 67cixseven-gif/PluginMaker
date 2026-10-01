package com.aliucord.plugins.pluginmaker

import android.content.Context
import android.util.Log
import com.aliucord.entities.Plugin
import com.aliucord.wrappers.CommandsWrapper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * PluginMaker - Main Plugin Class (Refactored)
 * 
 * Integrates:
 *  - StorageManager for persistent data
 *  - JavaScriptExecutor for safe script execution
 *  - PluginSettings for unified configuration
 *  - Command handling with crash protection
 */
class PluginMaker : Plugin() {
    companion object {
        private const val TAG = "PluginMaker"
        private const val PLUGIN_DIR = "/sdcard/Aliucord/plugins/PluginMaker"
    }

    // Core managers
    private lateinit var storage: StorageManager
    private lateinit var jsExecutor: JavaScriptExecutor
    private var settings: PluginSettings? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // In-memory cache
    private val scripts = mutableMapOf<String, UserScript>()

    override fun start(context: Context) {
        Log.d(TAG, "Starting PluginMaker...")

        try {
            // Initialize managers
            storage = StorageManager(PLUGIN_DIR)
            jsExecutor = JavaScriptExecutor()

            // Load settings and scripts
            settings = storage.loadSettings()
            scripts.putAll(storage.loadScripts())

            Log.d(TAG, "Loaded ${scripts.size} scripts, Safe Mode: ${settings?.safeMode}")

            // Check for previous crashes
            checkBreadcrumb()

            // Register commands
            registerCommands()

            // Load and execute enabled scripts on startup
            if (settings?.safeMode != true) {
                loadScriptsOnStartup()
            }

            Log.d(TAG, "PluginMaker started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PluginMaker", e)
        }
    }

    override fun stop(context: Context) {
        Log.d(TAG, "Stopping PluginMaker...")
        try {
            unregisterCommands()
            scope.cancel()
            Log.d(TAG, "PluginMaker stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop PluginMaker", e)
        }
    }

    // ================== Crash Detection & Prevention ==================

    /**
     * Check for previous crashes using breadcrumb file
     */
    private fun checkBreadcrumb() {
        val breadcrumb = storage.readBreadcrumb() ?: return
        val (scriptId, timestamp) = breadcrumb

        Log.e(TAG, "Crash detected for script: $scriptId")

        val crashLog = CrashLog(
            scriptId = scriptId,
            scriptName = scripts[scriptId]?.name ?: "Unknown",
            errorMessage = "Crash detected during execution",
            stackTrace = "Automatic crash recovery activated",
            timestamp = System.currentTimeMillis(),
            timestampFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        )

        if (settings?.autoDisableBrokenScripts == true && scripts.containsKey(scriptId)) {
            // Auto-disable the crashing script
            scripts[scriptId]?.enabled = false
            storage.saveScripts(scripts)
            storage.addCrashLog(crashLog)
            Log.d(TAG, "Auto-disabled script: $scriptId")
        } else {
            // Enable Safe Mode globally
            settings = settings?.copy(safeMode = true)
            settings?.let { storage.saveSettings(it) }
            storage.addCrashLog(crashLog)
            Log.d(TAG, "Safe Mode activated due to crash")
        }

        // Clear breadcrumb
        storage.clearBreadcrumb()
    }

    /**
     * Load and execute all enabled scripts on startup
     */
    private fun loadScriptsOnStartup() {
        scope.launch(Dispatchers.Default) {
            for ((scriptId, script) in scripts) {
                if (script.enabled) {
                    executeScript(scriptId)
                }
            }
        }
    }

    /**
     * Execute a script safely with breadcrumb protection
     */
    private fun executeScript(scriptId: String): Boolean {
        val script = scripts[scriptId] ?: return false

        if (!script.enabled || settings?.safeMode == true) {
            Log.d(TAG, "Script $scriptId is disabled or Safe Mode is active")
            return false
        }

        return try {
            // Write breadcrumb before execution
            storage.writeBreadcrumb(scriptId)

            // Execute with logging
            val result = jsExecutor.executeScript(script.code, scriptId) { logMessage ->
                Log.d(TAG, "[${script.name}] $logMessage")
            }

            // Clear breadcrumb on success
            storage.clearBreadcrumb()

            if (result.success) {
                Log.d(TAG, "Script executed successfully: ${script.name}")
            } else {
                Log.e(TAG, "Script execution failed: ${script.name} - ${result.error}")
                if (settings?.enableErrorLogging == true) {
                    val crashLog = CrashLog(
                        scriptId = scriptId,
                        scriptName = script.name,
                        errorMessage = result.error ?: "Unknown error",
                        stackTrace = result.error ?: "No stack trace",
                        timestamp = System.currentTimeMillis(),
                        timestampFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    )
                    storage.addCrashLog(crashLog)
                }
            }

            result.success
        } catch (e: Exception) {
            Log.e(TAG, "Script execution exception: $scriptId", e)

            if (settings?.enableErrorLogging == true) {
                val crashLog = CrashLog(
                    scriptId = scriptId,
                    scriptName = script.name,
                    errorMessage = e.message ?: "Unknown error",
                    stackTrace = e.stackTraceToString(),
                    timestamp = System.currentTimeMillis(),
                    timestampFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                )
                storage.addCrashLog(crashLog)
            }

            false
        }
    }

    // ================== Command Registration ==================

    /**
     * Register all slash commands
     */
    private fun registerCommands() {
        if (settings?.commandsEnabled != true) return

        try {
            CommandsWrapper.addCommand(
                "makeplugin",
                "Create a new custom plugin",
                listOf("name", "description", "code")
            ) { args ->
                handleMakePluginCommand(args)
            }

            CommandsWrapper.addCommand(
                "cplugin",
                "View and manage custom plugins",
                emptyList()
            ) { _ ->
                handleCPluginCommand()
            }

            CommandsWrapper.addCommand(
                "smcplugin",
                "Toggle Safe Mode",
                listOf("state")
            ) { args ->
                handleSafeModeCommand(args)
            }

            CommandsWrapper.addCommand(
                "logscplugin",
                "View crash logs",
                emptyList()
            ) { _ ->
                handleLogsCommand()
            }

            CommandsWrapper.addCommand(
                "cpluginsettings",
                "Open settings dashboard",
                emptyList()
            ) { _ ->
                handleSettingsCommand()
            }

            Log.d(TAG, "Commands registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register commands", e)
        }
    }

    /**
     * Unregister all commands
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

    // ================== Command Handlers ==================

    /**
     * Handle /makeplugin command
     * Usage: /makeplugin <name> [description] <code>
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
            val now = System.currentTimeMillis()
            val script = UserScript(
                id = scriptId,
                name = name,
                description = description,
                code = code,
                enabled = true,
                createdAt = now,
                updatedAt = now
            )

            scripts[scriptId] = script
            storage.saveScripts(scripts)

            "✅ Script '$name' created successfully!\nID: `$scriptId`"
        } catch (e: Exception) {
            Log.e(TAG, "Error in makeplugin command", e)
            "❌ Error creating script: ${e.message}"
        }
    }

    /**
     * Handle /cplugin command
     * Displays all scripts with their status
     */
    private fun handleCPluginCommand(): String {
        return try {
            if (scripts.isEmpty()) {
                return "📭 No custom scripts created yet.\nUse `/makeplugin` to create one!"
            }

            val sb = StringBuilder()
            sb.append("📋 **Your Custom Plugins (${scripts.size})**\n\n")

            scripts.forEach { (id, script) ->
                val status = if (script.enabled) "✅" else "❌"
                sb.append("$status **${script.name}**\n")
                sb.append("   ID: `${id.take(8)}...`\n")
                sb.append("   Description: ${script.description}\n")
                sb.append("   Created: ${SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(script.createdAt))}\n\n")
            }

            sb.append("\n*Use `/cpluginsettings` to edit, toggle, or delete scripts.*")
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in cplugin command", e)
            "❌ Error: ${e.message}"
        }
    }

    /**
     * Handle /smcplugin command
     * Toggle Safe Mode
     */
    private fun handleSafeModeCommand(args: List<String>): String {
        return try {
            val newState = when (args.getOrNull(0)?.lowercase()) {
                "on", "true", "enable", "1" -> true
                "off", "false", "disable", "0" -> false
                else -> !( settings?.safeMode ?: false)
            }

            settings = settings?.copy(safeMode = newState)
            settings?.let { storage.saveSettings(it) }

            val status = if (newState) "**ON** 🔒" else "**OFF** 🔓"
            val message = if (newState) {
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
     * Display crash logs
     */
    private fun handleLogsCommand(): String {
        return try {
            val crashLogs = storage.getRecentCrashLogs(10)

            if (crashLogs.isEmpty()) {
                return "✅ No crash logs. Everything is running smoothly!"
            }

            val sb = StringBuilder()
            sb.append("📄 **Crash Logs (Last ${crashLogs.size})**\n\n")

            crashLogs.forEach { log ->
                sb.append("⚠️ **${log.scriptName}** (`${log.scriptId.take(8)}...`)\n")
                sb.append("   Error: ${log.errorMessage}\n")
                sb.append("   Time: ${log.timestampFormatted}\n\n")
            }

            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in logscplugin command", e)
            "❌ Error: ${e.message}"
        }
    }

    /**
     * Handle /cpluginsettings command
     * Opens settings dashboard
     */
    private fun handleSettingsCommand(): String {
        return "⚙️ Opening settings dashboard...\n(Settings UI would open here in production)"
    }
}
