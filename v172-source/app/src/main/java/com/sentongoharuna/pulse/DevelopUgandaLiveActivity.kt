package com.sentongoharuna.pulse

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Environment
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.effects.Frame
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

class DevelopUgandaLiveActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var previewView: PreviewView

    private lateinit var liveBadge: TextView
    private lateinit var liveTitle: TextView
    private lateinit var liveSubTitle: TextView
    private lateinit var livePreviewMeta: TextView
    private lateinit var livePreviewTech: TextView
    private lateinit var netLamp: TextView
    private lateinit var gpsLamp: TextView
    private lateinit var micLamp: TextView
    private lateinit var camLamp: TextView
    private lateinit var recLamp: TextView
    private lateinit var batteryLamp: TextView

    private lateinit var profileButton: Button
    private lateinit var qualityButton: Button
    private lateinit var audioButton: Button
    private lateinit var graphicsButton: Button
    private lateinit var lensButton: Button
    private lateinit var lightButton: Button
    private lateinit var outputButton: Button
    private lateinit var viewModeButton: Button
    private lateinit var settingsButton: Button
    private lateinit var identityButton: Button
    private lateinit var resetButton: Button
    private lateinit var countdownButton: Button
    private lateinit var markButton: Button
    private lateinit var styleButton: Button
    private lateinit var liveLockButton: Button
    private lateinit var liveHudSizeButton: Button
    private lateinit var liveHudContrastButton: Button
    private lateinit var liveHudBackingButton: Button
    private lateinit var liveEffectButton: Button
    private lateinit var livePresetButton: Button
    private lateinit var liveSafeInfoButton: Button
    private lateinit var countdownView: TextView

    private val liveHudLabels =
        arrayOf(
            "COMPACT",
            "STANDARD",
            "LARGE"
        )

    private val liveHudScales =
        floatArrayOf(
            1.04f,
            1.16f,
            1.28f
        )

    private var liveHudSizeIndex = 1

    private val liveHudContrastLabels =
        arrayOf(
            "SOFT",
            "BALANCED",
            "STRONG"
        )

    private var liveHudContrastIndex = 1

    private val liveHudBackingLabels =
        arrayOf(
            "NONE",
            "SOFT",
            "STRONG"
        )

    private var liveHudBackingIndex = 1

    private val liveEffectLabels =
        arrayOf(
            "CLEAN",
            "NATURAL",
            "WARM",
            "COOL",
            "TEAL",
            "GOLD",
            "SOFT",
            "NIGHT"
        )

    private var liveEffectIndex = 0

    private val livePresetLabels =
        arrayOf(
            "CUSTOM",
            "BREAKING",
            "INTERVIEW",
            "EVENT",
            "COMMUNITY"
        )

    private var livePresetIndex = 0
    private lateinit var recordButton: LiveRecordButtonView

    private lateinit var timerView: TextView
    private lateinit var outputStatus: TextView

    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var overlayEffect: OverlayEffect? = null

    private var useFront = false
    private var audioEnabled = true
    private var graphicsEnabled = true

    private var quality = Quality.FHD
    private var qualityLabel = "FHD"

    private val profiles =
        arrayOf(
            "BREAKING",
            "INTERVIEW",
            "EVENT",
            "TRAFFIC",
            "COMMUNITY"
        )

    private var profileIndex = 0
    private var halfPreviewMode = false

    private val lowerThirdStyles =
        arrayOf(
            "BREAKING",
            "CLEAN",
            "URGENT",
            "MINIMAL"
        )

    private var lowerThirdStyleIndex = 0
    private var countdownEnabled = true
    private var countdownRunning = false
    private var liveControlsLocked = false
    private val liveMarkers =
        mutableListOf<Long>()

    private var recordStartMs = 0L
    private var liveRecordingName = ""
    private var liveAudioAmplitude = 0.0
    private var liveBlinkOn = true

    private var reporterName = "CITIZEN"
    private var storyId = "--"
    private var headline = "LIVE REPORT"

    private val uiHandler =
        Handler(Looper.getMainLooper())

    private val uiTicker =
        object : Runnable {
            override fun run() {
                updateSignals()
                updateTimer()
                updateBlink()

                uiHandler.postDelayed(
                    this,
                    500L
                )
            }
        }

    private val red = 0xFFC76D73.toInt()
    private val green = 0xFF91B6A0.toInt()
    private val amber = 0xFFAEBDEB.toInt()
    private val cyan = 0xFF8FA8E8.toInt()
    private val white = 0xFFF1F3F8.toInt()
    private val panel = 0xD9082236.toInt()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        loadLiveIdentity()
        loadLiveCameraPreferences()
        buildLiveUi()
        requestNeededPermissions()

        uiHandler.post(
            uiTicker
        )
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(
            null
        )

        recording?.stop()
        recording = null

        super.onDestroy()
    }

    private fun loadLiveIdentity() {
        val reporterPrefs =
            getSharedPreferences(
                "develop_uganda_reporter",
                Context.MODE_PRIVATE
            )

        reporterName =
            reporterPrefs.getString(
                "reporter_name",
                "CITIZEN"
            )
                ?.trim()
                ?.ifBlank {
                    "CITIZEN"
                }
                ?: "CITIZEN"

        storyId =
            reporterPrefs.getString(
                "story_id",
                "--"
            )
                ?.trim()
                ?.ifBlank {
                    "--"
                }
                ?: "--"

        val newsroomPrefs =
            getSharedPreferences(
                "develop_uganda_newsroom",
                Context.MODE_PRIVATE
            )

        headline =
            newsroomPrefs.getString(
                "headline",
                "LIVE REPORT"
            )
                ?.trim()
                ?.ifBlank {
                    "LIVE REPORT"
                }
                ?: "LIVE REPORT"
    }

    private fun buildLiveUi() {
        root =
            FrameLayout(this).apply {
                setBackgroundColor(
                    Color.BLACK
                )
            }

        previewView =
            PreviewView(this).apply {
                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE

                scaleType =
                    PreviewView.ScaleType.FILL_CENTER
            }

        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val topPanel =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(14),
                    dp(10),
                    dp(14),
                    dp(10)
                )

                background =
                    rounded(
                        0x3A000000,
                        Color.TRANSPARENT,
                        0
                    )
            }

        val header =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        liveBadge =
            label(
                "● LIVE",
                18f,
                red,
                true
            )

        liveTitle =
            label(
                "develop.uganda",
                20f,
                amber,
                true
            ).apply {
                setPadding(
                    dp(12),
                    0,
                    0,
                    0
                )
            }

        header.addView(
            liveBadge
        )

        header.addView(
            liveTitle
        )

        topPanel.addView(
            header
        )

        liveSubTitle =
            label(
                "LIVE STUDIO • ${profiles[profileIndex]} • READY",
                10f,
                white,
                true
            ).apply {
                setPadding(
                    0,
                    dp(3),
                    0,
                    dp(7)
                )
            }

        topPanel.addView(
            liveSubTitle
        )

        livePreviewMeta =
            label(
                "$reporterName • STORY $storyId • $headline",
                9f,
                white,
                true
            ).apply {
                maxLines = 1
                isSingleLine = true
            }

        livePreviewTech =
            label(
                "TC 00:00:00 • $qualityLabel • MIC ON",
                8.5f,
                cyan,
                true
            ).apply {
                maxLines = 1
                isSingleLine = true

                setPadding(
                    0,
                    dp(2),
                    0,
                    dp(3)
                )
            }

        topPanel.addView(
            livePreviewMeta
        )

        topPanel.addView(
            livePreviewTech
        )

        val signalRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        netLamp =
            signal("● NET")

        gpsLamp =
            signal("● GPS")

        micLamp =
            signal("● MIC")

        camLamp =
            signal("● CAM")

        recLamp =
            signal("● REC")

        batteryLamp =
            signal("● BAT")

        listOf(
            netLamp,
            gpsLamp,
            micLamp,
            camLamp,
            recLamp,
            batteryLamp
        ).forEach {
            signalRow.addView(
                it,
                LinearLayout.LayoutParams(
                    0,
                    dp(26),
                    1f
                )
            )
        }

        topPanel.addView(
            signalRow
        )

        timerView =
            label(
                "00:00:00",
                13f,
                white,
                true
            ).apply {
                typeface =
                    Typeface.MONOSPACE

                setPadding(
                    0,
                    dp(5),
                    0,
                    0
                )
            }

        topPanel.addView(
            timerView
        )

        val topPanelParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity =
                    Gravity.TOP

                // Screen-only safety inset. Recorded graphics keep their own
                // output-safe geometry and are not moved by this value.
                topMargin =
                    dp(42)

                leftMargin =
                    dp(8)

                rightMargin =
                    dp(8)
            }

        root.addView(
            topPanel,
            topPanelParams
        )

        countdownView =
            label(
                "",
                72f,
                white,
                true
            ).apply {
                gravity =
                    Gravity.CENTER

                visibility =
                    View.GONE

                setShadowLayer(
                    18f,
                    0f,
                    0f,
                    red
                )
            }

        root.addView(
            countdownView,
            FrameLayout.LayoutParams(
                dp(190),
                dp(190),
                Gravity.CENTER
            )
        )

        val liveDeck =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(10),
                    dp(9),
                    dp(10),
                    dp(10)
                )

                background =
                    rounded(
                        panel,
                        Color.TRANSPARENT,
                        24
                    )
            }

        outputStatus =
            label(
                "OUTPUT • LOCAL LIVE CAPTURE • STREAM DESTINATION NOT CONNECTED",
                9f,
                0xFFAEB7C7.toInt(),
                true
            ).apply {
                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }

        liveDeck.addView(
            outputStatus
        )

        val row1 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        profileButton =
            liveSettingButton(
                "PROFILE ▾\n${profiles[profileIndex]}",
                red
            ) {
                showLiveProfileDropdown(
                    profileButton
                )
            }

        qualityButton =
            liveSettingButton(
                "QUALITY ▾\nFHD",
                cyan
            ) {
                showLiveQualityDropdown(
                    qualityButton
                )
            }

        audioButton =
            liveSettingButton(
                "AUDIO ▾\nON",
                green
            ) {
                showLiveAudioDropdown(
                    audioButton
                )
            }

        row1.addView(
            profileButton,
            weight()
        )

        row1.addView(
            qualityButton,
            weight()
        )

        row1.addView(
            audioButton,
            weight()
        )

        liveDeck.addView(
            row1
        )

        val row2 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(6),
                    0,
                    0
                )
            }

        graphicsButton =
            liveSettingButton(
                "GRAPHICS ▾\nON",
                amber
            ) {
                showLiveGraphicsDropdown(
                    graphicsButton
                )
            }

        lensButton =
            liveSettingButton(
                "LENS ▾\nBACK",
                cyan
            ) {
                showLiveLensDropdown(
                    lensButton
                )
            }

        lightButton =
            liveSettingButton(
                "LIGHT ▾\nOFF",
                white
            ) {
                showLiveLightDropdown(
                    lightButton
                )
            }

        row2.addView(
            graphicsButton,
            weight()
        )

        row2.addView(
            lensButton,
            weight()
        )

        row2.addView(
            lightButton,
            weight()
        )

        liveDeck.addView(
            row2
        )

        val row3 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(6),
                    0,
                    0
                )
            }

        outputButton =
            liveSettingButton(
                "OUTPUT\nSETUP",
                red
            ) {
                showOutputSetup()
            }

        val headlineButton =
            liveSettingButton(
                "LOWER THIRD\nEDIT",
                amber
            ) {
                showLowerThirdEditor()
            }

        val infoButton =
            liveSettingButton(
                "SIGNALS\nINFO",
                green
            ) {
                showSignalInfo()
            }

        row3.addView(
            outputButton,
            weight()
        )

        row3.addView(
            headlineButton,
            weight()
        )

        row3.addView(
            infoButton,
            weight()
        )

        liveDeck.addView(
            row3
        )

        val row4 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(5),
                    0,
                    0
                )
            }

        viewModeButton =
            liveSettingButton(
                "VIEW ▾\nFULL",
                green
            ) {
                showLiveViewDropdown(
                    viewModeButton
                )
            }

        settingsButton =
            liveSettingButton(
                "SETTINGS\nLIVE",
                cyan
            ) {
                showLiveDetailedSettings()
            }

        identityButton =
            liveSettingButton(
                "REPORTER\nID",
                amber
            ) {
                showLiveIdentityEditor()
            }

        resetButton =
            liveSettingButton(
                "RESET\nLIVE",
                red
            ) {
                resetLiveSettings()
            }

        listOf(
            viewModeButton,
            settingsButton,
            identityButton,
            resetButton
        ).forEachIndexed { index, button ->
            row4.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(42),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart = dp(4)
                    }
                }
            )
        }

        liveDeck.addView(
            row4
        )

        val row5 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        countdownButton =
            liveSettingButton(
                "COUNTDOWN ▾\n3 SEC",
                amber
            ) {
                showLiveCountdownDropdown(
                    countdownButton
                )
            }.apply {
                isSelected =
                    true
            }

        markButton =
            liveSettingButton(
                "MARK\n0",
                cyan
            ) {
                addLiveMarker()
            }

        styleButton =
            liveSettingButton(
                "LOWER STYLE ▾\n${lowerThirdStyles[lowerThirdStyleIndex]}",
                red
            ) {
                showLiveStyleDropdown(
                    styleButton
                )
            }

        liveLockButton =
            liveSettingButton(
                "LOCK\nOFF",
                green
            ) {
                liveControlsLocked =
                    !liveControlsLocked

                liveLockButton.text =
                    "LOCK\n" +
                        if (liveControlsLocked) {
                            "ON"
                        } else {
                            "OFF"
                        }

                liveLockButton.isSelected =
                    liveControlsLocked
            }

        listOf(
            countdownButton,
            markButton,
            styleButton,
            liveLockButton
        ).forEachIndexed { index, button ->
            row5.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(39),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(4)
                    }
                }
            )
        }

        liveDeck.addView(
            row5
        )

        val displayRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(5),
                    0,
                    0
                )
            }

        liveHudSizeButton =
            liveSettingButton(
                "HUD SIZE ▾\n${liveHudLabels[liveHudSizeIndex]}",
                0xFF6D88A4.toInt()
            ) {
                showLiveHudSizeDropdown(
                    liveHudSizeButton
                )
            }.apply {
                isSelected =
                    true
            }

        liveSafeInfoButton =
            liveSettingButton(
                "OUTPUT\nSAFE",
                0xFF8B9499.toInt()
            ) {
                showLiveSafeAreaInfo()
            }

        liveHudContrastButton =
            liveSettingButton(
                "HUD CONTRAST ▾\n${liveHudContrastLabels[liveHudContrastIndex]}",
                0xFF83799A.toInt()
            ) {
                showLiveHudContrastDropdown(
                    liveHudContrastButton
                )
            }.apply {
                isSelected =
                    true
            }

        livePresetButton =
            liveSettingButton(
                "PRESET ▾\n${livePresetLabels[livePresetIndex]}",
                0xFF8B9499.toInt()
            ) {
                showLivePresetDropdown(
                    livePresetButton
                )
            }.apply {
                isSelected =
                    livePresetIndex !=
                        0
            }

        listOf(
            liveHudSizeButton,
            liveHudContrastButton,
            livePresetButton
        ).forEachIndexed { index, button ->
            displayRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(40),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(6)
                    }
                }
            )
        }

        liveDeck.addView(
            displayRow
        )

        val outputRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(5),
                    0,
                    0
                )
            }

        liveHudBackingButton =
            liveSettingButton(
                "HUD BACKING ▾\n${liveHudBackingLabels[liveHudBackingIndex]}",
                0xFF6F9C7C.toInt()
            ) {
                showLiveHudBackingDropdown(
                    liveHudBackingButton
                )
            }.apply {
                isSelected =
                    liveHudBackingIndex !=
                        0
            }

        liveEffectButton =
            liveSettingButton(
                "VIDEO FX ▾\n${liveEffectLabels[liveEffectIndex]}",
                0xFF6D88A4.toInt()
            ) {
                showLiveEffectDropdown(
                    liveEffectButton
                )
            }.apply {
                isSelected =
                    liveEffectIndex !=
                        0
            }

        listOf(
            liveHudBackingButton,
            liveEffectButton,
            liveSafeInfoButton
        ).forEachIndexed { index, button ->
            outputRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(40),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(6)
                    }
                }
            )
        }

        liveDeck.addView(
            outputRow
        )

        val recordArea =
            FrameLayout(this).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    0
                )
            }

        recordButton =
            LiveRecordButtonView(
                this
            ).apply {
                setOnClickListener {
                    if (
                        recording != null
                    ) {
                        toggleRecording()
                    } else {
                        beginLiveRecordSequence()
                    }
                }
            }

        recordArea.addView(
            recordButton,
            FrameLayout.LayoutParams(
                dp(108),
                dp(108),
                Gravity.CENTER
            )
        )

        liveDeck.addView(
            recordArea,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(118)
            )
        )

        root.addView(
            liveDeck,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        setContentView(
            root
        )
    }

    private fun togglePreviewMode() {
        halfPreviewMode =
            !halfPreviewMode

        val height =
            if (halfPreviewMode) {
                (
                    resources.displayMetrics.heightPixels *
                        0.50f
                    ).roundToInt()
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }

        val params =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            ).apply {
                gravity = Gravity.TOP
            }

        previewView.layoutParams =
            params

        previewView.scaleType =
            if (halfPreviewMode) {
                PreviewView.ScaleType.FIT_CENTER
            } else {
                PreviewView.ScaleType.FILL_CENTER
            }

        viewModeButton.text =
            "VIEW ▾\n" +
                if (halfPreviewMode) {
                    "HALF"
                } else {
                    "FULL"
                }

        // The top narration is screen UI. Keep it inside the visible camera
        // area in either mode without changing the saved-video overlay.
        val top =
            liveBadge.parent
                ?.parent as?
                View

        val topLayout =
            top?.layoutParams as?
                FrameLayout.LayoutParams

        if (topLayout != null) {
            topLayout.topMargin =
                if (halfPreviewMode) {
                    dp(32)
                } else {
                    dp(42)
                }

            top.layoutParams =
                topLayout
        }

        toast(
            if (halfPreviewMode) {
                "Half-screen LIVE view"
            } else {
                "Full-screen LIVE camera behind controls"
            }
        )
    }

    private fun showLiveIdentityEditor() {
        val box =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(8),
                    dp(18),
                    dp(4)
                )
            }

        val reporterField =
            EditText(this).apply {
                hint = "Reporter name"
                setText(reporterName)
            }

        val storyField =
            EditText(this).apply {
                hint = "Story ID"
                setText(storyId)
            }

        box.addView(reporterField)
        box.addView(storyField)

        AlertDialog.Builder(this)
            .setTitle("LIVE REPORTER ID")
            .setView(box)
            .setPositiveButton(
                "SAVE"
            ) { _, _ ->
                reporterName =
                    reporterField.text
                        .toString()
                        .trim()
                        .ifBlank {
                            "CITIZEN"
                        }

                storyId =
                    storyField.text
                        .toString()
                        .trim()
                        .ifBlank {
                            "--"
                        }

                getSharedPreferences(
                    "develop_uganda_reporter",
                    Context.MODE_PRIVATE
                )
                    .edit()
                    .putString(
                        "reporter_name",
                        reporterName
                    )
                    .putString(
                        "story_id",
                        storyId
                    )
                    .apply()

                toast("LIVE reporter identity saved")
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    private fun resetLiveSettings() {
        if (recording != null) {
            toast("Stop LIVE REC before reset")
            return
        }

        profileIndex = 0
        quality = Quality.FHD
        qualityLabel = "FHD"
        audioEnabled = true
        graphicsEnabled = true
        useFront = false
        halfPreviewMode = false

        profileButton.text =
            "PROFILE\\n${profiles[profileIndex]}"

        qualityButton.text =
            "QUALITY\\nFHD"

        audioButton.text =
            "AUDIO\\nON"

        graphicsButton.text =
            "GRAPHICS\\nON"

        lensButton.text =
            "LENS\\nBACK"

        viewModeButton.text =
            "VIEW\\nFULL"

        previewView.layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.TOP
            }

        previewView.scaleType =
            PreviewView.ScaleType.FILL_CENTER

        bindCamera()
        toast("LIVE STUDIO reset")
    }

    private fun showLiveDetailedSettings() {
        val streamPrefs =
            getSharedPreferences(
                "develop_uganda_stream",
                Context.MODE_PRIVATE
            )

        val destination =
            streamPrefs.getString(
                "url",
                ""
            )
                ?.trim()
                .orEmpty()

        val summary =
            buildString {
                append("LIVE-ONLY SETTINGS • V188\n\n")

                append("PROFILE: ")
                append(
                    profiles[
                        profileIndex
                    ]
                )
                append("\n")

                append("QUALITY: ")
                append(
                    qualityLabel
                )
                append("\n")

                append("AUDIO: ")
                append(
                    if (audioEnabled) {
                        "ON"
                    } else {
                        "OFF"
                    }
                )
                append("\n")

                append("GRAPHICS: ")
                append(
                    if (graphicsEnabled) {
                        "ON"
                    } else {
                        "OFF"
                    }
                )
                append("\n")

                append("LENS: ")
                append(
                    if (useFront) {
                        "FRONT"
                    } else {
                        "BACK"
                    }
                )
                append("\n")

                append("COUNTDOWN: ")
                append(
                    if (countdownEnabled) {
                        "3 SEC"
                    } else {
                        "OFF"
                    }
                )
                append("\n")

                append("HUD SIZE: ")
                append(
                    liveHudLabels[
                        liveHudSizeIndex
                    ]
                )
                append("\n")

                append("HUD CONTRAST: ")
                append(
                    liveHudContrastLabels[
                        liveHudContrastIndex
                    ]
                )
                append("\n")

                append("HUD BACKING: ")
                append(
                    liveHudBackingLabels[
                        liveHudBackingIndex
                    ]
                )
                append("\n")

                append("VIDEO FX: ")
                append(
                    liveEffectLabels[
                        liveEffectIndex
                    ]
                )
                append("\n")

                append("SOCIAL CAMERA: FHD PREFERRED • 20Mbps @ FHD • DEVICE AE/AF/AWB\n")

                append("PRESET: ")
                append(
                    livePresetLabels[
                        livePresetIndex
                    ]
                )
                append("\n")

                append("SETTINGS MEMORY: ON\n")

                append("LOWER STYLE: ")
                append(
                    lowerThirdStyles[
                        lowerThirdStyleIndex
                    ]
                )
                append("\n")

                append("MARKERS: ")
                append(
                    liveMarkers.size
                )
                append("\n")

                append("PREVIEW: ")
                append(
                    if (halfPreviewMode) {
                        "HALF SCREEN"
                    } else {
                        "FULL SCREEN"
                    }
                )
                append("\n")

                append("LOWER THIRD: ")
                append(
                    headline.ifBlank {
                        "LIVE REPORT"
                    }
                )
                append("\n")

                append("OUTPUT: ")
                append(
                    if (destination.isBlank()) {
                        "LOCAL GALLERY"
                    } else {
                        "DESTINATION SAVED"
                    }
                )
                append("\n\n")

                append("SIGNALS: NET • GPS • MIC • CAM • REC • BAT\n")
                append("RECORD GRAPHICS: develop.uganda LIVE + profile + reporter + story + lower third\n")
                append("STREAMING: destination may be stored, but internet streaming remains inactive until an RTMP/SRT/WebRTC engine is connected.")
            }

        AlertDialog.Builder(this)
            .setTitle(
                "LIVE STUDIO SETTINGS"
            )
            .setMessage(
                summary
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun requestNeededPermissions() {
        val wanted =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        val missing =
            wanted.filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) !=
                    PackageManager.PERMISSION_GRANTED
            }

        if (
            missing.isEmpty()
        ) {
            bindCamera()
        } else {
            requestPermissions(
                missing.toTypedArray(),
                1841
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            1841
        ) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                bindCamera()
            } else {
                toast(
                    "Camera permission is required"
                )
            }
        }
    }

    private fun bindCamera() {
        val future =
            ProcessCameraProvider.getInstance(
                this
            )

        future.addListener(
            {
                val provider =
                    future.get()

                provider.unbindAll()

                val preview =
                    Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(
                                previewView.surfaceProvider
                            )
                        }

                val recorder =
                    Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                quality
                            )
                        )
                        .setTargetVideoEncodingBitRate(
                            if (
                                quality ==
                                Quality.FHD
                            ) {
                                20_000_000
                            } else {
                                10_000_000
                            }
                        )
                        .build()

                videoCapture =
                    VideoCapture.withOutput(
                        recorder
                    )

                // V187: burn-in graphics target the saved VIDEO only.
                // Preview narration is native screen UI, so it cannot be
                // clipped by CameraX preview crop/scale transforms.
                overlayEffect =
                    OverlayEffect(
                        CameraEffect.VIDEO_CAPTURE,
                        0,
                        Handler(
                            Looper.getMainLooper()
                        )
                    ) { throwable ->
                        toast(
                            "LIVE graphics warning: " +
                                (
                                    throwable.message
                                        ?: "unknown"
                                    )
                        )
                    }.also { effect ->
                        effect.setOnDrawListener { frame ->
                            drawLiveBroadcastOverlay(
                                frame
                            )
                            true
                        }
                    }

                val session =
                    SessionConfig.Builder(
                        preview,
                        videoCapture!!
                    )
                        .addEffect(
                            overlayEffect!!
                        )
                        .build()

                val selector =
                    if (useFront) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                camera =
                    provider.bindToLifecycle(
                        this,
                        selector,
                        session
                    )

                // V200 social camera tuning: keep CameraX/device automatic
                // exposure, autofocus and white balance, while ensuring
                // compensation starts at a neutral supported value.
                camera
                    ?.cameraInfo
                    ?.exposureState
                    ?.let { exposure ->
                        if (
                            exposure.isExposureCompensationSupported
                        ) {
                            camera
                                ?.cameraControl
                                ?.setExposureCompensationIndex(
                                    0.coerceIn(
                                        exposure.exposureCompensationRange.lower,
                                        exposure.exposureCompensationRange.upper
                                    )
                                )
                        }
                    }

                camLamp.setTextColor(
                    green
                )
            },
            ContextCompat.getMainExecutor(
                this
            )
        )
    }

    private fun drawLiveVideoEffect(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {
        val color =
            when (
                liveEffectLabels[
                    liveEffectIndex
                ]
            ) {
                "NATURAL" ->
                    0x05FFF4E8

                "WARM" ->
                    0x0BFF9555

                "COOL" ->
                    0x0A3E7EFF

                "TEAL" ->
                    0x0B00A7A1

                "GOLD" ->
                    0x0CF2B43C

                "SOFT" ->
                    0x08FFFFFF

                "NIGHT" ->
                    0x12092346

                else ->
                    Color.TRANSPARENT
            }

        if (
            color ==
            Color.TRANSPARENT
        ) {
            return
        }

        val grade =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                this.color =
                    color
            }

        canvas.drawRect(
            0f,
            0f,
            width,
            height,
            grade
        )
    }

    private fun drawLiveBroadcastOverlay(
        frame: Frame
    ) {
        val canvas =
            frame.overlayCanvas

        val crop =
            frame.cropRect

        if (
            crop.width() <= 0 ||
            crop.height() <= 0
        ) {
            return
        }

        canvas.drawColor(
            Color.TRANSPARENT,
            android.graphics.PorterDuff.Mode.CLEAR
        )

        val rotation =
            (
                (
                    frame.rotationDegrees %
                        360
                    ) +
                    360
                ) %
                360

        val finalWidth =
            if (
                rotation == 90 ||
                rotation == 270
            ) {
                crop.height().toFloat()
            } else {
                crop.width().toFloat()
            }

        val finalHeight =
            if (
                rotation == 90 ||
                rotation == 270
            ) {
                crop.width().toFloat()
            } else {
                crop.height().toFloat()
            }

        val l =
            crop.left.toFloat()
        val t =
            crop.top.toFloat()
        val r =
            crop.right.toFloat()
        val b =
            crop.bottom.toFloat()

        val nonMirrored =
            when (
                rotation
            ) {
                90 ->
                    floatArrayOf(
                        l, b,
                        l, t,
                        r, t,
                        r, b
                    )

                180 ->
                    floatArrayOf(
                        r, b,
                        l, b,
                        l, t,
                        r, t
                    )

                270 ->
                    floatArrayOf(
                        r, t,
                        r, b,
                        l, b,
                        l, t
                    )

                else ->
                    floatArrayOf(
                        l, t,
                        r, t,
                        r, b,
                        l, b
                    )
            }

        val destination =
            if (
                frame.isMirroring
            ) {
                floatArrayOf(
                    nonMirrored[2],
                    nonMirrored[3],
                    nonMirrored[0],
                    nonMirrored[1],
                    nonMirrored[6],
                    nonMirrored[7],
                    nonMirrored[4],
                    nonMirrored[5]
                )
            } else {
                nonMirrored
            }

        val source =
            floatArrayOf(
                0f,
                0f,
                finalWidth,
                0f,
                finalWidth,
                finalHeight,
                0f,
                finalHeight
            )

        val matrix =
            Matrix()

        if (
            !matrix.setPolyToPoly(
                source,
                0,
                destination,
                0,
                4
            )
        ) {
            return
        }

        canvas.save()
        canvas.concat(
            matrix
        )

        drawLiveVideoEffect(
            canvas,
            finalWidth,
            finalHeight
        )

        if (
            !graphicsEnabled
        ) {
            canvas.restore()
            return
        }

        val u =
            minOf(
                finalWidth,
                finalHeight
            ) /
                1000f *
                liveHudScales[
                    liveHudSizeIndex
                ]

        // V186: conservative social-safe margins. V184/V185 used 8% and
        // the CameraX crop could place the left side outside the saved MP4.
        val safeLeft =
            finalWidth *
                0.16f

        val safeTop =
            finalHeight *
                0.075f

        val safeRight =
            finalWidth *
                0.84f

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                typeface =
                    Typeface.create(
                        Typeface.MONOSPACE,
                        Typeface.BOLD
                    )

                setShadowLayer(
                    liveHudShadowRadius(
                        u
                    ),
                    0.6f * u,
                    0.6f * u,
                    liveHudOutlineColor()
                )
            }

        val liveOn =
            recording != null

        val blink =
            (
                SystemClock.elapsedRealtime() /
                    500L
                ) %
                2L ==
                0L

        val livePaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    red

                alpha =
                    if (
                        liveOn &&
                        !blink
                    ) {
                        90
                    } else {
                        255
                    }

                style =
                    Paint.Style.FILL
            }

        canvas.drawCircle(
            safeLeft +
                (9f * u),
            safeTop -
                (6f * u),
            6f * u,
            livePaint
        )

        if (
            liveOn
        ) {
            val badgeAlpha =
                if (
                    blink
                ) {
                    245
                } else {
                    110
                }

            val badgeLeft =
                safeRight -
                    (178f * u)

            val badgeTop =
                safeTop -
                    (28f * u)

            val badgeRight =
                safeRight

            val badgeBottom =
                safeTop +
                    (10f * u)

            val badgeGlow =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color =
                        Color.argb(
                            if (blink) 95 else 38,
                            255,
                            59,
                            50
                        )

                    style =
                        Paint.Style.FILL
                }

            canvas.drawRoundRect(
                badgeLeft -
                    (6f * u),
                badgeTop -
                    (6f * u),
                badgeRight +
                    (2f * u),
                badgeBottom +
                    (6f * u),
                14f * u,
                14f * u,
                badgeGlow
            )

            val badgeBackground =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color =
                        Color.argb(
                            badgeAlpha,
                            184,
                            48,
                            44
                        )

                    style =
                        Paint.Style.FILL
                }

            canvas.drawRoundRect(
                badgeLeft,
                badgeTop,
                badgeRight,
                badgeBottom,
                12f * u,
                12f * u,
                badgeBackground
            )

            val signalDot =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color =
                        Color.argb(
                            if (blink) 255 else 145,
                            255,
                            255,
                            255
                        )

                    style =
                        Paint.Style.FILL
                }

            canvas.drawCircle(
                badgeLeft +
                    (15f * u),
                safeTop -
                    (8f * u),
                5.2f * u,
                signalDot
            )

            val pulseRing =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color =
                        Color.argb(
                            if (blink) 235 else 110,
                            255,
                            255,
                            255
                        )

                    style =
                        Paint.Style.STROKE

                    strokeWidth =
                        1.6f * u
                }

            canvas.drawCircle(
                badgeLeft +
                    (15f * u),
                safeTop -
                    (8f * u),
                if (blink) 9.5f * u else 7.2f * u,
                pulseRing
            )

            val badgeText =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color =
                        Color.argb(
                            if (blink) 255 else 168,
                            255,
                            255,
                            255
                        )

                    textSize =
                        14.8f *
                            u

                    typeface =
                        Typeface.create(
                            Typeface.MONOSPACE,
                            Typeface.BOLD
                        )
                }

            canvas.drawText(
                "LIVE FEED  REC",
                badgeLeft +
                    (28f * u),
                safeTop -
                    (2f * u),
                badgeText
            )
        }

        paint.textSize =
            38f * u

        paint.color =
            amber

        drawStrongLiveText(
            canvas,
            "develop.uganda",
            safeLeft +
                (23f * u),
            safeTop,
            paint
        )

        paint.textSize =
            17.5f * u

        paint.color =
            if (
                liveOn
            ) {
                red
            } else {
                white
            }

        paint.alpha =
            if (
                liveOn &&
                !blink
            ) {
                125
            } else {
                255
            }

        canvas.drawText(
            if (
                liveOn
            ) {
                "ON AIR"
            } else {
                "READY"
            },
            safeRight -
                (121f * u),
            safeTop,
            paint
        )

        paint.alpha =
            255

        val rule =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    0xB0FF3B32.toInt()

                strokeWidth =
                    1.6f * u
            }

        canvas.drawLine(
            safeLeft,
            safeTop +
                (13f * u),
            safeRight,
            safeTop +
                (14.0f * u),
            rule
        )

        paint.textSize =
            12f * u

        paint.color =
            white

        drawFitText(
            canvas,
            "LIVE PROFILE • ${profiles[profileIndex]}   |   REPORTER • $reporterName",
            safeLeft,
            safeTop +
                (38f * u),
            safeRight -
                safeLeft,
            paint,
            10.4f * u
        )

        paint.textSize =
            13.5f * u

        paint.color =
            white

        drawFitText(
            canvas,
            "STORY • $storyId   |   ${if (audioEnabled) "AUDIO ON" else "AUDIO OFF"}",
            safeLeft,
            safeTop +
                (57f * u),
            safeRight -
                safeLeft,
            paint,
            10.2f * u
        )

        paint.textSize =
            15.4f * u

        paint.color =
            cyan

        drawFitText(
            canvas,
            "${if (liveOn) "ON AIR" else "READY"}   |   TIMECODE • ${liveTimecode()}   |   FORMAT • $qualityLabel",
            safeLeft,
            safeTop +
                (77f * u),
            safeRight -
                safeLeft,
            paint,
            10.1f * u
        )

        val lowerY =
            finalHeight *
                0.76f

        val lowerStyle =
            lowerThirdStyles[
                lowerThirdStyleIndex
            ]

        val lowerAccent =
            when (
                lowerStyle
            ) {
                "CLEAN" ->
                    cyan

                "URGENT" ->
                    0xFFFF8A00.toInt()

                "MINIMAL" ->
                    white

                else ->
                    red
            }

        val lowerBg =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    when (
                        lowerStyle
                    ) {
                        "MINIMAL" ->
                            0x8205080A.toInt()

                        "CLEAN" ->
                            0x8F05080A.toInt()

                        else ->
                            0x8405080A.toInt()
                    }

                style =
                    Paint.Style.FILL
            }

        val lowerH =
            102f * u

        canvas.drawRoundRect(
            safeLeft,
            lowerY,
            safeRight,
            lowerY +
                lowerH,
            12f * u,
            12f * u,
            lowerBg
        )

        val redRail =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    lowerAccent

                strokeWidth =
                    5f * u
            }

        canvas.drawLine(
            safeLeft +
                (8f * u),
            lowerY +
                (10.6f * u),
            safeLeft +
                (8f * u),
            lowerY +
                lowerH -
                (12.0f * u),
            redRail
        )

        paint.color =
            lowerAccent

        paint.textSize =
            12f * u

        canvas.drawText(
            if (
                liveOn
            ) {
                "● LIVE NOW • $lowerStyle"
            } else {
                "LIVE READY • $lowerStyle"
            },
            safeLeft +
                (22f * u),
            lowerY +
                (24f * u),
            paint
        )

        paint.color =
            white

        paint.textSize =
            25.0f * u

        drawFitText(
            canvas,
            headline,
            safeLeft +
                (22f * u),
            lowerY +
                (60f * u),
            safeRight -
                safeLeft -
                (40f * u),
            paint,
            13f * u
        )

        paint.color =
            0xFFD0D8DC.toInt()

        paint.textSize =
            10f * u

        drawFitText(
            canvas,
            "REPORTER • $reporterName   |   STORY • $storyId   |   develop.uganda",
            safeLeft +
                (22f * u),
            lowerY +
                (84f * u),
            safeRight -
                safeLeft -
                (40f * u),
            paint,
            9.8f * u
        )

        canvas.restore()
    }

    private fun drawStrongLiveText(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawLiveTextBackplate(
            canvas,
            value,
            x,
            y,
            paint
        )

        val savedStyle =
            paint.style

        val savedColor =
            paint.color

        val savedStroke =
            paint.strokeWidth

        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            paint.textSize *
                liveHudOutlineScale()

        paint.color =
            liveHudOutlineColor()

        canvas.drawText(
            value,
            x,
            y,
            paint
        )

        paint.style =
            Paint.Style.FILL

        paint.strokeWidth =
            savedStroke

        paint.color =
            savedColor

        canvas.drawText(
            value,
            x,
            y,
            paint
        )

        paint.style =
            savedStyle
    }

    private fun drawFitText(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        minSize: Float
    ) {
        val original =
            paint.textSize

        var size =
            original

        while (
            paint.measureText(
                value
            ) >
                maxWidth &&
            size >
                minSize
        ) {
            size -=
                0.7f

            paint.textSize =
                size
        }

        drawLiveTextBackplate(
            canvas,
            value,
            x,
            y,
            paint
        )

        val savedStyle =
            paint.style

        val savedColor =
            paint.color

        val savedStroke =
            paint.strokeWidth

        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            paint.textSize *
                liveHudOutlineScale()

        paint.color =
            liveHudOutlineColor()

        canvas.drawText(
            value,
            x,
            y,
            paint
        )

        paint.style =
            Paint.Style.FILL

        paint.strokeWidth =
            savedStroke

        paint.color =
            savedColor

        canvas.drawText(
            value,
            x,
            y,
            paint
        )

        paint.style =
            savedStyle

        paint.textSize =
            original
    }

    private fun liveTimecode(): String {
        if (
            recording == null ||
            recordStartMs == 0L
        ) {
            return "00:00:00"
        }

        val total =
            (
                SystemClock.elapsedRealtime() -
                    recordStartMs
                ) /
                1000L

        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            total / 3600L,
            (total / 60L) % 60L,
            total % 60L
        )
    }

    private fun cycleProfile() {
        if (
            recording != null
        ) {
            toast(
                "Stop LIVE REC before changing profile"
            )
            return
        }

        profileIndex =
            (
                profileIndex +
                    1
                ) %
                profiles.size

        profileButton.text =
            "PROFILE ▾\n${profiles[profileIndex]}"

        liveSubTitle.text =
            "LIVE STUDIO • ${profiles[profileIndex]} • READY"
    }

    private fun cycleQuality() {
        if (
            recording != null
        ) {
            toast(
                "Stop LIVE REC before changing quality"
            )
            return
        }

        if (
            quality ==
            Quality.FHD
        ) {
            quality =
                Quality.HD

            qualityLabel =
                "HD"
        } else {
            quality =
                Quality.FHD

            qualityLabel =
                "FHD"
        }

        qualityButton.text =
            "QUALITY ▾\n$qualityLabel"

        bindCamera()
    }

    private fun toggleTorch() {
        val state =
            camera
                ?.cameraInfo
                ?.torchState
                ?.value
                ?: 0

        val enable =
            state !=
                androidx.camera.core.TorchState.ON

        camera
            ?.cameraControl
            ?.enableTorch(
                enable
            )

        lightButton.text =
            "LIGHT\n" +
                if (
                    enable
                ) {
                    "ON"
                } else {
                    "OFF"
                }
    }

    private fun beginLiveRecordSequence() {
        if (
            countdownRunning
        ) {
            return
        }

        if (
            !countdownEnabled
        ) {
            toggleRecording()
            return
        }

        countdownRunning =
            true

        recordButton.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP
        )

        runCountdownStep(
            3
        )
    }

    private fun runCountdownStep(
        value: Int
    ) {
        if (
            value <= 0
        ) {
            countdownView.visibility =
                View.GONE

            countdownRunning =
                false

            toggleRecording()
            return
        }

        countdownView.text =
            value.toString()

        countdownView.visibility =
            View.VISIBLE

        countdownView.alpha =
            1f

        countdownView.animate()
            .alpha(
                0.25f
            )
            .setDuration(
                700L
            )
            .start()

        uiHandler.postDelayed(
            {
                runCountdownStep(
                    value -
                        1
                )
            },
            1000L
        )
    }

    private fun addLiveMarker() {
        if (
            recording == null ||
            recordStartMs == 0L
        ) {
            toast(
                "Start LIVE REC before adding a marker"
            )
            return
        }

        val elapsed =
            (
                SystemClock.elapsedRealtime() -
                    recordStartMs
                )
                .coerceAtLeast(
                    0L
                )

        liveMarkers.add(
            elapsed
        )

        markButton.text =
            "MARK\\n${liveMarkers.size}"

        markButton.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP
        )

        toast(
            "MARK ${formatMarkerTime(elapsed)}"
        )
    }

    private fun formatMarkerTime(
        elapsedMs: Long
    ): String {
        val total =
            elapsedMs /
                1000L

        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            total / 3600L,
            (total / 60L) % 60L,
            total % 60L
        )
    }

    private fun saveLiveMarkers(
        recordingName: String
    ) {
        if (
            liveMarkers.isEmpty()
        ) {
            return
        }

        val snapshot =
            liveMarkers.toList()

        Thread {
            try {
                val content =
                    buildString {
                        append(
                            "develop.uganda LIVE MARKERS\\n"
                        )
                        append(
                            "FILE $recordingName.mp4\\n"
                        )
                        append(
                            "PROFILE ${profiles[profileIndex]}\\n"
                        )
                        append(
                            "REPORTER $reporterName\\n"
                        )
                        append(
                            "STORY $storyId\\n\\n"
                        )

                        snapshot.forEachIndexed {
                                index,
                                value ->
                            append(
                                "MARK "
                            )
                            append(
                                index +
                                    1
                            )
                            append(
                                " "
                            )
                            append(
                                formatMarkerTime(
                                    value
                                )
                            )
                            append(
                                "\\n"
                            )
                        }
                    }

                val dir =
                    File(
                        getExternalFilesDir(
                            Environment.DIRECTORY_DOCUMENTS
                        ),
                        "develop.uganda/LiveMarkers"
                    )

                dir.mkdirs()

                FileOutputStream(
                    File(
                        dir,
                        "${recordingName}_MARKERS.txt"
                    )
                ).use {
                    it.write(
                        content.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun toggleRecording() {
        if (
            recording != null
        ) {
            recording?.stop()
            return
        }

        val capture =
            videoCapture
                ?: run {
                    toast(
                        "Camera is not ready"
                    )
                    return
                }

        val stamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(
                Date()
            )

        liveRecordingName =
            "DEVELOP_UGANDA_LIVE_${profiles[profileIndex]}_$stamp"

        liveMarkers.clear()

        if (
            ::markButton.isInitialized
        ) {
            markButton.text =
                "MARK\n0"
        }

        val values =
            ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    "$liveRecordingName.mp4"
                )

                put(
                    MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4"
                )

                if (
                    android.os.Build.VERSION.SDK_INT >=
                    29
                ) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/develop.uganda/Live"
                    )
                }
            }

        val output =
            MediaStoreOutputOptions.Builder(
                contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(
                    values
                )
                .build()

        var pending =
            capture.output
                .prepareRecording(
                    this,
                    output
                )

        if (
            audioEnabled &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            pending =
                pending.withAudioEnabled()
        }

        recording =
            pending.start(
                ContextCompat.getMainExecutor(
                    this
                )
            ) { event ->
                when (
                    event
                ) {
                    is VideoRecordEvent.Start -> {
                        recordStartMs =
                            SystemClock.elapsedRealtime()

                        recordButton.setRecordingState(
                            true
                        )

                        liveSubTitle.text =
                            "LIVE STUDIO • ${profiles[profileIndex]} • RECORDING"

                        recLamp.setTextColor(
                            red
                        )

                        toast(
                            "LIVE REC started"
                        )
                    }

                    is VideoRecordEvent.Status -> {
                        liveAudioAmplitude =
                            event.recordingStats
                                .audioStats
                                .audioAmplitude
                                .coerceIn(
                                    0.0,
                                    1.0
                                )

                        if (
                            audioEnabled &&
                            liveAudioAmplitude >
                                0.0
                        ) {
                            micLamp.setTextColor(
                                green
                            )
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        recording =
                            null

                        recordButton.setRecordingState(
                            false
                        )

                        liveSubTitle.text =
                            "LIVE STUDIO • ${profiles[profileIndex]} • READY"

                        recLamp.setTextColor(
                            0xFF657078.toInt()
                        )

                        recordStartMs =
                            0L

                        liveAudioAmplitude =
                            0.0

                        if (
                            event.hasError()
                        ) {
                            toast(
                                "LIVE REC failed"
                            )
                        } else {
                            saveLiveMarkers(
                                liveRecordingName
                            )

                            toast(
                                "LIVE recording saved"
                            )
                        }
                    }
                }
            }
    }

    private fun showLowerThirdEditor() {
        val box =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(8),
                    dp(18),
                    dp(4)
                )
            }

        val headlineField =
            EditText(this).apply {
                hint =
                    "Live headline"

                setText(
                    headline
                )
            }

        box.addView(
            headlineField
        )

        AlertDialog.Builder(this)
            .setTitle(
                "LIVE LOWER THIRD"
            )
            .setView(
                box
            )
            .setPositiveButton(
                "SAVE"
            ) { _, _ ->
                headline =
                    headlineField.text
                        .toString()
                        .trim()
                        .ifBlank {
                            "LIVE REPORT"
                        }

                getSharedPreferences(
                    "develop_uganda_newsroom",
                    Context.MODE_PRIVATE
                )
                    .edit()
                    .putString(
                        "headline",
                        headline
                    )
                    .apply()
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    private fun showOutputSetup() {
        val prefs =
            getSharedPreferences(
                "develop_uganda_stream",
                Context.MODE_PRIVATE
            )

        val box =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(8),
                    dp(18),
                    dp(4)
                )
            }

        val url =
            EditText(this).apply {
                hint =
                    "RTMP / SRT / WebRTC destination"

                setText(
                    prefs.getString(
                        "url",
                        ""
                    )
                )
            }

        val key =
            EditText(this).apply {
                hint =
                    "Stream key / token"

                setText(
                    prefs.getString(
                        "key",
                        ""
                    )
                )
            }

        box.addView(
            url
        )

        box.addView(
            key
        )

        AlertDialog.Builder(this)
            .setTitle(
                "LIVE OUTPUT SETUP"
            )
            .setMessage(
                "V184 stores a destination for the next streaming-backend phase. " +
                    "This APK still records locally and does not pretend the internet stream is active."
            )
            .setView(
                box
            )
            .setPositiveButton(
                "SAVE"
            ) { _, _ ->
                prefs.edit()
                    .putString(
                        "url",
                        url.text
                            .toString()
                            .trim()
                    )
                    .putString(
                        "key",
                        key.text
                            .toString()
                            .trim()
                    )
                    .apply()

                val ready =
                    url.text
                        .toString()
                        .trim()
                        .isNotBlank()

                outputButton.text =
                    "OUTPUT\n" +
                        if (
                            ready
                        ) {
                            "SET"
                        } else {
                            "LOCAL"
                        }

                outputStatus.text =
                    if (
                        ready
                    ) {
                        "OUTPUT • DESTINATION SAVED • LOCAL CAPTURE ACTIVE"
                    } else {
                        "OUTPUT • LOCAL LIVE CAPTURE • STREAM DESTINATION NOT CONNECTED"
                    }
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    private fun showSignalInfo() {
        AlertDialog.Builder(this)
            .setTitle(
                "LIVE SIGNALS"
            )
            .setMessage(
                "GREEN = ready/healthy. AMBER = limited or permission dependent. " +
                    "RED = unavailable or actively recording for REC. " +
                    "NET reports internet capability, GPS reports permission readiness, MIC reports audio readiness, CAM reports the bound camera, and BAT changes by remaining charge."
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun updateSignals() {
        netLamp.setTextColor(
            if (
                isNetworkConnected()
            ) {
                green
            } else {
                red
            }
        )

        gpsLamp.setTextColor(
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                green
            } else {
                amber
            }
        )

        micLamp.setTextColor(
            if (
                audioEnabled &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                green
            } else {
                amber
            }
        )

        camLamp.setTextColor(
            if (
                camera != null
            ) {
                green
            } else {
                red
            }
        )

        recLamp.setTextColor(
            if (
                recording !=
                null
            ) {
                red
            } else {
                0xFF657078.toInt()
            }
        )

        val battery =
            getSystemService(
                Context.BATTERY_SERVICE
            ) as BatteryManager

        val percent =
            battery.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

        batteryLamp.text =
            "● BAT $percent%"

        batteryLamp.setTextColor(
            when {
                percent >= 30 ->
                    green

                percent >= 15 ->
                    amber

                else ->
                    red
            }
        )
    }

    private fun updateTimer() {
        timerView.text =
            liveTimecode()

        if (
            ::livePreviewMeta.isInitialized
        ) {
            livePreviewMeta.text =
                "${profiles[profileIndex]} • REPORTER $reporterName • STORY $storyId • $headline"

            val audioPercent =
                (
                    liveAudioAmplitude *
                        100.0
                    ).roundToInt()

            val battery =
                (
                    getSystemService(
                        Context.BATTERY_SERVICE
                    ) as BatteryManager
                    )
                    .getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY
                    )

            val netReady =
                isNetworkConnected()

            val health =
                when {
                    battery in 0..9 ->
                        "CRITICAL"

                    !netReady ->
                        "NET OFF"

                    audioEnabled &&
                        recording != null &&
                        audioPercent <
                        1 ->
                        "CHECK MIC"

                    else ->
                        "GOOD"
                }

            livePreviewTech.text =
                "TC ${liveTimecode()} • $qualityLabel • MIC ${audioPercent}% • NET ${
                    if (netReady) {
                        "READY"
                    } else {
                        "OFF"
                    }
                } • HEALTH $health"
        }
    }

    private fun updateBlink() {
        if (
            recording ==
            null
        ) {
            liveBadge.alpha =
                0.85f

            return
        }

        liveBlinkOn =
            !liveBlinkOn

        liveBadge.alpha =
            if (
                liveBlinkOn
            ) {
                1f
            } else {
                0.25f
            }
    }

    private fun isNetworkConnected(): Boolean {
        val cm =
            getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            cm.activeNetwork
                ?: return false

        val caps =
            cm.getNetworkCapabilities(
                network
            )
                ?: return false

        return caps.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    private fun liveSettingButton(
        value: String,
        accent: Int,
        action: () -> Unit
    ): Button {
        return LiveGlowButton(
            this,
            accent
        ).apply {
            text =
                value

            textSize =
                6.4f

            isAllCaps =
                false

            setTextColor(
                white
            )

            gravity =
                Gravity.CENTER

            includeFontPadding =
                false

            background =
                ColorDrawable(
                    Color.TRANSPARENT
                )

            stateListAnimator =
                null

            setOnClickListener {
                if (
                    liveControlsLocked &&
                    this !== liveLockButton &&
                    this !== markButton
                ) {
                    toast(
                        "LIVE controls locked"
                    )
                } else {
                    action.invoke()
                }
            }
        }
    }

    private fun livePillFillColor(
        accent: Int,
        selected: Boolean = false
    ): Int {
        val factor =
            if (selected) {
                0.72f
            } else {
                0.50f
            }

        return Color.rgb(
            (
                Color.red(accent) *
                    factor
                ).roundToInt(),
            (
                Color.green(accent) *
                    factor
                ).roundToInt(),
            (
                Color.blue(accent) *
                    factor
                ).roundToInt()
        )
    }

    private fun liveSolidPillBackground(
        accent: Int,
        selected: Boolean = false
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                dp(20).toFloat()

            setColor(
                livePillFillColor(
                    accent,
                    selected
                )
            )

            setStroke(
                dp(
                    if (selected) {
                        2
                    } else {
                        1
                    }
                ),
                accent
            )
        }
    }

    private fun liveOptionAccent(
        index: Int
    ): Int {
        val palette =
            intArrayOf(
                red,
                cyan,
                green,
                0xFFA77B92.toInt(),
                amber,
                0xFF83799A.toInt(),
                0xFF6D88A4.toInt(),
                0xFF8B9499.toInt()
            )

        return palette[
            index %
                palette.size
        ]
    }

    private fun showLivePillDropdown(
        anchor: View,
        title: String,
        options: Array<String>,
        selectedIndex: Int,
        onPick: (Int) -> Unit
    ) {
        val panel =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(7),
                    dp(7),
                    dp(7),
                    dp(7)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(18).toFloat()

                        setColor(
                            0xF20A0E11.toInt()
                        )

                        setStroke(
                            dp(1),
                            0x60FF3B32
                        )
                    }
            }

        panel.addView(
            label(
                title,
                9f,
                white,
                true
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val popup =
            PopupWindow(
                panel,
                dp(190),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {
                isOutsideTouchable =
                    true

                elevation =
                    dp(8).toFloat()

                setBackgroundDrawable(
                    ColorDrawable(
                        Color.TRANSPARENT
                    )
                )
            }

        options.forEachIndexed {
                index,
                option ->

            val accent =
                liveOptionAccent(
                    index
                )

            val pill =
                LiveGlowButton(
                    this,
                    accent
                ).apply {
                    text =
                        if (
                            index ==
                            selectedIndex
                        ) {
                            "✓  $option"
                        } else {
                            option
                        }

                    textSize =
                        8f

                    isAllCaps =
                        false

                    setTextColor(
                        white
                    )

                    gravity =
                        Gravity.CENTER

                    isSelected =
                        index ==
                            selectedIndex

                    background =
                        ColorDrawable(
                            Color.TRANSPARENT
                        )

                    setOnClickListener {
                        onPick(
                            index
                        )
                        popup.dismiss()
                    }
                }

            panel.addView(
                pill,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(38)
                ).apply {
                    topMargin =
                        dp(4)
                }
            )
        }

        popup.showAsDropDown(
            anchor,
            0,
            dp(4)
        )
    }

    private fun showLiveProfileDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop LIVE REC before changing profile"
            )
            return
        }

        showLivePillDropdown(
            anchor,
            "LIVE PROFILE",
            profiles,
            profileIndex
        ) { picked ->
            profileIndex =
                picked

            markLivePresetCustom()

            profileButton.text =
                "PROFILE ▾\n${profiles[profileIndex]}"

            liveSubTitle.text =
                "LIVE STUDIO • ${profiles[profileIndex]} • READY"

            saveLiveCameraPreferences()
        }
    }

    private fun showLiveQualityDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop LIVE REC before changing quality"
            )
            return
        }

        showLivePillDropdown(
            anchor,
            "QUALITY",
            arrayOf(
                "FHD",
                "HD"
            ),
            if (quality == Quality.FHD) 0 else 1
        ) { picked ->
            quality =
                if (picked == 0) {
                    Quality.FHD
                } else {
                    Quality.HD
                }

            qualityLabel =
                if (picked == 0) {
                    "FHD"
                } else {
                    "HD"
                }

            qualityButton.text =
                "QUALITY ▾\n$qualityLabel"

            bindCamera()
        }
    }

    private fun showLiveAudioDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "AUDIO",
            arrayOf(
                "ON",
                "OFF"
            ),
            if (audioEnabled) 0 else 1
        ) { picked ->
            audioEnabled =
                picked ==
                    0

            markLivePresetCustom()

            audioButton.text =
                "AUDIO ▾\n" +
                    if (audioEnabled) {
                        "ON"
                    } else {
                        "OFF"
                    }

            saveLiveCameraPreferences()
        }
    }

    private fun showLiveGraphicsDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "LIVE GRAPHICS",
            arrayOf(
                "ON",
                "OFF"
            ),
            if (graphicsEnabled) 0 else 1
        ) { picked ->
            graphicsEnabled =
                picked ==
                    0

            markLivePresetCustom()

            graphicsButton.text =
                "GRAPHICS ▾\n" +
                    if (graphicsEnabled) {
                        "ON"
                    } else {
                        "OFF"
                    }

            saveLiveCameraPreferences()
        }
    }

    private fun showLiveLensDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop LIVE REC before changing lens"
            )
            return
        }

        showLivePillDropdown(
            anchor,
            "LENS",
            arrayOf(
                "BACK",
                "FRONT"
            ),
            if (useFront) 1 else 0
        ) { picked ->
            useFront =
                picked ==
                    1

            lensButton.text =
                "LENS ▾\n" +
                    if (useFront) {
                        "FRONT"
                    } else {
                        "BACK"
                    }

            bindCamera()
        }
    }

    private fun showLiveLightDropdown(
        anchor: View
    ) {
        val isOn =
            camera
                ?.cameraInfo
                ?.torchState
                ?.value ==
                androidx.camera.core.TorchState.ON

        showLivePillDropdown(
            anchor,
            "LIGHT",
            arrayOf(
                "OFF",
                "ON"
            ),
            if (isOn) 1 else 0
        ) { picked ->
            val wantOn =
                picked ==
                    1

            camera
                ?.cameraControl
                ?.enableTorch(
                    wantOn
                )

            lightButton.text =
                "LIGHT ▾\n" +
                    if (wantOn) {
                        "ON"
                    } else {
                        "OFF"
                    }
        }
    }

    private fun showLiveViewDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "VIEW",
            arrayOf(
                "FULL SCREEN",
                "HALF SCREEN"
            ),
            if (halfPreviewMode) 1 else 0
        ) { picked ->
            val wantHalf =
                picked ==
                    1

            if (
                wantHalf !=
                halfPreviewMode
            ) {
                togglePreviewMode()
            }
        }
    }

    private fun showLiveCountdownDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "COUNTDOWN",
            arrayOf(
                "3 SEC",
                "OFF"
            ),
            if (countdownEnabled) 0 else 1
        ) { picked ->
            countdownEnabled =
                picked ==
                    0

            markLivePresetCustom()

            countdownButton.text =
                "COUNTDOWN ▾\n" +
                    if (countdownEnabled) {
                        "3 SEC"
                    } else {
                        "OFF"
                    }

            countdownButton.isSelected =
                countdownEnabled

            saveLiveCameraPreferences()
        }
    }

    private fun showLiveStyleDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Change lower-third style before recording"
            )
            return
        }

        showLivePillDropdown(
            anchor,
            "LOWER THIRD STYLE",
            lowerThirdStyles,
            lowerThirdStyleIndex
        ) { picked ->
            lowerThirdStyleIndex =
                picked

            markLivePresetCustom()

            styleButton.text =
                "LOWER STYLE ▾\n${lowerThirdStyles[lowerThirdStyleIndex]}"

            saveLiveCameraPreferences()
        }
    }

    private fun loadLiveCameraPreferences() {
        val prefs =
            getSharedPreferences(
                "develop_uganda_live_camera",
                Context.MODE_PRIVATE
            )

        profileIndex =
            prefs.getInt(
                "profile_index",
                profileIndex
            )
                .coerceIn(
                    0,
                    profiles.lastIndex
                )

        lowerThirdStyleIndex =
            prefs.getInt(
                "lower_style",
                lowerThirdStyleIndex
            )
                .coerceIn(
                    0,
                    lowerThirdStyles.lastIndex
                )

        liveHudSizeIndex =
            prefs.getInt(
                "hud_size",
                liveHudSizeIndex
            )
                .coerceIn(
                    0,
                    liveHudLabels.lastIndex
                )

        liveHudContrastIndex =
            prefs.getInt(
                "hud_contrast",
                liveHudContrastIndex
            )
                .coerceIn(
                    0,
                    liveHudContrastLabels.lastIndex
                )

        liveHudBackingIndex =
            prefs.getInt(
                "hud_backing",
                liveHudBackingIndex
            )
                .coerceIn(
                    0,
                    liveHudBackingLabels.lastIndex
                )

        liveEffectIndex =
            prefs.getInt(
                "video_fx",
                liveEffectIndex
            )
                .coerceIn(
                    0,
                    liveEffectLabels.lastIndex
                )

        livePresetIndex =
            prefs.getInt(
                "preset_index",
                livePresetIndex
            )
                .coerceIn(
                    0,
                    livePresetLabels.lastIndex
                )

        audioEnabled =
            prefs.getBoolean(
                "audio",
                audioEnabled
            )

        graphicsEnabled =
            prefs.getBoolean(
                "graphics",
                graphicsEnabled
            )

        countdownEnabled =
            prefs.getBoolean(
                "countdown",
                countdownEnabled
            )
    }

    private fun saveLiveCameraPreferences() {
        getSharedPreferences(
            "develop_uganda_live_camera",
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(
                "profile_index",
                profileIndex
            )
            .putInt(
                "lower_style",
                lowerThirdStyleIndex
            )
            .putInt(
                "hud_size",
                liveHudSizeIndex
            )
            .putInt(
                "hud_contrast",
                liveHudContrastIndex
            )
            .putInt(
                "hud_backing",
                liveHudBackingIndex
            )
            .putInt(
                "video_fx",
                liveEffectIndex
            )
            .putInt(
                "preset_index",
                livePresetIndex
            )
            .putBoolean(
                "audio",
                audioEnabled
            )
            .putBoolean(
                "graphics",
                graphicsEnabled
            )
            .putBoolean(
                "countdown",
                countdownEnabled
            )
            .apply()
    }

    private fun liveHudBackingAlpha(): Int {
        return when (
            liveHudBackingIndex
        ) {
            0 ->
                0

            2 ->
                58

            else ->
                32
        }
    }

    private fun drawLiveTextBackplate(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        val alpha =
            liveHudBackingAlpha()

        if (
            alpha <=
            0
        ) {
            return
        }

        val metrics =
            paint.fontMetrics

        val padX =
            paint.textSize *
                0.22f

        val padY =
            paint.textSize *
                0.12f

        val background =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    Color.argb(
                        alpha,
                        0,
                        0,
                        0
                    )

                style =
                    Paint.Style.FILL
            }

        canvas.drawRoundRect(
            x -
                padX,
            y +
                metrics.ascent -
                padY,
            x +
                paint.measureText(
                    value
                ) +
                padX,
            y +
                metrics.descent +
                padY,
            paint.textSize *
                0.22f,
            paint.textSize *
                0.22f,
            background
        )
    }

    private fun liveHudOutlineScale(): Float {
        return when (
            liveHudContrastIndex
        ) {
            0 ->
                0.014f

            2 ->
                0.032f

            else ->
                0.022f
        }
    }

    private fun liveHudOutlineColor(): Int {
        return when (
            liveHudContrastIndex
        ) {
            0 ->
                0x26000000

            2 ->
                0x52000000

            else ->
                0x38000000
        }
    }

    private fun liveHudShadowRadius(
        u: Float
    ): Float {
        return when (
            liveHudContrastIndex
        ) {
            0 ->
                0.35f * u

            2 ->
                1.0f * u

            else ->
                0.65f * u
        }
    }

    private fun showLiveEffectDropdown(
        anchor: View
    ) {
        if (
            recording !=
            null
        ) {
            toast(
                "Stop LIVE REC before changing video effect"
            )
            return
        }

        showLivePillDropdown(
            anchor,
            "SAVED VIDEO EFFECT",
            liveEffectLabels,
            liveEffectIndex
        ) { picked ->
            liveEffectIndex =
                picked

            markLivePresetCustom()

            liveEffectButton.text =
                "VIDEO FX ▾\n${liveEffectLabels[liveEffectIndex]}"

            liveEffectButton.isSelected =
                liveEffectIndex !=
                    0

            saveLiveCameraPreferences()

            toast(
                "VIDEO FX ${liveEffectLabels[liveEffectIndex]}"
            )
        }
    }

    private fun showLiveHudBackingDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "RECORDED HUD BACKING",
            liveHudBackingLabels,
            liveHudBackingIndex
        ) { picked ->
            liveHudBackingIndex =
                picked

            markLivePresetCustom()

            liveHudBackingButton.text =
                "HUD BACKING ▾\n${liveHudBackingLabels[liveHudBackingIndex]}"

            liveHudBackingButton.isSelected =
                liveHudBackingIndex !=
                    0

            saveLiveCameraPreferences()

            toast(
                "LIVE backing ${liveHudBackingLabels[liveHudBackingIndex]}"
            )
        }
    }

    private fun markLivePresetCustom() {
        if (
            livePresetIndex !=
            0
        ) {
            livePresetIndex =
                0

            if (
                ::livePresetButton.isInitialized
            ) {
                livePresetButton.text =
                    "PRESET ▾\nCUSTOM"

                livePresetButton.isSelected =
                    false
            }
        }
    }

    private fun liveArrayIndex(
        values: Array<String>,
        wanted: String,
        fallback: Int = 0
    ): Int {
        val index =
            values.indexOf(
                wanted
            )

        return if (
            index >=
            0
        ) {
            index
        } else {
            fallback.coerceIn(
                0,
                values.lastIndex
            )
        }
    }

    private fun applyLivePreset(
        picked: Int
    ) {
        livePresetIndex =
            picked.coerceIn(
                0,
                livePresetLabels.lastIndex
            )

        when (
            livePresetLabels[
                livePresetIndex
            ]
        ) {
            "BREAKING" -> {
                profileIndex =
                    liveArrayIndex(
                        profiles,
                        "BREAKING"
                    )

                lowerThirdStyleIndex =
                    liveArrayIndex(
                        lowerThirdStyles,
                        "BREAKING"
                    )

                liveHudSizeIndex =
                    1

                liveHudContrastIndex =
                    2

                audioEnabled =
                    true

                graphicsEnabled =
                    true

                countdownEnabled =
                    true

                liveHudBackingIndex =
                    1

                liveEffectIndex =
                    liveEffectLabels.indexOf(
                        "CLEAN"
                    ).coerceAtLeast(
                        0
                    )
            }

            "INTERVIEW" -> {
                profileIndex =
                    liveArrayIndex(
                        profiles,
                        "INTERVIEW"
                    )

                lowerThirdStyleIndex =
                    liveArrayIndex(
                        lowerThirdStyles,
                        "CLEAN"
                    )

                liveHudSizeIndex =
                    0

                liveHudContrastIndex =
                    1

                audioEnabled =
                    true

                graphicsEnabled =
                    true

                countdownEnabled =
                    false

                liveHudBackingIndex =
                    1

                liveEffectIndex =
                    liveEffectLabels.indexOf(
                        "NATURAL"
                    ).coerceAtLeast(
                        0
                    )
            }

            "EVENT" -> {
                profileIndex =
                    liveArrayIndex(
                        profiles,
                        "EVENT"
                    )

                lowerThirdStyleIndex =
                    liveArrayIndex(
                        lowerThirdStyles,
                        "CLEAN"
                    )

                liveHudSizeIndex =
                    1

                liveHudContrastIndex =
                    1

                audioEnabled =
                    true

                graphicsEnabled =
                    true

                countdownEnabled =
                    true

                liveHudBackingIndex =
                    1

                liveEffectIndex =
                    liveEffectLabels.indexOf(
                        "WARM"
                    ).coerceAtLeast(
                        0
                    )
            }

            "COMMUNITY" -> {
                profileIndex =
                    liveArrayIndex(
                        profiles,
                        "COMMUNITY"
                    )

                lowerThirdStyleIndex =
                    liveArrayIndex(
                        lowerThirdStyles,
                        "MINIMAL"
                    )

                liveHudSizeIndex =
                    1

                liveHudContrastIndex =
                    1

                audioEnabled =
                    true

                graphicsEnabled =
                    true

                countdownEnabled =
                    true
            }

            else -> {
                // CUSTOM leaves current values untouched.

                liveHudBackingIndex =
                    1

                liveEffectIndex =
                    liveEffectLabels.indexOf(
                        "NATURAL"
                    ).coerceAtLeast(
                        0
                    )
            }
        }

        livePresetButton.text =
            "PRESET ▾\n${livePresetLabels[livePresetIndex]}"

        livePresetButton.isSelected =
            livePresetIndex !=
                0

        profileButton.text =
            "PROFILE ▾\n${profiles[profileIndex]}"

        styleButton.text =
            "LOWER STYLE ▾\n${lowerThirdStyles[lowerThirdStyleIndex]}"

        liveHudSizeButton.text =
            "HUD SIZE ▾\n${liveHudLabels[liveHudSizeIndex]}"

        liveHudContrastButton.text =
            "HUD CONTRAST ▾\n${liveHudContrastLabels[liveHudContrastIndex]}"

        liveHudBackingButton.text =
            "HUD BACKING ▾\n${liveHudBackingLabels[liveHudBackingIndex]}"

        liveHudBackingButton.isSelected =
            liveHudBackingIndex !=
                0

        liveEffectButton.text =
            "VIDEO FX ▾\n${liveEffectLabels[liveEffectIndex]}"

        liveEffectButton.isSelected =
            liveEffectIndex !=
                0

        audioButton.text =
            "AUDIO ▾\n" +
                if (
                    audioEnabled
                ) {
                    "ON"
                } else {
                    "OFF"
                }

        graphicsButton.text =
            "GRAPHICS ▾\n" +
                if (
                    graphicsEnabled
                ) {
                    "ON"
                } else {
                    "OFF"
                }

        countdownButton.text =
            "COUNTDOWN ▾\n" +
                if (
                    countdownEnabled
                ) {
                    "3 SEC"
                } else {
                    "OFF"
                }

        countdownButton.isSelected =
            countdownEnabled

        liveSubTitle.text =
            "LIVE STUDIO • ${profiles[profileIndex]} • READY"

        saveLiveCameraPreferences()

        toast(
            "LIVE preset ${livePresetLabels[livePresetIndex]}"
        )
    }

    private fun showLivePresetDropdown(
        anchor: View
    ) {
        if (
            recording !=
            null
        ) {
            toast(
                "Stop LIVE REC before changing preset"
            )
            return
        }

        showLivePillDropdown(
            anchor,
            "LIVE PRESET",
            livePresetLabels,
            livePresetIndex
        ) { picked ->
            applyLivePreset(
                picked
            )
        }
    }

    private fun showLiveHudContrastDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "RECORDED HUD CONTRAST",
            liveHudContrastLabels,
            liveHudContrastIndex
        ) { picked ->
            liveHudContrastIndex =
                picked

            markLivePresetCustom()

            liveHudContrastButton.text =
                "HUD CONTRAST ▾\n${liveHudContrastLabels[liveHudContrastIndex]}"

            liveHudContrastButton.isSelected =
                true

            saveLiveCameraPreferences()

            toast(
                "LIVE contrast ${liveHudContrastLabels[liveHudContrastIndex]}"
            )
        }
    }

    private fun showLiveHudSizeDropdown(
        anchor: View
    ) {
        showLivePillDropdown(
            anchor,
            "RECORDED HUD SIZE",
            liveHudLabels,
            liveHudSizeIndex
        ) { picked ->
            liveHudSizeIndex =
                picked

            markLivePresetCustom()

            liveHudSizeButton.text =
                "HUD SIZE ▾\n${liveHudLabels[liveHudSizeIndex]}"

            liveHudSizeButton.isSelected =
                true

            saveLiveCameraPreferences()

            toast(
                "LIVE HUD ${liveHudLabels[liveHudSizeIndex]}"
            )
        }
    }

    private fun showLiveSafeAreaInfo() {
        AlertDialog.Builder(this)
            .setTitle(
                "LIVE OUTPUT SAFE AREA"
            )
            .setMessage(
                "The operator controls stay screen-only. Saved-video branding, ON AIR status and lower-third graphics remain inside the protected 9:16 output area."
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun signal(
        value: String
    ): TextView {
        return label(
            value,
            9f,
            0xFF657078.toInt(),
            true
        ).apply {
            gravity =
                Gravity.CENTER_VERTICAL
        }
    }

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text =
                value

            textSize =
                size

            setTextColor(
                color
            )

            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    if (
                        bold
                    ) {
                        Typeface.BOLD
                    } else {
                        Typeface.NORMAL
                    }
                )
        }
    }

    private fun rounded(
        fill: Int,
        stroke: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                dp(radius).toFloat()

            setColor(
                fill
            )

            if (
                stroke !=
                Color.TRANSPARENT
            ) {
                setStroke(
                    dp(1),
                    stroke
                )
            }
        }
    }

    private fun weight():
        LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            dp(40),
            1f
        ).apply {
            marginStart =
                dp(4)

            marginEnd =
                dp(4)
        }
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    private fun toast(
        value: String
    ) {
        Toast.makeText(
            this,
            value,
            Toast.LENGTH_SHORT
        ).show()
    }

    private class LiveGlowButton(
        context: Context,
        private val accent: Int
    ) : Button(context) {

        private val density =
            context.resources
                .displayMetrics
                .density

        private val ringPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.STROKE

                strokeWidth =
                    3.8f *
                        density

                color =
                    0xFFD9DEE8.toInt()
            }

        private val idleFill =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    0x52000000
            }

        private val pressedFill =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    Color.argb(
                        90,
                        Color.red(
                            accent
                        ),
                        Color.green(
                            accent
                        ),
                        Color.blue(
                            accent
                        )
                    )
            }

        private val selectedFill =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    0xFFAEBDEB.toInt()
            }

        private val glow =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.STROKE

                strokeWidth =
                    4.8f *
                        density

                color =
                    0xFFAEBDEB.toInt()

                setShadowLayer(
                    3.0f *
                        density,
                    0f,
                    0f,
                    0x66AEBDEB
                )
            }

        init {
            setLayerType(
                LAYER_TYPE_SOFTWARE,
                null
            )

            gravity =
                Gravity.CENTER

            background =
                ColorDrawable(
                    Color.TRANSPARENT
                )

            stateListAnimator =
                null
        }

        private fun selectedWordColor(): Int {
            val luminance =
                (
                    Color.red(
                        accent
                    ) *
                        299 +
                        Color.green(
                            accent
                        ) *
                        587 +
                        Color.blue(
                            accent
                        ) *
                        114
                    ) /
                    1000

            return if (
                luminance >=
                155
            ) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        }

        override fun drawableStateChanged() {
            super.drawableStateChanged()

            setTextColor(
                if (
                    isSelected
                ) {
                    selectedWordColor()
                } else {
                    Color.WHITE
                }
            )

            invalidate()
        }

        override fun onDraw(
            canvas: Canvas
        ) {
            val inset =
                4.5f *
                    density

            val radius =
                (
                    height -
                        inset *
                            2f
                    ) /
                    2f

            canvas.drawRoundRect(
                inset,
                inset,
                width -
                    inset,
                height -
                    inset,
                radius,
                radius,
                when {
                    isSelected ->
                        selectedFill

                    isPressed ->
                        pressedFill

                    else ->
                        idleFill
                }
            )

            canvas.drawRoundRect(
                inset,
                inset,
                width -
                    inset,
                height -
                    inset,
                radius,
                radius,
                ringPaint
            )

            if (
                isPressed &&
                !isSelected
            ) {
                canvas.drawRoundRect(
                    inset,
                    inset,
                    width -
                        inset,
                    height -
                        inset,
                    radius,
                    radius,
                    glow
                )
            }

            super.onDraw(
                canvas
            )
        }
    }

    private class LiveRecordButtonView(
        context: Context
    ) : View(context) {

        private val ring =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.STROKE

                strokeWidth =
                    5f

                color =
                    0xFFFF3B32.toInt()
            }

        private val glow =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.STROKE

                strokeWidth =
                    11f

                color =
                    0xFFFF3B32.toInt()
            }

        private val center =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    0xFF5E1110.toInt()
            }

        private val centerMark =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    Color.WHITE
            }

        private var isRecording =
            false

        private val pulseHandler =
            Handler(
                Looper.getMainLooper()
            )

        private val pulse =
            object : Runnable {
                override fun run() {
                    invalidate()

                    if (
                        isRecording
                    ) {
                        pulseHandler.postDelayed(
                            this,
                            33L
                        )
                    }
                }
            }

        init {
            isClickable =
                true
        }

        fun setRecordingState(
            value: Boolean
        ) {
            isRecording =
                value

            center.color =
                if (
                    value
                ) {
                    0xFFFF2D24.toInt()
                } else {
                    0xFF5E1110.toInt()
                }

            pulseHandler.removeCallbacks(
                pulse
            )

            if (
                value
            ) {
                pulseHandler.post(
                    pulse
                )
            }

            invalidate()
        }

        override fun onDraw(
            canvas: Canvas
        ) {
            super.onDraw(
                canvas
            )

            val cx =
                width / 2f

            val cy =
                height / 2f

            val base =
                minOf(
                    width,
                    height
                ) *
                    0.29f

            if (
                isRecording
            ) {
                val wave =
                    (
                        sin(
                            SystemClock.elapsedRealtime() /
                                180.0
                        ) +
                            1.0
                        ) /
                        2.0

                glow.alpha =
                    (
                        45 +
                            (
                                wave *
                                    120
                                )
                        ).roundToInt()

                canvas.drawCircle(
                    cx,
                    cy,
                    base +
                        13f +
                        (
                            wave *
                                8f
                            ).toFloat(),
                    glow
                )
            }

            canvas.drawCircle(
                cx,
                cy,
                base +
                    7f,
                ring
            )

            canvas.drawCircle(
                cx,
                cy,
                base,
                center
            )

            if (
                isRecording
            ) {
                val half =
                    base *
                        0.28f

                canvas.drawRoundRect(
                    cx -
                        half,
                    cy -
                        half,
                    cx +
                        half,
                    cy +
                        half,
                    half *
                        0.28f,
                    half *
                        0.28f,
                    centerMark
                )
            } else {
                canvas.drawCircle(
                    cx,
                    cy,
                    base *
                        0.17f,
                    centerMark
                )
            }
        }
    }
}
