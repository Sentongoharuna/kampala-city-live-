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
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
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

    private var recordStartMs = 0L
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

    private val red = 0xFFFF3B32.toInt()
    private val green = 0xFF62E889.toInt()
    private val amber = 0xFFFFC21A.toInt()
    private val cyan = 0xFF77E9FF.toInt()
    private val white = Color.WHITE
    private val panel = 0x280A0E11.toInt()

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

        root.addView(
            topPanel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
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
                0xFFB7C4CA.toInt(),
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
                "PROFILE\n${profiles[profileIndex]}",
                red
            ) {
                cycleProfile()
            }

        qualityButton =
            liveSettingButton(
                "QUALITY\nFHD",
                cyan
            ) {
                cycleQuality()
            }

        audioButton =
            liveSettingButton(
                "AUDIO\nON",
                green
            ) {
                audioEnabled =
                    !audioEnabled

                audioButton.text =
                    "AUDIO\n" +
                        if (audioEnabled) {
                            "ON"
                        } else {
                            "OFF"
                        }
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
                "GRAPHICS\nON",
                amber
            ) {
                graphicsEnabled =
                    !graphicsEnabled

                graphicsButton.text =
                    "GRAPHICS\n" +
                        if (graphicsEnabled) {
                            "ON"
                        } else {
                            "OFF"
                        }
            }

        lensButton =
            liveSettingButton(
                "LENS\nBACK",
                cyan
            ) {
                if (
                    recording ==
                    null
                ) {
                    useFront =
                        !useFront

                    lensButton.text =
                        "LENS\n" +
                            if (useFront) {
                                "FRONT"
                            } else {
                                "BACK"
                            }

                    bindCamera()
                } else {
                    toast(
                        "Stop LIVE REC before changing lens"
                    )
                }
            }

        lightButton =
            liveSettingButton(
                "LIGHT\nOFF",
                white
            ) {
                toggleTorch()
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
                "VIEW\nFULL",
                green
            ) {
                togglePreviewMode()
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
                    toggleRecording()
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
            "VIEW\n" +
                if (halfPreviewMode) {
                    "HALF"
                } else {
                    "FULL"
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
                append("LIVE-ONLY SETTINGS\n\n")

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
                        .build()

                videoCapture =
                    VideoCapture.withOutput(
                        recorder
                    )

                overlayEffect =
                    OverlayEffect(
                        CameraEffect.PREVIEW or
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

                camLamp.setTextColor(
                    green
                )
            },
            ContextCompat.getMainExecutor(
                this
            )
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

        if (
            !graphicsEnabled
        ) {
            return
        }

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

        val u =
            minOf(
                finalWidth,
                finalHeight
            ) /
                1000f

        // V186: conservative social-safe margins. V184/V185 used 8% and
        // the CameraX crop could place the left side outside the saved MP4.
        val safeLeft =
            finalWidth *
                0.19f

        val safeTop =
            finalHeight *
                0.09f

        val safeRight =
            finalWidth *
                0.81f

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
                    3.0f * u,
                    0.8f * u,
                    0.8f * u,
                    0xE0000000.toInt()
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

        paint.textSize =
            26f * u

        paint.color =
            amber

        canvas.drawText(
            "develop.uganda",
            safeLeft +
                (23f * u),
            safeTop,
            paint
        )

        paint.textSize =
            13f * u

        paint.color =
            if (
                liveOn
            ) {
                red
            } else {
                white
            }

        canvas.drawText(
            if (
                liveOn
            ) {
                "LIVE"
            } else {
                "LIVE READY"
            },
            safeRight -
                (120f * u),
            safeTop,
            paint
        )

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
                (12f * u),
            safeRight,
            safeTop +
                (12f * u),
            rule
        )

        paint.textSize =
            12f * u

        paint.color =
            white

        drawFitText(
            canvas,
            "${profiles[profileIndex]} • $reporterName • STORY $storyId",
            safeLeft,
            safeTop +
                (36f * u),
            safeRight -
                safeLeft,
            paint,
            8.5f * u
        )

        paint.textSize =
            11.5f * u

        paint.color =
            cyan

        drawFitText(
            canvas,
            "TC ${liveTimecode()} • ${ZoneId.systemDefault().id} • $qualityLabel • ${if (audioEnabled) "MIC ON" else "MIC OFF"}",
            safeLeft,
            safeTop +
                (56f * u),
            safeRight -
                safeLeft,
            paint,
            8.2f * u
        )

        val lowerY =
            finalHeight *
                0.76f

        val lowerBg =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    0x9A05080A.toInt()

                style =
                    Paint.Style.FILL
            }

        val lowerH =
            88f * u

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
                    red

                strokeWidth =
                    5f * u
            }

        canvas.drawLine(
            safeLeft +
                (8f * u),
            lowerY +
                (10f * u),
            safeLeft +
                (8f * u),
            lowerY +
                lowerH -
                (10f * u),
            redRail
        )

        paint.color =
            red

        paint.textSize =
            12f * u

        canvas.drawText(
            if (
                liveOn
            ) {
                "● LIVE"
            } else {
                "LIVE STUDIO"
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
            18f * u

        drawFitText(
            canvas,
            headline,
            safeLeft +
                (22f * u),
            lowerY +
                (51f * u),
            safeRight -
                safeLeft -
                (40f * u),
            paint,
            11f * u
        )

        paint.color =
            0xFFD0D8DC.toInt()

        paint.textSize =
            10f * u

        drawFitText(
            canvas,
            "REPORTER $reporterName • STORY $storyId",
            safeLeft +
                (22f * u),
            lowerY +
                (72f * u),
            safeRight -
                safeLeft -
                (40f * u),
            paint,
            7.8f * u
        )

        canvas.restore()
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

        canvas.drawText(
            value,
            x,
            y,
            paint
        )

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
            "PROFILE\n${profiles[profileIndex]}"

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
            "QUALITY\n$qualityLabel"

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

        val values =
            ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    "DEVELOP_UGANDA_LIVE_${profiles[profileIndex]}_$stamp.mp4"
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
                        if (
                            audioEnabled &&
                            event.recordingStats
                                .audioStats
                                .audioAmplitude >
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

                        if (
                            event.hasError()
                        ) {
                            toast(
                                "LIVE REC failed"
                            )
                        } else {
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
                7.7f

            isAllCaps =
                false

            setTextColor(
                white
            )

            background =
                rounded(
                    0x7A11171A,
                    accent,
                    18
                )

            setOnClickListener {
                action.invoke()
            }
        }
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
            dp(44),
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

        private val glowPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = accent
                setShadowLayer(
                    13f,
                    0f,
                    0f,
                    accent
                )
            }

        init {
            setLayerType(
                LAYER_TYPE_SOFTWARE,
                null
            )
        }

        override fun drawableStateChanged() {
            super.drawableStateChanged()
            invalidate()
        }

        override fun onDraw(
            canvas: Canvas
        ) {
            super.onDraw(canvas)

            if (
                isPressed ||
                isSelected
            ) {
                val inset = 4f
                val radius =
                    height *
                        0.44f

                canvas.drawRoundRect(
                    inset,
                    inset,
                    width - inset,
                    height - inset,
                    radius,
                    radius,
                    glowPaint
                )
            }
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
