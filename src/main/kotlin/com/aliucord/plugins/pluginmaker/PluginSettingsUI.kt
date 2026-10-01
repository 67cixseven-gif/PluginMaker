package com.aliucord.plugins.pluginmaker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Settings UI Fragment
 * 
 * Provides unified settings dashboard accessible via:
 * 1. `/cpluginsettings` command (as BottomSheet)
 * 2. Settings icon next to plugin name (as Fragment/Page)
 */
class PluginSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = createSettingsUI(requireContext())

    private fun createSettingsUI(context: Context): ScrollView {
        val scrollView = ScrollView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        // Header
        val header = AppCompatTextView(context).apply {
            text = "⚙️ PluginMaker Settings"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16 }
        }
        container.addView(header)

        // Safe Mode Toggle
        container.addView(
            createToggleCard(
                context,
                "🔒 Safe Mode",
                "Disable all custom scripts execution",
                false
            )
        )

        // Auto-Disable Broken Scripts
        container.addView(
            createToggleCard(
                context,
                "⚡ Auto-Disable Broken Scripts",
                "Automatically disable only the crashing script",
                true
            )
        )

        // Error Logging
        container.addView(
            createToggleCard(
                context,
                "📝 Enable Error Logging",
                "Log crashes and diagnostic information",
                true
            )
        )

        // Commands Enabled
        container.addView(
            createToggleCard(
                context,
                "💬 Commands Enabled",
                "Enable /makeplugin, /cplugin, /smcplugin, etc.",
                true
            )
        )

        // Divider
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = 16
                bottomMargin = 16
            }
            setBackgroundColor(0xFFDDDDDD.toInt())
        }
        container.addView(divider)

        // View Logs Button
        container.addView(
            createActionCard(
                context,
                "📄 View Crash Logs",
                "Show diagnostic crash information",
                "View Logs"
            )
        )

        // Clear All Scripts Button
        container.addView(
            createActionCard(
                context,
                "🗑️ Clear All Scripts",
                "Delete all custom plugins (irreversible)",
                "Clear All"
            )
        )

        // Plugin Info
        val info = AppCompatTextView(context).apply {
            text = "PluginMaker v1.0.0\nManage custom JavaScript snippets safely"
            textSize = 12f
            setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 24 }
        }
        container.addView(info)

        scrollView.addView(container)
        return scrollView
    }

    /**
     * Create a toggle setting card
     */
    private fun createToggleCard(
        context: Context,
        title: String,
        subtitle: String,
        defaultState: Boolean
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            cardElevation = 4f
            radius = 12f
            setCardBackgroundColor(0xFFFAFAFA.toInt())
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        // Text content
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val titleView = AppCompatTextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF212121.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val subtitleView = AppCompatTextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(0xFF757575.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }

        textContainer.addView(titleView)
        textContainer.addView(subtitleView)

        // Toggle switch
        val toggle = SwitchCompat(context).apply {
            isChecked = defaultState
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 16 }
            setOnCheckedChangeListener { _, isChecked ->
                android.util.Log.d("PluginSettings", "$title toggled to $isChecked")
                // Persist to storage in production
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
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            cardElevation = 4f
            radius = 12f
            setCardBackgroundColor(0xFFFAFAFA.toInt())
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        val titleView = AppCompatTextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF212121.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val subtitleView = AppCompatTextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(0xFF757575.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 4 }
        }

        val button = AppCompatButton(context).apply {
            text = buttonText
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
            setOnClickListener {
                android.util.Log.d("PluginSettings", "Action clicked: $title")
                // Handle action in production
            }
        }

        container.addView(titleView)
        container.addView(subtitleView)
        container.addView(button)
        card.addView(container)

        return card
    }
}

/**
 * Settings BottomSheet Dialog
 * Used when opening settings via /cpluginsettings command
 */
class PluginSettingsBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = PluginSettingsFragment().onCreateView(inflater, container, savedInstanceState)
}
