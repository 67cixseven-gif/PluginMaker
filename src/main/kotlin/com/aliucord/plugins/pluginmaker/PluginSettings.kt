package com.aliucord.plugins.pluginmaker

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.aliucord.api.SettingsAPI
import com.google.android.material.card.MaterialCardView

/**
 * Settings UI for PluginMaker - Provides centralized control panel
 * This is bound to the Aliucord SettingsTab and displays as both:
 * 1. A native settings page (when accessed via settings menu)
 * 2. A BottomSheet dialog (when /cpluginsettings is called)
 */
class PluginSettings(context: Context) : LinearLayout(context) {
    
    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        
        // Create scrollable container
        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        
        val settingsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        
        // Safe Mode Toggle Card
        settingsContainer.addView(
            createSettingCard(
                context,
                "🛡️ Safe Mode",
                "Disable all custom scripts",
                "safeMode"
            )
        )
        
        // Auto-Disable Broken Scripts Toggle Card
        settingsContainer.addView(
            createSettingCard(
                context,
                "⚡ Auto-Disable Broken Scripts",
                "Automatically disable crashing scripts",
                "autoDisableBrokenScripts"
            )
        )
        
        // Error Logging Toggle Card
        settingsContainer.addView(
            createSettingCard(
                context,
                "📋 Enable Error Logging",
                "Log crashes and diagnostics",
                "enableErrorLogging"
            )
        )
        
        // Commands Enabled Toggle Card
        settingsContainer.addView(
            createSettingCard(
                context,
                "💬 Commands Enabled",
                "Enable slash commands (/makeplugin, /cplugin, etc.)",
                "commandsEnabled"
            )
        )
        
        // View Logs Button Card
        settingsContainer.addView(
            createActionCard(
                context,
                "📜 View Crash Logs",
                "Show diagnostic crash information",
                "Show logs" // Would trigger /logscplugin
            )
        )
        
        // Clear All Scripts Button Card
        settingsContainer.addView(
            createActionCard(
                context,
                "🗑️ Clear All Scripts",
                "Delete all custom plugins (irreversible)",
                "Clear all" // Would show confirmation dialog
            )
        )
        
        scrollView.addView(settingsContainer)
        addView(scrollView)
    }
    
    /**
     * Create a toggle setting card
     */
    private fun createSettingCard(
        context: Context,
        title: String,
        subtitle: String,
        settingKey: String
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
            cardElevation = 2f
            radius = 8f
        }
        
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(16, 16, 16, 16)
        }
        
        // Text section
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f // Weight to fill available space
            )
        }
        
        val titleView = AppCompatTextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        
        val subtitleView = AppCompatTextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
            }
        }
        
        textContainer.addView(titleView)
        textContainer.addView(subtitleView)
        
        // Toggle switch
        val toggle = Switch(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = 16
            }
            // Load setting value from SettingsAPI (in production)
            // isChecked = SettingsAPI.getBool(settingKey, false)
            setOnCheckedChangeListener { _, isChecked ->
                // Save to SettingsAPI (in production)
                // SettingsAPI.setBool(settingKey, isChecked)
                android.util.Log.d("PluginSettings", "$settingKey toggled to $isChecked")
            }
        }
        
        container.addView(textContainer)
        container.addView(toggle)
        card.addView(container)
        
        return card
    }
    
    /**
     * Create an action button card
     */
    private fun createActionCard(
        context: Context,
        title: String,
        subtitle: String,
        buttonText: String
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
            cardElevation = 2f
            radius = 8f
        }
        
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setPadding(16, 16, 16, 16)
        }
        
        val titleView = AppCompatTextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        
        val subtitleView = AppCompatTextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4
            }
        }
        
        val button = AppCompatButton(context).apply {
            text = buttonText
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12
            }
            setOnClickListener {
                android.util.Log.d("PluginSettings", "Action clicked: $title")
                // Handle button click in production
            }
        }
        
        container.addView(titleView)
        container.addView(subtitleView)
        container.addView(button)
        card.addView(container)
        
        return card
    }
}
