package com.sentongoharuna.pulse

import android.Manifest
import android.app.AlertDialog
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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.GnssStatus
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.TorchState
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
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin

class DevelopUgandaCameraActivity : AppCompatActivity(), SensorEventListener {

    private companion object {
        const val ACTION_SCENE = 1
        const val ACTION_LOOK = 2
        const val ACTION_QUALITY = 3
        const val ACTION_CAPTURE_MODE = 4
        const val ACTION_LENS = 5
        const val ACTION_TORCH = 6
        const val ACTION_RECORD = 7
        const val ACTION_IDENTITY = 8
        const val ACTION_VIEW_MODE = 9
        const val ACTION_SETTINGS = 10
        const val ACTION_GUIDES = 11
        const val ACTION_RESET = 12
        const val ACTION_AUTO_UI = 13
        const val ACTION_LOCK = 14
        const val ACTION_INTEGRITY = 15
        const val ACTION_CAPABILITIES = 16
        const val ACTION_CLEAN = 17
    }


    private lateinit var root: FrameLayout
    private lateinit var previewView: PreviewView
    private lateinit var guidesView: GuidesView

    // V187: dedicated screen-space narration. This is visible to the
    // operator but is NOT burned into the recording. The recorded overlay
    // keeps its existing safe geometry independently.
    private lateinit var previewNarrationPanel: LinearLayout
    private lateinit var previewBrandView: TextView
    private lateinit var previewTagView: TextView
    private lateinit var previewIdentityView: TextView
    private lateinit var previewClockView: TextView
    private lateinit var previewModeView: TextView
    private lateinit var previewPlaceView: TextView
    private lateinit var previewGpsView: TextView
    private lateinit var previewNavView: TextView
    private lateinit var previewSystemView: TextView
    private lateinit var previewHealthView: TextView

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
    private lateinit var captureModeButton: Button
    private lateinit var identityButton: Button
    private lateinit var viewModeButton: Button
    private lateinit var settingsButton: Button
    private lateinit var guidesButton: Button
    private lateinit var resetButton: Button
    private lateinit var settingsSummaryView: TextView
    private lateinit var reportRecordGlow: ReportRecordGlowView

    private lateinit var bottomDeck: LinearLayout
    private lateinit var modeRow: LinearLayout
    private lateinit var identityRow: LinearLayout
    private lateinit var reportToolsRow: LinearLayout
    private lateinit var reportAdvancedRow: LinearLayout
    private lateinit var zoomRow: LinearLayout
    private lateinit var exposureRow: LinearLayout
    private lateinit var actionRow: LinearLayout

    private lateinit var autoUiButton: Button
    private lateinit var lockButton: Button
    private lateinit var integrityButton: Button
    private lateinit var capabilitiesButton: Button
    private lateinit var cleanModeButton: Button

    private var halfPreviewMode = false
    private var detailedSettingsVisible = false
    private var previewGuidesEnabled = true
    private var autoHideOperatorUi = true
    private var operatorLocked = false
    private var integrityEnabled = true
    private var operatorControlsHidden = false
    private var cleanModeEnabled = false

    private var gestureDownX = 0f
    private var gestureDownY = 0f
    private var gestureStartZoom = 1f
    private var gestureStartExposure = 0
    private var gestureMoved = false
    private var lastPreviewTapMs = 0L

    private val autoHideRunnable =
        Runnable {
            if (
                autoHideOperatorUi &&
                recording != null
            ) {
                setReportOperatorControlsHidden(
                    true
                )
            }
        }

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
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
        "NIGHT",
        "INTERVIEW",
        "DOCUMENTARY"
    )
    private val lookModes = listOf(
        "CLEAN",
        "NATURAL",
        "WARM",
        "COOL",
        "TEAL",
        "GOLD",
        "NIGHT",
        "MONO"
    )
    private val qualityModes = listOf(
        "SOCIAL FHD",
        "MASTER UHD",
        "FAST HD"
    )
    private val captureModes = listOf(
        "VIDEO",
        "PHOTO"
    )
    private var sceneIndex = 0
    private var lookIndex = 0
    private var qualityIndex = 0
    private var captureModeIndex = 0
    private var sceneExposureTarget = 0

    private val weather = WeatherRepository()
    private lateinit var telemetryRecorder: TelemetryRecorder
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private lateinit var locationManager: LocationManager

    @Volatile private var lat: Double? = null
    @Volatile private var lon: Double? = null
    @Volatile private var alt: Double? = null
    @Volatile private var accuracy: Float? = null
    @Volatile private var speedKmh: Float? = null
    @Volatile private var heading: Float? = null
    @Volatile private var placeName = "Locating…"
    @Volatile private var estimatedUploadKbps: Int? = null
    @Volatile private var lastGpsUpdateMs = 0L
    @Volatile private var distanceTravelledM = 0f
    @Volatile private var previousTrackLat: Double? = null
    @Volatile private var previousTrackLon: Double? = null
    @Volatile private var compassAzimuthDeg: Float? = null
    @Volatile private var phonePitchDeg: Float? = null
    @Volatile private var phoneRollDeg: Float? = null
    @Volatile private var gnssSatellitesVisible = -1
    @Volatile private var gnssSatellitesUsed = -1
    @Volatile private var audioAmplitude = 0.0
    @Volatile private var audioStateLabel = "MIC READY"

    private var gnssCallbackHolder: Any? = null
    private var reporterName = "CITIZEN"
    private var storyId = ""
    private var reportId = ""
    private var reportDisplayMode = "FIELD REPORT"
    private var clipSequence = 0
    private var recordStartUtc = "--"


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
            lastGpsUpdateMs = now

            if (recording != null) {
                val pLat = previousTrackLat
                val pLon = previousTrackLon

                if (pLat != null && pLon != null) {
                    val result = FloatArray(1)
                    android.location.Location.distanceBetween(
                        pLat,
                        pLon,
                        l.latitude,
                        l.longitude,
                        result
                    )

                    val segmentM = result[0]
                    val goodEnough =
                        (accuracy ?: 999f) <= 60f

                    if (
                        goodEnough &&
                        segmentM >= 0.7f &&
                        segmentM <= 250f
                    ) {
                        distanceTravelledM += segmentM
                    }
                }

                previousTrackLat = l.latitude
                previousTrackLon = l.longitude
            }

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
        sensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR
            )
        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        loadReporterIdentity()
        reportId = newReportId()

        reportDisplayMode =
            intent.getStringExtra("develop_uganda_mode")
                ?.trim()
                ?.uppercase(Locale.US)
                ?.takeIf { it.isNotBlank() }
                ?: "FIELD REPORT"

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

            // V181: show the full 9:16 recording canvas in the live camera.
            // FILL_CENTER was cropping the left/right sides of the preview,
            // even though the exported video was correct. FIT_START keeps the
            // entire recorded frame visible and places any spare screen space
            // below it, where the operator controls already live.
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // V187 PREVIEW HUD:
        // CameraX output graphics are no longer used for the PREVIEW target.
        // This panel is drawn in phone-screen coordinates, so develop.uganda
        // and every narration line remain inside the visible camera screen.
        previewNarrationPanel =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(12),
                    dp(7),
                    dp(12),
                    dp(7)
                )

                background =
                    ColorDrawable(
                        Color.TRANSPARENT
                    )
            }

        val previewBrandRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        previewBrandView =
            hud(
                "develop.uganda",
                13.8f,
                0xFFFFC21A.toInt(),
                bold = true
            )

        previewTagView =
            hud(
                "FIELD REPORT",
                7.1f,
                Color.WHITE,
                bold = true
            ).apply {
                setPadding(
                    dp(7),
                    0,
                    0,
                    0
                )
            }

        previewBrandRow.addView(
            previewBrandView
        )

        previewBrandRow.addView(
            previewTagView
        )

        previewNarrationPanel.addView(
            previewBrandRow
        )

        previewIdentityView =
            hud(
                "",
                6.1f,
                Color.WHITE,
                bold = true
            )

        previewClockView =
            hud(
                "",
                6.0f,
                0xFFFF6B63.toInt(),
                bold = true
            )

        previewModeView =
            hud(
                "",
                5.8f,
                0xFFFFC21A.toInt(),
                bold = true
            )

        previewPlaceView =
            hud(
                "",
                5.8f,
                Color.WHITE
            )

        previewGpsView =
            hud(
                "",
                5.6f,
                0xFF7FE8FF.toInt()
            )

        previewNavView =
            hud(
                "",
                5.6f,
                0xFF7FE8FF.toInt()
            )

        previewSystemView =
            hud(
                "",
                5.4f,
                0xFF76E39A.toInt()
            )

        previewHealthView =
            hud(
                "",
                5.7f,
                0xFF76E39A.toInt(),
                bold = true
            )

        listOf(
            previewIdentityView,
            previewClockView,
            previewModeView,
            previewPlaceView,
            previewGpsView,
            previewNavView,
            previewSystemView,
            previewHealthView
        ).forEach {
            it.maxLines = 1
            it.isSingleLine = true

            previewNarrationPanel.addView(
                it,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(16)
                )
            )
        }

        val previewHudParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity =
                    Gravity.TOP

                // Keep the operator HUD below Android status icons/time.
                topMargin =
                    dp(44)

                leftMargin =
                    dp(10)

                rightMargin =
                    dp(10)
            }

        root.addView(
            previewNarrationPanel,
            previewHudParams
        )

        applyAdaptiveReportPreviewTypography()

        guidesView = GuidesView(this)

        root.addView(
            guidesView,
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
            "SOCIAL FHD • DEVICE FPS",
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
        bottomDeck = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(10),
                dp(4),
                dp(10),
                dp(10)
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        modeRow = row().apply {
            gravity = Gravity.CENTER
        }

        sceneButton = deckButton(
            "SCENE ▾\n${sceneModes[sceneIndex]}",
            0xFFFFC21A.toInt()
        )
        lookButton = deckButton(
            "LOOK ▾\n${lookModes[lookIndex]}",
            0xFF7FE8FF.toInt()
        )
        qualityButton = deckButton(
            "FORMAT ▾\n${qualityDeckLabel()}",
            0xFFE8F1F2.toInt()
        )
        captureModeButton = deckButton(
            "CAPTURE ▾\n${captureModes[captureModeIndex]}",
            0xFF76E39A.toInt()
        )

        listOf(
            sceneButton,
            lookButton,
            qualityButton,
            captureModeButton
        ).forEachIndexed { index, button ->
            modeRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(40),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(5)
                    }
                }
            )
            if (false) {
                modeRow.addView(
                    space(dp(1)),
                    wrap(1, 1)
                )
            }
        }
        bottomDeck.addView(modeRow)

        identityRow = row().apply {
            gravity = Gravity.CENTER
        }
        identityButton = deckButton(
            identityButtonText(),
            0xFFFFC21A.toInt()
        )
        identityRow.addView(
            identityButton,
            LinearLayout.LayoutParams(
                0,
                dp(38),
                1f
            )
        )
        bottomDeck.addView(identityRow)

        // V185: FIELD REPORT camera has its own role-specific view and
        // settings controls. These are deliberately separate from LIVE STUDIO.
        reportToolsRow = row().apply {
            gravity = Gravity.CENTER
        }

        viewModeButton = deckButton(
            "VIEW ▾\nFULL",
            0xFF76E39A.toInt()
        )

        settingsButton = deckButton(
            "SETTINGS\nREPORT",
            0xFF7FE8FF.toInt()
        )

        guidesButton = deckButton(
            "GUIDES ▾\nON",
            0xFFFFC21A.toInt()
        )

        resetButton = deckButton(
            "RESET\nCAM",
            0xFFFF8A84.toInt()
        )

        listOf(
            viewModeButton,
            settingsButton,
            guidesButton,
            resetButton
        ).forEachIndexed { index, button ->
            reportToolsRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(34),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart = dp(5)
                    }
                }
            )
        }

        bottomDeck.addView(reportToolsRow)

        // V188: compact operator controls for the FIELD REPORT role.
        reportAdvancedRow =
            row().apply {
                gravity =
                    Gravity.CENTER
            }

        autoUiButton =
            deckButton(
                "AUTO UI ▾\nON",
                0xFF76E39A.toInt()
            ).apply {
                isSelected = true
            }

        lockButton =
            deckButton(
                "LOCK ▾\nOFF",
                0xFFFFC21A.toInt()
            )

        integrityButton =
            deckButton(
                "VERIFY ▾\nSHA-256",
                0xFF7FE8FF.toInt()
            ).apply {
                isSelected = true
            }

        capabilitiesButton =
            deckButton(
                "CAMERA\nCAPS",
                0xFFE8F1F2.toInt()
            )

        cleanModeButton =
            deckButton(
                "CLEAN ▾\nOFF",
                0xFF76E39A.toInt()
            )

        listOf(
            autoUiButton,
            lockButton,
            integrityButton,
            capabilitiesButton,
            cleanModeButton
        ).forEachIndexed { index, button ->
            reportAdvancedRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(31),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(4)
                    }
                }
            )
        }

        bottomDeck.addView(
            reportAdvancedRow
        )

        settingsSummaryView = hud(
            reportSettingsSummary(),
            5.9f,
            0xFFC9D7DD.toInt()
        ).apply {
            visibility = View.GONE
            setPadding(
                dp(8),
                dp(5),
                dp(8),
                dp(5)
            )
            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE
                    cornerRadius =
                        dp(10).toFloat()
                    setColor(
                        0x5A071014
                    )
                    setStroke(
                        dp(1),
                        0x607FE8FF
                    )
                }
        }

        bottomDeck.addView(settingsSummaryView)

        zoomRow = row()
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

        exposureRow = row()
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

        actionRow = row().apply {
            gravity = Gravity.CENTER
        }

        lensButton = deckButton(
            "LENS ▾\nBACK",
            0xFF7FE8FF.toInt()
        )
        torchButton = deckButton(
            "LIGHT ▾\nOFF",
            0xFFE8F1F2.toInt()
        )
        recordButton = makeRecordButton()

        reportRecordGlow =
            ReportRecordGlowView(this)

        val reportRecordArea =
            FrameLayout(this)

        reportRecordArea.addView(
            reportRecordGlow,
            FrameLayout.LayoutParams(
                dp(150),
                dp(62),
                Gravity.CENTER
            )
        )

        reportRecordArea.addView(
            recordButton,
            FrameLayout.LayoutParams(
                dp(132),
                dp(50),
                Gravity.CENTER
            )
        )

        actionRow.addView(
            lensButton,
            wrap(82, 46)
        )
        actionRow.addView(
            space(dp(8)),
            wrap(8, 1)
        )
        actionRow.addView(
            reportRecordArea,
            wrap(150, 62)
        )
        actionRow.addView(
            space(dp(8)),
            wrap(8, 1)
        )
        actionRow.addView(
            torchButton,
            wrap(82, 46)
        )

        bottomDeck.addView(actionRow)

        // Avoid the 3-argument FrameLayout.LayoutParams overload that
        // Kotlin/Android API 36 is resolving ambiguously in this project.
        val bottomDeckParams =
            FrameLayout.LayoutParams(
                -1, // MATCH_PARENT
                -2  // WRAP_CONTENT
            )
        bottomDeckParams.gravity =
            Gravity.BOTTOM
        root.addView(
            bottomDeck,
            bottomDeckParams
        )

        setContentView(root)

        sceneButton.setOnTouchListener(
            DeckTouchListener(ACTION_SCENE)
        )

        lookButton.setOnTouchListener(
            DeckTouchListener(ACTION_LOOK)
        )

        qualityButton.setOnTouchListener(
            DeckTouchListener(ACTION_QUALITY)
        )

        captureModeButton.setOnTouchListener(
            DeckTouchListener(ACTION_CAPTURE_MODE)
        )

        identityButton.setOnTouchListener(
            DeckTouchListener(ACTION_IDENTITY)
        )

        viewModeButton.setOnTouchListener(
            DeckTouchListener(ACTION_VIEW_MODE)
        )

        settingsButton.setOnTouchListener(
            DeckTouchListener(ACTION_SETTINGS)
        )

        guidesButton.setOnTouchListener(
            DeckTouchListener(ACTION_GUIDES)
        )

        resetButton.setOnTouchListener(
            DeckTouchListener(ACTION_RESET)
        )

        autoUiButton.setOnTouchListener(
            DeckTouchListener(ACTION_AUTO_UI)
        )

        lockButton.setOnTouchListener(
            DeckTouchListener(ACTION_LOCK)
        )

        integrityButton.setOnTouchListener(
            DeckTouchListener(ACTION_INTEGRITY)
        )

        capabilitiesButton.setOnTouchListener(
            DeckTouchListener(ACTION_CAPABILITIES)
        )

        cleanModeButton.setOnTouchListener(
            DeckTouchListener(ACTION_CLEAN)
        )

        lensButton.setOnTouchListener(
            DeckTouchListener(ACTION_LENS)
        )

        torchButton.setOnTouchListener(
            DeckTouchListener(ACTION_TORCH)
        )

        recordButton.setOnTouchListener(
            DeckTouchListener(ACTION_RECORD)
        )

        zoomSeek.setOnSeekBarChangeListener(
            simpleSeek {
                applyZoom(it)
            }
        )

        exposureSeek.setOnSeekBarChangeListener(
            simpleSeek {
                sceneExposureTarget = it - 6
                applyExposure(sceneExposureTarget)
            }
        )

        previewView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    showReportOperatorControlsTemporarily()

                    gestureDownX =
                        event.x

                    gestureDownY =
                        event.y

                    gestureStartZoom =
                        camera
                            ?.cameraInfo
                            ?.zoomState
                            ?.value
                            ?.zoomRatio
                            ?: 1f

                    gestureStartExposure =
                        camera
                            ?.cameraInfo
                            ?.exposureState
                            ?.exposureCompensationIndex
                            ?: 0

                    gestureMoved =
                        false

                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (
                        !operatorLocked
                    ) {
                        val dx =
                            event.x -
                                gestureDownX

                        val dy =
                            event.y -
                                gestureDownY

                        if (
                            maxOf(
                                abs(dx),
                                abs(dy)
                            ) >
                            dp(18)
                        ) {
                            gestureMoved =
                                true
                        }

                        val cam =
                            camera

                        if (
                            cam != null &&
                            gestureMoved
                        ) {
                            if (
                                abs(dy) >
                                abs(dx)
                            ) {
                                val zoomState =
                                    cam.cameraInfo
                                        .zoomState
                                        .value

                                if (
                                    zoomState != null
                                ) {
                                    val span =
                                        (
                                            zoomState.maxZoomRatio -
                                                zoomState.minZoomRatio
                                            )
                                            .coerceAtLeast(
                                                0.1f
                                            )

                                    val ratio =
                                        (
                                            gestureStartZoom -
                                                (
                                                    dy /
                                                        previewView.height
                                                    ) *
                                                    span
                                            )
                                            .coerceIn(
                                                zoomState.minZoomRatio,
                                                zoomState.maxZoomRatio
                                            )

                                    cam.cameraControl
                                        .setZoomRatio(
                                            ratio
                                        )
                                }
                            } else {
                                val state =
                                    cam.cameraInfo
                                        .exposureState

                                val range =
                                    state.exposureCompensationRange

                                val total =
                                    (
                                        range.upper -
                                            range.lower
                                        )
                                        .coerceAtLeast(
                                            1
                                        )

                                val delta =
                                    (
                                        dx /
                                            previewView.width *
                                            total
                                        )
                                        .roundToInt()

                                val target =
                                    (
                                        gestureStartExposure +
                                            delta
                                        )
                                        .coerceIn(
                                            range.lower,
                                            range.upper
                                        )

                                cam.cameraControl
                                    .setExposureCompensationIndex(
                                        target
                                    )
                            }
                        }
                    }

                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (
                        !gestureMoved
                    ) {
                        val now =
                            SystemClock.elapsedRealtime()

                        if (
                            now -
                                lastPreviewTapMs <
                            340L &&
                            !operatorLocked
                        ) {
                            if (
                                recording ==
                                null
                            ) {
                                useFront =
                                    !useFront

                                lensButton.text =
                                    if (useFront) {
                                        "LENS\nFRONT"
                                    } else {
                                        "LENS\nBACK"
                                    }

                                bindCamera()
                                toast(
                                    "Lens switched"
                                )
                            }
                        } else {
                            tapToFocus(
                                event.x,
                                event.y
                            )
                        }

                        lastPreviewTapMs =
                            now
                    }

                    true
                }

                MotionEvent.ACTION_CANCEL ->
                    true

                else ->
                    true
            }
        }
    }

    private fun togglePreviewMode() {
        halfPreviewMode =
            !halfPreviewMode

        val previewHeight =
            if (halfPreviewMode) {
                (
                    resources.displayMetrics.heightPixels *
                        0.50f
                    ).roundToInt()
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }

        val previewParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                previewHeight
            ).apply {
                gravity = Gravity.TOP
            }

        previewView.layoutParams =
            previewParams

        // FULL intentionally fills under every control including the red
        // record button. HALF shows the complete 9:16 camera frame in the
        // upper half and leaves a dedicated operator/settings area below.
        previewView.scaleType =
            if (halfPreviewMode) {
                PreviewView.ScaleType.FIT_CENTER
            } else {
                PreviewView.ScaleType.FILL_CENTER
            }

        val guideParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                previewHeight
            ).apply {
                gravity = Gravity.TOP
            }

        guidesView.layoutParams =
            guideParams

        viewModeButton.text =
            "VIEW ▾\n" +
                if (halfPreviewMode) {
                    "HALF"
                } else {
                    "FULL"
                }

        if (
            ::previewNarrationPanel.isInitialized
        ) {
            val hudParams =
                previewNarrationPanel.layoutParams as
                    FrameLayout.LayoutParams

            hudParams.topMargin =
                if (halfPreviewMode) {
                    dp(34)
                } else {
                    dp(44)
                }

            previewNarrationPanel.layoutParams =
                hudParams

            previewBrandView.textSize =
                if (halfPreviewMode) {
                    11.6f
                } else {
                    13.8f
                }
        }

        if (halfPreviewMode) {
            detailedSettingsVisible =
                true
            settingsSummaryView.visibility =
                View.VISIBLE
        } else {
            settingsSummaryView.visibility =
                if (detailedSettingsVisible) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        refreshReportSettingsSummary()

        toast(
            if (halfPreviewMode) {
                "Half-screen report view"
            } else {
                "Full-screen camera behind controls"
            }
        )
    }

    private fun showReportDetailedSettings() {
        detailedSettingsVisible =
            true

        settingsSummaryView.visibility =
            if (halfPreviewMode) {
                View.VISIBLE
            } else {
                View.GONE
            }

        refreshReportSettingsSummary()

        val message =
            buildString {
                append(reportSettingsSummary())
                append("\n\nRECORD OUTPUT\n")
                append("9:16 portrait • telemetry burn-in • Report ID • reporter/story identity")
                append("\n\nPREVIEW\n")
                append(
                    if (halfPreviewMode) {
                        "HALF — complete camera frame above operator controls"
                    } else {
                        "FULL — camera image fills the phone behind all controls"
                    }
                )
                append("\n\nROLE\n")
                append("FIELD REPORT settings remain independent from LIVE STUDIO settings.")
            }

        AlertDialog.Builder(this)
            .setTitle("FIELD REPORT SETTINGS")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun togglePreviewGuides() {
        previewGuidesEnabled =
            !previewGuidesEnabled

        guidesView.visibility =
            if (previewGuidesEnabled) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

        guidesButton.text =
            "GUIDES ▾\n" +
                if (previewGuidesEnabled) {
                    "ON"
                } else {
                    "OFF"
                }

        toast(
            if (previewGuidesEnabled) {
                "Preview guides on"
            } else {
                "Preview guides off"
            }
        )
    }

    private fun resetReportCameraSettings() {
        if (recording != null) {
            toast("Stop recording before reset")
            return
        }

        sceneIndex = 0
        lookIndex = 0
        qualityIndex = 0
        captureModeIndex = 0
        sceneExposureTarget = 0

        sceneButton.text =
            "SCENE ▾\n${sceneModes[sceneIndex]}"

        lookButton.text =
            "LOOK ▾\n${lookModes[lookIndex]}"

        qualityButton.text =
            "FORMAT ▾\n${qualityDeckLabel()}"

        captureModeButton.text =
            "CAPTURE ▾\n${captureModes[captureModeIndex]}"

        exposureSeek.progress = 6

        camera?.cameraControl
            ?.setExposureCompensationIndex(0)

        refreshReportSettingsSummary()
        bindCamera()
        toast("Field Report camera reset")
    }

    private fun refreshReportSettingsSummary() {
        if (
            !::settingsSummaryView.isInitialized
        ) {
            return
        }

        settingsSummaryView.text =
            reportSettingsSummary()
    }

    private fun reportSettingsSummary(): String {
        val lens =
            if (useFront) {
                "FRONT"
            } else {
                "BACK"
            }

        val capture =
            captureModes[
                captureModeIndex
            ]

        return buildString {
            append("REPORT SETTINGS • ")
            append("VIEW ")
            append(
                if (halfPreviewMode) {
                    "HALF"
                } else {
                    "FULL"
                }
            )
            append(" • CAPTURE ")
            append(capture)
            append("\n")

            append("SCENE ")
            append(
                sceneModes[
                    sceneIndex
                ]
            )
            append(" • LOOK ")
            append(
                lookModes[
                    lookIndex
                ]
            )
            append(" • FORMAT ")
            append(
                qualityModes[
                    qualityIndex
                ]
            )
            append("\n")

            append("LENS ")
            append(lens)
            append(" • EXP ")
            append(sceneExposureTarget)
            append(" • ZOOM LIVE • TAP AF")
            append("\n")

            append("GUIDES ")
            append(
                if (previewGuidesEnabled) {
                    "ON"
                } else {
                    "OFF"
                }
            )
            append(" • AUTO UI ")
            append(
                if (autoHideOperatorUi) {
                    "ON"
                } else {
                    "OFF"
                }
            )
            append(" • VERIFY ")
            append(
                if (integrityEnabled) {
                    "SHA-256"
                } else {
                    "OFF"
                }
            )
            append(" • CLEAN ")
            append(
                if (cleanModeEnabled) {
                    "ON"
                } else {
                    "OFF"
                }
            )
            append(" • SWIPE ZOOM/EXP • DOUBLE TAP LENS")
            append(" • TELEMETRY BURN-IN ON • GPS/GNSS • COMPASS • WEATHER • LEVEL • MIC • NET • BAT • STORAGE")
            append("\n")

            append("REPORT ID ")
            append(
                reportId.ifBlank {
                    "--"
                }
            )
            append(" • REPORTER ")
            append(
                reporterName.ifBlank {
                    "CITIZEN"
                }
            )
        }
    }

    private fun toggleReportAutoUi() {
        autoHideOperatorUi =
            !autoHideOperatorUi

        autoUiButton.text =
            "AUTO UI ▾\n" +
                if (autoHideOperatorUi) {
                    "ON"
                } else {
                    "OFF"
                }

        autoUiButton.isSelected =
            autoHideOperatorUi

        if (!autoHideOperatorUi) {
            uiHandler.removeCallbacks(
                autoHideRunnable
            )
            setReportOperatorControlsHidden(
                false
            )
        } else if (
            recording != null
        ) {
            showReportOperatorControlsTemporarily()
        }

        refreshReportSettingsSummary()
    }

    private fun toggleReportOperatorLock() {
        operatorLocked =
            !operatorLocked

        lockButton.text =
            "LOCK ▾\n" +
                if (operatorLocked) {
                    "ON"
                } else {
                    "OFF"
                }

        lockButton.isSelected =
            operatorLocked

        toast(
            if (operatorLocked) {
                "Report controls locked"
            } else {
                "Report controls unlocked"
            }
        )
    }

    private fun toggleReportIntegrity() {
        if (recording != null) {
            toast(
                "Change verification before recording"
            )
            return
        }

        integrityEnabled =
            !integrityEnabled

        integrityButton.text =
            "VERIFY ▾\n" +
                if (integrityEnabled) {
                    "SHA-256"
                } else {
                    "OFF"
                }

        integrityButton.isSelected =
            integrityEnabled

        refreshReportSettingsSummary()
    }

    private fun setReportOperatorControlsHidden(
        hidden: Boolean
    ) {
        operatorControlsHidden =
            hidden

        val visibility =
            if (hidden) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

        listOf(
            modeRow,
            identityRow,
            reportToolsRow,
            reportAdvancedRow,
            zoomRow,
            exposureRow
        ).forEach {
            it.visibility =
                visibility
        }

        settingsSummaryView.visibility =
            if (
                hidden
            ) {
                View.GONE
            } else if (
                detailedSettingsVisible &&
                halfPreviewMode
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

        // Lens / record / light remain visible at all times.
        actionRow.visibility =
            View.VISIBLE
    }

    private fun showReportOperatorControlsTemporarily() {
        if (
            !autoHideOperatorUi
        ) {
            setReportOperatorControlsHidden(
                false
            )
            return
        }

        setReportOperatorControlsHidden(
            false
        )

        uiHandler.removeCallbacks(
            autoHideRunnable
        )

        if (
            recording != null
        ) {
            uiHandler.postDelayed(
                autoHideRunnable,
                3000L
            )
        }
    }

    private fun showCameraCapabilities() {
        val info =
            camera?.cameraInfo

        if (info == null) {
            toast(
                "Camera capabilities not ready"
            )
            return
        }

        val zoom =
            info.zoomState.value

        val exposure =
            info.exposureState

        val range =
            exposure.exposureCompensationRange

        val message =
            buildString {
                append("DEVICE CAMERA CAPABILITIES\n\n")
                append("LENS: ")
                append(
                    if (useFront) {
                        "FRONT"
                    } else {
                        "BACK"
                    }
                )
                append("\n")

                append("FLASH/TORCH: ")
                append(
                    if (info.hasFlashUnit()) {
                        "AVAILABLE"
                    } else {
                        "UNAVAILABLE"
                    }
                )
                append("\n")

                append("ZOOM: ")
                append(
                    String.format(
                        Locale.US,
                        "%.1fx – %.1fx",
                        zoom?.minZoomRatio ?: 1f,
                        zoom?.maxZoomRatio ?: 1f
                    )
                )
                append("\n")

                append("EXPOSURE COMP: ")
                append(range.lower)
                append(" to ")
                append(range.upper)
                append("\n")

                append("CURRENT EXP: ")
                append(
                    exposure.exposureCompensationIndex
                )
                append("\n")

                append("REQUESTED FORMAT: ")
                append(
                    qualityModes[
                        qualityIndex
                    ]
                )
                append("\n")

                append("CAPTURE: ")
                append(
                    captureModes[
                        captureModeIndex
                    ]
                )
                append("\n\n")

                append(
                    "FPS, HDR, codec, stabilization and manual sensor controls remain DEVICE unless the CameraX/Camera2 layer confirms direct control."
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                "FIELD CAMERA CAPABILITIES"
            )
            .setMessage(
                message
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    private fun createIntegrityRecord(
        videoUri: Uri,
        finishedUtc: String
    ) {
        if (
            !integrityEnabled
        ) {
            return
        }

        val reportIdSnapshot =
            reportId
        val reporterSnapshot =
            reporterDisplayName()
        val storySnapshot =
            storyDisplayId()
        val startSnapshot =
            recordStartUtc
        val baseSnapshot =
            baseName
        val latSnapshot =
            lat
        val lonSnapshot =
            lon
        val altSnapshot =
            alt
        val accSnapshot =
            accuracy
        val sceneSnapshot =
            sceneModes[
                sceneIndex
            ]
        val lookSnapshot =
            lookModes[
                lookIndex
            ]

        Thread {
            try {
                val digest =
                    MessageDigest.getInstance(
                        "SHA-256"
                    )

                contentResolver
                    .openInputStream(
                        videoUri
                    )
                    ?.use { input ->
                        val buffer =
                            ByteArray(
                                256 * 1024
                            )

                        while (true) {
                            val read =
                                input.read(
                                    buffer
                                )

                            if (read <= 0) {
                                break
                            }

                            digest.update(
                                buffer,
                                0,
                                read
                            )
                        }
                    }
                    ?: error(
                        "Unable to read final video"
                    )

                val sha256 =
                    digest.digest()
                        .joinToString(
                            ""
                        ) {
                            "%02x".format(
                                it
                            )
                        }

                val json =
                    JSONObject().apply {
                        put(
                            "schema",
                            "develop.uganda.report.integrity.v1"
                        )
                        put(
                            "app_version",
                            "V188"
                        )
                        put(
                            "report_id",
                            reportIdSnapshot
                        )
                        put(
                            "reporter",
                            reporterSnapshot
                        )
                        put(
                            "story_id",
                            storySnapshot
                        )
                        put(
                            "filename",
                            "$baseSnapshot.mp4"
                        )
                        put(
                            "record_start_utc",
                            startSnapshot
                        )
                        put(
                            "record_end_utc",
                            finishedUtc
                        )
                        put(
                            "sha256",
                            sha256
                        )
                        put(
                            "scene",
                            sceneSnapshot
                        )
                        put(
                            "look",
                            lookSnapshot
                        )
                        put(
                            "latitude",
                            latSnapshot
                        )
                        put(
                            "longitude",
                            lonSnapshot
                        )
                        put(
                            "altitude_m",
                            altSnapshot
                        )
                        put(
                            "gps_accuracy_m",
                            accSnapshot
                        )
                        put(
                            "note",
                            "SHA-256 identifies this finalized file; it is not a digital signature or proof of authorship."
                        )
                    }

                saveIntegrityJson(
                    "${baseSnapshot}_INTEGRITY.json",
                    json.toString(
                        2
                    )
                )

                runOnUiThread {
                    toast(
                        "Integrity SHA-256 saved"
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    toast(
                        "Integrity record failed"
                    )
                }
            }
        }.start()
    }

    private fun saveIntegrityJson(
        fileName: String,
        content: String
    ) {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            val values =
                ContentValues().apply {
                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        fileName
                    )
                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/json"
                    )
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        "Download/develop.uganda/Integrity"
                    )
                }

            val uri =
                contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                    ?: error(
                        "Could not create integrity file"
                    )

            contentResolver
                .openOutputStream(
                    uri
                )
                ?.use {
                    it.write(
                        content.toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }
                ?: error(
                    "Could not write integrity file"
                )
        } else {
            val dir =
                File(
                    getExternalFilesDir(
                        Environment.DIRECTORY_DOCUMENTS
                    ),
                    "develop.uganda/Integrity"
                )

            dir.mkdirs()

            FileOutputStream(
                File(
                    dir,
                    fileName
                )
            ).use {
                it.write(
                    content.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }
        }
    }

    private fun applyAdaptiveReportPreviewTypography() {
        val widthDp =
            resources.configuration
                .screenWidthDp

        val brandSize =
            when {
                widthDp <= 360 ->
                    11.5f

                widthDp <= 420 ->
                    12.8f

                else ->
                    13.8f
            }

        val rowSize =
            when {
                widthDp <= 360 ->
                    5.2f

                widthDp <= 420 ->
                    5.7f

                else ->
                    6.1f
            }

        previewBrandView.textSize =
            brandSize

        previewTagView.textSize =
            rowSize +
                0.8f

        listOf(
            previewIdentityView,
            previewClockView,
            previewModeView,
            previewPlaceView,
            previewGpsView,
            previewNavView,
            previewSystemView,
            previewHealthView
        ).forEach {
            it.textSize =
                rowSize
        }
    }

    private fun toggleReportCleanMode() {
        cleanModeEnabled =
            !cleanModeEnabled

        cleanModeButton.text =
            "CLEAN ▾\n" +
                if (cleanModeEnabled) {
                    "ON"
                } else {
                    "OFF"
                }

        cleanModeButton.isSelected =
            cleanModeEnabled

        if (
            cleanModeEnabled
        ) {
            listOf(
                modeRow,
                identityRow,
                reportToolsRow,
                reportAdvancedRow,
                zoomRow,
                exposureRow
            ).forEach {
                it.visibility =
                    View.INVISIBLE
            }

            settingsSummaryView.visibility =
                View.GONE

            previewIdentityView.visibility =
                View.GONE

            previewModeView.visibility =
                View.GONE

            previewPlaceView.visibility =
                View.GONE

            previewGpsView.visibility =
                View.GONE

            previewNavView.visibility =
                View.GONE

            previewSystemView.visibility =
                View.GONE

            previewClockView.visibility =
                View.VISIBLE

            previewHealthView.visibility =
                View.VISIBLE

            actionRow.visibility =
                View.VISIBLE
        } else {
            setReportOperatorControlsHidden(
                false
            )

            listOf(
                previewIdentityView,
                previewClockView,
                previewModeView,
                previewPlaceView,
                previewGpsView,
                previewNavView,
                previewSystemView,
                previewHealthView
            ).forEach {
                it.visibility =
                    View.VISIBLE
            }
        }

        toast(
            if (cleanModeEnabled) {
                "Clean operator mode"
            } else {
                "Full report controls"
            }
        )
    }

    private fun recordingHealthText(): String {
        val battery =
            batteryPct()
                ?: -1

        val storage =
            freeStorageGb()
                ?: -1L

        val gps =
            accuracy

        val micReady =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) ==
                PackageManager.PERMISSION_GRANTED

        val state =
            when {
                battery in 0..9 ->
                    "CRITICAL"

                storage in 0..1L ->
                    "CRITICAL"

                !micReady ->
                    "CHECK MIC"

                gps != null &&
                    gps >
                    50f ->
                    "GPS WEAK"

                battery in 10..19 ->
                    "BAT LOW"

                storage in 2..4L ->
                    "SPACE LOW"

                else ->
                    "GOOD"
            }

        return buildString {
            append("REC HEALTH ")
            append(state)

            append(" • BAT ")
            append(
                if (battery >= 0) {
                    "$battery%"
                } else {
                    "--"
                }
            )

            append(" • FREE ")
            append(
                if (storage >= 0L) {
                    "${storage}GB"
                } else {
                    "--"
                }
            )

            append(" • GPS ")
            append(
                gps?.let {
                    String.format(
                        Locale.US,
                        "±%.0fm",
                        it
                    )
                } ?: "--"
            )

            append(" • MIC ")
            append(
                if (micReady) {
                    "READY"
                } else {
                    "OFF"
                }
            )
        }
    }

    private fun recordingHealthColor(): Int {
        val text =
            recordingHealthText()

        return when {
            text.contains(
                "CRITICAL"
            ) ->
                0xFFFF4D42.toInt()

            text.contains(
                "LOW"
            ) ||
                text.contains(
                    "WEAK"
                ) ||
                text.contains(
                    "CHECK"
                ) ->
                0xFFFFC21A.toInt()

            else ->
                0xFF76E39A.toInt()
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
        startGnssMonitor()
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

        val photoMode =
            captureModes[captureModeIndex] == "PHOTO"

        videoCapture = null
        imageCapture = null

        if (photoMode) {
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(
                    ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                )
                .setJpegQuality(100)
                .build()
        } else {
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    buildQualitySelector()
                )
                .setTargetVideoEncodingBitRate(
                    targetVideoBitrate()
                )
                .build()

            videoCapture =
                VideoCapture.withOutput(
                    recorder
                )
        }

        // V187: keep CameraX burn-in graphics off the live PreviewView.
        // The preview has its own screen-space narration panel, while the
        // IMAGE/VIDEO output keeps the proven recorded overlay geometry.
        val effectTargets =
            if (photoMode) {
                CameraEffect.IMAGE_CAPTURE
            } else {
                CameraEffect.VIDEO_CAPTURE
            }

        overlayEffect = OverlayEffect(
            effectTargets,
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

        val session =
            if (photoMode) {
                SessionConfig.Builder(
                    preview,
                    imageCapture!!
                )
                    .addEffect(
                        overlayEffect!!
                    )
                    .build()
            } else {
                SessionConfig.Builder(
                    preview,
                    videoCapture!!
                )
                    .addEffect(
                        overlayEffect!!
                    )
                    .build()
            }

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

            statusView.text =
                if (photoMode) "PHOTO READY" else "STBY"
            statusView.setTextColor(0xFFFF5A52.toInt())
            recordButton.text =
                if (photoMode) "● PHOTO" else "● RECORD"

            if (
                ::reportRecordGlow.isInitialized
            ) {
                reportRecordGlow.setRecordingState(
                    false
                )
            }

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

        // V177 social-safe broadcast HUD:
        // all telemetry is retained, but reorganized into deliberate rows.
        // Values such as LAT/LON/ALT/HDG/SPD/FIX/DIST continue changing live.
        // V178 uses a deeper social-safe inset. The previous transform
        // placed the left edge too close to the exported crop on some phones.
        // V181 WYSIWYG preview + V180 broadcast-safe recorded layout:
        // identity stays inside social-media safe margins while the
        // upper-right is reserved for the live compass/level instruments.
        // V180: deliberately deeper safe margins for both the CameraX
        // preview crop and the exported 9:16 video. This prevents the brand,
        // telemetry and instruments from being clipped at the phone edges.
        val safeLeft =
            finalWidth * 0.19f
        val safeTop =
            finalHeight * 0.125f
        val maxWidth =
            finalWidth * 0.47f

        var y = safeTop

        val text = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            typeface = Typeface.create(
                Typeface.MONOSPACE,
                Typeface.NORMAL
            )

            setShadowLayer(
                2.8f * u,
                0.8f * u,
                0.8f * u,
                0xED000000.toInt()
            )
        }

        // Minimal broadcast signature: no black panel, only a brand rail.
        val rail = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                if (recording != null) {
                    0xFFFF4138.toInt()
                } else {
                    0xFFFFC21A.toInt()
                }
            strokeWidth = 3.2f * u
        }

        c.drawLine(
            safeLeft - (10f * u),
            y - (4f * u),
            safeLeft - (10f * u),
            y + (246f * u),
            rail
        )

        // 1. Signature.
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.color =
            0xFFFFC21A.toInt()
        text.textSize =
            32f * u

        val brand = "develop.uganda"

        c.drawText(
            brand,
            safeLeft,
            y,
            text
        )

        // V182: keep develop.uganda and the report type on one broadcast line.
        // The WYSIWYG preview now shows the complete 9:16 frame, so both labels
        // can stay together without being cropped.
        val brandWidth =
            text.measureText(brand)

        text.color =
            if (reportDisplayMode == "LIVE EFFECT") {
                0xFFFF5A52.toInt()
            } else {
                Color.WHITE
            }
        text.textSize =
            11.5f * u

        c.drawText(
            sceneTag(),
            safeLeft +
                brandWidth +
                (14f * u),
            y,
            text
        )

        val accent = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                0xB3FFC21A.toInt()
            strokeWidth =
                1.35f * u
        }

        c.drawLine(
            safeLeft,
            y + (10f * u),
            safeLeft + maxWidth,
            y + (10f * u),
            accent
        )

        // Real live field instruments. These are part of the recorded HUD.
        val instrumentCenterX =
            finalWidth * 0.74f
        val compassCenterY =
            safeTop + (61f * u)

        drawCompassInstrument(
            c,
            instrumentCenterX,
            compassCenterY,
            39f * u,
            u
        )

        drawAudioMeterInstrument(
            c,
            finalWidth * 0.675f,
            safeTop + (117f * u),
            finalWidth * 0.13f,
            9f * u,
            u
        )

        drawLevelInstrument(
            c,
            instrumentCenterX,
            safeTop + (149f * u),
            78f * u,
            26f * u,
            u
        )

        // 2. Stable report identity rows. Shorter rows keep the full
        // develop.uganda identity readable instead of shrinking off-screen.
        y += 28f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.color = Color.WHITE
        text.textSize = 12.4f * u

        drawFitText(
            c,
            "REPORT ID $reportId • CLIP ${clipSequenceText()}",
            safeLeft,
            y,
            maxWidth,
            text,
            9.3f * u
        )

        y += 15f * u
        drawFitText(
            c,
            "REPORTER ${reporterDisplayName()} • STORY ${storyDisplayId()}",
            safeLeft,
            y,
            maxWidth,
            text,
            9.2f * u
        )

        // 3. REC / timecode / date and local clock.
        y += 17f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.textSize = 13.0f * u
        text.color =
            if (recording != null) {
                0xFFFF4138.toInt()
            } else {
                0xFFE6EEF0.toInt()
            }

        val recState =
            when {
                recording != null -> "● REC"
                captureModes[captureModeIndex] == "PHOTO" -> "● PHOTO"
                else -> "STBY"
            }

        drawFitText(
            c,
            "$recState • TC ${tc()} • ${clock.format(Date())}",
            safeLeft,
            y,
            maxWidth,
            text,
            10.3f * u
        )

        y += 15f * u
        text.textSize = 11.4f * u
        drawFitText(
            c,
            "${ZoneId.systemDefault().id} • UTC ${utcClockText()} • START $recordStartUtc",
            safeLeft,
            y,
            maxWidth,
            text,
            8.3f * u
        )

        // 4. Editorial / camera identity.
        y += 16f * u
        text.color =
            0xFFFFC21A.toInt()
        text.textSize =
            11.7f * u

        drawFitText(
            c,
            "SCENE ${sceneModes[sceneIndex]} • LOOK ${lookModes[lookIndex]} • ${qualityModes[qualityIndex]} • ${captureModes[captureModeIndex]}",
            safeLeft,
            y,
            maxWidth,
            text,
            8.3f * u
        )

        // 5. Place.
        y += 16f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.NORMAL
        )
        text.color = Color.WHITE
        text.textSize = 12.0f * u

        drawFitText(
            c,
            placeName,
            safeLeft,
            y,
            maxWidth,
            text,
            8.6f * u
        )

        // 6. Live changing coordinates.
        y += 16f * u
        text.color =
            0xFF7FE8FF.toInt()
        text.textSize =
            11.5f * u

        drawFitText(
            c,
            coordinatePrimaryOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            8.2f * u
        )

        // 7. GPS altitude / accuracy / satellites / fix age.
        y += 15f * u
        text.color =
            0xFF7FE8FF.toInt()
        text.textSize =
            11.1f * u

        drawFitText(
            c,
            gnssOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            7.9f * u
        )

        // 8. Compass / GPS bearing / speed / motion / distance.
        y += 15f * u
        text.color =
            0xFF7FE8FF.toInt()
        text.textSize =
            11.0f * u

        drawFitText(
            c,
            navigationOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            7.8f * u
        )

        // 9. Real phone orientation / horizon angle.
        y += 15f * u
        text.color =
            0xFFDDE8EA.toInt()
        text.textSize =
            10.7f * u

        drawFitText(
            c,
            orientationOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            7.6f * u
        )

        // 10. Weather.
        y += 15f * u
        text.color =
            0xFF8ECFFF.toInt()
        text.textSize =
            10.2f * u

        drawFitText(
            c,
            weatherOverlay(),
            safeLeft,
            y,
            maxWidth,
            text,
            7.8f * u
        )

        // 11. Real recording audio amplitude + network / battery / storage.
        y += 15f * u
        text.color =
            0xFF76E39A.toInt()
        text.textSize =
            10.9f * u

        drawFitText(
            c,
            "${audioLevelOverlay()} • ${systemOverlay()}",
            safeLeft,
            y,
            maxWidth,
            text,
            7.7f * u
        )

        // 12. Active camera state.
        y += 16f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.BOLD
        )
        text.color =
            0xFFFFC21A.toInt()
        text.textSize =
            10.2f * u

        drawFitText(
            c,
            "CAM ${qualityModes[qualityIndex]} • ${if (useFront) "FRONT" else "BACK"} • EXP $sceneExposureTarget • TAP AF • HIGH BITRATE",
            safeLeft,
            y,
            maxWidth,
            text,
            7.7f * u
        )

        // 13. Keep all automatic/manual-capability information.
        y += 15f * u
        text.typeface = Typeface.create(
            Typeface.MONOSPACE,
            Typeface.NORMAL
        )
        text.color =
            0xFFDDE8EA.toInt()
        text.textSize =
            9.5f * u

        drawFitText(
            c,
            "ISO AUTO • SHUTTER AUTO • WB AUTO • MIC • GPS • GRID • LEVEL • ${captureModes[captureModeIndex]}",
            safeLeft,
            y,
            maxWidth,
            text,
            7.3f * u
        )

        c.restore()
    }

    private fun instrumentStateColor(): Int {
        val acc = accuracy

        return when {
            acc == null ->
                0xFFFF5A52.toInt()

            acc <= 8f ->
                0xFF76E39A.toInt()

            acc <= 25f ->
                0xFFFFC21A.toInt()

            else ->
                0xFFFF6B57.toInt()
        }
    }

    private fun drawCompassInstrument(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        u: Float
    ) {
        val stateColor =
            instrumentStateColor()

        val ring =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xCCFFFFFF.toInt()
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    1.2f * u
                setShadowLayer(
                    2.0f * u,
                    0.6f * u,
                    0.6f * u,
                    0xCC000000.toInt()
                )
            }

        val accentPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stateColor
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    2.0f * u
            }

        val tickPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xBFFFFFFF.toInt()
                strokeWidth =
                    1.0f * u
            }

        val labelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(
                    Typeface.MONOSPACE,
                    Typeface.BOLD
                )
                textAlign =
                    Paint.Align.CENTER
                textSize =
                    9.2f * u
                setShadowLayer(
                    2.0f * u,
                    0.5f * u,
                    0.5f * u,
                    0xDD000000.toInt()
                )
            }

        val valuePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xFF7FE8FF.toInt()
                typeface = Typeface.create(
                    Typeface.MONOSPACE,
                    Typeface.BOLD
                )
                textAlign =
                    Paint.Align.CENTER
                textSize =
                    9.5f * u
                setShadowLayer(
                    2.2f * u,
                    0.6f * u,
                    0.6f * u,
                    0xE6000000.toInt()
                )
            }

        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            ring
        )
        canvas.drawCircle(
            centerX,
            centerY,
            radius - (4f * u),
            accentPaint
        )

        val headingValue =
            compassAzimuthDeg
        val headingDeg =
            headingValue ?: 0f

        for (index in 0 until 24) {
            val absoluteDeg =
                index * 15f
            val relativeDeg =
                absoluteDeg - headingDeg

            val rad =
                Math.toRadians(
                    (relativeDeg - 90f)
                        .toDouble()
                )

            val outer =
                radius - (3f * u)

            val inner =
                when {
                    index % 6 == 0 ->
                        radius - (12f * u)

                    index % 3 == 0 ->
                        radius - (9f * u)

                    else ->
                        radius - (6f * u)
                }

            val x1 =
                centerX +
                    (cos(rad) * inner)
                        .toFloat()
            val y1 =
                centerY +
                    (sin(rad) * inner)
                        .toFloat()
            val x2 =
                centerX +
                    (cos(rad) * outer)
                        .toFloat()
            val y2 =
                centerY +
                    (sin(rad) * outer)
                        .toFloat()

            canvas.drawLine(
                x1,
                y1,
                x2,
                y2,
                tickPaint
            )
        }

        listOf(
            "N" to 0f,
            "E" to 90f,
            "S" to 180f,
            "W" to 270f
        ).forEach { item ->
            val relativeDeg =
                item.second - headingDeg

            val rad =
                Math.toRadians(
                    (relativeDeg - 90f)
                        .toDouble()
                )

            val labelRadius =
                radius - (20f * u)

            val x =
                centerX +
                    (cos(rad) * labelRadius)
                        .toFloat()
            val y =
                centerY +
                    (sin(rad) * labelRadius)
                        .toFloat() +
                    (3f * u)

            canvas.drawText(
                item.first,
                x,
                y,
                labelPaint
            )
        }

        val pointer =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xFFFFC21A.toInt()
                style =
                    Paint.Style.FILL
            }

        val triangle =
            android.graphics.Path().apply {
                moveTo(
                    centerX,
                    centerY - radius -
                        (4f * u)
                )
                lineTo(
                    centerX - (5f * u),
                    centerY - radius +
                        (5f * u)
                )
                lineTo(
                    centerX + (5f * u),
                    centerY - radius +
                        (5f * u)
                )
                close()
            }

        canvas.drawPath(
            triangle,
            pointer
        )

        val headingText =
            headingValue?.let {
                String.format(
                    Locale.US,
                    "%.0f° %s",
                    it,
                    cardinalDirection(it)
                )
            } ?: "--° --"

        canvas.drawText(
            headingText,
            centerX,
            centerY + (3f * u),
            valuePaint
        )

        valuePaint.textSize =
            7.6f * u
        valuePaint.color =
            stateColor

        canvas.drawText(
            "COMPASS",
            centerX,
            centerY + (16f * u),
            valuePaint
        )
    }

    private fun drawAudioMeterInstrument(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        u: Float
    ) {
        val bg =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0x35000000
                style =
                    Paint.Style.FILL
            }

        val outline =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xBFFFFFFF.toInt()
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    1f * u
            }

        canvas.drawRoundRect(
            left,
            top,
            left + width,
            top + height,
            height / 2f,
            height / 2f,
            bg
        )
        canvas.drawRoundRect(
            left,
            top,
            left + width,
            top + height,
            height / 2f,
            height / 2f,
            outline
        )

        val level =
            if (recording != null) {
                audioAmplitude
                    .coerceIn(
                        0.0,
                        1.0
                    )
                    .toFloat()
            } else {
                0f
            }

        val fillColor =
            when {
                level >= 0.92f ->
                    0xFFFF4138.toInt()

                level >= 0.72f ->
                    0xFFFFC21A.toInt()

                else ->
                    0xFF76E39A.toInt()
            }

        val fill =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fillColor
                style =
                    Paint.Style.FILL
            }

        val inner =
            2f * u
        val usable =
            (width - (inner * 2f))
                .coerceAtLeast(0f)

        canvas.drawRoundRect(
            left + inner,
            top + inner,
            left + inner +
                (usable * level),
            top + height - inner,
            (height - (inner * 2f)) / 2f,
            (height - (inner * 2f)) / 2f,
            fill
        )

        val label =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = Typeface.create(
                    Typeface.MONOSPACE,
                    Typeface.BOLD
                )
                textSize =
                    7.2f * u
                setShadowLayer(
                    1.8f * u,
                    0.5f * u,
                    0.5f * u,
                    0xDD000000.toInt()
                )
            }

        val pct =
            (level * 100f)
                .roundToInt()

        canvas.drawText(
            "MIC $pct%",
            left,
            top - (4f * u),
            label
        )
    }

    private fun drawLevelInstrument(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        u: Float
    ) {
        val roll =
            phoneRollDeg
        val pitch =
            phonePitchDeg

        val stateColor =
            when {
                roll == null ->
                    0xFFFF5A52.toInt()

                kotlin.math.abs(roll) <= 1f ->
                    0xFF76E39A.toInt()

                kotlin.math.abs(roll) <= 3f ->
                    0xFFFFC21A.toInt()

                else ->
                    0xFFFF6B57.toInt()
            }

        val framePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    0xBFFFFFFF.toInt()
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    1f * u
            }

        val levelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stateColor
                strokeWidth =
                    2f * u
            }

        val halfW =
            width / 2f
        val halfH =
            height / 2f

        canvas.drawRoundRect(
            centerX - halfW,
            centerY - halfH,
            centerX + halfW,
            centerY + halfH,
            5f * u,
            5f * u,
            framePaint
        )

        val rollValue =
            (roll ?: 0f)
                .coerceIn(
                    -30f,
                    30f
                )

        val rad =
            Math.toRadians(
                rollValue.toDouble()
            )

        val lineHalf =
            halfW - (10f * u)

        val dx =
            (cos(rad) * lineHalf)
                .toFloat()
        val dy =
            (sin(rad) * lineHalf)
                .toFloat()

        canvas.drawLine(
            centerX - dx,
            centerY - dy,
            centerX + dx,
            centerY + dy,
            levelPaint
        )

        canvas.drawLine(
            centerX - (6f * u),
            centerY,
            centerX + (6f * u),
            centerY,
            framePaint
        )

        val label =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stateColor
                typeface = Typeface.create(
                    Typeface.MONOSPACE,
                    Typeface.BOLD
                )
                textAlign =
                    Paint.Align.CENTER
                textSize =
                    7.0f * u
                setShadowLayer(
                    1.8f * u,
                    0.5f * u,
                    0.5f * u,
                    0xDD000000.toInt()
                )
            }

        val value =
            String.format(
                Locale.US,
                "ROLL %s • TILT %s",
                roll?.let {
                    String.format(
                        Locale.US,
                        "%.1f°",
                        it
                    )
                } ?: "--",
                pitch?.let {
                    String.format(
                        Locale.US,
                        "%.1f°",
                        it
                    )
                } ?: "--"
            )

        canvas.drawText(
            value,
            centerX,
            centerY + halfH +
                (10f * u),
            label
        )
    }

    private fun nextClipSequence(): Int {
        val prefs =
            getSharedPreferences(
                "develop_uganda_reporter",
                Context.MODE_PRIVATE
            )

        val next =
            prefs.getInt(
                "clip_sequence",
                0
            ) + 1

        prefs.edit()
            .putInt(
                "clip_sequence",
                next
            )
            .apply()

        return next
    }

    private fun clipSequenceText(): String {
        return String.format(
            Locale.US,
            "%04d",
            clipSequence
        )
    }

    private fun utcClockText(): String {
        val iso = Instant.now().toString()
        return if (iso.length >= 19) {
            iso.substring(11, 19) + "Z"
        } else {
            iso
        }
    }

    private fun coordinatePrimaryOverlay(): String {
        return if (lat != null && lon != null) {
            String.format(
                Locale.US,
                "GPS LIVE • LAT %.5f • LON %.5f",
                lat,
                lon
            )
        } else {
            "GPS ACQUIRING • LAT -- • LON --"
        }
    }

    private fun movementOverlay(): String {
        val altText =
            alt?.let {
                String.format(
                    Locale.US,
                    "ALT %.0fm",
                    it
                )
            } ?: "ALT --"

        val accText =
            accuracy?.let {
                String.format(
                    Locale.US,
                    "ACC ±%.0fm",
                    it
                )
            } ?: "ACC --"

        val headingText =
            heading?.let {
                String.format(
                    Locale.US,
                    "HDG %.0f° %s",
                    it,
                    cardinalDirection(it)
                )
            } ?: "HDG --"

        val speedText =
            speedKmh?.let {
                String.format(
                    Locale.US,
                    "SPD %.1fkm/h",
                    it
                )
            } ?: "SPD --"

        val distanceText =
            if (recording != null) {
                if (distanceTravelledM >= 1000f) {
                    String.format(
                        Locale.US,
                        "DIST %.2fkm",
                        distanceTravelledM / 1000f
                    )
                } else {
                    String.format(
                        Locale.US,
                        "DIST %.0fm",
                        distanceTravelledM
                    )
                }
            } else {
                "DIST STBY"
            }

        return "$altText • $accText • $headingText • $speedText • ${motionLabel()} • ${gpsQualityLabel()} • FIX ${gpsFixAgeText()} • $distanceText"
    }

    private fun gpsFixAgeText(): String {
        if (lastGpsUpdateMs <= 0L) {
            return "--"
        }

        val ageMs =
            (
                System.currentTimeMillis() -
                    lastGpsUpdateMs
                ).coerceAtLeast(0L)

        return if (ageMs < 10_000L) {
            String.format(
                Locale.US,
                "%.1fs",
                ageMs / 1000f
            )
        } else {
            "${ageMs / 1000L}s"
        }
    }

    private fun gpsQualityLabel(): String {
        val a = accuracy
            ?: return "FIX WAIT"

        return when {
            a <= 8f -> "FIX EXCELLENT"
            a <= 20f -> "FIX GOOD"
            a <= 50f -> "FIX FAIR"
            else -> "FIX LOW"
        }
    }

    private fun motionLabel(): String {
        val s = speedKmh
            ?: return "MOTION --"

        return when {
            s < 1.2f -> "STILL"
            s < 8f -> "WALK"
            s < 25f -> "MOVE"
            else -> "VEHICLE"
        }
    }

    private fun cardinalDirection(
        degrees: Float
    ): String {
        val dirs = arrayOf(
            "N",
            "NE",
            "E",
            "SE",
            "S",
            "SW",
            "W",
            "NW"
        )

        val normalized =
            (
                (degrees % 360f) +
                    360f
                ) % 360f

        val index =
            (
                (normalized + 22.5f) /
                    45f
                ).toInt() % 8

        return dirs[index]
    }

    override fun onResume() {
        super.onResume()

        rotationVectorSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {
        }

        super.onPause()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }

    override fun onSensorChanged(
        event: SensorEvent?
    ) {
        if (
            event == null ||
            event.sensor.type !=
            Sensor.TYPE_ROTATION_VECTOR
        ) {
            return
        }

        try {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                event.values
            )
            SensorManager.getOrientation(
                rotationMatrix,
                orientation
            )

            val azimuth =
                Math.toDegrees(
                    orientation[0].toDouble()
                ).toFloat()
            val pitch =
                Math.toDegrees(
                    orientation[1].toDouble()
                ).toFloat()
            val roll =
                Math.toDegrees(
                    orientation[2].toDouble()
                ).toFloat()

            compassAzimuthDeg =
                (
                    (azimuth % 360f) +
                        360f
                    ) % 360f
            phonePitchDeg = pitch
            phoneRollDeg = roll
        } catch (_: Exception) {
        }
    }

    private fun startGnssMonitor() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.N
        ) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (gnssCallbackHolder != null) {
            return
        }

        try {
            val callback =
                object : GnssStatus.Callback() {
                    override fun onSatelliteStatusChanged(
                        status: GnssStatus
                    ) {
                        gnssSatellitesVisible =
                            status.satelliteCount

                        var used = 0
                        for (
                            i in
                            0 until status.satelliteCount
                        ) {
                            if (status.usedInFix(i)) {
                                used++
                            }
                        }
                        gnssSatellitesUsed = used
                    }

                    override fun onStopped() {
                        gnssSatellitesVisible = -1
                        gnssSatellitesUsed = -1
                    }
                }

            gnssCallbackHolder = callback
            locationManager.registerGnssStatusCallback(
                callback,
                Handler(Looper.getMainLooper())
            )
        } catch (_: Exception) {
            gnssCallbackHolder = null
        }
    }

    private fun stopGnssMonitor() {
        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.N
        ) {
            return
        }

        val callback =
            gnssCallbackHolder as?
                GnssStatus.Callback
                ?: return

        try {
            locationManager.unregisterGnssStatusCallback(
                callback
            )
        } catch (_: Exception) {
        }

        gnssCallbackHolder = null
    }

    private fun gnssOverlay(): String {
        val altText =
            alt?.let {
                String.format(
                    Locale.US,
                    "ALT %.0fm",
                    it
                )
            } ?: "ALT --"

        val accText =
            accuracy?.let {
                String.format(
                    Locale.US,
                    "ACC ±%.0fm",
                    it
                )
            } ?: "ACC --"

        val satText =
            if (
                gnssSatellitesVisible >= 0 &&
                gnssSatellitesUsed >= 0
            ) {
                "SAT ${gnssSatellitesUsed}/${gnssSatellitesVisible}"
            } else {
                "SAT --/--"
            }

        return "$altText • $accText • $satText • ${gpsQualityLabel()} • FIX ${gpsFixAgeText()}"
    }

    private fun navigationOverlay(): String {
        val compassText =
            compassAzimuthDeg?.let {
                String.format(
                    Locale.US,
                    "COMP %.0f° %s",
                    it,
                    cardinalDirection(it)
                )
            } ?: "COMP --"

        val gpsHeadingText =
            heading?.let {
                String.format(
                    Locale.US,
                    "GPS HDG %.0f° %s",
                    it,
                    cardinalDirection(it)
                )
            } ?: "GPS HDG --"

        val speedText =
            speedKmh?.let {
                String.format(
                    Locale.US,
                    "SPD %.1fkm/h",
                    it
                )
            } ?: "SPD --"

        val distanceText =
            if (recording != null) {
                if (distanceTravelledM >= 1000f) {
                    String.format(
                        Locale.US,
                        "DIST %.2fkm",
                        distanceTravelledM / 1000f
                    )
                } else {
                    String.format(
                        Locale.US,
                        "DIST %.0fm",
                        distanceTravelledM
                    )
                }
            } else {
                "DIST STBY"
            }

        return "$compassText • $gpsHeadingText • $speedText • ${motionLabel()} • $distanceText"
    }

    private fun orientationOverlay(): String {
        val pitch =
            phonePitchDeg?.let {
                String.format(
                    Locale.US,
                    "TILT %.1f°",
                    it
                )
            } ?: "TILT --"

        val roll =
            phoneRollDeg?.let {
                String.format(
                    Locale.US,
                    "HORIZON %.1f°",
                    it
                )
            } ?: "HORIZON --"

        val level =
            phoneRollDeg?.let {
                when {
                    kotlin.math.abs(it) <= 1.0f ->
                        "LEVEL LOCK"
                    kotlin.math.abs(it) <= 3.0f ->
                        "LEVEL NEAR"
                    else ->
                        "LEVEL ADJUST"
                }
            } ?: "LEVEL --"

        return "$pitch • $roll • $level"
    }

    private fun audioLevelOverlay(): String {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "MIC OFF"
        }

        if (recording == null) {
            return "MIC READY"
        }

        val amplitude =
            audioAmplitude.coerceIn(
                0.0,
                1.0
            )

        val percent =
            (amplitude * 100.0)
                .roundToInt()

        val dbfs =
            if (amplitude > 0.0001) {
                (
                    20.0 *
                        log10(amplitude)
                    ).coerceAtLeast(-80.0)
            } else {
                -80.0
            }

        return String.format(
            Locale.US,
            "%s • LVL %d%% • %.1fdBFS",
            audioStateLabel,
            percent,
            dbfs
        )
    }

    private fun loadReporterIdentity() {
        val prefs =
            getSharedPreferences(
                "develop_uganda_reporter",
                Context.MODE_PRIVATE
            )

        reporterName =
            prefs.getString(
                "reporter_name",
                "CITIZEN"
            )
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "CITIZEN"

        storyId =
            prefs.getString(
                "story_id",
                ""
            )
                ?.trim()
                ?: ""
    }

    private fun reporterDisplayName(): String {
        return reporterName
            .ifBlank { "CITIZEN" }
            .take(28)
    }

    private fun storyDisplayId(): String {
        return storyId
            .ifBlank { "--" }
            .take(24)
    }

    private fun newReportId(): String {
        clipSequence =
            nextClipSequence()

        return "DU-" +
            SimpleDateFormat(
                "yyMMdd-HHmmss",
                Locale.US
            ).format(Date()) +
            "-" +
            clipSequenceText()
    }

    private fun identityButtonText(): String {
        return "REPORT ID\n$reportId • ${reporterDisplayName()} • ${storyDisplayId()}"
    }

    private fun showIdentityDialog() {
        val container =
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

        val reporterInput =
            EditText(this).apply {
                hint = "Reporter / citizen name"
                setText(
                    if (
                        reporterName ==
                        "CITIZEN"
                    ) {
                        ""
                    } else {
                        reporterName
                    }
                )
                isSingleLine = true
            }

        val storyInput =
            EditText(this).apply {
                hint = "Story ID / assignment (optional)"
                setText(storyId)
                isSingleLine = true
            }

        container.addView(reporterInput)
        container.addView(storyInput)

        AlertDialog.Builder(this)
            .setTitle(
                "develop.uganda Report Identity"
            )
            .setMessage(
                "These fields are burned into the recorded report."
            )
            .setView(container)
            .setPositiveButton(
                "SAVE"
            ) { _, _ ->
                reporterName =
                    reporterInput.text
                        .toString()
                        .trim()
                        .ifBlank {
                            "CITIZEN"
                        }

                storyId =
                    storyInput.text
                        .toString()
                        .trim()

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

                refreshHud()
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
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
        if (
            captureModes[captureModeIndex] ==
            "PHOTO"
        ) {
            takePhoto()
            return
        }

        val vc = videoCapture ?: run {
            toast("Camera is still starting")
            return
        }

        if (recording != null) {
            recording?.stop()
            return
        }

        reportId = newReportId()
        recordStartUtc = "--"

        baseName = "DEVELOP_UGANDA_${reportId}_${sceneModes[sceneIndex]}_${lookModes[lookIndex]}_" +
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
                    recordStartUtc =
                        Instant.ofEpochMilli(
                            recStarted
                        ).toString()
                    distanceTravelledM = 0f
                    audioAmplitude = 0.0
                    audioStateLabel = "MIC REC"
                    previousTrackLat = lat
                    previousTrackLon = lon
                    statusView.text = "● REC"
                    statusView.setTextColor(
                        0xFFFF4138.toInt()
                    )
                    recordButton.text = "■ STOP"

                    if (
                        ::reportRecordGlow.isInitialized
                    ) {
                        reportRecordGlow.setRecordingState(
                            true
                        )
                    }

                    showReportOperatorControlsTemporarily()
                }

                is VideoRecordEvent.Status -> {
                    val audioStats =
                        event.recordingStats.audioStats
                    audioAmplitude =
                        audioStats.audioAmplitude
                    audioStateLabel =
                        when {
                            audioStats.hasError() -> "MIC ERROR"
                            audioStats.hasAudio() -> "MIC LIVE"
                            else -> "MIC OFF"
                        }
                }

                is VideoRecordEvent.Finalize -> {
                    val hadError = event.hasError()

                    recording = null
                    recStarted = 0L
                    audioAmplitude = 0.0
                    audioStateLabel =
                        if (
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            "MIC READY"
                        } else {
                            "MIC OFF"
                        }
                    recordButton.text = "● RECORD"

                    if (
                        ::reportRecordGlow.isInitialized
                    ) {
                        reportRecordGlow.setRecordingState(
                            false
                        )
                    }

                    if (hadError) {
                        statusView.text = "ERROR"
                        statusView.setTextColor(
                            0xFFFF4138.toInt()
                        )
                        setReportOperatorControlsHidden(
                            false
                        )
                        uiHandler.removeCallbacks(
                            autoHideRunnable
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

                        if (
                            integrityEnabled
                        ) {
                            createIntegrityRecord(
                                event.outputResults.outputUri,
                                Instant.now().toString()
                            )
                        }

                        setReportOperatorControlsHidden(
                            false
                        )

                        uiHandler.removeCallbacks(
                            autoHideRunnable
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

    private fun takePhoto() {
        val capture = imageCapture ?: run {
            toast("Photo camera is still starting")
            return
        }

        reportId = newReportId()
        recordStartUtc =
            Instant.now().toString()

        val photoName =
            "DEVELOP_UGANDA_${reportId}_${sceneModes[sceneIndex]}_${lookModes[lookIndex]}_" +
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                photoName
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )

            if (
                android.os.Build.VERSION.SDK_INT >=
                29
            ) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/develop.uganda"
                )
            }
        }

        val output =
            ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
                .build()

        statusView.text = "CAPTURING"

        capture.takePicture(
            output,
            ContextCompat.getMainExecutor(this),
            object :
                ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    result:
                    ImageCapture.OutputFileResults
                ) {
                    statusView.text = "PHOTO SAVED"
                    statusView.setTextColor(
                        0xFF76E39A.toInt()
                    )
                    toast(
                        "Photo saved • develop.uganda"
                    )

                    uiHandler.postDelayed({
                        statusView.text =
                            "PHOTO READY"
                        statusView.setTextColor(
                            0xFFFF5A52.toInt()
                        )
                    }, 1600L)
                }

                override fun onError(
                    exception:
                    ImageCaptureException
                ) {
                    statusView.text = "PHOTO ERROR"
                    statusView.setTextColor(
                        0xFFFF4138.toInt()
                    )
                    toast(
                        "Photo capture failed"
                    )
                }
            }
        )
    }

    private fun refreshReportPillStates() {
        val values =
            listOf(
                sceneButton to
                    0xFFFFC21A.toInt(),
                lookButton to
                    0xFF7FE8FF.toInt(),
                qualityButton to
                    0xFFE8F1F2.toInt(),
                captureModeButton to
                    0xFF76E39A.toInt(),
                viewModeButton to
                    0xFF76E39A.toInt(),
                settingsButton to
                    0xFF7FE8FF.toInt(),
                guidesButton to
                    0xFFFFC21A.toInt(),
                resetButton to
                    0xFFFF8A84.toInt()
            )

        values.forEach {
            pair ->
            pair.first.background =
                solidPillBackground(
                    pair.second,
                    pair.first.isSelected
                )
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

        if (
            ::previewNarrationPanel.isInitialized
        ) {
            previewTagView.text =
                sceneTag()

            previewIdentityView.text =
                "REPORT ID $reportId • REPORTER ${reporterDisplayName()} • STORY ${storyDisplayId()}"

            previewClockView.text =
                "${
                    when {
                        recording != null -> "● REC"
                        captureModes[captureModeIndex] == "PHOTO" -> "● PHOTO"
                        else -> "STBY"
                    }
                } • TC ${tc()} • ${clock.format(Date())}"

            previewModeView.text =
                "SCENE ${sceneModes[sceneIndex]} • LOOK ${lookModes[lookIndex]} • ${qualityModes[qualityIndex]} • ${captureModes[captureModeIndex]}"

            previewPlaceView.text =
                placeName

            previewGpsView.text =
                "${coordinatePrimaryOverlay()} • ${gnssOverlay()}"

            previewNavView.text =
                "${navigationOverlay()} • ${orientationOverlay()}"

            previewSystemView.text =
                "${weatherOverlay()} • ${audioLevelOverlay()} • ${systemOverlay()}"

            previewHealthView.text =
                recordingHealthText()

            previewHealthView.setTextColor(
                recordingHealthColor()
            )
        }

        if (::sceneButton.isInitialized) {
            sceneButton.text =
                "SCENE ▾\n${sceneModes[sceneIndex]}"
        }

        if (::lookButton.isInitialized) {
            lookButton.text =
                "LOOK ▾\n${lookModes[lookIndex]}"
        }

        if (::qualityButton.isInitialized) {
            qualityButton.text =
                "FORMAT ▾\n${qualityDeckLabel()}"
        }

        if (::captureModeButton.isInitialized) {
            captureModeButton.text =
                "CAPTURE ▾\n${captureModes[captureModeIndex]}"
        }

        if (::identityButton.isInitialized) {
            identityButton.text =
                identityButtonText()
        }

        if (
            ::recordButton.isInitialized &&
            recording == null
        ) {
            recordButton.text =
                if (
                    captureModes[
                        captureModeIndex
                    ] == "PHOTO"
                ) {
                    "● PHOTO"
                } else {
                    "● RECORD"
                }
        }

        if (::lensButton.isInitialized) {
            lensButton.text =
                "LENS ▾\n${if (useFront) "FRONT" else "BACK"}"
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

    private fun cycleCaptureMode() {
        if (recording != null) {
            toast(
                "Stop recording before changing capture mode"
            )
            return
        }

        captureModeIndex =
            (captureModeIndex + 1) %
                captureModes.size

        refreshHud()
        bindCamera()
    }

    private fun qualityDeckLabel(): String {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "MASTER UHD" -> "4K MASTER"
            "FAST HD" -> "HD FAST"
            else -> "SOCIAL"
        }
    }

    private fun targetVideoBitrate(): Int {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "MASTER UHD" ->
                48_000_000

            "FAST HD" ->
                10_000_000

            else ->
                20_000_000
        }
    }

    private fun sceneTag(): String {
        if (reportDisplayMode == "LIVE EFFECT") {
            return "LIVE EFFECT"
        }

        return when (
            sceneModes[
                sceneIndex
            ]
        ) {
            "NEWS" -> "NEWS DESK"
            "CINEMA" -> "CINEMA UNIT"
            "MOVIE" -> "MOVIE UNIT"
            "OUTDOOR" -> "OUTDOOR UNIT"
            "INDOOR" -> "INTERIOR UNIT"
            "NIGHT" -> "NIGHT DESK"
            "INTERVIEW" -> "INTERVIEW UNIT"
            "DOCUMENTARY" -> "DOCUMENTARY UNIT"
            else -> "FIELD REPORT"
        }
    }

    private fun buildQualitySelector():
        QualitySelector {

        val ordered =
            when (
                qualityModes[
                    qualityIndex
                ]
            ) {
                "MASTER UHD" -> listOf(
                    Quality.UHD,
                    Quality.FHD,
                    Quality.HD
                )

                "FAST HD" -> listOf(
                    Quality.HD,
                    Quality.FHD,
                    Quality.UHD
                )

                else -> listOf(
                    Quality.FHD,
                    Quality.UHD,
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
                "INTERVIEW" -> 0
                "DOCUMENTARY" -> 0
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
                "NATURAL" ->
                    0x06FFF4E8

                "WARM" ->
                    0x0DFF8A45

                "COOL" ->
                    0x0C2F72FF

                "TEAL" ->
                    0x0D00A7A1

                "GOLD" ->
                    0x0EF2B43C

                "NIGHT" ->
                    0x14092346

                "MONO" ->
                    0x12000000

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
        val net = networkType()
        val uplink =
            estimatedUploadKbps?.let {
                "UP~${it}kbps"
            } ?: "UP~--"

        return "NET $net • $uplink" +
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

    private inner class DeckTouchListener(
        private val actionCode: Int
    ) : View.OnTouchListener {

        override fun onTouch(
            v: View?,
            event: MotionEvent
        ): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (
                        operatorLocked &&
                        actionCode != ACTION_RECORD &&
                        actionCode != ACTION_LOCK
                    ) {
                        toast(
                            "Operator controls locked"
                        )
                        return true
                    }

                    v?.isPressed = true
                    v?.alpha = 0.94f
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    v?.isPressed = false
                    v?.alpha = 1f
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    v?.isPressed = false
                    v?.alpha = 1f
                    v?.performClick()

                    when (actionCode) {
                        ACTION_SCENE ->
                            showReportSceneDropdown(
                                v ?: sceneButton
                            )

                        ACTION_LOOK ->
                            showReportLookDropdown(
                                v ?: lookButton
                            )

                        ACTION_QUALITY ->
                            showReportQualityDropdown(
                                v ?: qualityButton
                            )

                        ACTION_CAPTURE_MODE ->
                            showReportCaptureDropdown(
                                v ?: captureModeButton
                            )
                        ACTION_IDENTITY -> showIdentityDialog()
                        ACTION_VIEW_MODE ->
                            showReportViewDropdown(
                                v ?: viewModeButton
                            )

                        ACTION_SETTINGS ->
                            showReportDetailedSettings()

                        ACTION_GUIDES ->
                            showReportGuidesDropdown(
                                v ?: guidesButton
                            )

                        ACTION_RESET ->
                            resetReportCameraSettings()

                        ACTION_AUTO_UI ->
                            showReportAutoUiDropdown(
                                v ?: autoUiButton
                            )

                        ACTION_LOCK ->
                            showReportLockDropdown(
                                v ?: lockButton
                            )

                        ACTION_INTEGRITY ->
                            showReportIntegrityDropdown(
                                v ?: integrityButton
                            )

                        ACTION_CAPABILITIES ->
                            showCameraCapabilities()

                        ACTION_CLEAN ->
                            showReportCleanDropdown(
                                v ?: cleanModeButton
                            )

                        ACTION_LENS ->
                            showReportLensDropdown(
                                v ?: lensButton
                            )

                        ACTION_TORCH ->
                            showReportLightDropdown(
                                v ?: torchButton
                            )
                        ACTION_RECORD -> toggleRecording()
                    }

                    return true
                }
            }

            return true
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
        return GlowSettingButton(
            this,
            accentColor
        ).apply {
            text = value
            textSize = 5.5f
            isAllCaps = false
            setTextColor(Color.WHITE)
            minHeight = dp(34)
            setPadding(
                dp(4),
                0,
                dp(4),
                0
            )

            background =
                solidPillBackground(
                    accentColor,
                    isSelected
                )
        }
    }

    private fun pillFillColor(
        accent: Int,
        selected: Boolean = false
    ): Int {
        val factor =
            if (selected) {
                0.68f
            } else {
                0.48f
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

    private fun solidPillBackground(
        accent: Int,
        selected: Boolean = false
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape =
                GradientDrawable.RECTANGLE

            cornerRadius =
                dp(20).toFloat()

            setColor(
                pillFillColor(
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

    private fun reportOptionAccent(
        index: Int
    ): Int {
        val colors =
            intArrayOf(
                0xFFFFC21A.toInt(), // gold
                0xFF4EA7FF.toInt(), // blue
                0xFF62E889.toInt(), // green
                0xFFFF5AA5.toInt(), // pink
                0xFF8F7CFF.toInt(), // purple
                0xFF00C9B7.toInt(), // teal
                0xFFFF8A3D.toInt(), // orange
                0xFFB7C1C8.toInt()  // silver
            )

        return colors[
            index %
                colors.size
        ]
    }

    private fun showReportPillDropdown(
        anchor: View,
        title: String,
        options: List<String>,
        selectedIndex: Int,
        onPick: (Int) -> Unit
    ) {
        showReportPillDropdown(
            anchor,
            title,
            options.toTypedArray(),
            selectedIndex,
            onPick
        )
    }

    private fun showReportPillDropdown(
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
                            0xF20A1014.toInt()
                        )

                        setStroke(
                            dp(1),
                            0x507FE8FF
                        )
                    }
            }

        panel.addView(
            hud(
                title,
                7.4f,
                Color.WHITE,
                bold = true
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(27)
            )
        )

        val popup =
            PopupWindow(
                panel,
                dp(188),
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
                reportOptionAccent(
                    index
                )

            val pill =
                GlowSettingButton(
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
                        7.2f

                    isAllCaps =
                        false

                    setTextColor(
                        Color.WHITE
                    )

                    gravity =
                        Gravity.CENTER

                    setPadding(
                        dp(9),
                        0,
                        dp(9),
                        0
                    )

                    background =
                        solidPillBackground(
                            accent,
                            index ==
                                selectedIndex
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
                    dp(36)
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

    private fun showReportSceneDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop recording before changing scene"
            )
            return
        }

        showReportPillDropdown(
            anchor,
            "SCENE",
            sceneModes,
            sceneIndex
        ) { picked ->
            sceneIndex =
                picked

            applyScenePreset()
            refreshHud()
        }
    }

    private fun showReportLookDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "LOOK",
            lookModes,
            lookIndex
        ) { picked ->
            lookIndex =
                picked
            refreshHud()
        }
    }

    private fun showReportQualityDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop recording before changing format"
            )
            return
        }

        showReportPillDropdown(
            anchor,
            "FORMAT",
            qualityModes,
            qualityIndex
        ) { picked ->
            qualityIndex =
                picked
            refreshHud()
            bindCamera()
        }
    }

    private fun showReportCaptureDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop recording before changing capture mode"
            )
            return
        }

        showReportPillDropdown(
            anchor,
            "CAPTURE",
            captureModes,
            captureModeIndex
        ) { picked ->
            captureModeIndex =
                picked
            refreshHud()
            bindCamera()
        }
    }

    private fun showReportViewDropdown(
        anchor: View
    ) {
        val selected =
            if (halfPreviewMode) {
                1
            } else {
                0
            }

        showReportPillDropdown(
            anchor,
            "VIEW",
            arrayOf(
                "FULL SCREEN",
                "HALF SCREEN"
            ),
            selected
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

    private fun showReportGuidesDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "GUIDES",
            arrayOf(
                "ON",
                "OFF"
            ),
            if (previewGuidesEnabled) 0 else 1
        ) { picked ->
            val want =
                picked ==
                    0

            if (
                want !=
                previewGuidesEnabled
            ) {
                togglePreviewGuides()
            }
        }
    }

    private fun showReportAutoUiDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "AUTO UI",
            arrayOf(
                "ON",
                "OFF"
            ),
            if (autoHideOperatorUi) 0 else 1
        ) { picked ->
            val want =
                picked ==
                    0

            if (
                want !=
                autoHideOperatorUi
            ) {
                toggleReportAutoUi()
            }
        }
    }

    private fun showReportLockDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "OPERATOR LOCK",
            arrayOf(
                "OFF",
                "ON"
            ),
            if (operatorLocked) 1 else 0
        ) { picked ->
            val want =
                picked ==
                    1

            if (
                want !=
                operatorLocked
            ) {
                toggleReportOperatorLock()
            }
        }
    }

    private fun showReportIntegrityDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Change verification before recording"
            )
            return
        }

        showReportPillDropdown(
            anchor,
            "VERIFY",
            arrayOf(
                "SHA-256 ON",
                "OFF"
            ),
            if (integrityEnabled) 0 else 1
        ) { picked ->
            val want =
                picked ==
                    0

            if (
                want !=
                integrityEnabled
            ) {
                toggleReportIntegrity()
            }
        }
    }

    private fun showReportCleanDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "CLEAN MODE",
            arrayOf(
                "OFF",
                "ON"
            ),
            if (cleanModeEnabled) 1 else 0
        ) { picked ->
            val want =
                picked ==
                    1

            if (
                want !=
                cleanModeEnabled
            ) {
                toggleReportCleanMode()
            }
        }
    }

    private fun showReportLensDropdown(
        anchor: View
    ) {
        if (recording != null) {
            toast(
                "Stop recording before changing lens"
            )
            return
        }

        showReportPillDropdown(
            anchor,
            "LENS",
            arrayOf(
                "BACK",
                "FRONT"
            ),
            if (useFront) 1 else 0
        ) { picked ->
            val wantFront =
                picked ==
                    1

            if (
                wantFront !=
                useFront
            ) {
                useFront =
                    wantFront

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
    }

    private fun showReportLightDropdown(
        anchor: View
    ) {
        val current =
            camera
                ?.cameraInfo
                ?.torchState
                ?.value ==
                TorchState.ON

        showReportPillDropdown(
            anchor,
            "LIGHT",
            arrayOf(
                "OFF",
                "ON"
            ),
            if (current) 1 else 0
        ) { picked ->
            val wantOn =
                picked ==
                    1

            camera
                ?.cameraControl
                ?.enableTorch(
                    wantOn
                )

            torchButton.text =
                "LIGHT ▾\n" +
                    if (wantOn) {
                        "ON"
                    } else {
                        "OFF"
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

    private fun makeRecordButton(): Button {
        return Button(this).apply {
            text = "● RECORD"
            textSize = 9.5f
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
                        0xD9FF2D20.toInt()
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

    private class GlowSettingButton(
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
                    6f,
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
                        0.45f

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

    private class ReportRecordGlowView(
        context: Context
    ) : View(context) {

        private val green =
            0xFF62E889.toInt()

        private val ring =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    3f
                color =
                    green
                alpha =
                    140
            }

        private val glow =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    9f
                color =
                    green
                alpha =
                    0
            }

        private var recording =
            false

        private val handler =
            Handler(
                Looper.getMainLooper()
            )

        private val pulse =
            object : Runnable {
                override fun run() {
                    invalidate()

                    if (recording) {
                        handler.postDelayed(
                            this,
                            33L
                        )
                    }
                }
            }

        fun setRecordingState(
            active: Boolean
        ) {
            recording =
                active

            handler.removeCallbacks(
                pulse
            )

            if (active) {
                handler.post(
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

            val inset =
                8f

            val radius =
                height *
                    0.42f

            canvas.drawRoundRect(
                inset,
                inset,
                width - inset,
                height - inset,
                radius,
                radius,
                ring
            )

            if (recording) {
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
                        40 +
                            wave *
                                130
                        ).roundToInt()

                val grow =
                    (
                        wave *
                            4.5
                        ).toFloat()

                canvas.drawRoundRect(
                    inset - grow,
                    inset - grow,
                    width - inset + grow,
                    height - inset + grow,
                    radius + grow,
                    radius + grow,
                    glow
                )
            }
        }
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

            // V181: guides belong to the actual 9:16 capture frame.
            // PreviewView uses FIT_START, so the capture frame begins at the
            // top of the view and any unused screen area sits underneath it.
            val frameH =
                minOf(
                    h,
                    w * (16f / 9f)
                )

            canvas.drawLine(
                w / 3f,
                0f,
                w / 3f,
                frameH,
                gridPaint
            )

            canvas.drawLine(
                w * 2f / 3f,
                0f,
                w * 2f / 3f,
                frameH,
                gridPaint
            )

            canvas.drawLine(
                0f,
                frameH / 3f,
                w,
                frameH / 3f,
                gridPaint
            )

            canvas.drawLine(
                0f,
                frameH * 2f / 3f,
                w,
                frameH * 2f / 3f,
                gridPaint
            )

            // Thin capture boundary: everything above this line is the exact
            // 9:16 frame that will be exported.
            canvas.drawLine(
                0f,
                frameH - dp(1),
                w,
                frameH - dp(1),
                levelPaint
            )

            val cy = frameH / 2f
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

            // Social title-safe corner marks.
            val insetX =
                w * 0.08f
            val insetY =
                h * 0.08f
            val arm =
                dp(18).toFloat()

            listOf(
                floatArrayOf(
                    insetX,
                    insetY,
                    insetX + arm,
                    insetY,
                    insetX,
                    insetY + arm
                ),
                floatArrayOf(
                    w - insetX,
                    insetY,
                    w - insetX - arm,
                    insetY,
                    w - insetX,
                    insetY + arm
                ),
                floatArrayOf(
                    insetX,
                    h - insetY,
                    insetX + arm,
                    h - insetY,
                    insetX,
                    h - insetY - arm
                ),
                floatArrayOf(
                    w - insetX,
                    h - insetY,
                    w - insetX - arm,
                    h - insetY,
                    w - insetX,
                    h - insetY - arm
                )
            ).forEach { p ->
                canvas.drawLine(
                    p[0], p[1], p[2], p[3],
                    focusPaint
                )
                canvas.drawLine(
                    p[0], p[1], p[4], p[5],
                    focusPaint
                )
            }

            val innerOrbit =
                dp(28).toFloat()
            canvas.drawCircle(
                cx,
                cy,
                innerOrbit,
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

        stopGnssMonitor()

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
