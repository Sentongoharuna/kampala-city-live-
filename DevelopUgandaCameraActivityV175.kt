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
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.ZoomState
import androidx.camera.effects.Frame
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class DevelopUgandaCameraActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout
    private lateinit var previewView: PreviewView
    private lateinit var brandView: TextView
    private lateinit var statusView: TextView
    private lateinit var timecodeView: TextView
    private lateinit var formatView: TextView
    private lateinit var locationView: TextView
    private lateinit var weatherView: TextView
    private lateinit var systemView: TextView
    private lateinit var recordButton: Button
    private lateinit var lensButton: Button
    private lateinit var torchButton: Button
    private lateinit var zoomSeek: SeekBar
    private lateinit var exposureSeek: SeekBar
    private lateinit var sceneButton: Button
    private lateinit var lookButton: Button
    private lateinit var qualityButton: Button

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var overlayEffect: OverlayEffect? = null
    private var useFront = false
    private var torchOn = false

    private val sceneModes = listOf(
        "REPORTER",
        "NEWS",
        "CINEMA",
        "MOVIE",
        "OUTDOOR",
        "INDOOR",
        "NIGHT"
    )
    private val lookModes = listOf(
        "CLEAN",
        "WARM",
        "COOL",
        "TEAL",
        "GOLD",
        "NIGHT"
    )
    private val qualityModes = listOf(
        "UHD",
        "FHD",
        "HD"
    )
    private var sceneIndex = 0
    private var lookIndex = 0
    private var qualityIndex = 0
    private var sceneExposureTarget = 0

    private val weather = WeatherRepository()
    private lateinit var telemetryRecorder: TelemetryRecorder
    private lateinit var fused: FusedLocationProviderClient

    @Volatile private var lat: Double? = null
    @Volatile private var lon: Double? = null
    @Volatile private var alt: Double? = null
    @Volatile private var accuracy: Float? = null
    @Volatile private var speedKmh: Float? = null
    @Volatile private var heading: Float? = null
    @Volatile private var placeName = "Locating…"
    @Volatile private var estimatedUploadKbps: Int? = null

    private var recStarted = 0L
    private var lastWeatherAt = 0L
    private var lastPlaceAt = 0L
    private var baseName = ""

    private val uiHandler = Handler(Looper.getMainLooper())
    private val clock = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val tick = object : Runnable {
        override fun run() {
            refreshHud()
            if (recording != null) writeTelemetry()
            uiHandler.postDelayed(this, 1000L)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val l = result.lastLocation ?: return

            lat = l.latitude
            lon = l.longitude
            alt = if (l.hasAltitude()) l.altitude else null
            accuracy = if (l.hasAccuracy()) l.accuracy else null
            speedKmh = if (l.hasSpeed()) l.speed * 3.6f else null
            heading = if (l.hasBearing()) l.bearing else null

            val now = System.currentTimeMillis()

            if (now - lastPlaceAt > 15_000L) {
                lastPlaceAt = now
                Thread {
                    try {
                        val a: Address? = Geocoder(
                            this@DevelopUgandaCameraActivity,
                            Locale.getDefault()
                        ).getFromLocation(l.latitude, l.longitude, 1)?.firstOrNull()

                        if (a != null) {
                            val p = listOfNotNull(
                                a.subLocality,
                                a.locality,
                                a.subAdminArea,
                                a.adminArea,
                                a.countryName
                            ).distinct().joinToString(", ")

                            if (p.isNotBlank()) placeName = p
                        }
                    } catch (_: Exception) {
                    }
                }.start()
            }

            if (now - lastWeatherAt > 10 * 60_000L) {
                lastWeatherAt = now
                weather.refresh(l.latitude, l.longitude) { }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        telemetryRecorder = TelemetryRecorder(this)
        fused = LocationServices.getFusedLocationProviderClient(this)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        buildUi()
        requestPermissionsAndStart()
        uiHandler.post(tick)
    }

    private fun buildUi() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            GuidesView(this),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Internal state views. The visible HUD is rendered by OverlayEffect so
        // preview and saved video share the same telemetry layout.
        brandView = hud(
            "develop.uganda",
            10f,
            0xFFFFC21A.toInt(),
            bold = true
        )
        statusView = hud(
            "STBY",
            8f,
            0xFFFF5A52.toInt(),
            bold = true
        )
        timecodeView = hud(
            "TC 00:00:00",
            7f,
            Color.WHITE
        )
        formatView = hud(
            "UHD • DEVICE FPS",
            7f,
            0xFFFFC21A.toInt()
        )
        locationView = hud(
            "GPS acquiring…",
            6f,
            0xFF7FE8FF.toInt()
        )
        weatherView = hud(
            "WX --",
            6f,
            0xFF8ECFFF.toInt()
        )
        systemView = hud(
            "MIC READY • NET -- • BAT -- • FREE --",
            6f,
            0xFF76E39A.toInt()
        )

        // ORBIT DECK: a custom floating control system. No large black panel.
        val bottomDeck = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(10),
                dp(4),
                dp(10),
                dp(10)
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        val modeRow = row().apply {
            gravity = Gravity.CENTER
        }

        sceneButton = deckButton(
            "SCENE\n${sceneModes[sceneIndex]}",
            0xFFFFC21A.toInt()
        )
        lookButton = deckButton(
            "LOOK\n${lookModes[lookIndex]}",
            0xFF7FE8FF.toInt()
        )
        qualityButton = deckButton(
            "FORMAT\n${qualityModes[qualityIndex]}",
            0xFFE8F1F2.toInt()
        )

        modeRow.addView(
            sceneButton,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )
        )
        modeRow.addView(
            space(dp(8)),
            wrap(8, 1)
        )
        modeRow.addView(
            lookButton,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )
        )
        modeRow.addView(
            space(dp(8)),
            wrap(8, 1)
        )
        modeRow.addView(
            qualityButton,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            )
        )
        bottomDeck.addView(modeRow)

        val zoomRow = row()
        zoomRow.addView(
            hud(
                "ZOOM",
                6.6f,
                Color.WHITE,
                bold = true
            ),
            wrap(48, 28)
        )
        zoomSeek = SeekBar(this).apply {
            max = 100
        }
        zoomRow.addView(
            zoomSeek,
            LinearLayout.LayoutParams(
                0,
                dp(28),
                1f
            )
        )
        bottomDeck.addView(zoomRow)

        val exposureRow = row()
        exposureRow.addView(
            hud(
                "EXP",
                6.6f,
                Color.WHITE,
                bold = true
            ),
            wrap(48, 28)
        )
        exposureSeek = SeekBar(this).apply {
            max = 12
            progress = 6
        }
        exposureRow.addView(
            exposureSeek,
            LinearLayout.LayoutParams(
                0,
                dp(28),
                1f
            )
        )
        bottomDeck.addView(exposureRow)

        val actionRow = row().apply {
            gravity = Gravity.CENTER
        }

        lensButton = deckButton(
            "LENS\nBACK",
            0xFF7FE8FF.toInt()
        )
        torchButton = deckButton(
            "LIGHT\nOFF",
            0xFFE8F1F2.toInt()
        )
        recordButton = recordButton()

        actionRow.addView(
            lensButton,
            wrap(86, 46)
        )
        actionRow.addView(
            space(dp(14)),
            wrap(14, 1)
        )
        actionRow.addView(
            recordButton,
            wrap(138, 54)
        )
        actionRow.addView(
            space(dp(14)),
            wrap(14, 1)
        )
        actionRow.addView(
            torchButton,
            wrap(86, 46)
        )

        bottomDeck.addView(actionRow)

        root.addView(
            bottomDeck,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        setContentView(root)

        sceneButton.setOnClickListener {
            cycleScene()
        }

        lookButton.setOnClickListener {
            cycleLook()
        }

        qualityButton.setOnClickListener {
            cycleQuality()
        }

        lensButton.setOnClickListener {
            if (recording == null) {
                useFront = !useFront
                lensButton.text =
                    "LENS\n${if (useFront) "FRONT" else "BACK"}"
                bindCamera()
            } else {
                toast(
                    "Stop recording before changing lens"
                )
            }
        }

        torchButton.setOnClickListener {
            toggleTorch()
        }

        recordButton.setOnClickListener {
            toggleRecording()
        }

        zoomSeek.setOnSeekBarChangeListener(
            simpleSeek {
                applyZoom(it)
            }
        )

        exposureSeek.setOnSeekBarChangeListener(
            simpleSeek {
                applyExposure(it - 6)
            }
        )

        previewView.setOnTouchListener { _, event ->
            if (
                event.action ==
                MotionEvent.ACTION_UP
            ) {
                tapToFocus(
                    event.x,
                    event.y
                )
                true
            } else {
                true
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val needed = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missing = needed.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                173
            )
        } else {
            startEverything()
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
        startEverything()
    }

    private fun startEverything() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }

        startLocation()
    }

    private fun startLocation() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        )
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdateDelayMillis(1000L)
            .build()

        fused.requestLocationUpdates(
            req,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            try {
                provider = future.get()
                bindCamera()
            } catch (e: Exception) {
                toast("Camera could not start")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera() {
        val p = provider ?: return

        p.unbindAll()

        try {
            overlayEffect?.close()
        } catch (_: Exception) {
        }

        overlayEffect = null

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(
                buildQualitySelector()
            )
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        overlayEffect = OverlayEffect(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            0,
            Handler(Looper.getMainLooper())
        ) { throwable ->
            toast(
                "Overlay warning: ${throwable.message ?: "unknown"}"
            )
        }.also { effect ->
            effect.setOnDrawListener { frame ->
                drawReporterOverlay(frame)
                true
            }
        }

        val session = SessionConfig.Builder(
            preview,
            videoCapture!!
        )
            .addEffect(overlayEffect!!)
            .build()

        val selector = if (useFront) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            camera = p.bindToLifecycle(
                this,
                selector,
                session
            )

            torchOn = false
            torchButton.text = "LIGHT\nOFF"

            syncCameraRanges()
            applyScenePreset()

            statusView.text = "STBY"
            statusView.setTextColor(0xFFFF5A52.toInt())
            refreshHud()
        } catch (e: Exception) {
            toast("Selected camera is unavailable")
        }
    }

    private fun drawReporterOverlay(frame: Frame) {
        val c = frame.overlayCanvas
        val crop = frame.cropRect

        if (
            crop.width() <= 0 ||
            crop.height() <= 0
        ) {
            return
        }

        c.drawColor(
            Color.TRANSPARENT,
            android.graphics.PorterDuff.Mode.CLEAR
        )

        val rotation = (
            (frame.rotationDegrees % 360) + 360
        ) % 360

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

        val l = crop.left.toFloat()
        val t = crop.top.toFloat()
        val r = crop.right.toFloat()
        val b = crop.bottom.toFloat()

        val nonMirrored = when (rotation) {
            90 -> floatArrayOf(
                l, b,
                l, t,
                r, t,
                r, b
            )

            180 -> floatArrayOf(
                r, b,
                l, b,
                l, t,
                r, t
            )

            270 -> floatArrayOf(
                r, t,
                r, b,
                l, b,
                l, t
            )

            else -> floatArrayOf(
                l, t,
                r, t,
                r, b,
                l, b
            )
        }

        val destination =
            if (frame.isMirroring) {
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

        val source = floatArrayOf(
            0f,
            0f,
            finalWidth,
            0f,
            finalWidth,
            finalHeight,
            0f,
            finalHeight
        )

        val finalToBuffer = Matrix()

        if (
            !finalToBuffer.setPolyToPoly(
                source,
                0,
                destination,
                0,
                4
            )
        ) {
            return
        }

        c.save()
        c.concat(finalToBuffer)

        drawCreativeLook(
            c,
            finalWidth,
            finalHeight
        )

        val u = minOf(
            finalWidth,
            finalHeight
        ) / 1000f

        // Wide title-safe area fixes the left-edge crop seen on V174.
        val safeLeft =
            finalWidth * 0.22f
        val safeTop =
            finalHeight * 0.060f
        val maxWidth =
            finalWidth * 0.72f

        var y = safeTop

        val text = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            typeface = Typeface.create(
                Typeface.MONOSPACE,
                Typeface.NORMAL
            )

            setShadowLayer(
                2.2f * u,
                0.7f * u,
                0.7f * u,
                0xE0000000.toInt()
            )
        }

        // Signature brand line.
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.color =
            0xFFFFC21A.toInt()
        text.textSize =
            26f * u

        val brand = "develop.uganda"

        c.drawText(
            brand,
            safeLeft,
            y,
            text
        )

        val brandWidth =
            text.measureText(brand)

        text.color = Color.WHITE
        text.textSize =
            11.5f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )

        c.drawText(
            "CITIZEN REPORT",
            safeLeft +
                brandWidth +
                (16f * u),
            y,
            text
        )

        val accent = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                0xB3FFC21A.toInt()
            strokeWidth =
                1.3f * u
        }

        c.drawLine(
            safeLeft,
            y + (7f * u),
            safeLeft + maxWidth,
            y + (7f * u),
            accent
        )

        // REC / time / date / timezone.
        y += 28f * u
        text.textSize =
            12f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.color =
            if (recording != null) {
                0xFFFF4138.toInt()
            } else {
                0xFFDCE7E9.toInt()
            }

        val recState =
            if (recording != null) {
                "● REC"
            } else {
                "STBY"
            }

        drawFitText(
            c,
            "$recState • TC ${tc()} • ${clock.format(Date())} • ${ZoneId.systemDefault().id}",
            safeLeft,
            y,
            maxWidth,
            text,
            9.2f * u
        )

        // Place.
        y += 17f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.NORMAL
        )
        text.color =
            Color.WHITE
        text.textSize =
            11.2f * u

        drawFitText(
            c,
            placeName,
            safeLeft,
            y,
            maxWidth,
            text,
            8.5f * u
        )

        // Coordinates.
        y += 16f * u
        text.color =
            0xFF7FE8FF.toInt()
        text.textSize =
            10.7f * u

        drawFitText(
            c,
            coordinateOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            8.2f * u
        )

        // Weather.
        y += 16f * u
        text.color =
            0xFF8ECFFF.toInt()
        text.textSize =
            10.5f * u

        drawFitText(
            c,
            weatherOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            8f * u
        )

        // System status.
        y += 16f * u
        text.color =
            0xFF76E39A.toInt()
        text.textSize =
            10.3f * u

        drawFitText(
            c,
            systemOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            7.9f * u
        )

        // Scene / look / camera status.
        y += 17f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.color =
            0xFFFFC21A.toInt()
        text.textSize =
            10.5f * u

        drawFitText(
            c,
            "SCENE ${sceneModes[sceneIndex]} • LOOK ${lookModes[lookIndex]} • ${qualityModes[qualityIndex]} • ${if (useFront) "FRONT" else "BACK"} • EXP $sceneExposureTarget • TAP AF",
            safeLeft,
            y,
            maxWidth,
            text,
            7.8f * u
        )

        // Truthful professional state.
        y += 16f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.NORMAL
        )
        text.color =
            0xFFDDE8EA.toInt()
        text.textSize =
            9.6f * u

        drawFitText(
            c,
            "ISO AUTO • SHUTTER AUTO • WB AUTO • MIC • GPS • GRID • LEVEL • CODEC DEVICE",
            safeLeft,
            y,
            maxWidth,
            text,
            7.3f * u
        )

        c.restore()
    }

    private fun coordinateOverlay(): String {
        val b = StringBuilder("GPS")

        if (lat != null && lon != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • LAT %.5f LON %.5f",
                    lat,
                    lon
                )
            )
        } else {
            b.append(" --")
        }

        if (alt != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • ALT %.0fm",
                    alt
                )
            )
        }

        if (accuracy != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • ±%.0fm",
                    accuracy
                )
            )
        }

        if (heading != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • HDG %.0f°",
                    heading
                )
            )
        }

        if (speedKmh != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • SPD %.1fkm/h",
                    speedKmh
                )
            )
        }

        return b.toString()
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
        var size = paint.textSize

        while (
            paint.measureText(value) > maxWidth &&
            size > minSize
        ) {
            size *= 0.94f
            paint.textSize = size
        }

        val textToDraw = if (
            paint.measureText(value) <= maxWidth
        ) {
            value
        } else {
            var end = value.length
            val ellipsis = "…"

            while (
                end > 1 &&
                paint.measureText(
                    value.substring(0, end) + ellipsis
                ) > maxWidth
            ) {
                end--
            }

            value.substring(0, end) + ellipsis
        }

        canvas.drawText(
            textToDraw,
            x,
            y,
            paint
        )
    }

    private fun toggleRecording() {
        val vc = videoCapture ?: run {
            toast("Camera is still starting")
            return
        }

        if (recording != null) {
            recording?.stop()
            return
        }

        baseName = "DEVELOP_UGANDA_${sceneModes[sceneIndex]}_${lookModes[lookIndex]}_" +
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date())

        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                baseName
            )
            put(
                MediaStore.Video.Media.MIME_TYPE,
                "video/mp4"
            )

            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "Movies/develop.uganda"
                )
            }
        }

        val out = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(values)
            .build()

        var pending: PendingRecording =
            vc.output.prepareRecording(
                this,
                out
            )

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pending = pending.withAudioEnabled()
        }

        recording = pending.start(
            ContextCompat.getMainExecutor(this)
        ) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    recStarted = System.currentTimeMillis()
                    statusView.text = "● REC"
                    statusView.setTextColor(
                        0xFFFF4138.toInt()
                    )
                    recordButton.text = "■ STOP"
                }

                is VideoRecordEvent.Finalize -> {
                    val hadError = event.hasError()

                    recording = null
                    recStarted = 0L
                    recordButton.text = "● RECORD"

                    if (hadError) {
                        statusView.text = "ERROR"
                        statusView.setTextColor(
                            0xFFFF4138.toInt()
                        )
                        toast("Recording failed")
                    } else {
                        statusView.text = "SAVED"
                        statusView.setTextColor(
                            0xFF76E39A.toInt()
                        )
                        telemetryRecorder.exportSidecar(
                            baseName
                        )
                        toast(
                            "Saved to Gallery • develop.uganda"
                        )

                        uiHandler.postDelayed({
                            statusView.text = "STBY"
                            statusView.setTextColor(
                                0xFFFF5A52.toInt()
                            )
                        }, 1800L)
                    }
                }

                else -> Unit
            }
        }
    }

    private fun refreshHud() {
        timecodeView.text =
            "TC ${tc()}"

        formatView.text =
            "${qualityModes[qualityIndex]} • DEVICE FPS • SCENE ${sceneModes[sceneIndex]} • LOOK ${lookModes[lookIndex]}"

        locationView.text =
            locationOverlay()
        weatherView.text =
            weatherOverlay()
        systemView.text =
            systemOverlay()

        if (::sceneButton.isInitialized) {
            sceneButton.text =
                "SCENE\n${sceneModes[sceneIndex]}"
        }

        if (::lookButton.isInitialized) {
            lookButton.text =
                "LOOK\n${lookModes[lookIndex]}"
        }

        if (::qualityButton.isInitialized) {
            qualityButton.text =
                "FORMAT\n${qualityModes[qualityIndex]}"
        }

        if (::lensButton.isInitialized) {
            lensButton.text =
                "LENS\n${if (useFront) "FRONT" else "BACK"}"
        }
    }

    private fun cycleScene() {
        if (recording != null) {
            toast(
                "Stop recording before changing scene"
            )
            return
        }

        sceneIndex =
            (sceneIndex + 1) %
                sceneModes.size

        applyScenePreset()
        refreshHud()
    }

    private fun cycleLook() {
        lookIndex =
            (lookIndex + 1) %
                lookModes.size

        refreshHud()
    }

    private fun cycleQuality() {
        if (recording != null) {
            toast(
                "Stop recording before changing format"
            )
            return
        }

        qualityIndex =
            (qualityIndex + 1) %
                qualityModes.size

        refreshHud()
        bindCamera()
    }

    private fun buildQualitySelector():
        QualitySelector {

        val ordered =
            when (
                qualityModes[
                    qualityIndex
                ]
            ) {
                "FHD" -> listOf(
                    Quality.FHD,
                    Quality.HD,
                    Quality.UHD
                )

                "HD" -> listOf(
                    Quality.HD,
                    Quality.FHD,
                    Quality.UHD
                )

                else -> listOf(
                    Quality.UHD,
                    Quality.FHD,
                    Quality.HD
                )
            }

        return QualitySelector
            .fromOrderedList(
                ordered,
                FallbackStrategy
                    .lowerQualityOrHigherThan(
                        Quality.HD
                    )
            )
    }

    private fun applyScenePreset() {
        sceneExposureTarget =
            when (
                sceneModes[
                    sceneIndex
                ]
            ) {
                "CINEMA" -> -1
                "MOVIE" -> -1
                "OUTDOOR" -> -1
                "INDOOR" -> 1
                "NIGHT" -> 2
                else -> 0
            }

        applyExposure(
            sceneExposureTarget
        )

        if (
            ::exposureSeek.isInitialized
        ) {
            exposureSeek.progress =
                (
                    sceneExposureTarget +
                        6
                    ).coerceIn(
                        0,
                        12
                    )
        }
    }

    private fun drawCreativeLook(
        canvas: Canvas,
        width: Float,
        height: Float
    ) {
        val color: Int =
            when (
                lookModes[
                    lookIndex
                ]
            ) {
                "WARM" ->
                    0x12FF8A45

                "COOL" ->
                    0x102F72FF

                "TEAL" ->
                    0x1200A7A1

                "GOLD" ->
                    0x12F2B43C

                "NIGHT" ->
                    0x18092346

                else ->
                    Color.TRANSPARENT
            }

        if (
            color ==
            Color.TRANSPARENT
        ) {
            return
        }

        val grade = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            this.color = color
        }

        canvas.drawRect(
            0f,
            0f,
            width,
            height,
            grade
        )
    }

    private fun writeTelemetry() {
        val now = System.currentTimeMillis()
        val elapsed = if (recStarted == 0L) {
            0L
        } else {
            now - recStarted
        }

        val wx = weather.latest

        telemetryRecorder.add(
            TelemetrySample(
                elapsedMs = elapsed,
                timestampIso = Instant.ofEpochMilli(now).toString(),
                timezone = ZoneId.systemDefault().id,
                latitude = lat,
                longitude = lon,
                altitudeM = alt,
                accuracyM = accuracy,
                headingDeg = heading,
                speedKmh = speedKmh,
                placeName = placeName,
                temperatureC = wx.temperatureC,
                condition = wx.condition,
                humidityPct = wx.humidityPct,
                windKmh = wx.windKmh,
                windDirectionDeg = wx.windDirectionDeg,
                networkType = networkType(),
                estimatedUploadKbps = estimatedUploadKbps,
                droppedFrames = 0L,
                batteryPct = batteryPct(),
                freeStorageGb = freeStorageGb()
            )
        )
    }

    private fun locationOverlay(): String {
        val b = StringBuilder(placeName)

        if (lat != null && lon != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • LAT %.5f LON %.5f",
                    lat,
                    lon
                )
            )
        }

        if (alt != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • ALT %.0fm",
                    alt
                )
            )
        }

        if (accuracy != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • GPS ±%.0fm",
                    accuracy
                )
            )
        }

        if (heading != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • HDG %.0f°",
                    heading
                )
            )
        }

        if (speedKmh != null) {
            b.append(
                String.format(
                    Locale.US,
                    " • SPD %.1fkm/h",
                    speedKmh
                )
            )
        }

        return b.toString()
    }

    private fun weatherOverlay(): String {
        val wx = weather.latest

        return buildString {
            append("WX ")

            if (wx.temperatureC != null) {
                append(
                    String.format(
                        Locale.US,
                        "%.1f°C",
                        wx.temperatureC
                    )
                )
            } else {
                append("--")
            }

            append(" • ${wx.condition}")

            if (wx.humidityPct != null) {
                append(" • RH ${wx.humidityPct}%")
            }

            if (wx.windKmh != null) {
                append(
                    String.format(
                        Locale.US,
                        " • WIND %.1fkm/h",
                        wx.windKmh
                    )
                )
            }

            if (wx.windDirectionDeg != null) {
                append(
                    String.format(
                        Locale.US,
                        " %.0f°",
                        wx.windDirectionDeg
                    )
                )
            }
        }
    }

    private fun systemOverlay(): String {
        val mic = if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (recording != null) {
                "MIC REC"
            } else {
                "MIC READY"
            }
        } else {
            "MIC OFF"
        }

        return "$mic • NET ${networkType()}" +
            " • BAT ${batteryPct() ?: "--"}%" +
            " • FREE ${freeStorageGb() ?: "--"}GB"
    }

    private fun networkType(): String {
        val cm = getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val n = cm.activeNetwork ?: return "OFFLINE"
        val caps = cm.getNetworkCapabilities(n)
            ?: return "OFFLINE"

        estimatedUploadKbps =
            caps.linkUpstreamBandwidthKbps
                .takeIf { it > 0 }

        return when {
            caps.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) -> "WiFi"

            caps.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) -> "CELL"

            caps.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            ) -> "ETH"

            else -> "NET"
        }
    }

    private fun batteryPct(): Int? {
        val bm = getSystemService(
            BATTERY_SERVICE
        ) as BatteryManager

        return bm.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        ).takeIf { it >= 0 }
    }

    private fun freeStorageGb(): Long? {
        return try {
            StatFs(
                Environment.getExternalStorageDirectory().path
            ).availableBytes / (
                1024L * 1024L * 1024L
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun tc(): String {
        val ms = if (recStarted == 0L) {
            0L
        } else {
            System.currentTimeMillis() - recStarted
        }

        val seconds = ms / 1000L

        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            seconds / 3600L,
            (seconds % 3600L) / 60L,
            seconds % 60L
        )
    }

    private fun toggleTorch() {
        val cam = camera ?: return

        if (
            !cam.cameraInfo.hasFlashUnit() ||
            useFront
        ) {
            toast("Torch is unavailable on this lens")
            return
        }

        torchOn = !torchOn
        cam.cameraControl.enableTorch(torchOn)
        torchButton.text = if (torchOn) {
            "LIGHT\nON"
        } else {
            "LIGHT\nOFF"
        }
    }

    private fun syncCameraRanges() {
        val cam = camera ?: return

        val z: ZoomState? =
            cam.cameraInfo.zoomState.value

        if (z != null) {
            val progress =
                if (
                    z.maxZoomRatio >
                    z.minZoomRatio
                ) {
                    (
                        (
                            z.zoomRatio -
                                z.minZoomRatio
                            ) /
                            (
                                z.maxZoomRatio -
                                    z.minZoomRatio
                                ) *
                            100f
                        ).roundToInt()
                } else {
                    0
                }

            zoomSeek.progress =
                progress.coerceIn(0, 100)
        }

        val e: ExposureState =
            cam.cameraInfo.exposureState

        exposureSeek.isEnabled =
            e.isExposureCompensationSupported

        if (
            e.isExposureCompensationSupported
        ) {
            exposureSeek.progress =
                (
                    e.exposureCompensationIndex +
                        6
                    ).coerceIn(0, 12)
        }
    }

    private fun applyZoom(progress: Int) {
        val cam = camera ?: return
        val z =
            cam.cameraInfo.zoomState.value
                ?: return

        val ratio =
            z.minZoomRatio +
                (
                    z.maxZoomRatio -
                        z.minZoomRatio
                    ) *
                (progress / 100f)

        cam.cameraControl.setZoomRatio(
            ratio
        )
    }

    private fun applyExposure(value: Int) {
        val cam = camera ?: return
        val e = cam.cameraInfo.exposureState

        if (
            !e.isExposureCompensationSupported
        ) {
            return
        }

        cam.cameraControl
            .setExposureCompensationIndex(
                value.coerceIn(
                    e.exposureCompensationRange.lower,
                    e.exposureCompensationRange.upper
                )
            )
    }

    private fun tapToFocus(
        x: Float,
        y: Float
    ) {
        val cam = camera ?: return

        try {
            val point =
                previewView.meteringPointFactory
                    .createPoint(x, y)

            val action =
                FocusMeteringAction.Builder(point)
                    .setAutoCancelDuration(
                        3,
                        TimeUnit.SECONDS
                    )
                    .build()

            cam.cameraControl
                .startFocusAndMetering(action)

            toast("Focus")
        } catch (_: Exception) {
        }
    }

    private fun simpleSeek(
        onChange: (Int) -> Unit
    ) = object :
        SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(
            seekBar: SeekBar?,
            progress: Int,
            fromUser: Boolean
        ) {
            if (fromUser) {
                onChange(progress)
            }
        }

        override fun onStartTrackingTouch(
            seekBar: SeekBar?
        ) {
        }

        override fun onStopTrackingTouch(
            seekBar: SeekBar?
        ) {
        }
    }

    private fun row(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
        }
    }

    private fun hud(
        value: String,
        sp: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = sp
            setTextColor(color)
            gravity =
                Gravity.CENTER_VERTICAL

            typeface = Typeface.create(
                Typeface.MONOSPACE,
                if (bold) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )

            setShadowLayer(
                2f,
                0.7f,
                0.7f,
                0xCC000000.toInt()
            )

            setPadding(
                dp(3),
                dp(1),
                dp(3),
                dp(1)
            )
        }
    }

    private fun deckButton(
        value: String,
        accentColor: Int
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 7.1f
            isAllCaps = false
            setTextColor(Color.WHITE)
            minHeight = dp(40)
            setPadding(
                dp(4),
                0,
                dp(4),
                0
            )

            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE
                    cornerRadius =
                        dp(18).toFloat()

                    setColor(
                        0x3D030A0E
                    )

                    setStroke(
                        dp(1),
                        accentColor
                    )
                }
        }
    }

    private fun actionButton(
        value: String
    ): Button {
        return deckButton(
            value,
            0x99FFFFFF.toInt()
        )
    }

    private fun recordButton(): Button {
        return Button(this).apply {
            text = "● RECORD"
            textSize = 10f
            isAllCaps = false
            setTextColor(Color.WHITE)
            minHeight = dp(48)

            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE
                    cornerRadius =
                        dp(26).toFloat()

                    setColor(
                        0xCCFF2D20.toInt()
                    )

                    setStroke(
                        dp(1),
                        0xFFFF8A84.toInt()
                    )
                }
        }
    }

    private fun space(width: Int): View {
        return View(this).apply {
            minimumWidth = width
        }
    }

    private fun weight():
        LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )
    }

    private fun wrap(
        widthDp: Int,
        heightDp: Int
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            dp(widthDp),
            dp(heightDp)
        )
    }

    private fun dp(value: Int): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    private fun toast(value: String) {
        Toast.makeText(
            this,
            value,
            Toast.LENGTH_SHORT
        ).show()
    }

    private inner class GuidesView(
        context: Context
    ) : View(context) {

        private val gridPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0x40FFFFFF
                strokeWidth = dp(1).toFloat()
            }

        private val levelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xCCFFC21A.toInt()
                strokeWidth = dp(1).toFloat()
            }

        private val focusPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xCC7FE8FF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = dp(1).toFloat()
            }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val w = width.toFloat()
            val h = height.toFloat()

            if (w <= 0f || h <= 0f) return

            canvas.drawLine(
                w / 3f,
                0f,
                w / 3f,
                h,
                gridPaint
            )

            canvas.drawLine(
                w * 2f / 3f,
                0f,
                w * 2f / 3f,
                h,
                gridPaint
            )

            canvas.drawLine(
                0f,
                h / 3f,
                w,
                h / 3f,
                gridPaint
            )

            canvas.drawLine(
                0f,
                h * 2f / 3f,
                w,
                h * 2f / 3f,
                gridPaint
            )

            val cy = h / 2f
            val cx = w / 2f

            canvas.drawLine(
                cx - dp(42),
                cy,
                cx - dp(8),
                cy,
                levelPaint
            )

            canvas.drawLine(
                cx + dp(8),
                cy,
                cx + dp(42),
                cy,
                levelPaint
            )

            canvas.drawLine(
                cx,
                cy - dp(5),
                cx,
                cy + dp(5),
                levelPaint
            )

            val r = dp(20).toFloat()

            canvas.drawRect(
                cx - r,
                cy - r,
                cx + r,
                cy + r,
                focusPaint
            )

            // Signature ORBIT marks around the focus zone.
            val orbitPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        0x88FFC21A.toInt()
                    style =
                        Paint.Style.STROKE
                    strokeWidth =
                        dp(1).toFloat()
                }

            val orbitRadius =
                dp(42).toFloat()

            canvas.drawArc(
                cx - orbitRadius,
                cy - orbitRadius,
                cx + orbitRadius,
                cy + orbitRadius,
                205f,
                70f,
                false,
                orbitPaint
            )

            canvas.drawArc(
                cx - orbitRadius,
                cy - orbitRadius,
                cx + orbitRadius,
                cy + orbitRadius,
                25f,
                70f,
                false,
                orbitPaint
            )

            // Title-safe corner marks.
            val insetX =
                w * 0.08f
            val insetY =
                h * 0.08f
            val arm =
                dp(20).toFloat()

            canvas.drawLine(
                insetX,
                insetY,
                insetX + arm,
                insetY,
                focusPaint
            )
            canvas.drawLine(
                insetX,
                insetY,
                insetX,
                insetY + arm,
                focusPaint
            )

            canvas.drawLine(
                w - insetX,
                insetY,
                w - insetX - arm,
                insetY,
                focusPaint
            )
            canvas.drawLine(
                w - insetX,
                insetY,
                w - insetX,
                insetY + arm,
                focusPaint
            )
        }
    }

    override fun onDestroy() {
        uiHandler.removeCallbacksAndMessages(
            null
        )

        try {
            fused.removeLocationUpdates(
                locationCallback
            )
        } catch (_: Exception) {
        }

        try {
            recording?.stop()
        } catch (_: Exception) {
        }

        try {
            overlayEffect?.close()
        } catch (_: Exception) {
        }

        try {
            provider?.unbindAll()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
