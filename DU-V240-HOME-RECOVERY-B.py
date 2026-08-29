#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SRC = ROOT / "v172-source/app/src/main/java/com/sentongoharuna/pulse"
GRADLE = ROOT / "v172-source/app/build.gradle.kts"
MANIFEST = ROOT / "v172-source/app/src/main/AndroidManifest.xml"
STORY = SRC / "DevelopUgandaStoryPackager.kt"
PROHUD = SRC / "DevelopUgandaProCameraHud.kt"
HOME = SRC / "DevelopUgandaHomeActivity.kt"
README = ROOT / "README-V240-HOME-HUB.txt"

for path in [SRC, GRADLE, MANIFEST, STORY, PROHUD]:
    if not path.exists():
        raise SystemExit(f"V240 required file missing: {path}")

HOME_SOURCE = r'''package com.sentongoharuna.pulse

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * V240 Home Hub.
 *
 * A purpose-built front door for the develop.uganda camera system. The home
 * screen does not replace the camera engines. It organizes them into clear
 * groups and keeps the proven V235 LUT, V236 field intelligence, V237 adaptive
 * layouts, V238 operator experience and V239 professional HUD intact.
 */
class DevelopUgandaHomeActivity : AppCompatActivity() {

    private data class Module(
        val group: String,
        val title: String,
        val subtitle: String,
        val target: String,
        val accent: Int,
        val expert: Boolean = false,
        val modeExtra: String? = null
    )

    private lateinit var contentHost: LinearLayout
    private lateinit var statusLine: TextView
    private lateinit var categoryTitle: TextView

    private val ink = 0xFF06131F.toInt()
    private val panel = 0xFF0C2232.toInt()
    private val panel2 = 0xFF102B3D.toInt()
    private val white = 0xFFF4F7FB.toInt()
    private val muted = 0xFFA8B6C2.toInt()
    private val cyan = 0xFF71D6D0.toInt()
    private val gold = 0xFFE0B86E.toInt()
    private val blue = 0xFF79A9E8.toInt()
    private val green = 0xFF86C99A.toInt()
    private val red = 0xFFE47878.toInt()
    private val violet = 0xFFB59AE8.toInt()

    private val prefs by lazy {
        getSharedPreferences("develop_uganda_v240_home", Context.MODE_PRIVATE)
    }

    private var activeGroup = "ALL"

    private val modules by lazy {
        listOf(
            Module("CAMERA", "PRO CAMERA", "V239 professional HUD • LUT • format • record • clean operator view", "DevelopUgandaCameraActivity", gold),
            Module("CAMERA", "ALL PRO CAMERA", "Full professional reporting camera with the complete shared toolset", "DevelopUgandaAllProCameraActivity", cyan),
            Module("CAMERA", "SOCIAL CAMERA", "Fast social capture for Shorts, TikTok, Reels and quick posts", "DevelopUgandaSocialMediaCameraActivity", blue),
            Module("CAMERA", "AUTO DIRECTOR", "Automatic operator assistance for changing field situations", "DevelopUgandaAutoDirectorCameraActivity", violet, true),
            Module("CAMERA", "FOCUS ASSIST", "People and interview focus tools with clear focus guidance", "DevelopUgandaFocusAssistCameraActivity", green),
            Module("CAMERA", "SUBJECT METERING", "Backlight, windows and mixed-light subject metering", "DevelopUgandaMeteringLockCameraActivity", gold),
            Module("CAMERA", "HORIZON / BUILDINGS", "Architecture and straight-level composition assistance", "DevelopUgandaHorizonCameraActivity", green),
            Module("CAMERA", "STEADY / ACTION", "Walking, vehicles and motion-focused stabilization guidance", "DevelopUgandaSteadyShotCameraActivity", cyan),
            Module("CAMERA", "NIGHT INTELLIGENCE", "Low-light camera guidance and night operating tools", "DevelopUgandaNightIntelligenceCameraActivity", violet),
            Module("CAMERA", "AUDIO GUARD", "Interview and field audio monitoring with clipping awareness", "DevelopUgandaAudioGuardCameraActivity", red),
            Module("CAMERA", "VERIFIED CAMERA", "Evidence-minded capture path with integrity-oriented tools", "DevelopUgandaVerifiedCameraActivity", green, true),
            Module("CAMERA", "THERMAL SAFE", "Long-record camera with thermal-risk awareness", "DevelopUgandaThermalSafeCameraActivity", red, true),
            Module("CAMERA", "MODE SIGNATURE", "Camera mode and telemetry signature view", "DevelopUgandaModeSignatureCameraActivity", blue, true),

            Module("PHOTO", "PHOTO PRO", "Professional still-photo camera", "DevelopUgandaPhotoProCameraActivity", blue),
            Module("PHOTO", "BUILDING PHOTO", "Architecture and property photography", "DevelopUgandaBuildingPhotoCameraActivity", green),
            Module("PHOTO", "PEOPLE PHOTO", "Portrait and people photography", "DevelopUgandaPeoplePhotoCameraActivity", gold),
            Module("PHOTO", "NIGHT PHOTO", "Low-light still photography", "DevelopUgandaNightPhotoCameraActivity", violet),
            Module("PHOTO", "VERIFIED PHOTO", "Integrity-oriented still photography", "DevelopUgandaVerifiedPhotoCameraActivity", green, true),

            Module("COLOR", "COLOR / LUT STUDIO", "Uganda scene LUTs • grade strength • color families • cinematic looks", "DevelopUgandaColorStudioActivity", violet),
            Module("COLOR", "BRAND & METADATA", "Saved-video name, organization, telemetry visibility and output profile", "DevelopUgandaBrandMetadataActivity", gold),

            Module("FIELD", "SATELLITE FIELD CAMERA", "GNSS satellites • accuracy • horizon • sun direction • field intelligence", "DevelopUgandaCameraActivity", cyan, false, "FIELD INTELLIGENCE"),
            Module("FIELD", "CAMERA HEALTH", "See the real camera, lens, FPS, HDR, storage, encoder and device capabilities", "DevelopUgandaCameraHealthActivity", blue),

            Module("MEDIA", "STORY PACKAGES", "Original video • metadata • integrity • thumbnails • reporting packages", "DevelopUgandaStoryPackagesActivity", green),
            Module("MEDIA", "VIDEO EDITOR", "Open the develop.uganda video editing workspace", "DevelopUgandaEditorActivity", blue),
            Module("MEDIA", "LIVE STUDIO", "Live reporting and live-production workspace", "DevelopUgandaLiveActivity", red),
            Module("MEDIA", "NEWSROOM DESK", "The previous full newsroom home remains available here", "DevelopUgandaNewsroomActivity", gold),

            Module("SETTINGS", "CAMERA SETTINGS", "Open the professional camera and use SET CAM for camera-level controls", "DevelopUgandaCameraActivity", cyan),
            Module("SETTINGS", "COLOR SETTINGS", "LUT profiles, scene color and grade controls", "DevelopUgandaColorStudioActivity", violet),
            Module("SETTINGS", "BRAND / PRIVACY SETTINGS", "Control visible metadata, public profile and saved-video branding", "DevelopUgandaBrandMetadataActivity", gold),
            Module("SETTINGS", "DEVICE / CAMERA HEALTH", "Inspect hardware support before choosing demanding recording modes", "DevelopUgandaCameraHealthActivity", blue)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        renderModules("ALL")
        refreshFieldStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusLine.isInitialized) refreshFieldStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
        }

        root.addView(buildTopBar(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(66)
        ))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(26))
        }

        page.addView(buildHero())
        page.addView(buildFieldRail())
        page.addView(buildCategoryStrip())

        categoryTitle = text("ALL FUNCTIONS", 15f, white, true).apply {
            setPadding(dp(3), dp(15), 0, dp(8))
        }
        page.addView(categoryTitle)

        contentHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(contentHost)

        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        root.addView(buildBottomDock(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(68)
        ))

        setContentView(root)
    }

    private fun buildTopBar(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(7), dp(16), dp(7))
            setBackgroundColor(0xFF071B29.toInt())

            addView(text("develop.uganda", 22f, gold, true), LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
            })

            addView(text("V240\nHOME HUB", 10f, white, true).apply {
                gravity = Gravity.CENTER
                background = rounded(panel2, 0x5579A9E8, 14)
            }, LinearLayout.LayoutParams(dp(88), dp(46)))
        }
    }

    private fun buildHero(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(14), dp(15), dp(14))
            background = rounded(panel, 0x5571D6D0, 20)
        }

        box.addView(text("ONE HOME • EVERY CAMERA TOOL", 12f, cyan, true))
        box.addView(text(
            "Camera • LUTs • formats • satellite field intelligence • photos • live • editor • story packages • settings",
            12f, white, false
        ).apply { setPadding(0, dp(4), 0, dp(12)) })

        val quick = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        quick.addView(actionButton("OPEN\nCAMERA", gold) {
            launchDefaultQuickStart()
        }, weightParams())
        quick.addView(actionButton("YOUTUBE\n16:9", blue) {
            launchFormat(1)
        }, weightParams())
        quick.addView(actionButton("SHORTS\n9:16", red) {
            launchFormat(0)
        }, weightParams())

        box.addView(quick, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(62)
        ))
        return box
    }

    private fun buildFieldRail(): View {
        statusLine = text("FIELD STATUS • loading", 10f, muted, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(0xFF081C2A.toInt(), 0x334B6578, 13)
        }
        return statusLine
    }

    private fun buildCategoryStrip(): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(9), 0, 0)
        }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        listOf("ALL", "CAMERA", "PHOTO").forEach { group ->
            row1.addView(categoryButton(group), weightParams(dp(42)))
        }
        listOf("COLOR", "FIELD", "MEDIA", "SETTINGS").forEach { group ->
            row2.addView(categoryButton(group), weightParams(dp(42)))
        }
        wrap.addView(row1)
        wrap.addView(row2)
        return wrap
    }

    private fun categoryButton(group: String): Button =
        actionButton(group, if (group == "SETTINGS") gold else cyan) {
            renderModules(group)
        }.apply { textSize = 8.5f }

    private fun renderModules(group: String) {
        activeGroup = group
        if (!::contentHost.isInitialized) return
        contentHost.removeAllViews()

        categoryTitle.text = when (group) {
            "ALL" -> "ALL FUNCTIONS"
            "CAMERA" -> "CAMERA & VIDEO"
            "PHOTO" -> "PHOTO CAMERAS"
            "COLOR" -> "COLOR • LUT • BRAND"
            "FIELD" -> "FIELD INTELLIGENCE"
            "MEDIA" -> "MEDIA • LIVE • STORY"
            "SETTINGS" -> "SETTINGS & SYSTEM"
            else -> group
        }

        if (group == "SETTINGS") {
            contentHost.addView(buildHomePreferences())
        }

        val showExpert = prefs.getBoolean("show_expert", true)
        val visible = modules.filter {
            (group == "ALL" || it.group == group) && (showExpert || !it.expert)
        }

        visible.forEach { module ->
            contentHost.addView(moduleCard(module))
        }
    }

    private fun moduleCard(module: Module): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(11), dp(10), dp(11))
            background = rounded(panel2, module.accent and 0x55FFFFFF, 17)
            isClickable = true
            isFocusable = true
            setOnClickListener { launchModule(module) }

            val accentBar = View(this@DevelopUgandaHomeActivity).apply {
                setBackgroundColor(module.accent)
            }
            addView(accentBar, LinearLayout.LayoutParams(dp(4), dp(54)).apply {
                marginEnd = dp(11)
            })

            val copy = LinearLayout(this@DevelopUgandaHomeActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            copy.addView(text(module.title, 12f, white, true))
            copy.addView(text(module.subtitle, 9.5f, muted, false).apply {
                setPadding(0, dp(3), 0, 0)
            })
            addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            addView(text("›", 26f, module.accent, true).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(30), dp(54)))
        }.also { cardView ->
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(if (prefs.getBoolean("compact_cards", false)) 5 else 8)
            cardView.layoutParams = lp
        }
    }

    private fun buildHomePreferences(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = rounded(0xFF0B2030.toInt(), 0x55E0B86E, 18)
        }

        box.addView(text("HOME EXPERIENCE", 12f, gold, true))
        box.addView(text("These settings only organize the V240 home screen. Camera recording engines remain unchanged.", 9.5f, muted, false).apply {
            setPadding(0, dp(3), 0, dp(8))
        })

        val defaultButton = actionButton(defaultQuickStartLabel(), gold) {
            val values = listOf("PRO CAMERA", "SOCIAL CAMERA", "LIVE STUDIO", "NEWSROOM")
            val now = prefs.getString("quick_start", values[0]) ?: values[0]
            val next = values[(values.indexOf(now).coerceAtLeast(0) + 1) % values.size]
            prefs.edit().putString("quick_start", next).apply()
            renderModules(activeGroup)
        }
        box.addView(defaultButton, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        ))

        val switches = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        switches.addView(actionButton(expertLabel(), violet) {
            prefs.edit().putBoolean("show_expert", !prefs.getBoolean("show_expert", true)).apply()
            renderModules(activeGroup)
        }, weightParams(dp(48)))
        switches.addView(actionButton(densityLabel(), cyan) {
            prefs.edit().putBoolean("compact_cards", !prefs.getBoolean("compact_cards", false)).apply()
            renderModules(activeGroup)
        }, weightParams(dp(48)))
        box.addView(switches)

        return box.also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
    }

    private fun defaultQuickStartLabel(): String =
        "QUICK START • ${prefs.getString("quick_start", "PRO CAMERA") ?: "PRO CAMERA"}"

    private fun expertLabel(): String =
        "EXPERT TOOLS\n${if (prefs.getBoolean("show_expert", true)) "SHOWN" else "HIDDEN"}"

    private fun densityLabel(): String =
        "HOME DENSITY\n${if (prefs.getBoolean("compact_cards", false)) "COMPACT" else "COMFORT"}"

    private fun buildBottomDock(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(8))
            setBackgroundColor(0xFF071B29.toInt())

            addView(navButton("HOME", gold) { renderModules("ALL") }, weightParams(dp(54)))
            addView(navButton("CAMERA", cyan) { launchClass("DevelopUgandaCameraActivity") }, weightParams(dp(54)))
            addView(navButton("LIVE", red) { launchClass("DevelopUgandaLiveActivity") }, weightParams(dp(54)))
            addView(navButton("MEDIA", blue) { renderModules("MEDIA") }, weightParams(dp(54)))
            addView(navButton("SETTINGS", violet) { renderModules("SETTINGS") }, weightParams(dp(54)))
        }
    }

    private fun refreshFieldStatus() {
        try {
            val snapshot = DevelopUgandaFieldIntelligencePanel.snapshotJson(this)
            val visible = snapshot.optInt("satellites_visible", 0)
            val used = snapshot.optInt("satellites_used", 0)
            val accuracy = snapshot.opt("accuracy_m")
            val accuracyText = if (accuracy is Number && accuracy.toDouble() > 0.0) {
                "±${String.format(Locale.US, "%.0f", accuracy.toDouble())}m"
            } else "GPS --"
            val format = snapshot.optString("capture_format", "FORMAT --")
                .replace(" • ", " ")
            statusLine.text = "FIELD STATUS  •  SAT $used/$visible  •  $accuracyText  •  $format"
        } catch (_: Exception) {
            statusLine.text = "FIELD STATUS  •  SAT --  •  GPS --  •  FORMAT READY"
        }
    }

    private fun launchFormat(index: Int) {
        getSharedPreferences("develop_uganda_v236_field_intelligence", Context.MODE_PRIVATE)
            .edit()
            .putInt("format_index", index.coerceIn(0, 5))
            .apply()
        launchClass("DevelopUgandaCameraActivity")
    }

    private fun launchDefaultQuickStart() {
        when (prefs.getString("quick_start", "PRO CAMERA") ?: "PRO CAMERA") {
            "SOCIAL CAMERA" -> launchClass("DevelopUgandaSocialMediaCameraActivity")
            "LIVE STUDIO" -> launchClass("DevelopUgandaLiveActivity")
            "NEWSROOM" -> launchClass("DevelopUgandaNewsroomActivity")
            else -> launchClass("DevelopUgandaCameraActivity")
        }
    }

    private fun launchModule(module: Module) {
        launchClass(module.target, module.modeExtra)
    }

    private fun launchClass(simpleName: String, modeExtra: String? = null) {
        try {
            val pkg = packageName
            val intent = Intent().apply {
                setClassName(pkg, "$pkg.$simpleName")
                modeExtra?.let { putExtra("develop_uganda_mode", it) }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "$simpleName is not available in this build", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionButton(label: String, accent: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 9f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            isAllCaps = false
            setPadding(dp(6), dp(3), dp(6), dp(3))
            background = rounded(0xFF102B3D.toInt(), accent, 15)
            setOnClickListener { action() }
        }

    private fun navButton(label: String, accent: Int, action: () -> Unit): Button =
        actionButton(label, accent, action).apply { textSize = 8.5f }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun rounded(fill: Int, stroke: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun weightParams(height: Int = dp(52)): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, height, 1f).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
'''

HOME.write_text(HOME_SOURCE, encoding="utf-8")

# Update app version safely. V240.1 is the Home Hub + camera recovery build.
gradle = GRADLE.read_text(encoding="utf-8")
if 'versionCode = 24010' not in gradle:
    if 'versionCode = 24000' in gradle:
        gradle = gradle.replace('versionCode = 24000', 'versionCode = 24010', 1)
        gradle = gradle.replace('versionName = "240.0-home-hub"', 'versionName = "240.1-home-hub-recovery"', 1)
    elif 'versionCode = 23900' in gradle:
        gradle = gradle.replace('versionCode = 23900', 'versionCode = 24010', 1)
        gradle = gradle.replace('versionName = "239.0-professional-camera-hud"', 'versionName = "240.1-home-hub-recovery"', 1)
    else:
        raise SystemExit("V240 recovery expects V239 or V240 source as its base")
GRADLE.write_text(gradle, encoding="utf-8")

# V240.1 CAMERA RECOVERY
# Keep the V239 HUD source installed, but default it OFF. This returns the
# shooting screen to the already-proven V237/V238 operator UI path.
prohud = PROHUD.read_text(encoding="utf-8")
guard_marker = "develop_uganda_v240_enable_v239_hud"
if guard_marker not in prohud:
    body_open = """        onZoomRatio: (Float) -> Unit
    ) {
        if (root.findViewWithTag<View>(TAG) != null) return
"""
    guarded = """        onZoomRatio: (Float) -> Unit
    ) {
        val v240HudEnabled =
            activity.getSharedPreferences(
                "develop_uganda_v240_home",
                android.content.Context.MODE_PRIVATE
            ).getBoolean(
                "develop_uganda_v240_enable_v239_hud",
                false
            )

        if (!v240HudEnabled) return
        if (root.findViewWithTag<View>(TAG) != null) return
"""
    if body_open not in prohud:
        raise SystemExit("V240 recovery could not locate V239 HUD attach() body")
    prohud = prohud.replace(body_open, guarded, 1)
PROHUD.write_text(prohud, encoding="utf-8")

# Make V240 Home the launcher while retaining the full old newsroom as a module.
manifest = MANIFEST.read_text(encoding="utf-8")
if 'android:name=".DevelopUgandaHomeActivity"' not in manifest:
    old = '''        <activity\n            android:name=".DevelopUgandaNewsroomActivity"\n            android:screenOrientation="portrait"\n            android:exported="true">\n\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN"/>\n                <category android:name="android.intent.category.LAUNCHER"/>\n            </intent-filter>\n\n        </activity>'''
    new = '''        <activity\n            android:name=".DevelopUgandaHomeActivity"\n            android:screenOrientation="portrait"\n            android:exported="true">\n\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN"/>\n                <category android:name="android.intent.category.LAUNCHER"/>\n            </intent-filter>\n\n        </activity>\n\n        <activity\n            android:name=".DevelopUgandaNewsroomActivity"\n            android:screenOrientation="portrait"\n            android:exported="false"/>'''
    if old not in manifest:
        raise SystemExit("V240 could not find the existing launcher block")
    manifest = manifest.replace(old, new, 1)
MANIFEST.write_text(manifest, encoding="utf-8")

# Story packages should identify the new application build.
story = STORY.read_text(encoding="utf-8")
for old_version in ['V239', 'V238', 'V237', 'V236']:
    story = story.replace(f'.put("app_version", "{old_version}")', '.put("app_version", "V240")', 1)
    if '.put("app_version", "V240")' in story:
        break
STORY.write_text(story, encoding="utf-8")

README.write_text(
    """develop.uganda V240 — HOME HUB

A new launcher/home page that brings the full develop.uganda system into one organized place.

HOME GROUPS
• Camera & video
• Photo cameras
• Color / LUT / brand
• Field intelligence
• Media / live / story
• Settings & system

QUICK START
• Open Camera
• YouTube 16:9
• Shorts / TikTok / Reels 9:16

HOME SETTINGS
• Choose default quick-start destination
• Show/hide expert tools
• Compact/comfortable card density

PROTECTED
V240.1 keeps LUT/GNSS/adaptive/operator engines. The V239 HUD source remains installed but defaults OFF to recover the blank-screen issue.
The previous Newsroom home remains available as NEWSROOM DESK.
""",
    encoding="utf-8"
)

print("PASS: V240.1 Home Hub + camera recovery patch applied")
