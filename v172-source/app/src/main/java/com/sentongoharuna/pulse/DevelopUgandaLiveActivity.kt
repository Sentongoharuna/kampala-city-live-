package com.sentongoharuna.pulse

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
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
    private lateinit var qualityButton: Button
    private lateinit var audioButton: Button
    private lateinit var graphicsButton: Button
    private lateinit var lensButton: Button
    private lateinit var lightButton: Button
    private lateinit var outputButton: Button
    private lateinit var recordButton: LiveRecordButtonView
    private lateinit var timerView: TextView
    private lateinit var outputStatus: TextView

    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var useFront = false
    private var audioEnabled = true
    private var graphicsEnabled = true
    private var quality = Quality.FHD
    private var qualityLabel = "FHD"
    private var recordStartMs = 0L

    private val uiHandler =
        Handler(Looper.getMainLooper())

    private var liveBlinkOn = true

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
    private val panel = 0xB80A0E11.toInt()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        buildLiveUi()
        requestNeededPermissions()
        uiHandler.post(uiTicker)
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(
            null
        )
        recording?.stop()
        recording = null
        super.onDestroy()
    }

    private fun buildLiveUi() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        previewView =
            PreviewView(this).apply {
                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE
                scaleType =
                    PreviewView.ScaleType.FIT_START
            }

        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val graphics =
            LiveGraphicsView(this)

        root.addView(
            graphics,
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
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )
                background =
                    rounded(
                        0x71000000,
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
                "● LIVE REC",
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

        topPanel.addView(header)

        liveSubTitle =
            label(
                "LIVE STUDIO • LOCAL CAPTURE",
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
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(12)
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
                "OUTPUT • LOCAL GALLERY • INTERNET STREAM NOT CONNECTED",
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

        val settingsRow1 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
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

        graphicsButton =
            liveSettingButton(
                "GRAPHICS\nON",
                amber
            ) {
                graphicsEnabled =
                    !graphicsEnabled
                graphics.visibility =
                    if (graphicsEnabled) {
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }

                graphicsButton.text =
                    "GRAPHICS\n" +
                        if (graphicsEnabled) {
                            "ON"
                        } else {
                            "OFF"
                        }
            }

        settingsRow1.addView(
            qualityButton,
            weight()
        )
        settingsRow1.addView(
            audioButton,
            weight()
        )
        settingsRow1.addView(
            graphicsButton,
            weight()
        )

        liveDeck.addView(
            settingsRow1
        )

        val settingsRow2 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                setPadding(
                    0,
                    dp(7),
                    0,
                    0
                )
            }

        lensButton =
            liveSettingButton(
                "LENS\nBACK",
                cyan
            ) {
                if (
                    recording == null
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
                val current =
                    camera
                        ?.cameraInfo
                        ?.torchState
                        ?.value
                        ?: TorchState.OFF

                camera
                    ?.cameraControl
                    ?.enableTorch(
                        current !=
                            TorchState.ON
                    )

                lightButton.text =
                    "LIGHT\n" +
                        if (
                            current ==
                            TorchState.ON
                        ) {
                            "OFF"
                        } else {
                            "ON"
                        }
            }

        outputButton =
            liveSettingButton(
                "OUTPUT\nLOCAL",
                red
            ) {
                showOutputInfo()
            }

        settingsRow2.addView(
            lensButton,
            weight()
        )
        settingsRow2.addView(
            lightButton,
            weight()
        )
        settingsRow2.addView(
            outputButton,
            weight()
        )

        liveDeck.addView(
            settingsRow2
        )

        val recordArea =
            FrameLayout(this).apply {
                setPadding(
                    0,
                    dp(10),
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
                dp(120)
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

        setContentView(root)
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

        if (missing.isEmpty()) {
            bindCamera()
        } else {
            requestPermissions(
                missing.toTypedArray(),
                1831
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

        if (requestCode == 1831) {
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
        val providerFuture =
            ProcessCameraProvider.getInstance(
                this
            )

        providerFuture.addListener(
            {
                val provider =
                    providerFuture.get()

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
                        preview,
                        videoCapture
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

    private fun cycleQuality() {
        if (recording != null) {
            toast(
                "Stop LIVE REC before changing quality"
            )
            return
        }

        if (quality == Quality.FHD) {
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

    private fun toggleRecording() {
        if (recording != null) {
            recording?.stop()
            return
        }

        val capture =
            videoCapture ?: run {
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
                    "DEVELOP_UGANDA_LIVE_$stamp.mp4"
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
                when (event) {
                    is VideoRecordEvent.Start -> {
                        recordStartMs =
                            SystemClock.elapsedRealtime()

                        recordButton.setRecordingState(
                            true
                        )

                        liveSubTitle.text =
                            "LIVE STUDIO • RECORDING TO GALLERY"

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
                                .audioAmplitude > 0.0
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
                            "LIVE STUDIO • LOCAL CAPTURE"

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

    private fun showOutputInfo() {
        AlertDialog.Builder(this)
            .setTitle(
                "LIVE OUTPUT"
            )
            .setMessage(
                "V183 LIVE STUDIO records the dedicated live-feed camera to the phone Gallery. " +
                    "The interface is ready for a future RTMP/SRT/WebRTC destination, but this build does not falsely claim an internet livestream is connected."
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun updateSignals() {
        netLamp.setTextColor(
            if (isNetworkConnected()) {
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
            if (camera != null) {
                green
            } else {
                red
            }
        )

        recLamp.setTextColor(
            if (recording != null) {
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
                percent >= 30 -> green
                percent >= 15 -> amber
                else -> red
            }
        )
    }

    private fun updateTimer() {
        if (
            recording == null ||
            recordStartMs == 0L
        ) {
            timerView.text =
                "00:00:00"
            return
        }

        val seconds =
            (
                SystemClock.elapsedRealtime() -
                    recordStartMs
                ) / 1000L

        timerView.text =
            String.format(
                Locale.US,
                "%02d:%02d:%02d",
                seconds / 3600L,
                (seconds / 60L) % 60L,
                seconds % 60L
            )
    }

    private fun updateBlink() {
        if (recording == null) {
            liveBadge.alpha =
                0.85f
            return
        }

        liveBlinkOn =
            !liveBlinkOn

        liveBadge.alpha =
            if (liveBlinkOn) {
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
            ) ?: return false

        return caps.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    private fun liveSettingButton(
        textValue: String,
        accent: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text =
                textValue
            textSize =
                9f
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
                    if (bold) {
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
            setColor(fill)

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
            dp(58),
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
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
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
                    10f
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

        private val stop =
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

        private val pulseRunnable =
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
                if (value) {
                    0xFFFF2D24.toInt()
                } else {
                    0xFF5E1110.toInt()
                }

            pulseHandler
                .removeCallbacks(
                    pulseRunnable
                )

            if (value) {
                pulseHandler.post(
                    pulseRunnable
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
                ) * 0.29f

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
                        ) / 2.0

                glow.alpha =
                    (
                        45 +
                            (wave * 115)
                        ).roundToInt()

                canvas.drawCircle(
                    cx,
                    cy,
                    base +
                        13f +
                        (wave * 7f).toFloat(),
                    glow
                )
            }

            canvas.drawCircle(
                cx,
                cy,
                base + 7f,
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
                    base * 0.28f

                canvas.drawRoundRect(
                    cx - half,
                    cy - half,
                    cx + half,
                    cy + half,
                    half * 0.28f,
                    half * 0.28f,
                    stop
                )
            } else {
                val dot =
                    base * 0.17f

                canvas.drawCircle(
                    cx,
                    cy,
                    dot,
                    stop
                )
            }
        }
    }

    private class LiveGraphicsView(
        context: Context
    ) : View(context) {

        private val grid =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    0x40FFFFFF
                strokeWidth =
                    1f
            }

        private val red =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    0xAAFF3B32.toInt()
                strokeWidth =
                    3f
                style =
                    Paint.Style.STROKE
            }

        private val green =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    0xAA62E889.toInt()
                strokeWidth =
                    2f
                style =
                    Paint.Style.STROKE
            }

        override fun onDraw(
            canvas: Canvas
        ) {
            super.onDraw(
                canvas
            )

            val w =
                width.toFloat()
            val h =
                minOf(
                    height.toFloat(),
                    w * 16f / 9f
                )

            canvas.drawLine(
                w / 3f,
                0f,
                w / 3f,
                h,
                grid
            )
            canvas.drawLine(
                w * 2f / 3f,
                0f,
                w * 2f / 3f,
                h,
                grid
            )
            canvas.drawLine(
                0f,
                h / 3f,
                w,
                h / 3f,
                grid
            )
            canvas.drawLine(
                0f,
                h * 2f / 3f,
                w,
                h * 2f / 3f,
                grid
            )

            val cx =
                w / 2f
            val cy =
                h / 2f

            canvas.drawCircle(
                cx,
                cy,
                42f,
                green
            )

            canvas.drawCircle(
                cx,
                cy,
                58f,
                red
            )
        }
    }
}
