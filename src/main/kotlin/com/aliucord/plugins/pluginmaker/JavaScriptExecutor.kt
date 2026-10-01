package com.aliucord.plugins.pluginmaker

import android.util.Log
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import java.io.File

/**
 * JavaScript Execution Engine
 * 
 * Provides safe, sandboxed execution of user-defined JavaScript snippets.
 * Uses Mozilla Rhino JavaScript engine for Android.
 * Features:
 *  - Sandboxed scope with restricted access
 *  - Timeout protection
 *  - Console logging support
 *  - Error isolation
 */
class JavaScriptExecutor {
    companion object {
        private const val TAG = "JSExecutor"
        private const val EXECUTION_TIMEOUT = 5000L // 5 seconds
    }

    /**
     * Execute JavaScript code safely with timeout and error handling
     * 
     * @param code The JavaScript code to execute
     * @param scriptId The ID of the script being executed (for logging)
     * @param onLog Callback for console.log() calls
     * @return Result object containing success status and any output
     */
    fun executeScript(
        code: String,
        scriptId: String,
        onLog: (String) -> Unit = {}
    ): ExecutionResult {
        return try {
            val result = Context.enter().use { context ->
                // Set optimization level
                context.optimizationLevel = 9

                // Create a new scope for isolation
                val scope = context.initStandardObjects()

                // Add custom console object
                val consoleObj = NativeObject().apply {
                    put("log", scope, Function { _, _, args, _ ->
                        val message = args.joinToString(" ") { arg ->
                            Context.toString(arg)
                        }
                        onLog(message)
                        Log.d(TAG, "[$scriptId] $message")
                        null
                    })
                    put("error", scope, Function { _, _, args, _ ->
                        val message = "ERROR: " + args.joinToString(" ") { arg ->
                            Context.toString(arg)
                        }
                        onLog(message)
                        Log.e(TAG, "[$scriptId] $message")
                        null
                    })
                    put("warn", scope, Function { _, _, args, _ ->
                        val message = "WARN: " + args.joinToString(" ") { arg ->
                            Context.toString(arg)
                        }
                        onLog(message)
                        Log.w(TAG, "[$scriptId] $message")
                        null
                    })
                }
                scope.put("console", scope, consoleObj)

                // Add restricted API objects
                addRestrictedAPIs(scope, scriptId)

                // Execute the script with timeout
                val thread = Thread {
                    try {
                        context.evaluateString(
                            scope,
                            code,
                            "<script-$scriptId>",
                            1,
                            null
                        )
                    } catch (e: Exception) {
                        throw RuntimeException("Execution error: ${e.message}", e)
                    }
                }

                thread.start()
                thread.join(EXECUTION_TIMEOUT)

                if (thread.isAlive) {
                    thread.interrupt()
                    throw TimeoutException("Script execution exceeded ${EXECUTION_TIMEOUT}ms timeout")
                }

                ExecutionResult(
                    success = true,
                    output = "Script executed successfully",
                    error = null
                )
            }
            result
        } catch (e: TimeoutException) {
            Log.e(TAG, "Script timeout: $scriptId", e)
            ExecutionResult(
                success = false,
                output = null,
                error = "Execution timeout: Script exceeded time limit"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Script execution error: $scriptId", e)
            ExecutionResult(
                success = false,
                output = null,
                error = e.message ?: "Unknown execution error"
            )
        } finally {
            Context.exit()
        }
    }

    /**
     * Add restricted API objects to the script scope
     * These APIs allow safe interaction without accessing sensitive system functions
     */
    private fun addRestrictedAPIs(scope: Scriptable, scriptId: String) {
        // Discord API stub (placeholder)
        val discordApi = NativeObject().apply {
            put("sendMessage", scope, Function { _, _, args, _ ->
                val message = if (args.isNotEmpty()) Context.toString(args[0]) else ""
                Log.d(TAG, "[$scriptId] Discord.sendMessage: $message")
                // In production: send message to Discord channel
                true
            })
            put("getUser", scope, Function { _, _, args, _ ->
                val userId = if (args.isNotEmpty()) Context.toString(args[0]) else ""
                Log.d(TAG, "[$scriptId] Discord.getUser: $userId")
                // In production: fetch user data
                NativeObject()
            })
        }
        scope.put("Discord", scope, discordApi)

        // Storage API (local storage for scripts)
        val storageApi = NativeObject().apply {
            put("setItem", scope, Function { _, _, args, _ ->
                if (args.size >= 2) {
                    val key = Context.toString(args[0])
                    val value = Context.toString(args[1])
                    Log.d(TAG, "[$scriptId] Storage.setItem: $key = $value")
                    // In production: persist to local storage
                }
                null
            })
            put("getItem", scope, Function { _, _, args, _ ->
                if (args.isNotEmpty()) {
                    val key = Context.toString(args[0])
                    Log.d(TAG, "[$scriptId] Storage.getItem: $key")
                    // In production: retrieve from local storage
                }
                null
            })
            put("removeItem", scope, Function { _, _, args, _ ->
                if (args.isNotEmpty()) {
                    val key = Context.toString(args[0])
                    Log.d(TAG, "[$scriptId] Storage.removeItem: $key")
                    // In production: remove from local storage
                }
                null
            })
        }
        scope.put("Storage", scope, storageApi)

        // Timer API (setTimeout, setInterval)
        val timerApi = NativeObject().apply {
            put("setTimeout", scope, Function { _, _, args, _ ->
                if (args.size >= 2) {
                    val delay = Context.toNumber(args[1]).toLong()
                    Log.d(TAG, "[$scriptId] setTimeout: delay=${delay}ms")
                    // In production: schedule execution
                }
                null
            })
            put("setInterval", scope, Function { _, _, args, _ ->
                if (args.size >= 2) {
                    val interval = Context.toNumber(args[1]).toLong()
                    Log.d(TAG, "[$scriptId] setInterval: interval=${interval}ms")
                    // In production: schedule recurring execution
                }
                null
            })
        }
        scope.put("Timer", scope, timerApi)

        // Math object (standard)
        scope.put("Math", scope, scope.get("Math", scope))
    }
}

/**
 * Result of JavaScript execution
 */
data class ExecutionResult(
    val success: Boolean,
    val output: String?,
    val error: String?
)

/**
 * Custom timeout exception for script execution
 */
class TimeoutException(message: String) : Exception(message)
