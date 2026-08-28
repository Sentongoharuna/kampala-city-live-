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
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
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
import androidx.camera.core.AspectRatio
import androidx.camera.core.DynamicRange
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.FileInputStream
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

@OptIn(UnstableApi::class)
open class DevelopUgandaCameraActivity : AppCompatActivity(), SensorEventListener {

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
        const val ACTION_HUD_SIZE = 18
        const val ACTION_HUD_CONTRAST = 19
        const val ACTION_REPORT_PRESET = 20
        const val ACTION_HUD_BACKING = 21
        const val ACTION_AUTO_DIRECTOR = 22
        const val ACTION_SHOT_ASSIST = 23
        const val ACTION_DIRECTOR = 24
        const val ACTION_CONTINUITY = 25
        const val ACTION_HEALTH = 26
        const val ACTION_BRAND_METADATA = 27
        const val ACTION_COLOR_ENGINE = 28
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
    private lateinit var cameraExperienceBannerView: TextView
    private lateinit var autoViewDescriptionView: TextView
    private lateinit var shotQualityGuardView: TextView
    private lateinit var shotAssistView: DevelopUgandaShotAssistView
    private lateinit var directorOverlayView: DevelopUgandaDirectorOverlayView
    private lateinit var focusReticleView: TextView
    private lateinit var horizonGuardView: TextView
    private lateinit var motionGuardView: TextView
    private lateinit var lightAdvisorView: TextView
    private lateinit var audioGuardView: TextView
    private lateinit var thermalGuardView: TextView
    private lateinit var previewModeToneView: View

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
    private lateinit var colorButton: Button
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
    private lateinit var hudSizeButton: Button
    private lateinit var hudContrastButton: Button
    private lateinit var hudBackingButton: Button
    private lateinit var reportPresetButton: Button
    private lateinit var autoDirectorButton: Button
    private lateinit var assistButton: Button
    private lateinit var directorButton: Button
    private lateinit var continuityButton: Button
    private lateinit var healthButton: Button
    private lateinit var brandMetadataButton: Button
    private lateinit var reportDisplayRow: LinearLayout
    private lateinit var reportOutputRow: LinearLayout
    private lateinit var reportDirectorRow: LinearLayout

    private val reportPresetLabels =
        arrayOf(
            "CUSTOM",
            "FIELD",
            "OUTDOOR",
            "NIGHT",
            "INTERVIEW",
            "CINEMA"
        )

    private var reportPresetIndex = 0

    private var autoDirectorEnabled = false
    private var autoDirectorLastSwitchMs = 0L
    private var autoDirectorReason = "MANUAL"

    private val reportHudLabels =
        arrayOf(
            "COMPACT",
            "STANDARD",
            "LARGE"
        )

    private val reportHudScales =
        floatArrayOf(
            1.04f,
            1.16f,
            1.28f
        )

    private var reportHudSizeIndex = 1

    private val reportHudContrastLabels =
        arrayOf(
            "SOFT",
            "BALANCED",
            "STRONG"
        )

    private var reportHudContrastIndex = 1

    private val reportHudBackingLabels =
        arrayOf(
            "NONE",
            "SOFT",
            "STRONG"
        )

    private var reportHudBackingIndex = 1

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
    private var focusLongPressTriggered = false
    private var focusLockActive = false
    private var focusAttempted = false
    private var focusSuccessful: Boolean? = null
    private var preflightApprovedOnce = false
    private var shotAssistModeIndex = DevelopUgandaShotAssistView.MODE_OFF
    private val shotAssistModeLabels = arrayOf("OFF", "PEAK", "ZEBRA", "BOTH")
    private val recordingWarningsSeen = linkedSetOf<String>()
    private lateinit var autoViewLabeler: ImageLabeler
    private var autoViewBusy = false
    private var autoViewSummary = "AUTO VIEW • analysing scene"

    private val directorRunnable =
        object : Runnable {
            override fun run() {
                if (
                    ::directorOverlayView.isInitialized
                ) {
                    directorOverlayView.visibility =
                        if (
                            directorEnabled &&
                            !cleanModeEnabled
                        ) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }

                if (
                    directorEnabled &&
                    !cleanModeEnabled &&
                    ::previewView.isInitialized &&
                    ::directorOverlayView.isInitialized &&
                    previewView.width > 0 &&
                    previewView.height > 0
                ) {
                    val bitmap =
                        try {
                            previewView.bitmap
                        } catch (_: Exception) {
                            null
                        }

                    if (
                        bitmap != null
                    ) {
                        directorOverlayView.submitFrame(
                            bitmap,
                            isDirectorPeopleMode()
                        )
                    }
                }

                uiHandler.postDelayed(
                    this,
                    1100L
                )
            }
        }

    private val hideFocusReticleRunnable =
        Runnable {
            if (
                ::focusReticleView.isInitialized &&
                !focusLockActive
            ) {
                focusReticleView.visibility =
                    View.GONE
            }
        }

    private val focusLockRunnable =
        Runnable {
            if (
                !gestureMoved &&
                !operatorLocked &&
                ::previewView.isInitialized
            ) {
                focusLongPressTriggered =
                    true

                togglePersistentFocusLock(
                    gestureDownX,
                    gestureDownY
                )
            }
        }

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
    private var automaticSocialTransformer: Transformer? = null
    private var automaticSocialExportActive = false
    private var selectedCameraDeviceId: String? = null
    private var directorEnabled = true
    private var lastV233ColorMonitorKey = ""
    private var v229ColorOverlayLabel = "AUTO"
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
        "SOFT",
        "SUNSET",
        "BLUE HOUR",
        "NIGHT",
        "MONO"
    )
    // V203 capability-driven recording profiles.
    // SOCIAL FHD remains the safest upload master for TikTok/Instagram.
    private val qualityModes = listOf(
        "SOCIAL FHD",
        "SOCIAL 60",
        "MASTER UHD",
        "UHD 60",
        "MASTER HDR",
        "SOCIAL HDR",
        "ACTION STAB",
        "ACTION 60",
        "LOW LIGHT",
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

    private var activeVideoFpsLabel = "AUTO FPS"
    private var activeVideoStabilizationLabel = "STAB AUTO"
    private var activeVideoDynamicRangeLabel = "SDR"
    private var activeVideoAspectLabel = "9:16 SOCIAL SAFE"

    private val weather = WeatherRepository()
    private lateinit var telemetryRecorder: TelemetryRecorder
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var ambientLightSensor: Sensor? = null
    private lateinit var powerManager: PowerManager
    @Volatile private var thermalStatus =
        PowerManager.THERMAL_STATUS_NONE
    private var thermalListenerRegistered = false

    private val thermalStatusListener =
        PowerManager.OnThermalStatusChangedListener {
                status ->
            thermalStatus =
                status

            runOnUiThread {
                updateThermalGuard()
                refreshHud()
            }
        }

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
    @Volatile private var cameraShakeScore = 0f
    @Volatile private var lastMotionSampleMs = 0L
    @Volatile private var ambientLux: Float? = null
    @Volatile private var gnssSatellitesVisible = -1
    @Volatile private var gnssSatellitesUsed = -1
    @Volatile private var audioAmplitude = 0.0
    @Volatile private var audioPeakAmplitude = 0.0
    @Volatile private var audioStateLabel = "MIC READY"

    private var gnssCallbackHolder: Any? = null
    private var reporterName = "CITIZEN"
    private var storyId = ""
    private var reportId = ""
    private var cameraExperienceId =
        "V210_ALL_PRO"

    protected open fun defaultCameraExperienceId(): String {
        return "V210_ALL_PRO"
    }

    private var reportDisplayMode = "FIELD REPORT"
    private var clipSequence = 0
    private var recordStartUtc = "--"


    private var recStarted = 0L
    private var lastWeatherAt = 0L
    private var lastPlaceAt = 0L
    private var baseName = ""

    private val uiHandler = Handler(Looper.getMainLooper())

    private val shotAssistRunnable =
        object : Runnable {
            override fun run() {
                if (
                    shotAssistModeIndex !=
                        DevelopUgandaShotAssistView.MODE_OFF &&
                    ::previewView.isInitialized &&
                    ::shotAssistView.isInitialized
                ) {
                    val bitmap =
                        try {
                            previewView.bitmap
                        } catch (_: Exception) {
                            null
                        }

                    if (bitmap != null) {
                        shotAssistView.submitFrame(
                            bitmap
                        )
                    }
                }

                uiHandler.postDelayed(
                    this,
                    700L
                )
            }
        }


    private val autoViewRunnable =
        object : Runnable {
            override fun run() {
                analyzeAutoViewFrame()

                uiHandler.postDelayed(
                    this,
                    3500L
                )
            }
        }
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

        ambientLightSensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_LIGHT
            )

        powerManager =
            getSystemService(
                Context.POWER_SERVICE
            ) as PowerManager

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
        ) {
            thermalStatus =
                powerManager.currentThermalStatus
        }

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        cameraExperienceId =
            intent.getStringExtra(
                "develop_uganda_camera_experience"
            )
                ?.trim()
                ?.uppercase(
                    Locale.US
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: defaultCameraExperienceId()

        loadReporterIdentity()
        loadReportCameraPreferences()
        applyIndependentCameraDefaultsIfNeeded()
        recordingWarningsSeen.clear()

        reportId = newReportId()

        reportDisplayMode =
            intent.getStringExtra("develop_uganda_mode")
                ?.trim()
                ?.uppercase(Locale.US)
                ?.takeIf { it.isNotBlank() }
                ?: cameraExperienceDisplayName()

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enforceImmersiveCameraWindow()

        buildUi()
        showRecordingRecoveryNoticeIfNeeded()
        startAutoViewDescription()
        startShotAssistLoop()
        startDirectorLoop()
        requestPermissionsAndStart()
        uiHandler.post(tick)
    }

    private fun enforceImmersiveCameraWindow() {
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            hide(
                WindowInsetsCompat.Type.systemBars()
            )

            systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {
        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {
            enforceImmersiveCameraWindow()
        }
    }

    private fun buildUi() {
        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE

            // V217: full-frame operator camera.
            // The camera image fills the phone behind all controls.
            // Gallery output remains the authoritative CameraX recording.
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        previewModeToneView =
            View(this).apply {
                isClickable =
                    false

                isFocusable =
                    false

                setBackgroundColor(
                    Color.TRANSPARENT
                )
            }

        root.addView(
            previewModeToneView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        shotAssistView =
            DevelopUgandaShotAssistView(
                this
            ).apply {
                setAssistMode(
                    shotAssistModeIndex
                )

                isClickable =
                    false

                isFocusable =
                    false
            }

        root.addView(
            shotAssistView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )


        directorOverlayView =
            DevelopUgandaDirectorOverlayView(
                this
            ).apply {
                setDirectorEnabled(
                    directorEnabled
                )

                isClickable =
                    false

                isFocusable =
                    false
            }

        root.addView(
            directorOverlayView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )


        focusReticleView =
            TextView(this).apply {
                text =
                    "AF"

                textSize =
                    8.5f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.DEFAULT_BOLD

                visibility =
                    View.GONE

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(12).toFloat()

                        setColor(
                            0x26031829
                        )

                        setStroke(
                            dp(2),
                            0xFFDCE4F7.toInt()
                        )
                    }
            }

        root.addView(
            focusReticleView,
            FrameLayout.LayoutParams(
                dp(76),
                dp(76)
            )
        )

        horizonGuardView =
            TextView(this).apply {
                text =
                    "━━━━━━━━  LEVEL --  ━━━━━━━━"

                textSize =
                    8.6f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.MONOSPACE

                setTextColor(
                    0xFFAEB7C7.toInt()
                )

                setShadowLayer(
                    1.1f,
                    0.3f,
                    0.3f,
                    0x66000000
                )

                visibility =
                    View.VISIBLE
            }

        root.addView(
            horizonGuardView,
            FrameLayout.LayoutParams(
                dp(270),
                dp(34),
                Gravity.CENTER
            )
        )

        motionGuardView =
            TextView(this).apply {
                text =
                    "STEADYSHOT • --"

                textSize =
                    8.0f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.MONOSPACE

                setTextColor(
                    0xFFAEB7C7.toInt()
                )

                setPadding(
                    dp(9),
                    dp(4),
                    dp(9),
                    dp(4)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(14).toFloat()

                        setColor(
                            0x42031829
                        )

                        setStroke(
                            dp(1),
                            0x607A91A4
                        )
                    }
            }

        val motionParams =
            FrameLayout.LayoutParams(
                dp(170),
                dp(30),
                Gravity.CENTER
            ).apply {
                topMargin =
                    dp(54)
            }

        root.addView(
            motionGuardView,
            motionParams
        )

        lightAdvisorView =
            TextView(this).apply {
                text =
                    "LIGHT • SENSOR --"

                textSize =
                    7.8f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.MONOSPACE

                setTextColor(
                    0xFFAEB7C7.toInt()
                )

                setPadding(
                    dp(9),
                    dp(4),
                    dp(9),
                    dp(4)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(14).toFloat()

                        setColor(
                            0x42031829
                        )

                        setStroke(
                            dp(1),
                            0x607A91A4
                        )
                    }
            }

        val lightParams =
            FrameLayout.LayoutParams(
                dp(220),
                dp(30),
                Gravity.CENTER
            ).apply {
                topMargin =
                    dp(92)
            }

        root.addView(
            lightAdvisorView,
            lightParams
        )

        audioGuardView =
            TextView(this).apply {
                text =
                    "AUDIO • MIC READY"

                textSize =
                    7.8f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.MONOSPACE

                setTextColor(
                    0xFFAEB7C7.toInt()
                )

                setPadding(
                    dp(9),
                    dp(4),
                    dp(9),
                    dp(4)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(14).toFloat()

                        setColor(
                            0x42031829
                        )

                        setStroke(
                            dp(1),
                            0x607A91A4
                        )
                    }
            }

        val audioGuardParams =
            FrameLayout.LayoutParams(
                dp(220),
                dp(30),
                Gravity.CENTER
            ).apply {
                topMargin =
                    dp(130)
            }

        root.addView(
            audioGuardView,
            audioGuardParams
        )

        thermalGuardView =
            TextView(this).apply {
                text =
                    "THERMAL • NORMAL"

                textSize =
                    7.8f

                gravity =
                    Gravity.CENTER

                typeface =
                    Typeface.MONOSPACE

                setTextColor(
                    0xFF91B6A0.toInt()
                )

                setPadding(
                    dp(9),
                    dp(4),
                    dp(9),
                    dp(4)
                )

                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE

                        cornerRadius =
                            dp(14).toFloat()

                        setColor(
                            0x42031829
                        )

                        setStroke(
                            dp(1),
                            0x607A91A4
                        )
                    }
            }

        val thermalGuardParams =
            FrameLayout.LayoutParams(
                dp(220),
                dp(30),
                Gravity.CENTER
            ).apply {
                topMargin =
                    dp(168)
            }

        root.addView(
            thermalGuardView,
            thermalGuardParams
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
                DevelopUgandaBrandMetadataStore.previewTitle(
                    this,
                    "V237"
                ),
                13.8f,
                0xFFD8B85B.toInt(),
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

        cameraExperienceBannerView =
            hud(
                cameraExperienceDisplayName(),
                7.5f,
                cameraExperienceAccentColor(),
                bold = true
            ).apply {
                maxLines = 2
                setPadding(
                    dp(7),
                    dp(2),
                    dp(7),
                    dp(2)
                )
                background =
                    GradientDrawable().apply {
                        shape =
                            GradientDrawable.RECTANGLE
                        cornerRadius =
                            dp(9).toFloat()
                        setColor(
                            0x52031829
                        )
                        setStroke(
                            dp(1),
                            cameraExperienceAccentColor()
                        )
                    }
            }

        previewNarrationPanel.addView(
            cameraExperienceBannerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(27)
            )
        )

        autoViewDescriptionView =
            hud(
                "AUTO VIEW • analysing scene",
                7.2f,
                0xFF62D8C9.toInt(),
                bold = true
            ).apply {
                maxLines = 1
                isSingleLine = true
            }

        previewNarrationPanel.addView(
            autoViewDescriptionView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(24)
            )
        )

        shotQualityGuardView =
            hud(
                "SHOT GUARD • READY",
                7.0f,
                0xFF91B6A0.toInt(),
                bold = true
            ).apply {
                maxLines =
                    2
            }

        previewNarrationPanel.addView(
            shotQualityGuardView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
            )
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
                0xFFD8B85B.toInt(),
                bold = true
            )

        previewPlaceView =
            hud(
                "",
                5.8f,
                Color.WHITE
           ,
                bold = true
            )

        previewGpsView =
            hud(
                "",
                5.6f,
                0xFF83C7D4.toInt()
           ,
                bold = true
            )

        previewNavView =
            hud(
                "",
                5.6f,
                0xFF83C7D4.toInt()
           ,
                bold = true
            )

        previewSystemView =
            hud(
                "",
                5.4f,
                0xFF83B995.toInt()
           ,
                bold = true
            )

        previewHealthView =
            hud(
                "",
                5.7f,
                0xFF83B995.toInt(),
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
            0xFFD8B85B.toInt(),
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
            0xFFD8B85B.toInt()
        )
        locationView = hud(
            "GPS acquiring…",
            6f,
            0xFF83C7D4.toInt()
        )
        weatherView = hud(
            "WX --",
            6f,
            0xFF9FD9FF.toInt()
        )
        systemView = hud(
            "MIC READY • NET -- • BAT -- • FREE --",
            6f,
            0xFF83B995.toInt()
        )

        // ORBIT DECK: a custom floating control system. No large black panel.
        bottomDeck = LinearLayout(this).apply {
            tag = "v237_camera_deck"
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
            tag = "v237_camera_mode_row"
            gravity = Gravity.CENTER
        }

        sceneButton = deckButton(
            "SCENE ▾\n${sceneModes[sceneIndex]}",
            0xFFD8B85B.toInt()
        )
        lookButton = deckButton(
            "LOOK ▾\n${lookModes[lookIndex]}",
            0xFF83C7D4.toInt()
        )
        qualityButton = deckButton(
            "FORMAT ▾\n${qualityDeckLabel()}",
            0xFFE8F1F2.toInt()
        )
        captureModeButton = deckButton(
            "CAPTURE ▾\n${captureModes[captureModeIndex]}",
            0xFF83B995.toInt()
        )
        colorButton = deckButton(
            "COLOR ▾\n${v229ColorDeckLabel()}",
            0xFFA793D8.toInt()
        )

        sceneButton.tag = "v237_scene_button"
        lookButton.tag = "v237_look_button"
        qualityButton.tag = "v237_quality_button"
        captureModeButton.tag = "v237_capture_button"
        colorButton.tag = "v237_color_button"

        listOf(
            sceneButton,
            lookButton,
            qualityButton,
            captureModeButton,
            colorButton
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
            tag = "v237_camera_identity_row"
            gravity = Gravity.CENTER
        }
        identityButton = deckButton(
            identityButtonText(),
            0xFFD8B85B.toInt()
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
            tag = "v237_camera_tools_row"
            gravity = Gravity.CENTER
        }

        viewModeButton = deckButton(
            "VIEW\nFULL SCREEN",
            0xFF83B995.toInt()
        )

        settingsButton = deckButton(
            "SETTINGS\nREPORT",
            0xFF83C7D4.toInt()
        )

        guidesButton = deckButton(
            "GUIDES ▾\nON",
            0xFFD8B85B.toInt()
        )

        resetButton = deckButton(
            "RESET\nCAM",
            0xFFB66B67.toInt()
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
                tag = "v237_camera_advanced_row"
                gravity =
                    Gravity.CENTER
            }

        autoUiButton =
            deckButton(
                "AUTO UI ▾\nON",
                0xFF83B995.toInt()
            ).apply {
                isSelected = true
            }

        lockButton =
            deckButton(
                "LOCK ▾\nOFF",
                0xFFD8B85B.toInt()
            )

        integrityButton =
            deckButton(
                "VERIFY ▾\nSHA-256",
                0xFF83C7D4.toInt()
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
                0xFF83B995.toInt()
            )

        assistButton =
            deckButton(
                "ASSIST ▾\n${shotAssistModeLabels[shotAssistModeIndex]}",
                0xFF62D8C9.toInt()
            )


        listOf(
            autoUiButton,
            lockButton,
            cleanModeButton,
            assistButton
        ).forEachIndexed { index, button ->
            reportAdvancedRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(34),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(7)
                    }
                }
            )
        }

        bottomDeck.addView(
            reportAdvancedRow
        )

        reportDisplayRow =
            row().apply {
                tag = "v237_camera_display_row"
                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        hudSizeButton =
            deckButton(
                "HUD SIZE ▾\n${reportHudLabels[reportHudSizeIndex]}",
                0xFF6D88A4.toInt()
            ).apply {
                isSelected =
                    true
            }

        listOf(
            integrityButton,
            capabilitiesButton,
            hudSizeButton
        ).forEachIndexed { index, button ->
            reportDisplayRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(34),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(7)
                    }
                }
            )
        }

        bottomDeck.addView(
            reportDisplayRow
        )

        reportOutputRow =
            row().apply {
                tag = "v237_camera_output_row"
                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        hudContrastButton =
            deckButton(
                "HUD CONTRAST ▾\n${reportHudContrastLabels[reportHudContrastIndex]}",
                0xFF83799A.toInt()
            ).apply {
                isSelected =
                    true
            }

        hudBackingButton =
            deckButton(
                "HUD BACKING ▾\n${reportHudBackingLabels[reportHudBackingIndex]}",
                0xFF6F9C7C.toInt()
            ).apply {
                isSelected =
                    reportHudBackingIndex !=
                        0
            }

        reportPresetButton =
            deckButton(
                "PRESET ▾\n${reportPresetLabels[reportPresetIndex]}",
                0xFF8B9499.toInt()
            ).apply {
                isSelected =
                    reportPresetIndex !=
                        0
            }

        autoDirectorButton =
            deckButton(
                "AUTO DIRECTOR ▾\n" +
                    if (
                        autoDirectorEnabled
                    ) {
                        "ON"
                    } else {
                        "OFF"
                    },
                0xFF73B7D9.toInt()
            ).apply {
                isSelected =
                    autoDirectorEnabled
            }

        listOf(
            hudContrastButton,
            hudBackingButton,
            reportPresetButton,
            autoDirectorButton
        ).forEachIndexed { index, button ->
            reportOutputRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(34),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart =
                            dp(6)
                    }
                }
            )
        }

        bottomDeck.addView(
            reportOutputRow
        )

        reportDirectorRow =
            row().apply {
                tag = "v237_camera_director_row"
                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

        directorButton =
            deckButton(
                "DIRECTOR ▾\n" +
                    if (
                        directorEnabled
                    ) {
                        "ON"
                    } else {
                        "OFF"
                    },
                0xFF91B6A0.toInt()
            ).apply {
                isSelected =
                    directorEnabled
            }

        continuityButton =
            deckButton(
                "MATCH LAST\nSHOT",
                0xFFAEBDEB.toInt()
            ).apply {
                isSelected =
                    DevelopUgandaContinuityMemory.load(
                        this@DevelopUgandaCameraActivity,
                        cameraExperienceId
                    ) != null
            }

        healthButton =
            deckButton(
                "CAMERA\nHEALTH",
                0xFF73B7D9.toInt()
            )

        brandMetadataButton =
            deckButton(
                "BRAND\nTAGS",
                0xFFD0B06F.toInt()
            )

        listOf(
            directorButton,
            continuityButton,
            healthButton,
            brandMetadataButton
        ).forEachIndexed {
                index,
                button ->
            reportDirectorRow.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(34),
                    1f
                ).apply {
                    if (
                        index >
                            0
                    ) {
                        marginStart =
                            dp(7)
                    }
                }
            )
        }

        bottomDeck.addView(
            reportDirectorRow
        )

        settingsSummaryView = hud(
            reportSettingsSummary(),
            5.9f,
            0xFFC9D7DD.toInt()
        ).apply {
            tag = "v237_camera_settings_summary"
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
                        0xC4082236.toInt()
                    )
                    setStroke(
                        dp(1),
                        0x80D9DEE8.toInt()
                    )
                }
        }

        bottomDeck.addView(settingsSummaryView)

        zoomRow = row().apply { tag = "v237_camera_zoom_row" }
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

        exposureRow = row().apply { tag = "v237_camera_exposure_row" }
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
            tag = "v237_camera_action_row"
            gravity = Gravity.CENTER
        }

        lensButton = deckButton(
            "LENS ▾\n${currentLensDeckLabel()}",
            0xFF83C7D4.toInt()
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

        DevelopUgandaLiveGradePanel.attach(
            activity = this,
            root = root,
            previewView = previewView,
            scopeProvider = { v229ColorScope() },
            hintProvider = { v229ColorHint() }
        )
        DevelopUgandaUnifiedControlDeck.attach(
            activity = this,
            root = root,
            scopeProvider = { v229ColorScope() },
            hintProvider = { v229ColorHint() },
            mode = DevelopUgandaUnifiedControlDeck.Mode.REPORT
        )
        DevelopUgandaFieldIntelligencePanel.attach(
            activity = this,
            root = root,
            previewView = previewView
        )
        DevelopUgandaAdaptiveFormatUi.attach(
            activity = this,
            root = root,
            role = DevelopUgandaAdaptiveFormatUi.Role.REPORT
        )
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

        colorButton.setOnTouchListener(
            DeckTouchListener(ACTION_COLOR_ENGINE)
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

        hudSizeButton.setOnTouchListener(
            DeckTouchListener(ACTION_HUD_SIZE)
        )

        hudContrastButton.setOnTouchListener(
            DeckTouchListener(ACTION_HUD_CONTRAST)
        )

        hudBackingButton.setOnTouchListener(
            DeckTouchListener(ACTION_HUD_BACKING)
        )

        reportPresetButton.setOnTouchListener(
            DeckTouchListener(ACTION_REPORT_PRESET)
        )

        autoDirectorButton.setOnTouchListener(
            DeckTouchListener(ACTION_AUTO_DIRECTOR)
        )

        assistButton.setOnTouchListener(
            DeckTouchListener(ACTION_SHOT_ASSIST)
        )

        directorButton.setOnTouchListener(
            DeckTouchListener(ACTION_DIRECTOR)
        )

        continuityButton.setOnTouchListener(
            DeckTouchListener(ACTION_CONTINUITY)
        )

        healthButton.setOnTouchListener(
            DeckTouchListener(ACTION_HEALTH)
        )

        brandMetadataButton.setOnTouchListener(
            DeckTouchListener(ACTION_BRAND_METADATA)
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

                    focusLongPressTriggered =
                        false

                    showFocusReticle(
                        event.x,
                        event.y,
                        false
                    )

                    uiHandler.removeCallbacks(
                        focusLockRunnable
                    )

                    uiHandler.postDelayed(
                        focusLockRunnable,
                        650L
                    )

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

                            uiHandler.removeCallbacks(
                                focusLockRunnable
                            )

                            if (
                                ::focusReticleView.isInitialized &&
                                !focusLockActive
                            ) {
                                focusReticleView.visibility =
                                    View.GONE
                            }
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
                    uiHandler.removeCallbacks(
                        focusLockRunnable
                    )

                    if (
                        !gestureMoved &&
                        !focusLongPressTriggered
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
                                selectedCameraDeviceId =
                                    null

                                useFront =
                                    !useFront

                                lensButton.text =
                                    "LENS\n${currentLensDeckLabel()}"

                                bindCamera()
                                toast(
                                    "Lens switched"
                                )
                            }
                        } else {
                            releaseFocusLockForTap()

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

                MotionEvent.ACTION_CANCEL -> {
                    uiHandler.removeCallbacks(
                        focusLockRunnable
                    )
                    true
                }

                else ->
                    true
            }
        }
    }

    private fun enforceFullFramePreview() {
        halfPreviewMode =
  false

        previewView.layoutParams =
  FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
  ).apply {
      gravity =
          Gravity.TOP
  }

        previewView.scaleType =
  PreviewView.ScaleType.FILL_CENTER

        guidesView.layoutParams =
  FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
  ).apply {
      gravity =
          Gravity.TOP
  }

        if (::viewModeButton.isInitialized) {
  viewModeButton.text =
      "VIEW\nFULL SCREEN"
        }

        if (::previewNarrationPanel.isInitialized) {
  val hudParams =
      previewNarrationPanel.layoutParams as
          FrameLayout.LayoutParams

  hudParams.topMargin =
      dp(44)

  previewNarrationPanel.layoutParams =
      hudParams

  previewBrandView.textSize =
      13.8f
        }
    }

    private fun togglePreviewMode() {
        enforceFullFramePreview()
        enforceImmersiveCameraWindow()

        toast(
  "Full-screen camera view"
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

        saveReportCameraPreferences()

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
            append(" • COLOR ")
            append(
                v229ColorResolved().statusLabel()
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
            append(" • HUD ")
            append(
                reportHudLabels[
                    reportHudSizeIndex
                ]
            )
            append(" / ")
            append(
                reportHudContrastLabels[
                    reportHudContrastIndex
                ]
            )
            append(" / BACKING ")
            append(
                reportHudBackingLabels[
                    reportHudBackingIndex
                ]
            )
            append(" • PRESET ")
            append(
                reportPresetLabels[
                    reportPresetIndex
                ]
            )
            append(" • SETTINGS MEMORY ON")
            append(" • SWIPE ZOOM/EXP • DOUBLE TAP LENS")
            append(" • SOCIAL CAMERA 1080P/HIGH BITRATE • DEVICE AE/AF/AWB • TELEMETRY BURN-IN ON • GPS/GNSS • COMPASS • WEATHER • LEVEL • MIC • NET • BAT • STORAGE")
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

        saveReportCameraPreferences()
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

        saveReportCameraPreferences()
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
            reportDisplayRow,
            reportOutputRow,
            reportDirectorRow,
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


    private fun isSocialMediaCamera(): Boolean {
        return cameraExperienceId ==
            "V222_SOCIAL"
    }

    private fun exportAutomaticSocialMaster(
        inputUri: Uri,
        storyPackageId: String
    ) {
        if (
            !isSocialMediaCamera() ||
            automaticSocialExportActive
        ) {
            return
        }

        automaticSocialExportActive =
            true

        runOnUiThread {
            if (
                ::statusView.isInitialized
            ) {
                statusView.text =
                    "SM OPTIMIZING"

                statusView.setTextColor(
                    0xFF62D8C9.toInt()
                )
            }

            toast(
                "SM Camera • creating social-ready copy"
            )
        }

        val exportDir =
            File(
                cacheDir,
                "v222_social_camera_exports"
            ).apply {
                mkdirs()
            }

        val temp =
            File(
                exportDir,
                "sm_${System.currentTimeMillis()}.mp4"
            )

        if (
            temp.exists()
        ) {
            temp.delete()
        }

        val sourceItem =
            MediaItem.Builder()
                .setUri(
                    inputUri
                )
                .build()

        val socialEffects =
            Effects(
                emptyList(),
                listOf(
                    Presentation.createForWidthAndHeight(
                        1080,
                        1920,
                        Presentation.LAYOUT_SCALE_TO_FIT
                    ),

                    // Deliberately non-zero so Media3 cannot transmux the
                    // camera bitstream unchanged. This forces a real encode.
                    Brightness(
                        0.0001f
                    )
                )
            )

        val edited =
            EditedMediaItem.Builder(
                sourceItem
            )
                .setFrameRate(
                    30
                )
                .setEffects(
                    socialEffects
                )
                .build()

        val sequence =
            EditedMediaItemSequence.withAudioAndVideoFrom(
                listOf(
                    edited
                )
            )

        val compositionBuilder =
            Composition.Builder(
                listOf(
                    sequence
                )
            )

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
        ) {
            compositionBuilder.setHdrMode(
                Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
            )
        }

        val composition =
            compositionBuilder
                .build()

        val videoSettings =
            VideoEncoderSettings.Builder()
                .setBitrate(
                    16_000_000
                )
                .setiFrameIntervalSeconds(
                    2f
                )
                .build()

        val audioSettings =
            AudioEncoderSettings.Builder()
                .setBitrate(
                    256_000
                )
                .build()

        val encoderFactory =
            DefaultEncoderFactory.Builder(
                this
            )
                .setRequestedVideoEncoderSettings(
                    videoSettings
                )
                .setRequestedAudioEncoderSettings(
                    audioSettings
                )
                .build()

        val listener =
            object :
                Transformer.Listener {

                override fun onCompleted(
                    composition: Composition,
                    result: ExportResult
                ) {
                    automaticSocialTransformer =
                        null

                    Thread {
                        try {
                            val verification =
                                verifyAutomaticSocialMaster(
                                    temp
                                )

                            val socialUri =
                                publishAutomaticSocialMaster(
                                    temp
                                )

                            DevelopUgandaStoryPackager.attachSocialMaster(
                                this@DevelopUgandaCameraActivity,
                                storyPackageId,
                                socialUri
                            )

                            temp.delete()

                            automaticSocialExportActive =
                                false

                            runOnUiThread {
                                if (
                                    ::statusView.isInitialized
                                ) {
                                    statusView.text =
                                        "SM READY"

                                    statusView.setTextColor(
                                        0xFF62D8C9.toInt()
                                    )
                                }

                                toast(
                                    "SM READY • develop.uganda / SM Posts • $verification"
                                )
                            }
                        } catch (
                            e: Exception
                        ) {
                            automaticSocialExportActive =
                                false

                            temp.delete()

                            runOnUiThread {
                                if (
                                    ::statusView.isInitialized
                                ) {
                                    statusView.text =
                                        "SM EXPORT ERROR"

                                    statusView.setTextColor(
                                        0xFFFF5A54.toInt()
                                    )
                                }

                                toast(
                                    "SM optimization failed • original video is safe"
                                )
                            }
                        }
                    }.start()
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    automaticSocialTransformer =
                        null

                    automaticSocialExportActive =
                        false

                    temp.delete()

                    runOnUiThread {
                        if (
                            ::statusView.isInitialized
                        ) {
                            statusView.text =
                                "SM EXPORT ERROR"

                            statusView.setTextColor(
                                0xFFFF5A54.toInt()
                            )
                        }

                        DevelopUgandaStoryPackager.markSocialMasterFailed(
                            this@DevelopUgandaCameraActivity,
                            storyPackageId,
                            "Media3 social export failed"
                        )

                        toast(
                            "SM optimization failed • original video is safe"
                        )
                    }
                }
            }

        automaticSocialTransformer =
            Transformer.Builder(
                this
            )
                .setEncoderFactory(
                    encoderFactory
                )
                .setVideoMimeType(
                    MimeTypes.VIDEO_H264
                )
                .setAudioMimeType(
                    MimeTypes.AUDIO_AAC
                )
                .addListener(
                    listener
                )
                .build()
                .also {
                    it.start(
                        composition,
                        temp.absolutePath
                    )
                }
    }

    private fun verifyAutomaticSocialMaster(
        temp: File
    ): String {
        if (
            !temp.exists() ||
            temp.length() <=
                0L
        ) {
            error(
                "SM output is empty"
            )
        }

        val retriever =
            MediaMetadataRetriever()

        return try {
            retriever.setDataSource(
                temp.absolutePath
            )

            val width =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )
                    ?.toIntOrNull()
                    ?: 0

            val height =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )
                    ?.toIntOrNull()
                    ?: 0

            val totalBitrate =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_BITRATE
                )
                    ?.toLongOrNull()
                    ?: 0L

            if (
                width !=
                    1080 ||
                height !=
                    1920
            ) {
                error(
                    "Unexpected SM dimensions ${width}×${height}"
                )
            }

            if (
                totalBitrate >
                    21_600_000L
            ) {
                error(
                    "SM bitrate remained too close to original"
                )
            }

            if (
                totalBitrate <
                    4_000_000L
            ) {
                error(
                    "SM bitrate unexpectedly low"
                )
            }

            val mbps =
                String.format(
                    Locale.US,
                    "%.1f",
                    totalBitrate /
                        1_000_000.0
                )

            "1080×1920 • $mbps Mbps"
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun publishAutomaticSocialMaster(
        temp: File
    ): Uri {
        val stamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(
                Date()
            )

        val name =
            "DEVELOP_UGANDA_V222_SM_POST_" +
                stamp +
                ".mp4"

        val values =
            ContentValues().apply {
                put(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    name
                )

                put(
                    MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4"
                )

                if (
                    Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.Q
                ) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/develop.uganda/SM Posts"
                    )

                    put(
                        MediaStore.Video.Media.IS_PENDING,
                        1
                    )
                }
            }

        val uri =
            contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: error(
                "Could not create SM Posts video"
            )

        try {
            contentResolver.openOutputStream(
                uri,
                "w"
            )?.use { output ->
                FileInputStream(
                    temp
                ).use { input ->
                    input.copyTo(
                        output,
                        1024 * 1024
                    )
                }
            } ?: error(
                "Could not write SM Posts video"
            )

            if (
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
            ) {
                contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(
                            MediaStore.Video.Media.IS_PENDING,
                            0
                        )
                    },
                    null,
                    null
                )
            }

            return uri
        } catch (
            e: Exception
        ) {
            try {
                contentResolver.delete(
                    uri,
                    null,
                    null
                )
            } catch (_: Exception) {
            }

            throw e
        }
    }


    private fun startAutoViewDescription() {
        if (
            ::autoViewLabeler.isInitialized
        ) {
            return
        }

        autoViewLabeler =
            ImageLabeling.getClient(
                ImageLabelerOptions.Builder()
                    .setConfidenceThreshold(
                        0.62f
                    )
                    .build()
            )

        uiHandler.removeCallbacks(
            autoViewRunnable
        )

        uiHandler.postDelayed(
            autoViewRunnable,
            1700L
        )
    }

    private fun analyzeAutoViewFrame() {
        if (
            autoViewBusy ||
            !::previewView.isInitialized ||
            previewView.width <= 0 ||
            previewView.height <= 0
        ) {
            return
        }

        val bitmap =
            try {
                previewView.bitmap
            } catch (_: Exception) {
                null
            } ?: return

        autoViewBusy =
            true

        autoViewLabeler.process(
            InputImage.fromBitmap(
                bitmap,
                0
            )
        )
            .addOnSuccessListener {
                    labels ->
                val top =
                    labels
                        .sortedByDescending {
                            it.confidence
                        }
                        .filter {
                            it.confidence >= 0.62f
                        }
                        .take(3)
                        .map {
                            it.text.trim()
                        }
                        .filter {
                            it.isNotBlank()
                        }

                autoViewSummary =
                    if (
                        top.isEmpty()
                    ) {
                        "AUTO VIEW • scene not confidently identified"
                    } else {
                        "AUTO VIEW • likely " +
                            top.joinToString(
                                " • "
                            )
                    }

                if (
                    ::autoViewDescriptionView.isInitialized
                ) {
                    autoViewDescriptionView.text =
                        autoViewSummary
                }
            }
            .addOnFailureListener {
                autoViewSummary =
                    "AUTO VIEW • analysing scene"

                if (
                    ::autoViewDescriptionView.isInitialized
                ) {
                    autoViewDescriptionView.text =
                        autoViewSummary
                }
            }
            .addOnCompleteListener {
                autoViewBusy =
                    false
            }
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
        val qualitySnapshot =
            qualityModes[
                qualityIndex
            ]

        val modePurposeSnapshot =
            reportModePurposeLabel()

        val autoDirectorSnapshot =
            autoDirectorStateText()

        val cameraExperienceIdSnapshot =
            cameraExperienceId

        val cameraExperienceLabelSnapshot =
            cameraExperienceDisplayName()

        val sceneSnapshot =
            sceneModes[
                sceneIndex
            ]
        val lookSnapshot =
            lookModes[
                lookIndex
            ]

        val colorProfileSnapshot =
            v229ColorResolved().statusLabel()

        val thermalSnapshot =
            thermalStateLabel()

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
                            "V237"
                        )
                        put(
                            "camera_engine",
                            "V217 FULL FRAME HUD + V221 SHOT FINDER"
                        )
                        put(
                            "camera_modules",
                            "V204,V205,V206,V207,V208,V209,V210,V211,V212,V213,V214,V215,V216,V217"
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
                            "quality_mode",
                            qualitySnapshot
                        )
                        put(
                            "mode_purpose",
                            modePurposeSnapshot
                        )
                        put(
                            "auto_director",
                            autoDirectorSnapshot
                        )
                        put(
                            "camera_experience_id",
                            cameraExperienceIdSnapshot
                        )
                        put(
                            "camera_experience_label",
                            cameraExperienceLabelSnapshot
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
                            "color_profile",
                            colorProfileSnapshot
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
                            "thermal_status",
                            thermalSnapshot
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

        val scale =
            reportHudScales[
                reportHudSizeIndex
            ]

        val brandSize =
            when {
                widthDp <= 360 ->
                    11.5f

                widthDp <= 420 ->
                    12.8f

                else ->
                    13.8f
            } *
                scale

        val rowSize =
            when {
                widthDp <= 360 ->
                    5.2f

                widthDp <= 420 ->
                    5.7f

                else ->
                    6.1f
            } *
                scale

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
                reportDisplayRow,
                reportOutputRow,
                reportDirectorRow,
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

    private data class FieldPreflight(
        val critical: List<String>,
        val warnings: List<String>,
        val ready: List<String>
    )

    private fun shotQualityWarnings(): List<String> {
        val warnings =
            mutableListOf<String>()

        ambientLux?.let {
            if (
                it <
                    25f
            ) {
                warnings.add(
                    "TOO DARK"
                )
            }
        }

        if (
            recording !=
                null &&
            audioPeakAmplitude >=
                0.90
        ) {
            warnings.add(
                "MIC CLIPPING"
            )
        }

        if (
            cameraShakeScore >
                22f
        ) {
            warnings.add(
                "SHAKE HIGH"
            )
        }

        phoneRollDeg?.let {
            if (
                kotlin.math.abs(
                    it
                ) >
                    3.0f
            ) {
                warnings.add(
                    "HORIZON OFF"
                )
            }
        }

        if (
            isThermalSevereOrWorse()
        ) {
            warnings.add(
                "THERMAL RISK"
            )
        }

        freeStorageGb()?.let {
            if (
                it <=
                    4L
            ) {
                warnings.add(
                    "STORAGE LOW"
                )
            }
        }

        val gpsAge =
            if (
                lastGpsUpdateMs >
                    0L
            ) {
                System.currentTimeMillis() -
                    lastGpsUpdateMs
            } else {
                Long.MAX_VALUE
            }

        if (
            accuracy ==
                null ||
            (
                accuracy ?: 999f
                ) >
                50f ||
            gpsAge >
                10_000L
        ) {
            warnings.add(
                "GPS WEAK"
            )
        }

        if (
            focusAttempted &&
            focusSuccessful ==
                false
        ) {
            warnings.add(
                "SUBJECT NOT FOCUSED"
            )
        }

        return warnings.distinct()
    }

    private fun updateShotQualityGuard() {
        if (
            !::shotQualityGuardView.isInitialized
        ) {
            return
        }

        val warnings =
            shotQualityWarnings()

        if (recording != null) {
            recordingWarningsSeen.addAll(
                warnings
            )
        }

        shotQualityGuardView.text =
            if (
                warnings.isEmpty()
            ) {
                "SHOT GUARD • READY"
            } else {
                "SHOT GUARD • " +
                    warnings.joinToString(
                        " • "
                    )
            }

        shotQualityGuardView.setTextColor(
            if (
                warnings.isEmpty()
            ) {
                0xFF91B6A0.toInt()
            } else if (
                warnings.any {
                    it ==
                        "MIC CLIPPING" ||
                    it ==
                        "THERMAL RISK"
                }
            ) {
                0xFFC76D73.toInt()
            } else {
                0xFFD0B06F.toInt()
            }
        )
    }

    private fun fieldPreflight(): FieldPreflight {
        val critical =
            mutableListOf<String>()

        val warnings =
            mutableListOf<String>()

        val ready =
            mutableListOf<String>()

        if (
            camera ==
                null
        ) {
            critical.add(
                "CAMERA NOT READY"
            )
        } else {
            ready.add(
                "CAM READY"
            )
        }

        val storage =
            freeStorageGb()

        when {
            storage ==
                null ->
                    warnings.add(
                        "SPACE UNKNOWN"
                    )

            storage <=
                1L ->
                    critical.add(
                        "STORAGE CRITICAL ${storage}GB"
                    )

            storage <=
                4L ->
                    warnings.add(
                        "STORAGE LOW ${storage}GB"
                    )

            else ->
                ready.add(
                    "SPACE ${storage}GB"
                )
        }

        val battery =
            batteryPct()

        when {
            battery ==
                null ->
                    warnings.add(
                        "BATTERY UNKNOWN"
                    )

            battery <=
                3 ->
                    critical.add(
                        "BATTERY CRITICAL $battery%"
                    )

            battery <=
                10 ->
                    warnings.add(
                        "BATTERY LOW $battery%"
                    )

            else ->
                ready.add(
                    "BATTERY $battery%"
                )
        }

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
            thermalStatus >=
                PowerManager.THERMAL_STATUS_CRITICAL
        ) {
            critical.add(
                "THERMAL ${thermalStateLabel()}"
            )
        } else if (
            isThermalSevereOrWorse()
        ) {
            warnings.add(
                "THERMAL ${thermalStateLabel()}"
            )
        } else {
            ready.add(
                "THERMAL ${thermalStateLabel()}"
            )
        }

        val micReady =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) ==
                PackageManager.PERMISSION_GRANTED

        if (
            micReady
        ) {
            ready.add(
                "MIC OK"
            )
        } else {
            warnings.add(
                "MIC OFF"
            )
        }

        val gpsAge =
            if (
                lastGpsUpdateMs >
                    0L
            ) {
                System.currentTimeMillis() -
                    lastGpsUpdateMs
            } else {
                Long.MAX_VALUE
            }

        if (
            accuracy !=
                null &&
            (
                accuracy ?: 999f
                ) <=
                50f &&
            gpsAge <=
                10_000L
        ) {
            ready.add(
                "GPS FIX"
            )
        } else {
            warnings.add(
                "GPS WEAK"
            )
        }

        if (
            isSocialMediaCamera()
        ) {
            if (
                automaticSocialExportActive
            ) {
                warnings.add(
                    "SOCIAL EXPORT BUSY"
                )
            } else {
                ready.add(
                    "SOCIAL MASTER READY"
                )
            }
        }

        shotQualityWarnings()
            .filterNot {
                it in
                    setOf(
                        "STORAGE LOW",
                        "GPS WEAK",
                        "THERMAL RISK"
                    )
            }
            .forEach {
                if (
                    it !in
                        warnings
                ) {
                    warnings.add(
                        it
                    )
                }
            }

        return FieldPreflight(
            critical =
                critical.distinct(),
            warnings =
                warnings.distinct(),
            ready =
                ready.distinct()
        )
    }

    private fun runFieldPreflightBeforeRecording(): Boolean {
        if (
            preflightApprovedOnce
        ) {
            preflightApprovedOnce =
                false
            return true
        }

        val result =
            fieldPreflight()

        val readyText =
            result.ready.joinToString(
                " • "
            )

        if (
            result.critical.isEmpty() &&
            result.warnings.isEmpty()
        ) {
            if (
                ::shotQualityGuardView.isInitialized
            ) {
                shotQualityGuardView.text =
                    "PREFLIGHT GOOD • $readyText"
                shotQualityGuardView.setTextColor(
                    0xFF91B6A0.toInt()
                )
            }

            return true
        }

        if (
            result.critical.isNotEmpty()
        ) {
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "RECORDING PREFLIGHT • BLOCKED"
                )
                .setMessage(
                    buildString {
                        append(
                            "Critical condition:\n"
                        )

                        result.critical.forEach {
                            append(
                                "• $it\n"
                            )
                        }

                        if (
                            result.warnings.isNotEmpty()
                        ) {
                            append(
                                "\nWarnings:\n"
                            )

                            result.warnings.forEach {
                                append(
                                    "• $it\n"
                                )
                            }
                        }

                        append(
                            "\nReady: $readyText"
                        )
                    }
                )
                .setPositiveButton(
                    "OK",
                    null
                )
                .show()

            return false
        }

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "FIELD RECORDING PREFLIGHT"
            )
            .setMessage(
                buildString {
                    append(
                        "Warnings:\n"
                    )

                    result.warnings.forEach {
                        append(
                            "• $it\n"
                        )
                    }

                    append(
                        "\nReady: $readyText"
                    )
                }
            )
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "RECORD ANYWAY"
            ) { _, _ ->
                preflightApprovedOnce =
                    true
                toggleRecording()
            }
            .show()

        return false
    }

    private fun recordingJournalPrefs() =
        getSharedPreferences(
            "develop_uganda_recording_recovery",
            Context.MODE_PRIVATE
        )

    private fun markRecordingJournalStarted() {
        recordingJournalPrefs()
            .edit()
            .putBoolean(
                "active",
                true
            )
            .putBoolean(
                "incomplete",
                false
            )
            .putString(
                "base_name",
                baseName
            )
            .putString(
                "report_id",
                reportId
            )
            .putString(
                "camera",
                cameraExperienceShortLabel()
            )
            .putString(
                "started_utc",
                recordStartUtc
            )
            .apply()
    }

    private fun markRecordingJournalFinished(
        hadError: Boolean
    ) {
        recordingJournalPrefs()
            .edit()
            .putBoolean(
                "active",
                false
            )
            .putBoolean(
                "incomplete",
                hadError
            )
            .putString(
                "last_result",
                if (
                    hadError
                ) {
                    "INCOMPLETE"
                } else {
                    "FINALIZED"
                }
            )
            .apply()
    }

    private fun showRecordingRecoveryNoticeIfNeeded() {
        val prefs =
            recordingJournalPrefs()

        val active =
            prefs.getBoolean(
                "active",
                false
            )

        val incomplete =
            prefs.getBoolean(
                "incomplete",
                false
            )

        if (
            !active &&
            !incomplete
        ) {
            return
        }

        val name =
            prefs.getString(
                "base_name",
                "--"
            ) ?: "--"

        val cameraName =
            prefs.getString(
                "camera",
                "--"
            ) ?: "--"

        val started =
            prefs.getString(
                "started_utc",
                "--"
            ) ?: "--"

        val itemFound =
            try {
                contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.Video.Media._ID
                    ),
                    "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?",
                    arrayOf(
                        "$name%"
                    ),
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )?.use {
                    it.moveToFirst()
                } ?: false
            } catch (_: Exception) {
                false
            }

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "RECOVERED / INCOMPLETE CLIP"
            )
            .setMessage(
                buildString {
                    append(
                        "The previous recording session did not reach a clean completion record.\n\n"
                    )
                    append(
                        "CAMERA • $cameraName\n"
                    )
                    append(
                        "START • $started\n"
                    )
                    append(
                        "FILE • $name.mp4\n\n"
                    )
                    append(
                        if (
                            itemFound
                        ) {
                            "A Gallery item with this name exists. Inspect/play it before relying on it. develop.uganda does not claim a damaged MP4 was repaired."
                        } else {
                            "No matching Gallery item was confirmed. The recovery journal preserves the recording identity so the loss is not silent."
                        }
                    )
                }
            )
            .setNegativeButton(
                "KEEP NOTICE",
                null
            )
            .setPositiveButton(
                "ACKNOWLEDGE"
            ) { _, _ ->
                prefs.edit()
                    .putBoolean(
                        "active",
                        false
                    )
                    .putBoolean(
                        "incomplete",
                        false
                    )
                    .apply()
            }
            .show()
    }

    private fun startShotAssistLoop() {
        uiHandler.removeCallbacks(
            shotAssistRunnable
        )

        uiHandler.postDelayed(
            shotAssistRunnable,
            900L
        )
    }

    private fun cycleShotAssist() {
        shotAssistModeIndex =
            (
                shotAssistModeIndex +
                    1
                ) %
                shotAssistModeLabels.size

        if (
            ::shotAssistView.isInitialized
        ) {
            shotAssistView.setAssistMode(
                shotAssistModeIndex
            )
        }

        if (
            ::assistButton.isInitialized
        ) {
            assistButton.text =
                "ASSIST ▾\n${shotAssistModeLabels[shotAssistModeIndex]}"

            assistButton.isSelected =
                shotAssistModeIndex !=
                    DevelopUgandaShotAssistView.MODE_OFF
        }

        toast(
            when (
                shotAssistModeIndex
            ) {
                DevelopUgandaShotAssistView.MODE_PEAK ->
                    "Edge peaking ON • screen only"

                DevelopUgandaShotAssistView.MODE_ZEBRA ->
                    "Exposure zebra ON • screen only"

                DevelopUgandaShotAssistView.MODE_BOTH ->
                    "Peak + zebra ON • screen only"

                else ->
                    "Shot assist OFF"
            }
        )
    }

    private fun isDirectorPeopleMode(): Boolean {
        return cameraExperienceId in
            setOf(
                "V205_FOCUS",
                "V211_AUDIO"
            ) ||
            sceneModes[
                sceneIndex
            ] ==
                "INTERVIEW"
    }

    private fun startDirectorLoop() {
        uiHandler.removeCallbacks(
            directorRunnable
        )

        uiHandler.postDelayed(
            directorRunnable,
            1100L
        )
    }

    private fun toggleDirectorGuidance() {
        directorEnabled =
            !directorEnabled

        if (
            ::directorOverlayView.isInitialized
        ) {
            directorOverlayView.setDirectorEnabled(
                directorEnabled
            )

            directorOverlayView.visibility =
                if (
                    directorEnabled
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        if (
            ::directorButton.isInitialized
        ) {
            directorButton.text =
                "DIRECTOR ▾\n" +
                    if (
                        directorEnabled
                    ) {
                        "ON"
                    } else {
                        "OFF"
                    }

            directorButton.isSelected =
                directorEnabled
        }

        saveReportCameraPreferences()

        toast(
            if (
                directorEnabled
            ) {
                "Director + preview histogram ON"
            } else {
                "Director guidance OFF"
            }
        )
    }

    private fun estimatedRecordingTimeText(): String {
        return try {
            val freeBytes =
                StatFs(
                    Environment.getExternalStorageDirectory().path
                ).availableBytes

            val bitsPerSecond =
                (
                    targetVideoBitrate()
                        .toLong() +
                        320_000L
                    )
                    .coerceAtLeast(
                        1L
                    )

            val seconds =
                (
                    freeBytes *
                        8L
                    ) /
                    bitsPerSecond

            when {
                seconds <=
                    0L ->
                        "EST REC --"

                seconds >=
                    3600L ->
                        String.format(
                            Locale.US,
                            "EST REC %dh %02dm",
                            seconds /
                                3600L,
                            (
                                seconds /
                                    60L
                                ) %
                                60L
                        )

                else ->
                    String.format(
                        Locale.US,
                        "EST REC %dm",
                        seconds /
                            60L
                    )
            }
        } catch (_: Exception) {
            "EST REC --"
        }
    }

    private fun currentLensDeckLabel(): String {
        val selected =
            selectedCameraDeviceId

        return if (
            !selected.isNullOrBlank()
        ) {
            "ID " +
                selected.takeLast(
                    7
                )
        } else if (
            useFront
        ) {
            "FRONT"
        } else {
            "BACK"
        }
    }

    private fun showRealCameraDevicePicker() {
        val p =
            provider

        if (
            p ==
                null
        ) {
            toast(
                "Camera map is still loading"
            )

            return
        }

        val devices =
            DevelopUgandaLensIntelligence.devices(
                p
            )

        if (
            devices.isEmpty()
        ) {
            toast(
                "No additional CameraX device IDs were exposed"
            )

            return
        }

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "REAL CAMERAS EXPOSED BY ANDROID"
            )
            .setMessage(
                "These are actual CameraX / Camera2 devices exposed by this phone. develop.uganda does not invent 0.5× / 1× / 3× lens buttons."
            )
            .setItems(
                devices
                    .map {
                        it.label()
                    }
                    .toTypedArray()
            ) {
                    _,
                    which ->
                val picked =
                    devices[
                        which
                    ]

                selectedCameraDeviceId =
                    picked.cameraId

                useFront =
                    picked.facing ==
                        "FRONT"

                saveReportCameraPreferences()

                bindCamera()

                toast(
                    "Selected ${picked.shortLabel()}"
                )
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    private fun saveV227ContinuitySnapshot() {
        val cam =
            camera

        val zoom =
            cam
                ?.cameraInfo
                ?.zoomState
                ?.value
                ?.zoomRatio
                ?: 1f

        val exposure =
            cam
                ?.cameraInfo
                ?.exposureState
                ?.exposureCompensationIndex
                ?: sceneExposureTarget

        DevelopUgandaContinuityMemory.save(
            this,
            cameraExperienceId,
            DevelopUgandaContinuityMemory.Snapshot(
                sceneIndex =
                    sceneIndex,
                lookIndex =
                    lookIndex,
                qualityIndex =
                    qualityIndex,
                presetIndex =
                    reportPresetIndex,
                useFront =
                    useFront,
                cameraDeviceId =
                    selectedCameraDeviceId,
                zoomRatio =
                    zoom,
                exposureCompensation =
                    exposure,
                savedUtc =
                    Instant.now().toString()
            )
        )

        if (
            ::continuityButton.isInitialized
        ) {
            continuityButton.text =
                "MATCH LAST\nREADY"

            continuityButton.isSelected =
                true
        }
    }

    private fun matchLastShotContinuity() {
        if (
            recording !=
                null
        ) {
            toast(
                "Stop recording before matching the last shot"
            )

            return
        }

        val snapshot =
            DevelopUgandaContinuityMemory.load(
                this,
                cameraExperienceId
            )

        if (
            snapshot ==
                null
        ) {
            toast(
                "No previous shot is stored for this camera"
            )

            return
        }

        sceneIndex =
            snapshot.sceneIndex
                .coerceIn(
                    0,
                    sceneModes.lastIndex
                )

        lookIndex =
            snapshot.lookIndex
                .coerceIn(
                    0,
                    lookModes.lastIndex
                )

        qualityIndex =
            snapshot.qualityIndex
                .coerceIn(
                    0,
                    qualityModes.lastIndex
                )

        reportPresetIndex =
            snapshot.presetIndex
                .coerceIn(
                    0,
                    reportPresetLabels.lastIndex
                )

        useFront =
            snapshot.useFront

        selectedCameraDeviceId =
            snapshot.cameraDeviceId
                ?.takeIf {
                    DevelopUgandaLensIntelligence.hasCamera(
                        provider,
                        it
                    )
                }

        sceneExposureTarget =
            snapshot.exposureCompensation

        saveReportCameraPreferences()

        bindCamera()

        uiHandler.postDelayed(
            {
                val cam =
                    camera

                val zoomState =
                    cam
                        ?.cameraInfo
                        ?.zoomState
                        ?.value

                if (
                    cam !=
                        null &&
                    zoomState !=
                        null
                ) {
                    val ratio =
                        snapshot.zoomRatio
                            .coerceIn(
                                zoomState.minZoomRatio,
                                zoomState.maxZoomRatio
                            )

                    cam.cameraControl
                        .setZoomRatio(
                            ratio
                        )

                    val span =
                        (
                            zoomState.maxZoomRatio -
                                zoomState.minZoomRatio
                            )
                            .coerceAtLeast(
                                0.01f
                            )

                    zoomSeek.progress =
                        (
                            (
                                ratio -
                                    zoomState.minZoomRatio
                                ) /
                                span *
                                100f
                            )
                            .roundToInt()
                            .coerceIn(
                                0,
                                100
                            )
                }

                val exposure =
                    cam
                        ?.cameraInfo
                        ?.exposureState

                if (
                    cam !=
                        null &&
                    exposure !=
                        null &&
                    exposure.isExposureCompensationSupported
                ) {
                    val value =
                        snapshot.exposureCompensation
                            .coerceIn(
                                exposure.exposureCompensationRange.lower,
                                exposure.exposureCompensationRange.upper
                            )

                    cam.cameraControl
                        .setExposureCompensationIndex(
                            value
                        )

                    sceneExposureTarget =
                        value

                    exposureSeek.progress =
                        (
                            value +
                                6
                            )
                            .coerceIn(
                                0,
                                12
                            )
                }

                refreshHud()

                toast(
                    "MATCH LAST SHOT • restored controllable settings"
                )
            },
            500L
        )
    }

    private fun v229ColorHint(): String {
        return buildString {
            append(cameraExperienceId)
            append(" • ")
            append(cameraExperienceDisplayName())
            append(" • ")
            append(sceneModes[sceneIndex])
            append(" • ")
            append(lookModes[lookIndex])
            append(" • ")
            append(qualityModes[qualityIndex])
            append(" • ")
            append(reportDisplayMode)
        }
    }

    private fun v229ColorScope(): String =
        cameraExperienceId.ifBlank {
            "REPORT"
        }

    private fun v229ColorResolved(): DevelopUgandaColorEngine.ResolvedSelection =
        DevelopUgandaColorEngine.resolve(
            this,
            v229ColorScope(),
            v229ColorHint()
        )

    private fun v229ColorDeckLabel(): String {
        val value = v229ColorResolved()
        return when {
            !value.enabled ->
                "ORIGINAL"

            value.autoResolved ->
                "AUTO " +
                    value.label
                        .removePrefix("DU ")
                        .take(10)

            else ->
                value.label
                    .removePrefix("DU ")
                    .take(12)
        }
    }

    private fun refreshV233ColorMonitor() {
        if (
            !::previewView.isInitialized ||
            !::colorButton.isInitialized
        ) {
            return
        }

        val value = v229ColorResolved()
        v229ColorOverlayLabel =
            if (value.enabled) {
                value.label
            } else {
                "ORIGINAL"
            }

        val key =
            "${value.requestedId}:${value.label}:${value.strength}:${DevelopUgandaColorEngine.monitorEnabled(this)}"

        colorButton.text =
            "COLOR ▾\n${v229ColorDeckLabel()}"

        colorButton.isSelected =
            value.enabled

        if (
            key !=
                lastV233ColorMonitorKey
        ) {
            lastV233ColorMonitorKey =
                key

            DevelopUgandaColorEngine.applyPreviewMonitor(
                previewView,
                value,
                v229ColorScope()
            )
        }
    }

    private fun showV233ColorDropdown(
        anchor: View
    ) {
        if (
            recording !=
                null
        ) {
            toast(
                "Choose the V233 color profile before recording"
            )
            return
        }

        val base =
            DevelopUgandaColorEngine.menuLabels()
                .toMutableList()

        base.add(
            "COLOR STUDIO • STRENGTH / MONITOR"
        )

        val selected =
            DevelopUgandaColorEngine.selectedMenuIndex(
                this,
                v229ColorScope()
            )

        showReportPillDropdown(
            anchor,
            "V233 PROFESSIONAL COLOR",
            base.toTypedArray(),
            selected
        ) {
                picked ->
            if (
                picked >=
                    base.lastIndex
            ) {
                openV233ColorStudio()
                return@showReportPillDropdown
            }

            DevelopUgandaColorEngine.setSelectedMenuIndex(
                this,
                v229ColorScope(),
                picked
            )

            lastV233ColorMonitorKey =
                ""

            refreshV233ColorMonitor()
            refreshReportSettingsSummary()

            toast(
                "V233 COLOR • ${v229ColorResolved().statusLabel()}"
            )
        }
    }

    private fun openV233ColorStudio() {
        startActivity(
            android.content.Intent(
                this,
                DevelopUgandaColorStudioActivity::class.java
            ).apply {
                putExtra(
                    DevelopUgandaColorStudioActivity.EXTRA_SCOPE,
                    v229ColorScope()
                )
                putExtra(
                    DevelopUgandaColorStudioActivity.EXTRA_HINT,
                    v229ColorHint()
                )
            }
        )
    }

    private fun scheduleV233ColorMaster(
        sourceUri: Uri,
        packageId: String
    ) {
        val selection =
            v229ColorResolved()

        if (
            !selection.enabled
        ) {
            DevelopUgandaStoryPackager.markColorMasterSkipped(
                applicationContext,
                packageId,
                "ORIGINAL selected • no V233 color master requested"
            )
            return
        }

        val scopeSnapshot =
            v229ColorScope()
        val hintSnapshot =
            v229ColorHint()

        fun waitForSafeStart(
            attempt: Int
        ) {
            val packageEntry =
                DevelopUgandaStoryPackager.listRegistry(
                    applicationContext
                )
                    .firstOrNull {
                        it.packageId ==
                            packageId
                    }

            val packageBusy =
                packageEntry ==
                    null ||
                    packageEntry.state.contains(
                        "BUILDING",
                        ignoreCase = true
                    )

            val socialBusy =
                isSocialMediaCamera() &&
                    automaticSocialExportActive

            if (
                (packageBusy || socialBusy) &&
                attempt <
                    180
            ) {
                uiHandler.postDelayed(
                    {
                        waitForSafeStart(
                            attempt +
                                1
                        )
                    },
                    1000L
                )
                return
            }

            DevelopUgandaStoryPackager.markColorMasterBuilding(
                applicationContext,
                packageId,
                selection.label,
                selection.strength
            )

            DevelopUgandaColorEngine.exportVideoMaster(
                applicationContext,
                sourceUri,
                packageId,
                scopeSnapshot,
                hintSnapshot
            ) {
                    outcome ->
                if (
                    outcome.success &&
                    outcome.uri !=
                        null
                ) {
                    DevelopUgandaStoryPackager.attachColorMaster(
                        applicationContext,
                        packageId,
                        outcome.uri,
                        outcome.profileLabel,
                        outcome.strength,
                        outcome.width,
                        outcome.height,
                        outcome.durationMs,
                        outcome.bitrate
                    )

                    runOnUiThread {
                        toast(
                            "V233 COLOR MASTER READY • ${outcome.profileLabel}"
                        )
                    }
                } else {
                    DevelopUgandaStoryPackager.markColorMasterFailed(
                        applicationContext,
                        packageId,
                        outcome.message
                    )

                    runOnUiThread {
                        toast(
                            "V233 color master not created • original video is safe"
                        )
                    }
                }
            }
        }

        uiHandler.postDelayed(
            {
                waitForSafeStart(
                    0
                )
            },
            if (
                isSocialMediaCamera()
            ) {
                4500L
            } else {
                1000L
            }
        )
    }

    private fun refreshV228BrandUi() {
        if (
            ::previewBrandView.isInitialized
        ) {
            previewBrandView.text =
                DevelopUgandaBrandMetadataStore.previewTitle(
                    this,
                    "V237"
                )
        }

        if (
            ::brandMetadataButton.isInitialized
        ) {
            val config =
                DevelopUgandaBrandMetadataStore.snapshot(
                    this
                )

            brandMetadataButton.text =
                "BRAND\n" +
                    config.preset
                        .take(
                            9
                        )
        }
    }

    private fun openV228BrandMetadataStudio() {
        startActivity(
            android.content.Intent(
                this,
                DevelopUgandaBrandMetadataActivity::class.java
            )
        )
    }

    private fun openV227CameraHealth() {
        startActivity(
            android.content.Intent(
                this,
                DevelopUgandaCameraHealthActivity::class.java
            )
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
                0xFFD8B85B.toInt()

            else ->
                0xFF83B995.toInt()
        }
    }

    private fun loadReportCameraPreferences() {
        val prefs =
            getSharedPreferences(
                reportCameraPrefsName(),
                Context.MODE_PRIVATE
            )

        sceneIndex =
            prefs.getInt(
                "scene_index",
                sceneIndex
            )
                .coerceIn(
                    0,
                    sceneModes.lastIndex
                )

        lookIndex =
            prefs.getInt(
                "look_index",
                lookIndex
            )
                .coerceIn(
                    0,
                    lookModes.lastIndex
                )

        qualityIndex =
            prefs.getInt(
                "quality_index",
                qualityIndex
            )
                .coerceIn(
                    0,
                    qualityModes.lastIndex
                )

        captureModeIndex =
            prefs.getInt(
                "capture_index",
                captureModeIndex
            )
                .coerceIn(
                    0,
                    captureModes.lastIndex
                )

        reportHudSizeIndex =
            prefs.getInt(
                "hud_size",
                reportHudSizeIndex
            )
                .coerceIn(
                    0,
                    reportHudLabels.lastIndex
                )

        reportHudContrastIndex =
            prefs.getInt(
                "hud_contrast",
                reportHudContrastIndex
            )
                .coerceIn(
                    0,
                    reportHudContrastLabels.lastIndex
                )

        reportHudBackingIndex =
            prefs.getInt(
                "hud_backing",
                reportHudBackingIndex
            )
                .coerceIn(
                    0,
                    reportHudBackingLabels.lastIndex
                )

        reportPresetIndex =
            prefs.getInt(
                "preset_index",
                reportPresetIndex
            )
                .coerceIn(
                    0,
                    reportPresetLabels.lastIndex
                )

        autoDirectorEnabled =
            prefs.getBoolean(
                "auto_director",
                autoDirectorEnabled
            )

        previewGuidesEnabled =
            prefs.getBoolean(
                "guides",
                previewGuidesEnabled
            )

        autoHideOperatorUi =
            prefs.getBoolean(
                "auto_ui",
                autoHideOperatorUi
            )

        integrityEnabled =
            prefs.getBoolean(
                "integrity",
                integrityEnabled
            )

        selectedCameraDeviceId =
            prefs.getString(
                "real_camera_id",
                selectedCameraDeviceId
            )

        directorEnabled =
            prefs.getBoolean(
                "director_enabled",
                directorEnabled
            )
    }

    private fun saveReportCameraPreferences() {
        getSharedPreferences(
            reportCameraPrefsName(),
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(
                "scene_index",
                sceneIndex
            )
            .putInt(
                "look_index",
                lookIndex
            )
            .putInt(
                "quality_index",
                qualityIndex
            )
            .putInt(
                "capture_index",
                captureModeIndex
            )
            .putInt(
                "hud_size",
                reportHudSizeIndex
            )
            .putInt(
                "hud_contrast",
                reportHudContrastIndex
            )
            .putInt(
                "hud_backing",
                reportHudBackingIndex
            )
            .putInt(
                "preset_index",
                reportPresetIndex
            )
            .putBoolean(
                "auto_director",
                autoDirectorEnabled
            )
            .putBoolean(
                "guides",
                previewGuidesEnabled
            )
            .putBoolean(
                "auto_ui",
                autoHideOperatorUi
            )
            .putBoolean(
                "integrity",
                integrityEnabled
            )
            .putString(
                "real_camera_id",
                selectedCameraDeviceId
            )
            .putBoolean(
                "director_enabled",
                directorEnabled
            )
            .apply()
    }

    private fun reportHudBackingAlpha(): Int {
        return when (
            reportHudBackingIndex
        ) {
            0 ->
                0

            2 ->
                58

            else ->
                32
        }
    }

    private fun drawReportTextBackplate(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        val alpha =
            reportHudBackingAlpha()

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

        val left =
            x -
                padX

        val top =
            y +
                metrics.ascent -
                padY

        val right =
            x +
                paint.measureText(
                    value
                ) +
                padX

        val bottom =
            y +
                metrics.descent +
                padY

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
            left,
            top,
            right,
            bottom,
            paint.textSize *
                0.22f,
            paint.textSize *
                0.22f,
            background
        )
    }

    private fun reportHudOutlineScale(): Float {
        return when (
            reportHudContrastIndex
        ) {
            0 ->
                0.014f

            2 ->
                0.032f

            else ->
                0.022f
        }
    }

    private fun reportHudOutlineColor(): Int {
        return when (
            reportHudContrastIndex
        ) {
            0 ->
                0x26000000

            2 ->
                0x52000000

            else ->
                0x38000000
        }
    }

    private fun reportHudShadowRadius(
        u: Float
    ): Float {
        return when (
            reportHudContrastIndex
        ) {
            0 ->
                0.39f * u

            2 ->
                1.0f * u

            else ->
                0.65f * u
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

    private fun showFocusReticle(
        x: Float,
        y: Float,
        locked: Boolean
    ) {
        if (
            !::focusReticleView.isInitialized
        ) {
            return
        }

        val size =
            dp(76).toFloat()

        val maxX =
            (
                previewView.width.toFloat() -
                    size
                ).coerceAtLeast(
                    0f
                )

        val maxY =
            (
                previewView.height.toFloat() -
                    size
                ).coerceAtLeast(
                    0f
                )

        focusReticleView.x =
            (
                x -
                    size /
                    2f
                ).coerceIn(
                    0f,
                    maxX
                )

        focusReticleView.y =
            (
                y -
                    size /
                    2f
                ).coerceIn(
                    0f,
                    maxY
                )

        focusReticleView.text =
            if (locked) {
                "AF + METER\nLOCK"
            } else {
                "AF"
            }

        focusReticleView.setTextColor(
            if (locked) {
                0xFFAEBDEB.toInt()
            } else {
                Color.WHITE
            }
        )

        focusReticleView.background =
            GradientDrawable().apply {
                shape =
                    GradientDrawable.RECTANGLE

                cornerRadius =
                    dp(12).toFloat()

                setColor(
                    if (locked) {
                        0x3A031829
                    } else {
                        0x26031829
                    }
                )

                setStroke(
                    dp(
                        if (locked) {
                            3
                        } else {
                            2
                        }
                    ),
                    if (locked) {
                        0xFFAEBDEB.toInt()
                    } else {
                        0xFFDCE4F7.toInt()
                    }
                )
            }

        focusReticleView.visibility =
            View.VISIBLE

        uiHandler.removeCallbacks(
            hideFocusReticleRunnable
        )

        if (!locked) {
            uiHandler.postDelayed(
                hideFocusReticleRunnable,
                950L
            )
        }
    }

    private fun togglePersistentFocusLock(
        x: Float,
        y: Float
    ) {
        val cam =
            camera
                ?: run {
                    toast(
                        "Camera is not ready"
                    )
                    return
                }

        if (focusLockActive) {
            try {
                cam.cameraControl
                    .cancelFocusAndMetering()
            } catch (_: Exception) {
            }

            focusLockActive =
                false

            if (
                ::focusReticleView.isInitialized
            ) {
                focusReticleView.text =
                    "AF AUTO"

                focusReticleView.visibility =
                    View.VISIBLE

                uiHandler.removeCallbacks(
                    hideFocusReticleRunnable
                )

                uiHandler.postDelayed(
                    hideFocusReticleRunnable,
                    700L
                )
            }

            toast(
                "AF / METER AUTO"
            )

            refreshHud()
            return
        }

        try {
            val point =
                previewView
                    .meteringPointFactory
                    .createPoint(
                        x,
                        y
                    )

            val action =
                FocusMeteringAction
                    .Builder(
                        point,
                        FocusMeteringAction.FLAG_AF or
                            FocusMeteringAction.FLAG_AE or
                            FocusMeteringAction.FLAG_AWB
                    )
                    .disableAutoCancel()
                    .build()


            val focusFuture =
                cam.cameraControl
                    .startFocusAndMetering(
                        action
                    )

            focusAttempted =
                true

            focusSuccessful =
                null

            focusFuture.addListener(
                {
                    focusSuccessful =
                        try {
                            focusFuture.get()
                                .isFocusSuccessful
                        } catch (_: Exception) {
                            false
                        }

                    runOnUiThread {
                        updateShotQualityGuard()

                        if (
                            focusSuccessful ==
                                false
                        ) {
                            toast(
                                "Subject focus not confirmed"
                            )
                        }
                    }
                },
                ContextCompat.getMainExecutor(
                    this
                )
            )

            focusLockActive =
                true

            showFocusReticle(
                x,
                y,
                true
            )

            toast(
                "AF + METER LOCK • hold again to release"
            )

            refreshHud()
        } catch (_: Exception) {
            focusLockActive =
                false

            toast(
                "Focus lock unavailable on this lens"
            )
        }
    }

    private fun releaseFocusLockForTap() {
        if (!focusLockActive) {
            return
        }

        try {
            camera
                ?.cameraControl
                ?.cancelFocusAndMetering()
        } catch (_: Exception) {
        }

        focusLockActive =
            false
    }

    private fun focusAssistLabel(): String {
        return if (focusLockActive) {
            "AF+METER LOCK"
        } else {
            "AF AUTO"
        }
    }

    private fun audioGuardLabel(): String {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return "MIC OFF"
        }

        if (
            audioStateLabel ==
                "MIC ERROR"
        ) {
            return "MIC ERROR"
        }

        if (
            recording ==
                null
        ) {
            return "MIC READY"
        }

        val level =
            audioAmplitude.coerceIn(
                0.0,
                1.0
            )

        return when {
            level <
                0.015 ->
                    "LOW"

            level <
                0.70 ->
                    "GOOD"

            level <
                0.90 ->
                    "HOT"

            else ->
                "CLIP RISK"
        }
    }

    private fun updateAudioGuard() {
        if (
            !::audioGuardView.isInitialized
        ) {
            return
        }

        if (
            operatorControlsHidden ||
            cleanModeEnabled
        ) {
            audioGuardView.visibility =
                View.GONE

            return
        }

        audioGuardView.visibility =
            View.VISIBLE

        val label =
            audioGuardLabel()

        val percent =
            (
                audioAmplitude
                    .coerceIn(
                        0.0,
                        1.0
                    ) *
                    100.0
                ).roundToInt()

        val peakPercent =
            (
                audioPeakAmplitude
                    .coerceIn(
                        0.0,
                        1.0
                    ) *
                    100.0
                ).roundToInt()

        audioGuardView.text =
            when (label) {
                "MIC READY",
                "MIC OFF",
                "MIC ERROR" ->
                    "AUDIO • $label"

                else ->
                    "AUDIO • $label • ${percent}% • PEAK ${peakPercent}%"
            }

        audioGuardView.setTextColor(
            when (label) {
                "GOOD" ->
                    0xFF91B6A0.toInt()

                "HOT" ->
                    0xFFAEBDEB.toInt()

                "CLIP RISK",
                "MIC ERROR" ->
                    0xFFC76D73.toInt()

                "LOW" ->
                    0xFFAEBDEB.toInt()

                else ->
                    0xFFAEB7C7.toInt()
            }
        )
    }

    private fun ambientLightLabel(): String {
        val lux =
            ambientLux
                ?: return "LIGHT --"

        return when {
            lux <
                25f ->
                    "DARK"

            lux <
                100f ->
                    "DIM"

            lux <
                500f ->
                    "NORMAL"

            else ->
                "BRIGHT"
        }
    }

    private fun ambientLightRecommendation(): String {
        val lux =
            ambientLux
                ?: return "SENSOR --"

        val selected =
            qualityModes[
                qualityIndex
            ]

        return when {
            lux <
                25f ->
                    if (
                        selected ==
                            "LOW LIGHT"
                    ) {
                        "LOW LIGHT ACTIVE"
                    } else {
                        "USE LOW LIGHT"
                    }

            lux <
                100f &&
                (
                    selected ==
                        "SOCIAL 60" ||
                    selected ==
                        "UHD 60" ||
                    selected ==
                        "ACTION 60"
                    ) ->
                        "30 FPS ADVISED"

            lux >
                500f &&
                (
                    selected ==
                        "SOCIAL FHD" ||
                    selected ==
                        "ACTION STAB"
                    ) ->
                        "60 FPS AVAILABLE"

            else ->
                "EXPOSURE OK"
        }
    }

    private fun updateLightAdvisor() {
        if (
            !::lightAdvisorView.isInitialized
        ) {
            return
        }

        if (
            operatorControlsHidden ||
            cleanModeEnabled
        ) {
            lightAdvisorView.visibility =
                View.GONE

            return
        }

        lightAdvisorView.visibility =
            View.VISIBLE

        val lux =
            ambientLux

        if (
            lux ==
                null
        ) {
            lightAdvisorView.text =
                "LIGHT • SENSOR --"

            lightAdvisorView.setTextColor(
                0xFFAEB7C7.toInt()
            )

            return
        }

        val label =
            ambientLightLabel()

        lightAdvisorView.text =
            String.format(
                Locale.US,
                "LIGHT • %s • %.0f LUX • %s",
                label,
                lux,
                ambientLightRecommendation()
            )

        lightAdvisorView.setTextColor(
            when (label) {
                "BRIGHT" ->
                    0xFFAEBDEB.toInt()

                "NORMAL" ->
                    0xFF91B6A0.toInt()

                "DIM" ->
                    0xFFAEBDEB.toInt()

                else ->
                    0xFFC76D73.toInt()
            }
        )
    }

    private fun motionGuardLabel(): String {
        return when {
            cameraShakeScore <=
                8f ->
                    "STEADY"

            cameraShakeScore <=
                22f ->
                    "MOVING"

            else ->
                "SHAKE"
        }
    }

    private fun updateMotionGuard() {
        if (
            !::motionGuardView.isInitialized
        ) {
            return
        }

        if (
            operatorControlsHidden ||
            cleanModeEnabled
        ) {
            motionGuardView.visibility =
                View.GONE

            return
        }

        motionGuardView.visibility =
            View.VISIBLE

        val label =
            motionGuardLabel()

        motionGuardView.text =
            String.format(
                Locale.US,
                "STEADYSHOT • %s • %.0f",
                label,
                cameraShakeScore
            )

        motionGuardView.setTextColor(
            when (label) {
                "STEADY" ->
                    0xFF91B6A0.toInt()

                "MOVING" ->
                    0xFFAEBDEB.toInt()

                else ->
                    0xFFC76D73.toInt()
            }
        )
    }

    private fun updateHorizonGuard() {
        if (
            !::horizonGuardView.isInitialized
        ) {
            return
        }

        if (
            operatorControlsHidden ||
            cleanModeEnabled
        ) {
            horizonGuardView.visibility =
                View.GONE

            return
        }

        horizonGuardView.visibility =
            View.VISIBLE

        val roll =
            phoneRollDeg

        if (roll == null) {
            horizonGuardView.rotation =
                0f

            horizonGuardView.text =
                "━━━━━━━━  HORIZON --  ━━━━━━━━"

            horizonGuardView.setTextColor(
                0xFFAEB7C7.toInt()
            )

            return
        }

        val absRoll =
            kotlin.math.abs(
                roll
            )

        val stateText =
            when {
                absRoll <=
                    1.0f ->
                        "LEVEL LOCK"

                absRoll <=
                    3.0f ->
                        "LEVEL NEAR"

                else ->
                    "ADJUST"
            }

        horizonGuardView.rotation =
            (
                -roll
            ).coerceIn(
                -12f,
                12f
            )

        horizonGuardView.text =
            String.format(
                Locale.US,
                "━━━━━━━━  %s  %+.1f°  ━━━━━━━━",
                stateText,
                roll
            )

        horizonGuardView.setTextColor(
            when {
                absRoll <=
                    1.0f ->
                        0xFF91B6A0.toInt()

                absRoll <=
                    3.0f ->
                        0xFFAEBDEB.toInt()

                else ->
                    0xFFC76D73.toInt()
            }
        )
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

        applyThermalSafeProfileIfNeeded()

        p.unbindAll()

        try {
            overlayEffect?.close()
        } catch (_: Exception) {
        }

        overlayEffect = null

        if (
            selectedCameraDeviceId != null &&
            !DevelopUgandaLensIntelligence.hasCamera(
                p,
                selectedCameraDeviceId
            )
        ) {
            selectedCameraDeviceId =
                null
        }

        val selector =
            DevelopUgandaLensIntelligence.selectorFor(
                p,
                selectedCameraDeviceId,
                useFront
            )

        val selectedCameraInfo =
            try {
                p.getCameraInfo(selector)
            } catch (_: Exception) {
                null
            }

        activeVideoFpsLabel =
            if (qualityModes[qualityIndex] == "LOW LIGHT") {
                "AUTO LOW-LIGHT FPS"
            } else {
                "AUTO FPS"
            }
        activeVideoStabilizationLabel = "STAB OFF"
        activeVideoDynamicRangeLabel = "SDR"
        activeVideoAspectLabel =
            DevelopUgandaFieldIntelligencePanel.activeFormatLabel(this)

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
            var selectedQualitySelector =
                buildQualitySelector()
            var selectedDynamicRange =
                DynamicRange.SDR
            var enableVideoStabilization =
                false

            if (selectedCameraInfo != null) {
                try {
                    val capabilities =
                        Recorder.getVideoCapabilities(
                            selectedCameraInfo
                        )

                    if (
                        wantsVideoHdr() &&
                        capabilities.supportedDynamicRanges.contains(
                            DynamicRange.HLG_10_BIT
                        )
                    ) {
                        val hdrQualities =
                            capabilities.getSupportedQualities(
                                DynamicRange.HLG_10_BIT
                            )

                        val orderedHdr =
                            listOf(
                                Quality.UHD,
                                Quality.FHD,
                                Quality.HD
                            ).filter {
                                hdrQualities.contains(it)
                            }

                        if (orderedHdr.isNotEmpty()) {
                            selectedQualitySelector =
                                QualitySelector.fromOrderedList(
                                    orderedHdr,
                                    FallbackStrategy
                                        .lowerQualityOrHigherThan(
                                            Quality.HD
                                        )
                                )
                            selectedDynamicRange =
                                DynamicRange.HLG_10_BIT
                            activeVideoDynamicRangeLabel =
                                "HLG10 HDR"
                        } else {
                            activeVideoDynamicRangeLabel =
                                "SDR HDR-FALLBACK"
                        }
                    } else if (wantsVideoHdr()) {
                        activeVideoDynamicRangeLabel =
                            "SDR HDR-FALLBACK"
                    }

                    enableVideoStabilization =
                        wantsVideoStabilization() &&
                            capabilities.isStabilizationSupported

                    activeVideoStabilizationLabel =
                        if (enableVideoStabilization) {
                            "STAB ON"
                        } else if (wantsVideoStabilization()) {
                            "STAB UNSUPPORTED"
                        } else {
                            "STAB OFF"
                        }
                } catch (_: Exception) {
                    activeVideoDynamicRangeLabel =
                        if (wantsVideoHdr()) {
                            "SDR HDR-FALLBACK"
                        } else {
                            "SDR"
                        }
                    activeVideoStabilizationLabel =
                        "STAB AUTO"
                }
            }

            val recorder =
                Recorder.Builder()
                    .setQualitySelector(
                        selectedQualitySelector
                    )
                    .setAspectRatio(
                        AspectRatio.RATIO_16_9
                    )
                    .setTargetVideoEncodingBitRate(
                        targetVideoBitrate()
                    )
                    .build()

            val videoBuilder =
                VideoCapture.Builder(
                    recorder
                )

            if (
                selectedDynamicRange !=
                DynamicRange.SDR
            ) {
                videoBuilder.setDynamicRange(
                    selectedDynamicRange
                )
            }

            if (enableVideoStabilization) {
                videoBuilder.setVideoStabilizationEnabled(
                    true
                )
            }

            videoCapture =
                videoBuilder.build()
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

        var session =
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

        if (!photoMode && selectedCameraInfo != null) {
            val requestedFps =
                requestedVideoFps()

            if (requestedFps > 0) {
                try {
                    val supportedRanges =
                        selectedCameraInfo
                            .getSupportedFrameRateRanges(
                                session
                            )

                    val exactRange =
                        supportedRanges.firstOrNull {
                            it.lower == requestedFps &&
                                it.upper == requestedFps
                        }

                    val compatibleRange =
                        exactRange
                            ?: supportedRanges
                                .filter {
                                    it.lower <= requestedFps &&
                                        it.upper >= requestedFps
                                }
                                .minByOrNull {
                                    it.upper - it.lower
                                }

                    if (compatibleRange != null) {
                        session =
                            SessionConfig.Builder(
                                preview,
                                videoCapture!!
                            )
                                .addEffect(
                                    overlayEffect!!
                                )
                                .setFrameRateRange(
                                    compatibleRange
                                )
                                .build()

                        activeVideoFpsLabel =
                            if (exactRange != null) {
                                "${requestedFps} FPS"
                            } else {
                                "${compatibleRange.lower}-${compatibleRange.upper} FPS FALLBACK"
                            }
                    } else {
                        activeVideoFpsLabel =
                            "AUTO FPS FALLBACK"
                    }
                } catch (_: Exception) {
                    activeVideoFpsLabel =
                        "AUTO FPS"
                }
            } else {
                activeVideoFpsLabel =
                    "AUTO LOW-LIGHT FPS"
            }
        }

        try {
            camera = p.bindToLifecycle(
                this,
                selector,
                session
            )

            torchOn = false
            torchButton.text = "LIGHT\nOFF"

            focusLockActive =
                false

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
            if (
                selectedCameraDeviceId !=
                    null
            ) {
                val failedId =
                    selectedCameraDeviceId

                selectedCameraDeviceId =
                    null

                saveReportCameraPreferences()

                toast(
                    "Camera ID $failedId cannot use this capture profile • returning to ${if (useFront) "front" else "back"} camera"
                )

                uiHandler.post {
                    bindCamera()
                }
            } else {
                toast(
                    "Selected camera is unavailable"
                )
            }
        }
    }

        private fun drawReporterOverlay(
        frame: Frame
    ) {
        val c =
            frame.overlayCanvas

        val crop =
            frame.cropRect

        if (
            crop.width() <=
                0 ||
            crop.height() <=
                0
        ) {
            return
        }

        c.drawColor(
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
                rotation ==
                    90 ||
                rotation ==
                    270
            ) {
                crop.height()
                    .toFloat()
            } else {
                crop.width()
                    .toFloat()
            }

        val finalHeight =
            if (
                rotation ==
                    90 ||
                rotation ==
                    270
            ) {
                crop.width()
                    .toFloat()
            } else {
                crop.height()
                    .toFloat()
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

        val finalToBuffer =
            Matrix()

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
        c.concat(
            finalToBuffer
        )

        drawCreativeLook(
            c,
            finalWidth,
            finalHeight
        )

        val u =
            minOf(
                finalWidth,
                finalHeight
            ) /
                1000f *
                reportHudScales[
                    reportHudSizeIndex
                ]

        val brandConfig =
            DevelopUgandaBrandMetadataStore
                .snapshot(
                    this
                )

        val safeLeft =
            finalWidth *
                0.050f

        val safeTop =
            finalHeight *
                0.100f

        val maxWidth =
            finalWidth *
                0.56f

        var y =
            safeTop

        val railStartY =
            y -
                (
                    14f *
                        u
                    )

        val telemetryPanel =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    0x30000000

                style =
                    Paint.Style.FILL
            }

        c.drawRoundRect(
            safeLeft -
                (
                    14f *
                        u
                    ),
            safeTop -
                (
                    26f *
                        u
                    ),
            safeLeft +
                maxWidth +
                (
                    14f *
                        u
                    ),
            safeTop +
                (
                    brandConfig
                        .reportPanelHeightUnits() *
                        u
                    ),
            16f *
                u,
            16f *
                u,
            telemetryPanel
        )

        val text =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                typeface =
                    Typeface.create(
                        Typeface.MONOSPACE,
                        Typeface.BOLD
                    )

                setShadowLayer(
                    reportHudShadowRadius(
                        u
                    ),
                    0.45f *
                        u,
                    0.45f *
                        u,
                    reportHudOutlineColor()
                )
            }

        val rail =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color =
                    if (
                        recording !=
                            null
                    ) {
                        0xFFFF4138.toInt()
                    } else {
                        0xFFD8B85B.toInt()
                    }

                strokeWidth =
                    2.3f *
                        u
            }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .BRAND
            )
        ) {
            text.color =
                0xFFD8B85B.toInt()

            text.textSize =
                34f *
                    u

            drawStrongRecordedText(
                c,
                brandConfig.displayName,
                safeLeft,
                y,
                text
            )

            if (
                brandConfig.organization
                    .isNotBlank()
            ) {
                y +=
                    18f *
                        u

                text.color =
                    Color.WHITE

                text.textSize =
                    13.0f *
                        u

                drawFitText(
                    c,
                    brandConfig.organization,
                    safeLeft,
                    y,
                    maxWidth,
                    text,
                    10.5f *
                        u
                )
            }

            y +=
                22f *
                    u
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .VERSION
            )
        ) {
            text.color =
                if (
                    reportDisplayMode ==
                        "LIVE EFFECT"
                ) {
                    0xFFFF5A52.toInt()
                } else {
                    Color.WHITE
                }

            text.textSize =
                15.8f *
                    u

            drawFitText(
                c,
                "${sceneTag()} • V237",
                safeLeft,
                y,
                maxWidth,
                text,
                12.4f *
                    u
            )

            y +=
                18f *
                    u
        }

        // The V227 instruments remain, but V228 lets the user decide
        // which of them is permanently burned into new saved media.
        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .COMPASS
            )
        ) {
            drawCompassInstrument(
                c,
                finalWidth *
                    0.80f,
                finalHeight *
                    0.875f,
                43f *
                    u,
                u
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .AUDIO
            )
        ) {
            drawAudioMeterInstrument(
                c,
                finalWidth *
                    0.705f,
                finalHeight *
                    0.815f,
                finalWidth *
                    0.15f,
                10f *
                    u,
                u
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .HORIZON
            )
        ) {
            drawLevelInstrument(
                c,
                finalWidth *
                    0.80f,
                finalHeight *
                    0.935f,
                92f *
                    u,
                28f *
                    u,
                u
            )
        }

        text.color =
            if (
                recording !=
                    null
            ) {
                0xFFFF4138.toInt()
            } else {
                Color.WHITE
            }

        text.textSize =
            18.0f *
                u

        val recState =
            when {
                recording !=
                    null ->
                        "● REC"

                captureModes[
                    captureModeIndex
                ] ==
                    "PHOTO" ->
                        "● PHOTO"

                else ->
                    "STBY"
            }

        val stateParts =
            mutableListOf(
                recState,
                "TC ${tc()}"
            )

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .VERSION
            )
        ) {
            stateParts.add(
                "V237"
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .THERMAL
            )
        ) {
            stateParts.add(
                "THERM ${thermalStateLabel()}"
            )
        }

        drawFitText(
            c,
            stateParts.joinToString(
                "   •   "
            ),
            safeLeft,
            y,
            maxWidth,
            text,
            14.2f *
                u
        )

        y +=
            19f *
                u

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .CAMERA_MODE
            )
        ) {
            text.color =
                cameraExperienceAccentColor()

            text.textSize =
                15.3f *
                    u

            drawFitText(
                c,
                "CAMERA • ${cameraExperienceShortLabel()}",
                safeLeft,
                y,
                maxWidth,
                text,
                13.6f *
                    u
            )

            y +=
                18f *
                    u

            text.color =
                reportModeAccentColor()

            text.textSize =
                15.2f *
                    u

            drawFitText(
                c,
                "MODE • ${qualityModes[qualityIndex]}   •   SCENE ${sceneModes[sceneIndex]}   •   LOOK ${lookModes[lookIndex]}   •   COLOR $v229ColorOverlayLabel",
                safeLeft,
                y,
                maxWidth,
                text,
                12.8f *
                    u
            )

            y +=
                18f *
                    u

            text.color =
                0xFFAEBDEB.toInt()

            text.textSize =
                14.6f *
                    u

            drawFitText(
                c,
                autoDirectorStateText(),
                safeLeft,
                y,
                maxWidth,
                text,
                12.6f *
                    u
            )

            y +=
                20f *
                    u
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .REPORTER
            ) ||
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .STORY
            )
        ) {
            val identityParts =
                mutableListOf<String>()

            if (
                brandConfig.show(
                    DevelopUgandaBrandMetadataStore
                        .Tag
                        .REPORTER
                )
            ) {
                identityParts.add(
                    "REPORTER • $reporterName"
                )
            }

            if (
                brandConfig.show(
                    DevelopUgandaBrandMetadataStore
                        .Tag
                        .STORY
                )
            ) {
                identityParts.add(
                    "STORY • $storyId"
                )
            }

            text.color =
                Color.WHITE

            text.textSize =
                14.4f *
                    u

            drawFitText(
                c,
                identityParts.joinToString(
                    "   |   "
                ),
                safeLeft,
                y,
                maxWidth,
                text,
                11.8f *
                    u
            )

            y +=
                20f *
                    u
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .DATE_TIME
            )
        ) {
            text.color =
                Color.WHITE

            text.textSize =
                16.0f *
                    u

            drawFitText(
                c,
                "LOCAL ${clock.format(Date())}   |   UTC ${utcClockText()}",
                safeLeft,
                y,
                maxWidth,
                text,
                13.6f *
                    u
            )

            y +=
                20f *
                    u
        }

        fun section(
            heading: String,
            value: String,
            accent: Int =
                0xFF9FD9FF.toInt(),
            valueColor: Int =
                0xFF83C7D4.toInt()
        ) {
            text.color =
                accent

            text.textSize =
                16.8f *
                    u

            drawFitText(
                c,
                heading,
                safeLeft,
                y,
                maxWidth,
                text,
                15.0f *
                    u
            )

            y +=
                16f *
                    u

            text.color =
                valueColor

            text.textSize =
                15.2f *
                    u

            drawFitText(
                c,
                value,
                safeLeft,
                y,
                maxWidth,
                text,
                12.9f *
                    u
            )

            y +=
                20f *
                    u
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .LOCATION
            )
        ) {
            section(
                "LOCATION",
                placeName
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .GPS_COORDS
            ) ||
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .ALTITUDE
            )
        ) {
            val positionParts =
                mutableListOf<String>()

            if (
                brandConfig.show(
                    DevelopUgandaBrandMetadataStore
                        .Tag
                        .GPS_COORDS
                )
            ) {
                positionParts.add(
                    "LAT " +
                        (
                            lat?.let {
                                String.format(
                                    Locale.US,
                                    "%.5f",
                                    it
                                )
                            } ?: "--"
                        )
                )

                positionParts.add(
                    "LON " +
                        (
                            lon?.let {
                                String.format(
                                    Locale.US,
                                    "%.5f",
                                    it
                                )
                            } ?: "--"
                        )
                )
            }

            if (
                brandConfig.show(
                    DevelopUgandaBrandMetadataStore
                        .Tag
                        .ALTITUDE
                )
            ) {
                positionParts.add(
                    "ALT " +
                        (
                            alt?.let {
                                String.format(
                                    Locale.US,
                                    "%.0fm",
                                    it
                                )
                            } ?: "--"
                        )
                )
            }

            section(
                "POSITION",
                positionParts.joinToString(
                    " • "
                )
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .GPS_ACCURACY
            )
        ) {
            val fixAge =
                if (
                    lastGpsUpdateMs >
                        0L
                ) {
                    (
                        System.currentTimeMillis() -
                            lastGpsUpdateMs
                        ) /
                        1000f
                } else {
                    null
                }

            section(
                "GPS STATUS",
                buildString {
                    append(
                        accuracy?.let {
                            String.format(
                                Locale.US,
                                "ACC ±%.0fm",
                                it
                            )
                        } ?: "ACC --"
                    )

                    append(
                        " • SAT ${gnssSatellitesUsed}/${gnssSatellitesVisible}"
                    )

                    append(
                        " • FIX " +
                            (
                                fixAge?.let {
                                    String.format(
                                        Locale.US,
                                        "%.1fs",
                                        it
                                    )
                                } ?: "--"
                            )
                    )
                }
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .COMPASS
            ) ||
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .SPEED_MOTION
            )
        ) {
            val navParts =
                mutableListOf<String>()

            if (
                brandConfig.show(
                    DevelopUgandaBrandMetadataStore
                        .Tag
                        .COMPASS
                )
            ) {
                navParts.add(
                    "COMP " +
                        (
                            compassAzimuthDeg?.let {
                                String.format(
                                    Locale.US,
                                    "%.0f°",
                                    it
                                )
                            } ?: "--"
                        )
                )

                navParts.add(
                    "GPS HDG " +
                        (
                            heading?.let {
                                String.format(
                                    Locale.US,
                                    "%.0f°",
                                    it
                                )
                            } ?: "--"
                        )
                )
            }

            if (
                brandConfig.show(
                    DevelopUgandaBrandMetadataStore
                        .Tag
                        .SPEED_MOTION
                )
            ) {
                navParts.add(
                    "SPD " +
                        (
                            speedKmh?.let {
                                String.format(
                                    Locale.US,
                                    "%.1fkm/h",
                                    it
                                )
                            } ?: "--"
                        )
                )

                navParts.add(
                    motionGuardLabel()
                )

                navParts.add(
                    String.format(
                        Locale.US,
                        "DIST %.0fm",
                        distanceTravelledM
                    )
                )
            }

            section(
                "NAVIGATION",
                navParts.joinToString(
                    " • "
                )
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .HORIZON
            )
        ) {
            section(
                "LEVEL",
                orientationOverlay(),
                Color.WHITE,
                Color.WHITE
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .WEATHER
            )
        ) {
            section(
                "WEATHER",
                weatherOverlay()
                    .removePrefix(
                        "WX "
                    )
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .AUDIO
            )
        ) {
            section(
                "AUDIO",
                audioLevelOverlay(),
                0xFF83B995.toInt(),
                0xFF83B995.toInt()
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .BATTERY_STORAGE
            )
        ) {
            val battery =
                batteryPct()

            val free =
                freeStorageGb()

            section(
                "DEVICE",
                "BAT " +
                    (
                        battery?.let {
                            "$it%"
                        } ?: "--"
                    ) +
                    " • FREE " +
                    (
                        free?.let {
                            "${it}GB"
                        } ?: "--"
                    ),
                0xFF83B995.toInt(),
                0xFF83B995.toInt()
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .NETWORK
            )
        ) {
            section(
                "NETWORK",
                networkType() +
                    " • " +
                    (
                        estimatedUploadKbps?.let {
                            "UP~${it}kbps"
                        } ?: "UP~--"
                    ),
                0xFF83B995.toInt(),
                0xFF83B995.toInt()
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .SHOT_GUARD
            )
        ) {
            val warnings =
                shotQualityWarnings()

            section(
                "SHOT GUARD",
                if (
                    warnings.isEmpty()
                ) {
                    "READY"
                } else {
                    warnings.joinToString(
                        " • "
                    )
                },
                if (
                    warnings.isEmpty()
                ) {
                    0xFF83B995.toInt()
                } else {
                    0xFFD8B85B.toInt()
                },
                if (
                    warnings.isEmpty()
                ) {
                    0xFF83B995.toInt()
                } else {
                    0xFFD8B85B.toInt()
                }
            )
        }

        if (
            brandConfig.show(
                DevelopUgandaBrandMetadataStore
                    .Tag
                    .INTEGRITY
            )
        ) {
            text.color =
                0xFFAEBDEB.toInt()

            text.textSize =
                13.0f *
                    u

            drawFitText(
                c,
                "INTEGRITY • " +
                    if (
                        integrityEnabled
                    ) {
                        "SHA-256 STORY PACKAGE"
                    } else {
                        "OFF"
                    },
                safeLeft,
                y,
                maxWidth,
                text,
                10.5f *
                    u
            )

            y +=
                18f *
                    u
        }

        val credit =
            brandConfig.creditLine()

        if (
            credit.isNotBlank()
        ) {
            text.color =
                0xFFAEB7C7.toInt()

            text.textSize =
                10.0f *
                    u

            drawFitText(
                c,
                credit,
                safeLeft,
                y,
                maxWidth,
                text,
                8.4f *
                    u
            )

            y +=
                14f *
                    u
        }

        val railEndY =
            y +
                (
                    6f *
                        u
                    )

        c.drawLine(
            safeLeft -
                (
                    8f *
                        u
                    ),
            railStartY,
            safeLeft -
                (
                    8f *
                        u
                    ),
            railEndY,
            rail
        )

        c.drawLine(
            safeLeft -
                (
                    8f *
                        u
                    ),
            railEndY,
            safeLeft +
                (
                    7f *
                        u
                    ),
            railEndY,
            rail
        )

        c.restore()
    }



    private fun drawStrongRecordedText(
        canvas: Canvas,
        value: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {
        drawReportTextBackplate(
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
                reportHudOutlineScale()

        paint.color =
            reportHudOutlineColor()

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

    private fun instrumentStateColor(): Int {
        val acc = accuracy

        return when {
            acc == null ->
                0xFFFF5A52.toInt()

            acc <= 8f ->
                0xFF83B995.toInt()

            acc <= 25f ->
                0xFFD8B85B.toInt()

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
                    0xFFFFFFFF.toInt()
                style =
                    Paint.Style.STROKE
                strokeWidth =
                    2.0f * u
                setShadowLayer(
                    1.3f * u,
                    0.4f * u,
                    0.4f * u,
                    0x88000000.toInt()
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
                    0xFFFFFFFF.toInt()
                strokeWidth =
                    1.4f * u
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
                    11.0f * u
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
                    0xFF83C7D4.toInt()
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
                    0xFFD8B85B.toInt()
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
            9.4f * u
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
                    0xFFD8B85B.toInt()

                else ->
                    0xFF83B995.toInt()
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
                    0xFF83B995.toInt()

                kotlin.math.abs(roll) <= 3f ->
                    0xFFD8B85B.toInt()

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

        refreshV228BrandUi()
        lastV233ColorMonitorKey = ""
        refreshV233ColorMonitor()

        rotationVectorSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        ambientLightSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
            !thermalListenerRegistered
        ) {
            try {
                powerManager.addThermalStatusListener(
                    thermalStatusListener
                )

                thermalStatus =
                    powerManager.currentThermalStatus

                thermalListenerRegistered =
                    true
            } catch (_: Exception) {
                thermalListenerRegistered =
                    false
            }
        }
    }

    override fun onPause() {
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {
        }

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
            thermalListenerRegistered
        ) {
            try {
                powerManager.removeThermalStatusListener(
                    thermalStatusListener
                )
            } catch (_: Exception) {
            }

            thermalListenerRegistered =
                false
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
            event ==
                null
        ) {
            return
        }

        if (
            event.sensor.type ==
                Sensor.TYPE_LIGHT
        ) {
            ambientLux =
                event.values
                    .firstOrNull()
                    ?.coerceAtLeast(
                        0f
                    )

            updateLightAdvisor()
            return
        }

        if (
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

            val previousAzimuth =
                compassAzimuthDeg
            val previousPitch =
                phonePitchDeg
            val previousRoll =
                phoneRollDeg

            val normalizedAzimuth =
                (
                    (azimuth % 360f) +
                        360f
                    ) % 360f

            compassAzimuthDeg =
                normalizedAzimuth
            phonePitchDeg =
                pitch
            phoneRollDeg =
                roll

            val now =
                SystemClock.elapsedRealtime()

            if (
                previousAzimuth !=
                    null &&
                previousPitch !=
                    null &&
                previousRoll !=
                    null &&
                lastMotionSampleMs >
                    0L
            ) {
                val dt =
                    (
                        now -
                            lastMotionSampleMs
                        ).coerceAtLeast(
                            1L
                        )

                val azRaw =
                    kotlin.math.abs(
                        normalizedAzimuth -
                            previousAzimuth
                    )

                val azDelta =
                    minOf(
                        azRaw,
                        360f -
                            azRaw
                    )

                val rollDelta =
                    kotlin.math.abs(
                        roll -
                            previousRoll
                    )

                val pitchDelta =
                    kotlin.math.abs(
                        pitch -
                            previousPitch
                    )

                val scale =
                    (
                        16.67f /
                            dt.toFloat()
                        ).coerceIn(
                            0.35f,
                            2.5f
                        )

                val rawScore =
                    (
                        (
                            rollDelta +
                                pitchDelta +
                                (
                                    azDelta *
                                        0.35f
                                    )
                            ) *
                            4.2f *
                            scale
                        ).coerceIn(
                            0f,
                            100f
                        )

                cameraShakeScore =
                    (
                        cameraShakeScore *
                            0.72f
                        ) +
                        (
                            rawScore *
                                0.28f
                            )
            }

            lastMotionSampleMs =
                now
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

        drawReportTextBackplate(
            canvas,
            textToDraw,
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

        // V192: high-contrast recorded text. A solid black outline sits
        // behind the chosen text colour so the burn-in stays readable on
        // white walls, sky, sunlight and other bright video backgrounds.
        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            paint.textSize *
                reportHudOutlineScale()

        paint.color =
            reportHudOutlineColor()

        canvas.drawText(
            textToDraw,
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
            textToDraw,
            x,
            y,
            paint
        )

        paint.style =
            savedStyle
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

        if (
            !runFieldPreflightBeforeRecording()
        ) {
            return
        }

        reportId = newReportId()
        recordStartUtc = "--"

        baseName = "DEVELOP_UGANDA_V236_${cameraExperienceId}_${reportId}_${sceneModes[sceneIndex]}_${lookModes[lookIndex]}_" +
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
                    enforceFullFramePreview()
                    enforceImmersiveCameraWindow()

                    recordingWarningsSeen.clear()
                    recStarted = System.currentTimeMillis()
                    recordStartUtc =
                        Instant.ofEpochMilli(
                            recStarted
                        ).toString()
                    markRecordingJournalStarted()
                    distanceTravelledM = 0f
                    audioAmplitude = 0.0
                    audioPeakAmplitude = 0.0
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

                    audioPeakAmplitude =
                        maxOf(
                            audioPeakAmplitude *
                                0.985,
                            audioAmplitude
                        )

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
                    audioPeakAmplitude = 0.0
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

                    markRecordingJournalFinished(
                        hadError
                    )

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
                        statusView.text = "FINALIZED • QC"
                        statusView.setTextColor(
                            0xFF83B995.toInt()
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

                        if (
                            !hadError
                        ) {
                            val packageFinishedUtc =
                                Instant.now().toString()

                            DevelopUgandaStoryPackager.createVideoPackage(
                                this@DevelopUgandaCameraActivity,
                                event.outputResults.outputUri,
                                DevelopUgandaStoryPackager.StoryMetadata(
                                    packageId = reportId.ifBlank {
                                        baseName.ifBlank {
                                            "DU_${System.currentTimeMillis()}"
                                        }
                                    },
                                    camera = cameraExperienceShortLabel(),
                                    reporter = reporterDisplayName(),
                                    storyId = storyDisplayId(),
                                    title = storyId.ifBlank {
                                        "FIELD REPORT"
                                    },
                                    place = placeName,
                                    latitude = lat,
                                    longitude = lon,
                                    gpsAccuracyM = accuracy,
                                    startedUtc = recordStartUtc,
                                    finishedUtc = packageFinishedUtc,
                                    scene = sceneModes[sceneIndex],
                                    look = lookModes[lookIndex],
                                    quality = qualityModes[qualityIndex],
                                    autoView = autoViewSummary,
                                    warnings = recordingWarningsSeen.toList(),
                                    sourceKind = "REPORT",
                                    autoTranscribe =
                                        cameraExperienceId == "V211_AUDIO" ||
                                            sceneModes[sceneIndex] == "INTERVIEW",
                                    expectSocialMaster = isSocialMediaCamera()
                                )
                            )
                        }

                        val v229ColorPackageId =
                            reportId.ifBlank {
                                baseName.ifBlank {
                                    "DU_${System.currentTimeMillis()}"
                                }
                            }

                        scheduleV233ColorMaster(
                            event.outputResults.outputUri,
                            v229ColorPackageId
                        )

                        saveV227ContinuitySnapshot()

                        val reviewPackageId =
                            reportId.ifBlank {
                                baseName.ifBlank {
                                    "DU_${System.currentTimeMillis()}"
                                }
                            }

                        DevelopUgandaClipQc.inspect(
                            this@DevelopUgandaCameraActivity,
                            event.outputResults.outputUri,
                            reviewPackageId
                        ) {
                                result ->
                            statusView.text =
                                if (
                                    result.playableFrame &&
                                    result.hasVideo &&
                                    result.sourceReadable
                                ) {
                                    "QC READY"
                                } else {
                                    "QC CHECK"
                                }

                            statusView.setTextColor(
                                if (
                                    result.playableFrame &&
                                    result.hasVideo &&
                                    result.sourceReadable
                                ) {
                                    0xFF83B995.toInt()
                                } else {
                                    0xFFD8B85B.toInt()
                                }
                            )

                            DevelopUgandaInstantReviewDialog.show(
                                this@DevelopUgandaCameraActivity,
                                result,
                                reviewPackageId,
                                isSocialMediaCamera()
                            ) {
                                matchLastShotContinuity()
                            }
                        }

                        if (
                            !hadError &&
                            isSocialMediaCamera()
                        ) {
                            exportAutomaticSocialMaster(
                                event.outputResults.outputUri,
                                reportId
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
                            if (
                                statusView.text.toString() !=
                                    "QC CHECK"
                            ) {
                                statusView.text = "STBY"
                                statusView.setTextColor(
                                    0xFFFF5A52.toInt()
                                )
                            }
                        }, 5200L)
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
                        0xFF83B995.toInt()
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
                    0xFFD8B85B.toInt(),
                lookButton to
                    0xFF83C7D4.toInt(),
                qualityButton to
                    0xFFE8F1F2.toInt(),
                captureModeButton to
                    0xFF83B995.toInt(),
                viewModeButton to
                    0xFF83B995.toInt(),
                settingsButton to
                    0xFF83C7D4.toInt(),
                guidesButton to
                    0xFFD8B85B.toInt(),
                resetButton to
                    0xFFB66B67.toInt()
            )

        values.forEach {
            pair ->
            pair.first.background =
                ColorDrawable(
                    Color.TRANSPARENT
                )

            pair.first.invalidate()
        }
    }

    private fun refreshHud() {
        refreshV233ColorMonitor()
        applyAutoDirectorIfNeeded()
        updateReportModePreviewTuning()
        applyIndependentCameraExperienceUi()
        updateHorizonGuard()
        updateMotionGuard()
        updateLightAdvisor()
        updateAudioGuard()
        updateThermalGuard()
        updateShotQualityGuard()

        timecodeView.text =
            "TC ${tc()}"

        formatView.text =
            "${verifiedCameraStateText()} • SCENE ${sceneModes[sceneIndex]} • LOOK ${lookModes[lookIndex]}"

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
                "${sceneTag()} • ${reportModePurposeLabel()} • ${lookModes[lookIndex]} • ${autoDirectorStateText()} • V237"

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
                "${recordingHealthText()} • ${motionGuardLabel()} • ${estimatedRecordingTimeText()}"

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
                "LENS ▾\n${currentLensDeckLabel()}"
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
            "SOCIAL FHD" -> "1080 SOCIAL"
            "SOCIAL 60" -> "1080 60"
            "MASTER UHD" -> "4K MASTER"
            "UHD 60" -> "4K 60"
            "MASTER HDR" -> "4K HDR"
            "SOCIAL HDR" -> "1080 HDR"
            "ACTION STAB" -> "ACTION"
            "ACTION 60" -> "ACTION 60"
            "LOW LIGHT" -> "NIGHT VIDEO"
            "FAST HD" -> "HD FAST"
            else -> "AUTO"
        }
    }

    private fun targetVideoBitrate(): Int {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "SOCIAL FHD" -> 24_000_000
            "SOCIAL 60" -> 42_000_000
            "MASTER UHD" -> 64_000_000
            "UHD 60" -> 90_000_000
            "MASTER HDR" -> 72_000_000
            "SOCIAL HDR" -> 34_000_000
            "ACTION STAB" -> 30_000_000
            "ACTION 60" -> 48_000_000
            "LOW LIGHT" -> 28_000_000
            "FAST HD" -> 12_000_000
            else -> 24_000_000
        }
    }

    private fun requestedVideoFps(): Int {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "SOCIAL 60",
            "UHD 60",
            "ACTION 60" -> 60
            "LOW LIGHT" -> 0
            else -> 30
        }
    }

    private fun wantsVideoHdr(): Boolean {
        return qualityModes[
            qualityIndex
        ] in
            setOf(
                "MASTER HDR",
                "SOCIAL HDR"
            )
    }

    private fun wantsVideoStabilization(): Boolean {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "SOCIAL FHD",
            "ACTION STAB",
            "ACTION 60",
            "LOW LIGHT" -> true
            else -> false
        }
    }

    private fun reportCameraPrefsName(): String {
        return "develop_uganda_report_camera_" +
            cameraExperienceId.lowercase(
                Locale.US
            )
    }

    private fun cameraExperienceDisplayName(): String {
        return when (cameraExperienceId) {
            "V205_FOCUS" ->
                "V205 • PEOPLE FOCUS"
            "V206_METER" ->
                "V206 • SUBJECT METERING"
            "V207_HORIZON" ->
                "V207 • BUILDINGS & LEVEL"
            "V208_STEADY" ->
                "V208 • WALK & ACTION STEADY"
            "V209_NIGHT" ->
                "V209 • NIGHT & LOW LIGHT"
            "V210_ALL_PRO" ->
                "V210 • EVERYDAY PRO"
            "V211_AUDIO" ->
                "V211 • INTERVIEW AUDIO"
            "V212_VERIFIED" ->
                "V212 • VERIFIED REPORT"
            "V213_THERMAL" ->
                "V213 • LONG RECORD SAFE"
            "V214_SIGNATURE" ->
                "V214 • CINEMATIC LOOKS"
            "V215_AUTO" ->
                "V215 • SMART AUTO"
            "V222_SOCIAL" ->
                "V222 • SOCIAL MEDIA CAM"
            else ->
                "V210 • EVERYDAY PRO"
        }
    }

    private fun cameraExperienceShortLabel(): String {
        return cameraExperienceDisplayName()
            .removeSuffix(
                " CAMERA"
            )
    }

    private fun cameraExperienceInstruction(): String {
        return when (cameraExperienceId) {
            "V205_FOCUS" ->
                "PEOPLE • PORTRAITS • INTERVIEWS • TAP AF • HOLD AF LOCK"
            "V206_METER" ->
                "BACKLIGHT • WINDOWS • FACES • HOLD AF+AE+AWB METERING"
            "V207_HORIZON" ->
                "BUILDINGS • ROOMS • HORIZONS • KEEP LEVEL GUIDE GREEN"
            "V208_STEADY" ->
                "WALKING • VEHICLES • ACTION • STABILIZATION WHEN SUPPORTED"
            "V209_NIGHT" ->
                "NIGHT • DARK INDOOR • LUX-DRIVEN LOW-LIGHT PROFILE"
            "V210_ALL_PRO" ->
                "EVERYDAY • NEWS • TRAVEL • ALL PRO TOOLS TOGETHER"
            "V211_AUDIO" ->
                "INTERVIEW • SPEECH • EVENTS • WATCH MIC GOOD/HOT/CLIP RISK"
            "V212_VERIFIED" ->
                "EVIDENCE • SITE REPORTS • INCIDENTS • TELEMETRY + INTEGRITY"
            "V213_THERMAL" ->
                "LONG RECORDINGS • HOT CONDITIONS • THERMAL SAFE FALLBACK"
            "V214_SIGNATURE" ->
                "CINEMATIC • PEOPLE • TRAVEL • HDR/WARM WITH DEVICE FALLBACK"
            "V215_AUTO" ->
                "QUICK SHOOTING • AUTO CHOOSES LOW-LIGHT/ACTION/60/BALANCED"
            "V222_SOCIAL" ->
                "RECORD → AUTO OPTIMIZE → SM POSTS • 9:16 • SOCIAL FHD • ORIGINAL + SOCIAL COPY"
            else ->
                "ALL PRO CAMERA TOOLS"
        }
    }


    private fun cameraExperienceBestFor(): String {
        return when (cameraExperienceId) {
            "V205_FOCUS" ->
                "PEOPLE / PORTRAITS / INTERVIEWS"
            "V206_METER" ->
                "BACKLIT FACES / WINDOWS / MIXED LIGHT"
            "V207_HORIZON" ->
                "BUILDINGS / ROOMS / LANDSCAPES"
            "V208_STEADY" ->
                "WALKING / VEHICLES / ACTION"
            "V209_NIGHT" ->
                "NIGHT / DARK INDOOR / STREETS"
            "V210_ALL_PRO" ->
                "EVERYDAY / NEWS / TRAVEL"
            "V211_AUDIO" ->
                "INTERVIEWS / SPEECH / EVENTS"
            "V212_VERIFIED" ->
                "SITE REPORTS / INCIDENTS / EVIDENCE"
            "V213_THERMAL" ->
                "LONG RECORDINGS / HOT CONDITIONS"
            "V214_SIGNATURE" ->
                "CINEMATIC / PEOPLE / TRAVEL"
            "V215_AUTO" ->
                "QUICK SHOOTING / WHEN UNSURE"
            "V222_SOCIAL" ->
                "TIKTOK / REELS / SHORTS / SOCIAL POSTS"
            else ->
                "EVERYDAY PROFESSIONAL CAPTURE"
        }
    }

    private fun cameraExperienceAccentColor(): Int {
        return when (cameraExperienceId) {
            "V205_FOCUS" -> 0xFFAEBDEB.toInt()
            "V206_METER" -> 0xFFD0B06F.toInt()
            "V207_HORIZON" -> 0xFF91B6A0.toInt()
            "V208_STEADY" -> 0xFF71B9A7.toInt()
            "V209_NIGHT" -> 0xFF8A86B8.toInt()
            "V210_ALL_PRO" -> 0xFF8FA8E8.toInt()
            "V211_AUDIO" -> 0xFF83B995.toInt()
            "V212_VERIFIED" -> 0xFF73B7D9.toInt()
            "V213_THERMAL" -> 0xFFC76D73.toInt()
            "V214_SIGNATURE" -> 0xFFA793D8.toInt()
            "V215_AUTO" -> 0xFF73B7D9.toInt()
            "V222_SOCIAL" -> 0xFF62D8C9.toInt()
            else -> 0xFFAEBDEB.toInt()
        }
    }

    private fun applyIndependentCameraDefaultsIfNeeded() {
        val prefs =
            getSharedPreferences(
                reportCameraPrefsName(),
                Context.MODE_PRIVATE
            )

        if (
            prefs.getBoolean(
                "experience_initialized",
                false
            )
        ) {
            return
        }

        fun quality(value: String) {
            val index = qualityModes.indexOf(value)
            if (index >= 0) qualityIndex = index
        }

        fun look(value: String) {
            val index = lookModes.indexOf(value)
            if (index >= 0) lookIndex = index
        }

        fun scene(value: String) {
            val index = sceneModes.indexOf(value)
            if (index >= 0) sceneIndex = index
        }

        when (cameraExperienceId) {
            "V205_FOCUS" -> {
                quality("SOCIAL FHD")
                scene("INTERVIEW")
                look("CLEAN")
            }
            "V206_METER" -> {
                quality("SOCIAL FHD")
                scene("REPORTER")
                look("NATURAL")
            }
            "V207_HORIZON" -> {
                quality("SOCIAL FHD")
                scene("OUTDOOR")
                look("NATURAL")
            }
            "V208_STEADY" -> {
                quality("ACTION STAB")
                scene("DOCUMENTARY")
                look("CLEAN")
            }
            "V209_NIGHT" -> {
                quality("LOW LIGHT")
                scene("NIGHT")
                look("NIGHT")
            }
            "V210_ALL_PRO" -> {
                quality("SOCIAL FHD")
                scene("REPORTER")
                look("CLEAN")
            }
            "V211_AUDIO" -> {
                quality("SOCIAL FHD")
                scene("INTERVIEW")
                look("CLEAN")
            }
            "V212_VERIFIED" -> {
                quality("SOCIAL FHD")
                scene("NEWS")
                look("NATURAL")
            }
            "V213_THERMAL" -> {
                quality("SOCIAL FHD")
                scene("REPORTER")
                look("CLEAN")
            }
            "V214_SIGNATURE" -> {
                quality("SOCIAL HDR")
                scene("CINEMA")
                look("WARM")
            }
            "V215_AUTO" -> {
                quality("SOCIAL FHD")
                scene("REPORTER")
                look("CLEAN")
                autoDirectorEnabled = true
            }
            "V222_SOCIAL" -> {
                quality("SOCIAL FHD")
                scene("REPORTER")
                look("CLEAN")
                reportHudSizeIndex = 0
                reportHudContrastIndex = 1
                reportHudBackingIndex = 1
                reportDisplayMode = "SOCIAL POST"
                autoDirectorEnabled = false
            }
        }

        prefs.edit()
            .putBoolean(
                "experience_initialized",
                true
            )
            .apply()

        saveReportCameraPreferences()
    }

    private fun applyIndependentCameraExperienceUi() {
        if (::cameraExperienceBannerView.isInitialized) {
            cameraExperienceBannerView.text =
                "${cameraExperienceDisplayName()}\nBEST FOR • ${cameraExperienceBestFor()}\n${cameraExperienceInstruction()}"
            cameraExperienceBannerView.setTextColor(
                cameraExperienceAccentColor()
            )
        }

        val mutedAlpha = 0.36f

        if (::horizonGuardView.isInitialized) {
            horizonGuardView.alpha =
                if (
                    cameraExperienceId in setOf(
                        "V207_HORIZON",
                        "V210_ALL_PRO",
                        "V212_VERIFIED",
                        "V215_AUTO"
                    )
                ) 1f else mutedAlpha
        }

        if (::motionGuardView.isInitialized) {
            motionGuardView.alpha =
                if (
                    cameraExperienceId in setOf(
                        "V208_STEADY",
                        "V210_ALL_PRO",
                        "V212_VERIFIED",
                        "V215_AUTO"
                    )
                ) 1f else mutedAlpha
        }

        if (::lightAdvisorView.isInitialized) {
            lightAdvisorView.alpha =
                if (
                    cameraExperienceId in setOf(
                        "V209_NIGHT",
                        "V210_ALL_PRO",
                        "V212_VERIFIED",
                        "V215_AUTO"
                    )
                ) 1f else mutedAlpha
        }

        if (::audioGuardView.isInitialized) {
            audioGuardView.alpha =
                if (
                    cameraExperienceId in setOf(
                        "V211_AUDIO",
                        "V210_ALL_PRO",
                        "V212_VERIFIED"
                    )
                ) 1f else mutedAlpha
        }

        if (::thermalGuardView.isInitialized) {
            thermalGuardView.alpha =
                if (
                    cameraExperienceId in setOf(
                        "V213_THERMAL",
                        "V210_ALL_PRO",
                        "V212_VERIFIED",
                        "V215_AUTO"
                    )
                ) 1f else mutedAlpha
        }

        if (::autoDirectorButton.isInitialized) {
            autoDirectorButton.alpha =
                if (
                    cameraExperienceId ==
                        "V215_AUTO"
                ) 1f else 0.64f
        }
    }

    private fun autoDirectorSuggestedMode(): Pair<String, String> {
        if (
            isThermalSevereOrWorse()
        ) {
            return Pair(
                "SOCIAL FHD",
                "THERMAL SAFE"
            )
        }

        val lux =
            ambientLux

        if (
            lux != null &&
            lux < 25f
        ) {
            return Pair(
                "LOW LIGHT",
                "DARK SCENE"
            )
        }

        if (
            cameraShakeScore > 22f &&
            (
                lux == null ||
                lux >= 100f
            )
        ) {
            return Pair(
                "ACTION STAB",
                "HANDHELD MOTION"
            )
        }

        if (
            lux != null &&
            lux >= 500f &&
            cameraShakeScore <= 8f
        ) {
            return Pair(
                "SOCIAL 60",
                "BRIGHT + STEADY"
            )
        }

        return Pair(
            "SOCIAL FHD",
            "BALANCED"
        )
    }

    private fun autoDirectorStateText(): String {
        if (!autoDirectorEnabled) {
            return "AUTO MANUAL"
        }

        val suggestion =
            autoDirectorSuggestedMode()

        return "AUTO ${suggestion.first} • ${suggestion.second}"
    }

    private fun toggleAutoDirector() {
        if (
            recording != null
        ) {
            toast(
                "Stop recording before changing Auto Director"
            )
            return
        }

        autoDirectorEnabled =
            !autoDirectorEnabled

        autoDirectorButton.text =
            "AUTO DIRECTOR ▾\n" +
                if (
                    autoDirectorEnabled
                ) {
                    "ON"
                } else {
                    "OFF"
                }

        autoDirectorButton.isSelected =
            autoDirectorEnabled

        autoDirectorReason =
            if (
                autoDirectorEnabled
            ) {
                autoDirectorSuggestedMode().second
            } else {
                "MANUAL"
            }

        saveReportCameraPreferences()

        toast(
            if (
                autoDirectorEnabled
            ) {
                "AUTO DIRECTOR ON • ${autoDirectorStateText()}"
            } else {
                "AUTO DIRECTOR OFF • manual camera mode"
            }
        )

        if (
            autoDirectorEnabled
        ) {
            applyAutoDirectorIfNeeded(
                force = true
            )
        }
    }

    private fun applyAutoDirectorIfNeeded(
        force: Boolean = false
    ) {
        if (
            !autoDirectorEnabled ||
            recording != null
        ) {
            return
        }

        val (targetMode, reason) =
            autoDirectorSuggestedMode()

        autoDirectorReason =
            reason

        val now =
            SystemClock.elapsedRealtime()

        if (
            !force &&
            now - autoDirectorLastSwitchMs < 3500L
        ) {
            return
        }

        val targetIndex =
            qualityModes.indexOf(
                targetMode
            )

        if (
            targetIndex < 0 ||
            targetIndex == qualityIndex
        ) {
            return
        }

        qualityIndex =
            targetIndex

        autoDirectorLastSwitchMs =
            now

        if (
            ::qualityButton.isInitialized
        ) {
            qualityButton.text =
                "FORMAT ▾\n${qualityDeckLabel()}"
        }

        saveReportCameraPreferences()

        toast(
            "AUTO DIRECTOR • $targetMode • $reason"
        )

        bindCamera()
    }

    private fun previewLookTintColor(): Int {
        // Very low alpha on purpose: this is an operator preview cue.
        // The recorded creative look is still produced by drawCreativeLook().
        return when (
            lookModes[
                lookIndex
            ]
        ) {
            "WARM" ->
                0x10FF8A3D.toInt()

            "COOL" ->
                0x10007AFF.toInt()

            "TEAL" ->
                0x1000A7A0.toInt()

            "GOLD" ->
                0x10D6A83A.toInt()

            "SOFT" ->
                0x0CF0D8D0.toInt()

            "SUNSET" ->
                0x10E06A46.toInt()

            "BLUE HOUR" ->
                0x102F5FA8.toInt()

            "NIGHT" ->
                0x12173363.toInt()

            "MONO" ->
                0x0D707070.toInt()

            "NATURAL" ->
                0x0600A070.toInt()

            else ->
                Color.TRANSPARENT
        }
    }

    private fun reportModeAccentColor(): Int {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "SOCIAL FHD" ->
                0xFF8FA8E8.toInt()

            "SOCIAL 60" ->
                0xFF73B7D9.toInt()

            "SOCIAL HDR" ->
                0xFFA793D8.toInt()

            "MASTER UHD" ->
                0xFFAEBDEB.toInt()

            "UHD 60" ->
                0xFF7FB8CA.toInt()

            "MASTER HDR" ->
                0xFFD0B06F.toInt()

            "ACTION STAB" ->
                0xFF91B6A0.toInt()

            "ACTION 60" ->
                0xFF71B9A7.toInt()

            "LOW LIGHT" ->
                0xFF8A86B8.toInt()

            "FAST HD" ->
                0xFFAEB7C7.toInt()

            else ->
                0xFFAEBDEB.toInt()
        }
    }

    private fun reportModePurposeLabel(): String {
        return when (
            qualityModes[
                qualityIndex
            ]
        ) {
            "SOCIAL FHD" ->
                "SOCIAL MASTER"

            "SOCIAL 60" ->
                "SMOOTH SOCIAL"

            "SOCIAL HDR" ->
                "SOCIAL HDR"

            "MASTER UHD" ->
                "4K MASTER"

            "UHD 60" ->
                "4K MOTION"

            "MASTER HDR" ->
                "HDR MASTER"

            "ACTION STAB" ->
                "STABLE ACTION"

            "ACTION 60" ->
                "FAST ACTION"

            "LOW LIGHT" ->
                "LOW LIGHT"

            "FAST HD" ->
                "FAST DELIVERY"

            else ->
                "CAMERA MODE"
        }
    }

    private fun updateReportModePreviewTuning() {
        if (
            ::previewModeToneView.isInitialized
        ) {
            previewModeToneView.setBackgroundColor(
                previewLookTintColor()
            )
        }

        if (
            ::previewTagView.isInitialized
        ) {
            previewTagView.setTextColor(
                reportModeAccentColor()
            )

            previewTagView.text =
                "${sceneTag()} • ${reportModePurposeLabel()} • ${lookModes[lookIndex]} • V221"
        }
    }

    private fun thermalStateLabel(): String {
        if (
            Build.VERSION.SDK_INT <
                Build.VERSION_CODES.Q
        ) {
            return "UNAVAILABLE"
        }

        return when (
            thermalStatus
        ) {
            PowerManager.THERMAL_STATUS_NONE ->
                "NORMAL"

            PowerManager.THERMAL_STATUS_LIGHT ->
                "LIGHT"

            PowerManager.THERMAL_STATUS_MODERATE ->
                "MODERATE"

            PowerManager.THERMAL_STATUS_SEVERE ->
                "SEVERE"

            PowerManager.THERMAL_STATUS_CRITICAL ->
                "CRITICAL"

            PowerManager.THERMAL_STATUS_EMERGENCY ->
                "EMERGENCY"

            PowerManager.THERMAL_STATUS_SHUTDOWN ->
                "SHUTDOWN"

            else ->
                "UNKNOWN"
        }
    }

    private fun isThermalSevereOrWorse(): Boolean {
        return (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q &&
            thermalStatus >=
                PowerManager.THERMAL_STATUS_SEVERE
            )
    }

    private fun updateThermalGuard() {
        if (
            !::thermalGuardView.isInitialized
        ) {
            return
        }

        if (
            operatorControlsHidden ||
            cleanModeEnabled
        ) {
            thermalGuardView.visibility =
                View.GONE

            return
        }

        thermalGuardView.visibility =
            View.VISIBLE

        val label =
            thermalStateLabel()

        thermalGuardView.text =
            "THERMAL • $label"

        thermalGuardView.setTextColor(
            when (label) {
                "NORMAL",
                "LIGHT" ->
                    0xFF91B6A0.toInt()

                "MODERATE" ->
                    0xFFAEBDEB.toInt()

                "SEVERE",
                "CRITICAL",
                "EMERGENCY",
                "SHUTDOWN" ->
                    0xFFC76D73.toInt()

                else ->
                    0xFFAEB7C7.toInt()
            }
        )
    }

    private fun applyThermalSafeProfileIfNeeded() {
        if (
            !isThermalSevereOrWorse() ||
            recording !=
                null
        ) {
            return
        }

        val current =
            qualityModes[
                qualityIndex
            ]

        val highDemand =
            current in
                setOf(
                    "MASTER UHD",
                    "UHD 60",
                    "MASTER HDR",
                    "SOCIAL HDR",
                    "ACTION 60"
                )

        if (!highDemand) {
            return
        }

        val safeIndex =
            qualityModes.indexOf(
                "SOCIAL FHD"
            )

        if (
            safeIndex >=
                0 &&
            safeIndex !=
                qualityIndex
        ) {
            qualityIndex =
                safeIndex

            if (
                ::qualityButton.isInitialized
            ) {
                qualityButton.text =
                    "FORMAT ▾\n${qualityDeckLabel()}"
            }

            toast(
                "THERMAL ${thermalStateLabel()} • switched to SOCIAL FHD"
            )
        }
    }

    private fun verifiedCameraStateText(): String {
        val rollText =
            phoneRollDeg?.let {
                String.format(
                    Locale.US,
                    "%+.1f°",
                    it
                )
            } ?: "--"

        val luxText =
            ambientLux?.let {
                String.format(
                    Locale.US,
                    "%.0fLUX",
                    it
                )
            } ?: "--"

        return buildString {
            append("V216 VERIFIED")
            append(" • ")
            append(qualityDeckLabel())
            append(" • ")
            append(
                reportModePurposeLabel()
            )
            append(" • LOOK ")
            append(
                lookModes[
                    lookIndex
                ]
            )
            append(" • ")
            append(activeVideoFpsLabel)
            append(" • ")
            append(activeVideoStabilizationLabel)
            append(" • ")
            append(activeVideoDynamicRangeLabel)
            append(" • ")
            append(focusAssistLabel())
            append(" • H ")
            append(rollText)
            append(" • ")
            append(motionGuardLabel())
            append(" • ")
            append(ambientLightLabel())
            append(" ")
            append(luxText)
            append(" • AUDIO ")
            append(audioGuardLabel())
            append(" • THERMAL ")
            append(
                thermalStateLabel()
            )
            append(" • ")
            append(
                autoDirectorStateText()
            )
            append(" • CAMERA ")
            append(
                cameraExperienceShortLabel()
            )
        }
    }

    private fun socialCameraStatus(): String {
        return buildString {
            append("CREATOR ENGINE • ")
            append(qualityDeckLabel())
            append(" • ")
            append(activeVideoFpsLabel)
            append(" • ")
            append(activeVideoStabilizationLabel)
            append(" • ")
            append(activeVideoDynamicRangeLabel)
            append(" • ")
            append(activeVideoAspectLabel)
            append(" • ")
            append(targetVideoBitrate() / 1_000_000)
            append("Mbps TARGET")
            append(" • TAP AF")
            append(" • HOLD AF+AE+AWB METER")
            append(" • HORIZON GUARD")
            append(" • STEADYSHOT ")
            append(
                motionGuardLabel()
            )
            append(" • ")
            append(
                ambientLightRecommendation()
            )
            append(" • AUDIO ")
            append(
                audioGuardLabel()
            )
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
                "MASTER UHD",
                "UHD 60",
                "MASTER HDR" ->
                    listOf(
                        Quality.UHD,
                        Quality.FHD,
                        Quality.HD
                    )

                "SOCIAL HDR" ->
                    listOf(
                        Quality.FHD,
                        Quality.UHD,
                        Quality.HD
                    )

                "FAST HD" ->
                    listOf(
                        Quality.HD,
                        Quality.FHD,
                        Quality.UHD
                    )

                else ->
                    listOf(
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

                "SOFT" ->
                    0x08FFFFFF

                "SUNSET" ->
                    0x10FF7040

                "BLUE HOUR" ->
                    0x102D62C7

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


            val focusFuture =
                cam.cameraControl
                    .startFocusAndMetering(
                        action
                    )

            focusAttempted =
                true

            focusSuccessful =
                null

            focusFuture.addListener(
                {
                    focusSuccessful =
                        try {
                            focusFuture.get()
                                .isFocusSuccessful
                        } catch (_: Exception) {
                            false
                        }

                    runOnUiThread {
                        updateShotQualityGuard()

                        toast(
                            if (
                                focusSuccessful ==
                                    true
                            ) {
                                "Focus confirmed"
                            } else {
                                "Subject focus not confirmed"
                            }
                        )
                    }
                },
                ContextCompat.getMainExecutor(
                    this
                )
            )
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
                            openV227CameraHealth()

                        ACTION_CLEAN ->
                            showReportCleanDropdown(
                                v ?: cleanModeButton
                            )

                        ACTION_HUD_SIZE ->
                            showReportHudSizeDropdown(
                                v ?: hudSizeButton
                            )

                        ACTION_HUD_CONTRAST ->
                            showReportHudContrastDropdown(
                                v ?: hudContrastButton
                            )

                        ACTION_HUD_BACKING ->
                            showReportHudBackingDropdown(
                                v ?: hudBackingButton
                            )

                        ACTION_REPORT_PRESET ->
                            showReportPresetDropdown(
                                v ?: reportPresetButton
                            )

                        ACTION_AUTO_DIRECTOR ->
                            toggleAutoDirector()

                        ACTION_SHOT_ASSIST ->
                            cycleShotAssist()

                        ACTION_DIRECTOR ->
                            toggleDirectorGuidance()

                        ACTION_CONTINUITY ->
                            matchLastShotContinuity()

                        ACTION_HEALTH ->
                            openV227CameraHealth()

                        ACTION_BRAND_METADATA ->
                            openV228BrandMetadataStudio()

                        ACTION_COLOR_ENGINE ->
                            showV233ColorDropdown(
                                colorButton
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
                2.0f,
                0.7f,
                0.7f,
                0xA6000000.toInt()
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
            textSize = 5.7f
            isAllCaps = false
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            minHeight = dp(34)

            setPadding(
                dp(5),
                0,
                dp(5),
                0
            )

            // V191: the custom button itself draws the thick bright ring.
            // It becomes fully filled only when isSelected == true.
            background =
                ColorDrawable(
                    Color.TRANSPARENT
                )

            stateListAnimator =
                null
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
        // V193: same family as the Home/Control Room, but deliberately
        // softened so the camera deck does not look neon.
        val colors =
            intArrayOf(
                0xFFD9DEE8.toInt(), // soft white
                0xFFAEBDEB.toInt(), // lavender
                0xFF9CAEC5.toInt(), // blue grey
                0xFFB5BECC.toInt(), // silver
                0xFFA9A1BF.toInt(), // muted violet
                0xFF9EB4B7.toInt(), // muted teal
                0xFFB8B3AA.toInt(), // warm grey
                0xFFD9DEE8.toInt()  // soft white
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

            markReportPresetCustom()
            applyScenePreset()
            saveReportCameraPreferences()
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
            markReportPresetCustom()
            saveReportCameraPreferences()
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
            markReportPresetCustom()
            saveReportCameraPreferences()
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
            markReportPresetCustom()
            saveReportCameraPreferences()
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

    private fun showReportHudSizeDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "RECORDED HUD SIZE",
            reportHudLabels,
            reportHudSizeIndex
        ) { picked ->
            reportHudSizeIndex =
                picked

            markReportPresetCustom()

            hudSizeButton.text =
                "HUD SIZE ▾\n${reportHudLabels[reportHudSizeIndex]}"

            hudSizeButton.isSelected =
                true

            applyAdaptiveReportPreviewTypography()
            saveReportCameraPreferences()
            refreshHud()

            toast(
                "HUD ${reportHudLabels[reportHudSizeIndex]}"
            )
        }
    }

    private fun showReportHudBackingDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "RECORDED HUD BACKING",
            reportHudBackingLabels,
            reportHudBackingIndex
        ) { picked ->
            reportHudBackingIndex =
                picked

            markReportPresetCustom()

            hudBackingButton.text =
                "HUD BACKING ▾\n${reportHudBackingLabels[reportHudBackingIndex]}"

            hudBackingButton.isSelected =
                reportHudBackingIndex !=
                    0

            saveReportCameraPreferences()
            refreshHud()

            toast(
                "HUD backing ${reportHudBackingLabels[reportHudBackingIndex]}"
            )
        }
    }

    private fun markReportPresetCustom() {
        if (
            reportPresetIndex !=
            0
        ) {
            reportPresetIndex =
                0

            if (
                ::reportPresetButton.isInitialized
            ) {
                reportPresetButton.text =
                    "PRESET ▾\nCUSTOM"

                reportPresetButton.isSelected =
                    false
            }
        }
    }

    private fun reportIndexOf(
        values: List<String>,
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

    private fun applyReportPreset(
        picked: Int
    ) {
        reportPresetIndex =
            picked.coerceIn(
                0,
                reportPresetLabels.lastIndex
            )

        when (
            reportPresetLabels[
                reportPresetIndex
            ]
        ) {
            "FIELD" -> {
                sceneIndex =
                    reportIndexOf(
                        sceneModes,
                        "REPORTER"
                    )

                lookIndex =
                    reportIndexOf(
                        lookModes,
                        "CLEAN"
                    )

                qualityIndex =
                    reportIndexOf(
                        qualityModes,
                        "SOCIAL FHD"
                    )

                captureModeIndex =
                    reportIndexOf(
                        captureModes,
                        "VIDEO"
                    )

                reportHudSizeIndex =
                    1

                reportHudContrastIndex =
                    1

                previewGuidesEnabled =
                    true

                integrityEnabled =
                    true

                reportHudBackingIndex =
                    1
            }

            "OUTDOOR" -> {
                sceneIndex =
                    reportIndexOf(
                        sceneModes,
                        "OUTDOOR"
                    )

                lookIndex =
                    reportIndexOf(
                        lookModes,
                        "NATURAL"
                    )

                qualityIndex =
                    reportIndexOf(
                        qualityModes,
                        "SOCIAL FHD"
                    )

                captureModeIndex =
                    reportIndexOf(
                        captureModes,
                        "VIDEO"
                    )

                reportHudSizeIndex =
                    1

                reportHudContrastIndex =
                    2

                previewGuidesEnabled =
                    true

                integrityEnabled =
                    true

                reportHudBackingIndex =
                    1
            }

            "NIGHT" -> {
                sceneIndex =
                    reportIndexOf(
                        sceneModes,
                        "NIGHT"
                    )

                lookIndex =
                    reportIndexOf(
                        lookModes,
                        "NIGHT"
                    )

                qualityIndex =
                    reportIndexOf(
                        qualityModes,
                        "SOCIAL FHD"
                    )

                captureModeIndex =
                    reportIndexOf(
                        captureModes,
                        "VIDEO"
                    )

                reportHudSizeIndex =
                    1

                reportHudContrastIndex =
                    2

                previewGuidesEnabled =
                    true

                integrityEnabled =
                    true

                reportHudBackingIndex =
                    1
            }

            "INTERVIEW" -> {
                sceneIndex =
                    reportIndexOf(
                        sceneModes,
                        "INTERVIEW"
                    )

                lookIndex =
                    reportIndexOf(
                        lookModes,
                        "NATURAL"
                    )

                qualityIndex =
                    reportIndexOf(
                        qualityModes,
                        "SOCIAL FHD"
                    )

                captureModeIndex =
                    reportIndexOf(
                        captureModes,
                        "VIDEO"
                    )

                reportHudSizeIndex =
                    0

                reportHudContrastIndex =
                    1

                previewGuidesEnabled =
                    true

                integrityEnabled =
                    true

                reportHudBackingIndex =
                    1
            }

            "CINEMA" -> {
                sceneIndex =
                    reportIndexOf(
                        sceneModes,
                        "CINEMA"
                    )

                lookIndex =
                    reportIndexOf(
                        lookModes,
                        "TEAL"
                    )

                qualityIndex =
                    reportIndexOf(
                        qualityModes,
                        "MASTER UHD"
                    )

                captureModeIndex =
                    reportIndexOf(
                        captureModes,
                        "VIDEO"
                    )

                reportHudSizeIndex =
                    0

                reportHudContrastIndex =
                    0

                previewGuidesEnabled =
                    true

                integrityEnabled =
                    false
            }

            else -> {
                // CUSTOM keeps the current manual values.

                reportHudBackingIndex =
                    1
            }
        }

        reportPresetButton.text =
            "PRESET ▾\n${reportPresetLabels[reportPresetIndex]}"

        reportPresetButton.isSelected =
            reportPresetIndex !=
                0

        hudSizeButton.text =
            "HUD SIZE ▾\n${reportHudLabels[reportHudSizeIndex]}"

        hudContrastButton.text =
            "HUD CONTRAST ▾\n${reportHudContrastLabels[reportHudContrastIndex]}"

        hudBackingButton.text =
            "HUD BACKING ▾\n${reportHudBackingLabels[reportHudBackingIndex]}"

        hudBackingButton.isSelected =
            reportHudBackingIndex !=
                0

        guidesButton.text =
            "GUIDES ▾\n" +
                if (
                    previewGuidesEnabled
                ) {
                    "ON"
                } else {
                    "OFF"
                }

        integrityButton.text =
            "VERIFY ▾\n" +
                if (
                    integrityEnabled
                ) {
                    "SHA-256"
                } else {
                    "OFF"
                }

        sceneButton.text =
            "SCENE ▾\n${sceneModes[sceneIndex]}"

        lookButton.text =
            "LOOK ▾\n${lookModes[lookIndex]}"

        qualityButton.text =
            "FORMAT ▾\n${qualityDeckLabel()}"

        captureModeButton.text =
            "CAPTURE ▾\n${captureModes[captureModeIndex]}"

        applyScenePreset()
        applyAdaptiveReportPreviewTypography()
        saveReportCameraPreferences()
        refreshHud()

        if (
            recording ==
            null
        ) {
            bindCamera()
        }

        toast(
            "Preset ${reportPresetLabels[reportPresetIndex]}"
        )
    }

    private fun showReportPresetDropdown(
        anchor: View
    ) {
        if (
            recording !=
            null
        ) {
            toast(
                "Stop recording before changing preset"
            )
            return
        }

        showReportPillDropdown(
            anchor,
            "REPORT PRESET",
            reportPresetLabels,
            reportPresetIndex
        ) { picked ->
            applyReportPreset(
                picked
            )
        }
    }

    private fun showReportHudContrastDropdown(
        anchor: View
    ) {
        showReportPillDropdown(
            anchor,
            "RECORDED HUD CONTRAST",
            reportHudContrastLabels,
            reportHudContrastIndex
        ) { picked ->
            reportHudContrastIndex =
                picked

            markReportPresetCustom()

            hudContrastButton.text =
                "HUD CONTRAST ▾\n${reportHudContrastLabels[reportHudContrastIndex]}"

            hudContrastButton.isSelected =
                true

            saveReportCameraPreferences()
            refreshHud()

            toast(
                "HUD contrast ${reportHudContrastLabels[reportHudContrastIndex]}"
            )
        }
    }

    private fun showReportLensDropdown(
        anchor: View
    ) {
        if (
            recording !=
                null
        ) {
            toast(
                "Stop recording before changing lens"
            )
            return
        }

        val selected =
            when {
                selectedCameraDeviceId !=
                    null ->
                        2

                useFront ->
                    1

                else ->
                    0
            }

        showReportPillDropdown(
            anchor,
            "REAL LENS INTELLIGENCE",
            arrayOf(
                "AUTO BACK",
                "AUTO FRONT",
                "REAL CAMERAS"
            ),
            selected
        ) {
                picked ->
            when (
                picked
            ) {
                0 -> {
                    selectedCameraDeviceId =
                        null

                    useFront =
                        false

                    saveReportCameraPreferences()
                    bindCamera()
                }

                1 -> {
                    selectedCameraDeviceId =
                        null

                    useFront =
                        true

                    saveReportCameraPreferences()
                    bindCamera()
                }

                else ->
                    showRealCameraDevicePicker()
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
                        0xFFB66B67.toInt()
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

        private val idleFillPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    0x52000000
            }

        private val pressedFillPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    0x4FAEBDEB
            }

        private val selectedFillPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                style =
                    Paint.Style.FILL

                color =
                    0xFFAEBDEB.toInt()
            }

        private val glowPaint =
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

            // Dark/transparent centre until a real selection is active.
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
                        selectedFillPaint

                    isPressed ->
                        pressedFillPaint

                    else ->
                        idleFillPaint
                }
            )

            // Thick bright coloured ring is always visible.
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

            // Pressing gives a glow, but does not count as a full selection.
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
                    glowPaint
                )
            }

            super.onDraw(
                canvas
            )
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

        if (
            ::autoViewLabeler.isInitialized
        ) {
            try {
                autoViewLabeler.close()
            } catch (_: Exception) {
            }
        }

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
