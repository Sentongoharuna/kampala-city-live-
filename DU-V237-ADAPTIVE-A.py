#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "v172-source/app/src/main/java/com/sentongoharuna/pulse"
GRADLE = ROOT / "v172-source/app/build.gradle.kts"
STORY = SRC / "DevelopUgandaStoryPackager.kt"
CAMERA = SRC / "DevelopUgandaCameraActivity.kt"
LIVE = SRC / "DevelopUgandaLiveActivity.kt"
FIELD = SRC / "DevelopUgandaFieldIntelligencePanel.kt"
HELPER = SRC / "DevelopUgandaAdaptiveFormatUi.kt"
README = ROOT / "README-V237-ADAPTIVE-MULTIFORMAT-UI.txt"

required = [CAMERA, LIVE, FIELD, STORY, GRADLE]
for path in required:
    if not path.exists():
        raise SystemExit(f"V237 required V236 base file missing: {path}")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"V237 patch could not find {label}")
    return text.replace(old, new, 1)

HELPER_SOURCE = r'''package com.sentongoharuna.pulse

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * V237 Adaptive Multi-Format UI.
 *
 * The V236 capture-format/GNSS layer owns orientation and safe frames.
 * This class only reorganizes screen-only operator controls so each format
 * has a layout that fits without overlaps. Recording, V235 LUT processing,
 * Story Packages and CameraX output are not modified here.
 */
object DevelopUgandaAdaptiveFormatUi {

    enum class Role {
        REPORT,
        LIVE
    }

    private const val TAG_PREFIX = "develop_uganda_v237_adaptive_"
    private const val FIELD_CHIP_TAG = "develop_uganda_v236_field_intelligence"
    private const val GRADE_CHIP_TAG = "develop_uganda_v235_live_grade_panel"
    private const val TOOLS_CHIP_TAG = "develop_uganda_v233_tools_chip"

    fun attach(
        activity: AppCompatActivity,
        root: FrameLayout,
        role: Role
    ) {
        val markerTag = TAG_PREFIX + role.name.lowercase()
        if (root.findViewWithTag<View>(markerTag) != null) return

        val marker = View(activity).apply {
            tag = markerTag
            visibility = View.GONE
        }
        root.addView(marker, FrameLayout.LayoutParams(1, 1))

        Controller(activity, root, role, marker).start()
    }

    private class Controller(
        private val activity: AppCompatActivity,
        private val root: FrameLayout,
        private val role: Role,
        private val marker: View
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private var lastKey = ""

        private val tick = object : Runnable {
            override fun run() {
                val formatId =
                    DevelopUgandaFieldIntelligencePanel.activeFormatId(activity)
                val orientation = activity.resources.configuration.orientation
                val key = "$formatId:$orientation:${activity.resources.displayMetrics.widthPixels}x${activity.resources.displayMetrics.heightPixels}"

                if (key != lastKey) {
                    lastKey = key
                    apply(formatId)
                }

                handler.postDelayed(this, 350L)
            }
        }

        fun start() {
            marker.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) = Unit
                    override fun onViewDetachedFromWindow(v: View) {
                        handler.removeCallbacksAndMessages(null)
                    }
                }
            )
            root.post { apply(DevelopUgandaFieldIntelligencePanel.activeFormatId(activity)) }
            handler.post(tick)
        }

        private fun apply(formatId: String) {
            val landscape =
                formatId == "YOUTUBE_16_9" ||
                    formatId == "CINEMA_239" ||
                    formatId == "DUAL_SAFE" ||
                    activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            when (role) {
                Role.REPORT -> applyReport(formatId, landscape)
                Role.LIVE -> applyLive(formatId, landscape)
            }

            placeSideChips(landscape)
        }

        private fun applyReport(
            formatId: String,
            landscape: Boolean
        ) {
            val deck = view<LinearLayout>("v237_camera_deck") ?: return
            val mode = view<View>("v237_camera_mode_row")
            val identity = view<View>("v237_camera_identity_row")
            val tools = view<View>("v237_camera_tools_row")
            val advanced = view<View>("v237_camera_advanced_row")
            val display = view<View>("v237_camera_display_row")
            val output = view<View>("v237_camera_output_row")
            val director = view<View>("v237_camera_director_row")
            val zoom = view<View>("v237_camera_zoom_row")
            val exposure = view<View>("v237_camera_exposure_row")
            val action = view<View>("v237_camera_action_row")
            val summary = view<View>("v237_camera_settings_summary")

            val scene = view<View>("v237_scene_button")
            val look = view<View>("v237_look_button")
            val quality = view<View>("v237_quality_button")
            val capture = view<View>("v237_capture_button")
            val color = view<View>("v237_color_button")

            // Restore the five primary controls before format-specific pruning.
            listOf(scene, look, quality, capture, color).forEach { it?.visibility = View.VISIBLE }
            action?.visibility = View.VISIBLE
            mode?.visibility = View.VISIBLE

            if (!landscape) {
                deck.setPadding(dp(10), dp(4), dp(10), dp(10))
                when (formatId) {
                    "INSTAGRAM_4_5", "SQUARE_1_1" -> {
                        identity?.visibility = View.VISIBLE
                        tools?.visibility = View.VISIBLE
                        advanced?.visibility = View.VISIBLE
                        display?.visibility = View.GONE
                        output?.visibility = View.GONE
                        director?.visibility = View.GONE
                        zoom?.visibility = View.VISIBLE
                        exposure?.visibility = View.VISIBLE
                    }
                    else -> {
                        identity?.visibility = View.VISIBLE
                        tools?.visibility = View.VISIBLE
                        advanced?.visibility = View.VISIBLE
                        display?.visibility = View.VISIBLE
                        output?.visibility = View.VISIBLE
                        director?.visibility = View.VISIBLE
                        zoom?.visibility = View.VISIBLE
                        exposure?.visibility = View.VISIBLE
                    }
                }
                summary?.visibility = View.GONE
                toolsChip()?.visibility = View.VISIBLE
                return
            }

            // LANDSCAPE: only controls needed while the shot is being made.
            deck.setPadding(dp(10), dp(2), dp(10), dp(4))
            identity?.visibility = View.GONE
            tools?.visibility = View.GONE
            advanced?.visibility = View.GONE
            display?.visibility = View.GONE
            output?.visibility = View.GONE
            director?.visibility = View.GONE
            summary?.visibility = View.GONE
            toolsChip()?.visibility = View.GONE

            when (formatId) {
                "CINEMA_239" -> {
                    scene?.visibility = View.GONE
                    capture?.visibility = View.GONE
                    zoom?.visibility = View.GONE
                    exposure?.visibility = View.VISIBLE
                }
                "DUAL_SAFE" -> {
                    scene?.visibility = View.GONE
                    zoom?.visibility = View.VISIBLE
                    exposure?.visibility = View.VISIBLE
                }
                else -> { // YouTube 16:9
                    zoom?.visibility = View.VISIBLE
                    exposure?.visibility = View.VISIBLE
                }
            }

            compactChildren(mode, dp(38))
            compactChildren(action, dp(54))
        }

        private fun applyLive(
            formatId: String,
            landscape: Boolean
        ) {
            val deck = view<LinearLayout>("v237_live_deck") ?: return
            val outputStatus = view<View>("v237_live_output_status")
            val row1 = view<View>("v237_live_row1")
            val row2 = view<View>("v237_live_row2")
            val row3 = view<View>("v237_live_row3")
            val row4 = view<View>("v237_live_row4")
            val row5 = view<View>("v237_live_row5")
            val display = view<View>("v237_live_display_row")
            val output = view<View>("v237_live_output_row")
            val recordArea = view<ViewGroup>("v237_live_record_area")
            val subtitle = view<View>("v237_live_subtitle")
            val autoView = view<View>("v237_live_autoview")
            val timer = view<View>("v237_live_timer")

            row1?.visibility = View.VISIBLE
            row2?.visibility = View.VISIBLE
            recordArea?.visibility = View.VISIBLE

            if (!landscape) {
                deck.setPadding(dp(10), dp(9), dp(10), dp(10))
                outputStatus?.visibility = View.VISIBLE
                subtitle?.visibility = View.VISIBLE
                autoView?.visibility = View.VISIBLE
                timer?.visibility = View.VISIBLE

                if (formatId == "INSTAGRAM_4_5" || formatId == "SQUARE_1_1") {
                    row3?.visibility = View.GONE
                    row4?.visibility = View.VISIBLE
                    row5?.visibility = View.GONE
                    display?.visibility = View.GONE
                    output?.visibility = View.GONE
                } else {
                    row3?.visibility = View.VISIBLE
                    row4?.visibility = View.VISIBLE
                    row5?.visibility = View.VISIBLE
                    display?.visibility = View.VISIBLE
                    output?.visibility = View.VISIBLE
                }
                resizeRecordArea(recordArea, 118, 108)
                return
            }

            // LIVE landscape keeps source/profile, lens/light and record only.
            deck.setPadding(dp(8), dp(3), dp(8), dp(5))
            outputStatus?.visibility = View.GONE
            row3?.visibility = View.GONE
            row4?.visibility = View.GONE
            row5?.visibility = View.GONE
            display?.visibility = View.GONE
            output?.visibility = View.GONE
            subtitle?.visibility = View.GONE
            autoView?.visibility = View.GONE
            timer?.visibility = View.GONE
            resizeRecordArea(recordArea, 72, 64)
            compactChildren(row1, dp(38))
            compactChildren(row2, dp(38))
        }

        private fun placeSideChips(landscape: Boolean) {
            placeChip(
                FIELD_CHIP_TAG,
                start = true,
                landscape = landscape,
                portraitWidth = 112,
                portraitHeight = 54,
                landscapeWidth = 92,
                landscapeHeight = 44
            )
            placeChip(
                GRADE_CHIP_TAG,
                start = false,
                landscape = landscape,
                portraitWidth = 96,
                portraitHeight = 50,
                landscapeWidth = 86,
                landscapeHeight = 44
            )
        }

        private fun placeChip(
            tag: String,
            start: Boolean,
            landscape: Boolean,
            portraitWidth: Int,
            portraitHeight: Int,
            landscapeWidth: Int,
            landscapeHeight: Int
        ) {
            val chip = root.findViewWithTag<View>(tag) ?: return
            val lp = chip.layoutParams as? FrameLayout.LayoutParams ?: return
            lp.width = dp(if (landscape) landscapeWidth else portraitWidth)
            lp.height = dp(if (landscape) landscapeHeight else portraitHeight)
            lp.gravity = (if (start) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
            lp.leftMargin = if (start) dp(if (landscape) 5 else 8) else 0
            lp.rightMargin = if (!start) dp(if (landscape) 5 else 8) else 0
            lp.topMargin = 0
            lp.bottomMargin = 0
            chip.layoutParams = lp
            if (chip is Button) {
                chip.textSize = if (landscape) 7.1f else 8.1f
                chip.setPadding(dp(4), 0, dp(4), 0)
            }
        }

        private fun toolsChip(): View? =
            root.findViewWithTag(TOOLS_CHIP_TAG)

        private fun resizeRecordArea(
            area: ViewGroup?,
            areaHeightDp: Int,
            buttonSizeDp: Int
        ) {
            if (area == null) return
            val lp = area.layoutParams
            lp.height = dp(areaHeightDp)
            area.layoutParams = lp
            for (i in 0 until area.childCount) {
                val child = area.getChildAt(i)
                val childLp = child.layoutParams as? FrameLayout.LayoutParams ?: continue
                childLp.width = dp(buttonSizeDp)
                childLp.height = dp(buttonSizeDp)
                childLp.gravity = Gravity.CENTER
                child.layoutParams = childLp
            }
        }

        private fun compactChildren(container: View?, heightPx: Int) {
            val group = container as? ViewGroup ?: return
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                val lp = child.layoutParams
                if (lp.height > 1) {
                    lp.height = heightPx
                    child.layoutParams = lp
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : View> view(tag: String): T? =
            root.findViewWithTag<View>(tag) as? T

        private fun dp(value: Int): Int =
            (value * activity.resources.displayMetrics.density).toInt()
    }
}
'''

HELPER.write_text(HELPER_SOURCE, encoding="utf-8")

# ---------------------------- FIELD REPORT CAMERA ----------------------------
camera = CAMERA.read_text(encoding="utf-8")

camera = replace_once(
    camera,
    'bottomDeck = LinearLayout(this).apply {\n            orientation = LinearLayout.VERTICAL',
    'bottomDeck = LinearLayout(this).apply {\n            tag = "v237_camera_deck"\n            orientation = LinearLayout.VERTICAL',
    'camera deck tag'
)
camera = replace_once(
    camera,
    'modeRow = row().apply {\n            gravity = Gravity.CENTER',
    'modeRow = row().apply {\n            tag = "v237_camera_mode_row"\n            gravity = Gravity.CENTER',
    'camera mode row tag'
)
camera = replace_once(
    camera,
    'identityRow = row().apply {\n            gravity = Gravity.CENTER',
    'identityRow = row().apply {\n            tag = "v237_camera_identity_row"\n            gravity = Gravity.CENTER',
    'camera identity row tag'
)
camera = replace_once(
    camera,
    'reportToolsRow = row().apply {\n            gravity = Gravity.CENTER',
    'reportToolsRow = row().apply {\n            tag = "v237_camera_tools_row"\n            gravity = Gravity.CENTER',
    'camera tools row tag'
)
camera = replace_once(
    camera,
    'reportAdvancedRow =\n            row().apply {\n                gravity =',
    'reportAdvancedRow =\n            row().apply {\n                tag = "v237_camera_advanced_row"\n                gravity =',
    'camera advanced row tag'
)
camera = replace_once(
    camera,
    'reportDisplayRow =\n            row().apply {\n                gravity =',
    'reportDisplayRow =\n            row().apply {\n                tag = "v237_camera_display_row"\n                gravity =',
    'camera display row tag'
)
camera = replace_once(
    camera,
    'reportOutputRow =\n            row().apply {\n                gravity =',
    'reportOutputRow =\n            row().apply {\n                tag = "v237_camera_output_row"\n                gravity =',
    'camera output row tag'
)
camera = replace_once(
    camera,
    'reportDirectorRow =\n            row().apply {\n                gravity =',
    'reportDirectorRow =\n            row().apply {\n                tag = "v237_camera_director_row"\n                gravity =',
    'camera director row tag'
)
camera = replace_once(
    camera,
    'settingsSummaryView = hud(',
    'settingsSummaryView = hud(',
    'settings summary anchor'
)
# Add summary tag inside its apply block.
camera = replace_once(
    camera,
    ').apply {\n            visibility = View.GONE\n            setPadding(\n                dp(8),',
    ').apply {\n            tag = "v237_camera_settings_summary"\n            visibility = View.GONE\n            setPadding(\n                dp(8),',
    'settings summary tag'
)
camera = replace_once(
    camera,
    'zoomRow = row()\n        zoomRow.addView(',
    'zoomRow = row().apply { tag = "v237_camera_zoom_row" }\n        zoomRow.addView(',
    'camera zoom row tag'
)
camera = replace_once(
    camera,
    'exposureRow = row()\n        exposureRow.addView(',
    'exposureRow = row().apply { tag = "v237_camera_exposure_row" }\n        exposureRow.addView(',
    'camera exposure row tag'
)
camera = replace_once(
    camera,
    'actionRow = row().apply {\n            gravity = Gravity.CENTER',
    'actionRow = row().apply {\n            tag = "v237_camera_action_row"\n            gravity = Gravity.CENTER',
    'camera action row tag'
)

# Tag the five format-sensitive primary buttons after they are created.
button_anchor = '''        colorButton = deckButton(
            "COLOR ▾\\n${v229ColorDeckLabel()}",
            0xFFA793D8.toInt()
        )
'''
button_new = button_anchor + '''
        sceneButton.tag = "v237_scene_button"
        lookButton.tag = "v237_look_button"
        qualityButton.tag = "v237_quality_button"
        captureModeButton.tag = "v237_capture_button"
        colorButton.tag = "v237_color_button"
'''
camera = replace_once(camera, button_anchor, button_new, 'camera button tags')

field_attach_camera = '''        DevelopUgandaFieldIntelligencePanel.attach(
            activity = this,
            root = root,
            previewView = previewView
        )
'''
field_attach_camera_new = field_attach_camera + '''        DevelopUgandaAdaptiveFormatUi.attach(
            activity = this,
            root = root,
            role = DevelopUgandaAdaptiveFormatUi.Role.REPORT
        )
'''
camera = replace_once(camera, field_attach_camera, field_attach_camera_new, 'camera adaptive attach')

# Visible build labels only. Function names and V233 color-engine wording remain intact.
camera = camera.replace('\"V233\"', '\"V237\"')
camera = camera.replace(' • V233\"', ' • V237\"')

CAMERA.write_text(camera, encoding="utf-8")

# ------------------------------ LIVE STUDIO ---------------------------------
live = LIVE.read_text(encoding="utf-8")

live = replace_once(
    live,
    'val topPanel =\n            LinearLayout(this).apply {\n                orientation =',
    'val topPanel =\n            LinearLayout(this).apply {\n                tag = "v237_live_top_panel"\n                orientation =',
    'live top panel tag'
)
live = replace_once(
    live,
    'liveSubTitle =\n            label(',
    'liveSubTitle =\n            label(',
    'live subtitle anchor'
)
live = replace_once(
    live,
    ').apply {\n                setPadding(\n                    0,\n                    dp(3),\n                    0,\n                    dp(7)\n                )\n            }\n\n        topPanel.addView(\n            liveSubTitle',
    ').apply {\n                tag = "v237_live_subtitle"\n                setPadding(\n                    0,\n                    dp(3),\n                    0,\n                    dp(7)\n                )\n            }\n\n        topPanel.addView(\n            liveSubTitle',
    'live subtitle tag'
)
live = replace_once(
    live,
    'liveAutoViewDescriptionView =\n            label(',
    'liveAutoViewDescriptionView =\n            label(',
    'live autoview anchor'
)
live = replace_once(
    live,
    ').apply {\n                maxLines = 1\n                isSingleLine = true\n\n                setPadding(\n                    0,\n                    dp(2),\n                    0,\n                    dp(3)\n                )\n            }\n\n        topPanel.addView(\n            liveAutoViewDescriptionView',
    ').apply {\n                tag = "v237_live_autoview"\n                maxLines = 1\n                isSingleLine = true\n\n                setPadding(\n                    0,\n                    dp(2),\n                    0,\n                    dp(3)\n                )\n            }\n\n        topPanel.addView(\n            liveAutoViewDescriptionView',
    'live autoview tag'
)
live = replace_once(
    live,
    'timerView =\n            label(',
    'timerView =\n            label(',
    'live timer anchor'
)
live = replace_once(
    live,
    ').apply {\n                typeface =\n                    Typeface.MONOSPACE',
    ').apply {\n                tag = "v237_live_timer"\n                typeface =\n                    Typeface.MONOSPACE',
    'live timer tag'
)
live = replace_once(
    live,
    'val liveDeck =\n            LinearLayout(this).apply {\n                orientation =',
    'val liveDeck =\n            LinearLayout(this).apply {\n                tag = "v237_live_deck"\n                orientation =',
    'live deck tag'
)
live = replace_once(
    live,
    'outputStatus =\n            label(',
    'outputStatus =\n            label(',
    'live output status anchor'
)
live = replace_once(
    live,
    ').apply {\n                gravity =\n                    Gravity.CENTER\n\n                setPadding(\n                    0,\n                    0,\n                    0,\n                    dp(8)\n                )\n            }\n\n        liveDeck.addView(\n            outputStatus',
    ').apply {\n                tag = "v237_live_output_status"\n                gravity =\n                    Gravity.CENTER\n\n                setPadding(\n                    0,\n                    0,\n                    0,\n                    dp(8)\n                )\n            }\n\n        liveDeck.addView(\n            outputStatus',
    'live output status tag'
)

for name, tag in [
    ('row1', 'v237_live_row1'),
    ('row2', 'v237_live_row2'),
    ('row3', 'v237_live_row3'),
    ('row4', 'v237_live_row4'),
    ('row5', 'v237_live_row5'),
    ('displayRow', 'v237_live_display_row'),
    ('outputRow', 'v237_live_output_row'),
]:
    old = f'''val {name} =\n            LinearLayout(this).apply {{\n                orientation ='''
    new = f'''val {name} =\n            LinearLayout(this).apply {{\n                tag = "{tag}"\n                orientation ='''
    live = replace_once(live, old, new, f'live {name} tag')

live = replace_once(
    live,
    'val recordArea =\n            FrameLayout(this).apply {\n                setPadding(',
    'val recordArea =\n            FrameLayout(this).apply {\n                tag = "v237_live_record_area"\n                setPadding(',
    'live record area tag'
)

field_attach_live = '''        DevelopUgandaFieldIntelligencePanel.attach(
            activity = this,
            root = root,
            previewView = previewView
        )
'''
field_attach_live_new = field_attach_live + '''        DevelopUgandaAdaptiveFormatUi.attach(
            activity = this,
            root = root,
            role = DevelopUgandaAdaptiveFormatUi.Role.LIVE
        )
'''
live = replace_once(live, field_attach_live, field_attach_live_new, 'live adaptive attach')

# Update exact app build labels but keep descriptive strings like "V233 PROFESSIONAL COLOR".
live = live.replace('\"V233\"', '\"V237\"')

LIVE.write_text(live, encoding="utf-8")

# ----------------------------- STORY PACKAGE --------------------------------
story = STORY.read_text(encoding="utf-8")
story = story.replace('.put("app_version", "V233")', '.put("app_version", "V237")')
STORY.write_text(story, encoding="utf-8")

# -------------------------------- VERSION ------------------------------------
gradle = GRADLE.read_text(encoding="utf-8")
if 'versionCode = 23700' not in gradle:
    gradle = gradle.replace('versionCode = 23600', 'versionCode = 23700', 1)
if 'versionName = "237.0-adaptive-multiformat-ui"' not in gradle:
    gradle = gradle.replace(
        'versionName = "236.0-multiformat-satellite-field-intelligence"',
        'versionName = "237.0-adaptive-multiformat-ui"',
        1
    )
GRADLE.write_text(gradle, encoding="utf-8")

README.write_text(
    """develop.uganda V237 — ADAPTIVE MULTI-FORMAT UI

PURPOSE
V237 rearranges screen-only camera controls for each V236 platform format.
It fixes landscape overlap without changing the CameraX recording path or V235 LUT/color engine.

LAYOUTS
• YouTube 16:9: primary mode row + zoom + exposure + lens/record/light.
• Cinema 2.39: look/format/color + exposure + lens/record/light.
• Dual Safe: look/format/capture/color + zoom/exposure + lens/record/light.
• Vertical 9:16: full portrait operator deck.
• Instagram 4:5 / Square 1:1: compact portrait hybrid.
• Live Studio gets the same landscape compaction.

SAFETY
The V235 LUT engine, Everyday Color Mixer, Live Grade panel and Navy sheet are unchanged.
V236 GNSS satellite, horizon, sun-position and format-safe-frame systems are retained.
""",
    encoding="utf-8"
)

print("PASS: V237 Adaptive Multi-Format UI patch applied")
