package com.sentongoharuna.pulse

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * V239 Professional Camera HUD.
 *
 * Screen-only operator layer inspired by dedicated cinema-camera ergonomics:
 * - V237 corner logic is retained as LUT on the upper left + FORMAT on upper right.
 * - Large center timecode and honest technical status row.
 * - Old reporter/ID strip and stacked legacy controls are removed from the shooting view.
 * - Real preview histogram, real audio level meter and a touchable zoom ruler.
 * - Focus lock, operator lock, auto reset and settings remain one tap away.
 *
 * The CameraX recording path, V235 LUT engine and V236 GNSS/format engine are not changed.
 */
object DevelopUgandaProCameraHud {

    private const val TAG = "develop_uganda_v239_pro_camera_hud"
    private const val FIELD_CHIP_TAG = "develop_uganda_v236_field_intelligence"
    private const val GRADE_CHIP_TAG = "develop_uganda_v235_live_grade_panel"
    private const val OLD_READY_TAG = "v239_v238_ready_chip"

    fun attach(
        activity: AppCompatActivity,
        root: FrameLayout,
        previewView: PreviewView,
        tcProvider: () -> String,
        formatProvider: () -> String,
        qualityProvider: () -> String,
        fpsProvider: () -> String,
        lensProvider: () -> String,
        exposureProvider: () -> Int,
        zoomProvider: () -> Float,
        minZoomProvider: () -> Float,
        maxZoomProvider: () -> Float,
        audioProvider: () -> Pair<Double, Double>,
        focusProvider: () -> String,
        lockProvider: () -> Boolean,
        onFocusToggle: () -> Unit,
        onLockToggle: () -> Unit,
        onAutoReset: () -> Unit,
        onSettings: () -> Unit,
        onZoomRatio: (Float) -> Unit
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

        Controller(
            activity = activity,
            root = root,
            previewView = previewView,
            tcProvider = tcProvider,
            formatProvider = formatProvider,
            qualityProvider = qualityProvider,
            fpsProvider = fpsProvider,
            lensProvider = lensProvider,
            exposureProvider = exposureProvider,
            zoomProvider = zoomProvider,
            minZoomProvider = minZoomProvider,
            maxZoomProvider = maxZoomProvider,
            audioProvider = audioProvider,
            focusProvider = focusProvider,
            lockProvider = lockProvider,
            onFocusToggle = onFocusToggle,
            onLockToggle = onLockToggle,
            onAutoReset = onAutoReset,
            onSettings = onSettings,
            onZoomRatio = onZoomRatio
        ).attach()
    }

    private class Controller(
        private val activity: AppCompatActivity,
        private val root: FrameLayout,
        private val previewView: PreviewView,
        private val tcProvider: () -> String,
        private val formatProvider: () -> String,
        private val qualityProvider: () -> String,
        private val fpsProvider: () -> String,
        private val lensProvider: () -> String,
        private val exposureProvider: () -> Int,
        private val zoomProvider: () -> Float,
        private val minZoomProvider: () -> Float,
        private val maxZoomProvider: () -> Float,
        private val audioProvider: () -> Pair<Double, Double>,
        private val focusProvider: () -> String,
        private val lockProvider: () -> Boolean,
        private val onFocusToggle: () -> Unit,
        private val onLockToggle: () -> Unit,
        private val onAutoReset: () -> Unit,
        private val onSettings: () -> Unit,
        private val onZoomRatio: (Float) -> Unit
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private var detailVisible = true
        private var lastHistogramMs = 0L

        private lateinit var lutButton: Button
        private lateinit var formatButton: Button
        private lateinit var timecodeView: TextView
        private lateinit var formatBadge: TextView
        private lateinit var readyChip: Button
        private lateinit var technicalRow: LinearLayout
        private lateinit var lensValue: TextView
        private lateinit var fpsValue: TextView
        private lateinit var shutterValue: TextView
        private lateinit var irisValue: TextView
        private lateinit var isoValue: TextView
        private lateinit var wbValue: TextView
        private lateinit var expValue: TextView
        private lateinit var focusButton: Button
        private lateinit var autoButton: Button
        private lateinit var lockButton: Button
        private lateinit var settingsButton: Button
        private lateinit var histogram: HistogramView
        private lateinit var audioMeter: AudioMeterView
        private lateinit var zoomRuler: ZoomRulerView

        private val tick = object : Runnable {
            override fun run() {
                forceCleanLegacyLayout()
                updateLabels()
                updateMeters()
                handler.postDelayed(this, 250L)
            }
        }

        fun attach() {
            val marker = View(activity).apply {
                tag = TAG
                visibility = View.GONE
            }
            root.addView(marker, FrameLayout.LayoutParams(1, 1))

            marker.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) = Unit
                    override fun onViewDetachedFromWindow(v: View) {
                        handler.removeCallbacksAndMessages(null)
                    }
                }
            )

            buildTopControls()
            buildMeters()
            buildQuickControls()
            forceCleanLegacyLayout()
            updateLabels()
            handler.post(tick)
        }

        private fun buildTopControls() {
            lutButton = proButton("LUT\nGRADE") {
                root.findViewWithTag<View>(GRADE_CHIP_TAG)?.performClick()
            }
            root.addView(
                lutButton,
                FrameLayout.LayoutParams(dp(86), dp(46)).apply {
                    gravity = Gravity.TOP or Gravity.START
                    topMargin = dp(38)
                    leftMargin = dp(10)
                }
            )

            formatButton = proButton("FORMAT\n--") {
                root.findViewWithTag<View>(FIELD_CHIP_TAG)?.performClick()
            }
            root.addView(
                formatButton,
                FrameLayout.LayoutParams(dp(116), dp(46)).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = dp(38)
                    rightMargin = dp(10)
                }
            )

            timecodeView = TextView(activity).apply {
                text = "00:00:00"
                gravity = Gravity.CENTER
                textSize = 26f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setShadowLayer(2.2f, 0f, 1f, Color.BLACK)
            }
            root.addView(
                timecodeView,
                FrameLayout.LayoutParams(dp(230), dp(44)).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = dp(36)
                }
            )

            formatBadge = TextView(activity).apply {
                text = "1080\n9:16"
                gravity = Gravity.CENTER
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = rounded(0xCC07131D.toInt(), 0xFFDAE4EC.toInt(), 9)
            }
            root.addView(
                formatBadge,
                FrameLayout.LayoutParams(dp(50), dp(36)).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = dp(41)
                    leftMargin = dp(-150)
                }
            )

            readyChip = proButton("READY ✓") {
                detailVisible = !detailVisible
                applyDetailVisibility()
                readyChip.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }.apply {
                textSize = 7.4f
            }
            root.addView(
                readyChip,
                FrameLayout.LayoutParams(dp(92), dp(28)).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = dp(82)
                }
            )

            technicalRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dp(6), 0, dp(6), 0)
                background = rounded(0x66031829, 0x407B91A4, 12)
            }

            lensValue = technicalCell("LENS")
            fpsValue = technicalCell("FPS")
            shutterValue = technicalCell("SHUTTER")
            irisValue = technicalCell("IRIS")
            isoValue = technicalCell("ISO")
            wbValue = technicalCell("WB")
            expValue = technicalCell("EXP")

            listOf(
                lensValue,
                fpsValue,
                shutterValue,
                irisValue,
                isoValue,
                wbValue,
                expValue
            ).forEachIndexed { index, view ->
                technicalRow.addView(
                    view,
                    LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                        if (index > 0) marginStart = dp(2)
                    }
                )
            }

            root.addView(
                technicalRow,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply {
                    gravity = Gravity.TOP
                    topMargin = dp(112)
                    leftMargin = dp(8)
                    rightMargin = dp(8)
                }
            )
        }

        private fun buildMeters() {
            histogram = HistogramView(activity)
            root.addView(
                histogram,
                FrameLayout.LayoutParams(dp(150), dp(60)).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    leftMargin = dp(12)
                    bottomMargin = dp(122)
                }
            )

            audioMeter = AudioMeterView(activity)
            root.addView(
                audioMeter,
                FrameLayout.LayoutParams(dp(150), dp(60)).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    rightMargin = dp(12)
                    bottomMargin = dp(122)
                }
            )

            zoomRuler = ZoomRulerView(activity).apply {
                onRatioChanged = { ratio ->
                    onZoomRatio(ratio)
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
            root.addView(
                zoomRuler,
                FrameLayout.LayoutParams(dp(320), dp(50)).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(68)
                }
            )
        }

        private fun buildQuickControls() {
            focusButton = proButton("FOCUS\nAUTO") {
                onFocusToggle()
                focusButton.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            autoButton = proButton("AUTO\nRESET") {
                onAutoReset()
                autoButton.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            lockButton = proButton("LOCK\nOFF") {
                onLockToggle()
                lockButton.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            settingsButton = proButton("SET\nCAM") {
                onSettings()
            }

            root.addView(
                focusButton,
                FrameLayout.LayoutParams(dp(72), dp(42)).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    leftMargin = dp(12)
                    bottomMargin = dp(72)
                }
            )
            root.addView(
                autoButton,
                FrameLayout.LayoutParams(dp(72), dp(42)).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    leftMargin = dp(90)
                    bottomMargin = dp(72)
                }
            )
            root.addView(
                lockButton,
                FrameLayout.LayoutParams(dp(72), dp(42)).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    rightMargin = dp(90)
                    bottomMargin = dp(72)
                }
            )
            root.addView(
                settingsButton,
                FrameLayout.LayoutParams(dp(72), dp(42)).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    rightMargin = dp(12)
                    bottomMargin = dp(72)
                }
            )
        }

        private fun forceCleanLegacyLayout() {
            // V239: the reporter identity is stored in setup/profile metadata;
            // it no longer consumes shooting-screen space.
            val hiddenTags = listOf(
                "v237_camera_mode_row",
                "v237_camera_identity_row",
                "v237_camera_tools_row",
                "v237_camera_advanced_row",
                "v237_camera_display_row",
                "v237_camera_output_row",
                "v237_camera_director_row",
                "v237_camera_zoom_row",
                "v237_camera_exposure_row",
                "v237_camera_settings_summary",
                "v239_legacy_narration",
                "v239_legacy_horizon",
                "v239_legacy_motion",
                "v239_legacy_light",
                "v239_legacy_audio",
                "v239_legacy_thermal",
                OLD_READY_TAG
            )
            hiddenTags.forEach { tag ->
                root.findViewWithTag<View>(tag)?.visibility = View.GONE
            }

            // V239 owns two replacement corner buttons but delegates clicks to
            // the proven V235/V236 panels.
            root.findViewWithTag<View>(FIELD_CHIP_TAG)?.visibility = View.GONE
            root.findViewWithTag<View>(GRADE_CHIP_TAG)?.visibility = View.GONE

            root.findViewWithTag<View>("v237_camera_action_row")?.visibility = View.VISIBLE
        }

        private fun updateLabels() {
            val format = formatProvider()
            val shortFormat = when {
                format.contains("YOUTUBE 16:9") -> "YOUTUBE\n16:9"
                format.contains("VERTICAL 9:16") -> "SHORTS\n9:16"
                format.contains("4:5") -> "FEED\n4:5"
                format.contains("1:1") -> "SQUARE\n1:1"
                format.contains("2.39") -> "CINEMA\n2.39"
                format.contains("DUAL SAFE") -> "DUAL\nSAFE"
                else -> "FORMAT\nAUTO"
            }
            formatButton.text = shortFormat

            val gradeOriginal = root.findViewWithTag<View>(GRADE_CHIP_TAG) as? Button
            val gradeText = gradeOriginal?.text?.toString().orEmpty()
            val strength = Regex("(\\d{1,3})%").find(gradeText)?.groupValues?.getOrNull(1)
            lutButton.text = if (strength != null) "LUT • $strength%\nGRADE" else "LUT\nGRADE"

            timecodeView.text = tcProvider()

            val quality = qualityProvider().uppercase(Locale.US)
            val res = when {
                quality.contains("4K") -> "4K"
                quality.contains("1080") -> "1080"
                quality.contains("HD") -> "HD"
                else -> "AUTO"
            }
            val ratio = when {
                format.contains("16:9") -> "16:9"
                format.contains("9:16") -> "9:16"
                format.contains("4:5") -> "4:5"
                format.contains("1:1") -> "1:1"
                format.contains("2.39") -> "2.39"
                else -> "--"
            }
            formatBadge.text = "$res\n$ratio"

            lensValue.text = "LENS\n${cleanValue(lensProvider(), 10)}"
            fpsValue.text = "FPS\n${cleanFps(fpsProvider())}"
            shutterValue.text = "SHUTTER\nAUTO"
            irisValue.text = "IRIS\nFIXED"
            isoValue.text = "ISO\nAUTO"
            wbValue.text = "WB\nAUTO"
            val ev = exposureProvider()
            expValue.text = "EXP\n${if (ev > 0) "+" else ""}$ev"

            val focus = focusProvider().uppercase(Locale.US)
            focusButton.text = if (focus.contains("LOCK")) "FOCUS\nLOCK" else "FOCUS\nAUTO"
            lockButton.text = if (lockProvider()) "LOCK\nON" else "LOCK\nOFF"

            val field = DevelopUgandaFieldIntelligencePanel.snapshotJson(activity)
            val roll = field.optDouble("horizon_roll_deg", 0.0)
            val sats = field.optInt("satellites_used", 0)
            val audio = audioProvider().first
            readyChip.text = when {
                abs(roll) > 2.2 -> "LEVEL ${String.format(Locale.US, "%.1f", roll)}°"
                audio >= 0.90 -> "AUDIO HOT"
                sats <= 0 -> "READY • GPS --"
                else -> "READY ✓ • SAT $sats"
            }
        }

        private fun updateMeters() {
            val audio = audioProvider()
            audioMeter.level = audio.first.toFloat().coerceIn(0f, 1f)
            audioMeter.peak = audio.second.toFloat().coerceIn(0f, 1f)
            audioMeter.invalidate()

            val minZoom = minZoomProvider().coerceAtLeast(0.1f)
            val maxZoom = maxZoomProvider().coerceAtLeast(minZoom)
            zoomRuler.minZoom = minZoom
            zoomRuler.maxZoom = maxZoom
            zoomRuler.zoom = zoomProvider().coerceIn(minZoom, maxZoom)
            zoomRuler.invalidate()

            val now = System.currentTimeMillis()
            if (now - lastHistogramMs >= 900L && detailVisible) {
                lastHistogramMs = now
                val bitmap = try { previewView.bitmap } catch (_: Exception) { null }
                if (bitmap != null) histogram.submit(bitmap)
            }
        }

        private fun applyDetailVisibility() {
            val visibility = if (detailVisible) View.VISIBLE else View.GONE
            technicalRow.visibility = visibility
            histogram.visibility = visibility
            audioMeter.visibility = visibility
            zoomRuler.visibility = visibility
            focusButton.visibility = visibility
            autoButton.visibility = visibility
            lockButton.visibility = visibility
            settingsButton.visibility = visibility
            lutButton.visibility = visibility
            formatButton.visibility = visibility
            formatBadge.visibility = visibility
        }

        private fun proButton(textValue: String, action: () -> Unit): Button =
            Button(activity).apply {
                text = textValue
                textSize = 8.1f
                isAllCaps = false
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(dp(5), 0, dp(5), 0)
                background = rounded(0xD9082236.toInt(), 0xFFB8CAD6.toInt(), 13)
                setOnClickListener { action() }
            }

        private fun technicalCell(label: String): TextView =
            TextView(activity).apply {
                text = "$label\n--"
                gravity = Gravity.CENTER
                textSize = 7.2f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setShadowLayer(1.2f, 0f, 1f, Color.BLACK)
            }

        private fun cleanValue(value: String, maxChars: Int): String =
            value.replace("\n", " ")
                .replace("LENS ▾", "", ignoreCase = true)
                .replace("LENS", "", ignoreCase = true)
                .trim()
                .ifBlank { "AUTO" }
                .take(maxChars)

        private fun cleanFps(value: String): String {
            val match = Regex("(\\d{2,3})").find(value)
            return match?.groupValues?.getOrNull(1) ?: "AUTO"
        }

        private fun rounded(fill: Int, stroke: Int, radiusDp: Int): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(radiusDp).toFloat()
                setColor(fill)
                setStroke(dp(1), stroke)
            }

        private fun dp(value: Int): Int =
            (value * activity.resources.displayMetrics.density).roundToInt()
    }

    private class HistogramView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bins = FloatArray(32)
        private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density
            color = 0x99FFFFFF.toInt()
        }

        fun submit(bitmap: android.graphics.Bitmap) {
            java.util.Arrays.fill(bins, 0f)
            if (bitmap.width <= 0 || bitmap.height <= 0) return

            var samples = 0
            val stepX = max(1, bitmap.width / 52)
            val stepY = max(1, bitmap.height / 30)
            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val c = bitmap.getPixel(x, y)
                    val r = Color.red(c)
                    val g = Color.green(c)
                    val b = Color.blue(c)
                    val luma = (r * 0.2126f + g * 0.7152f + b * 0.0722f).roundToInt().coerceIn(0, 255)
                    val index = (luma * (bins.size - 1) / 255f).roundToInt().coerceIn(0, bins.lastIndex)
                    bins[index] += 1f
                    samples += 1
                    x += stepX
                }
                y += stepY
            }
            if (samples > 0) {
                val peak = bins.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                bins.indices.forEach { bins[it] /= peak }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), dp(8f), dp(8f), frame)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1.4f)
            paint.color = Color.WHITE
            if (bins.isEmpty()) return
            val usableW = width - dp(12f)
            val usableH = height - dp(18f)
            val baseY = height - dp(7f)
            val dx = usableW / (bins.size - 1).coerceAtLeast(1)
            var lastX = dp(6f)
            var lastY = baseY - bins[0] * usableH
            for (i in 1 until bins.size) {
                val x = dp(6f) + i * dx
                val y = baseY - bins[i] * usableH
                canvas.drawLine(lastX, lastY, x, y, paint)
                lastX = x
                lastY = y
            }
            paint.style = Paint.Style.FILL
            paint.textSize = dp(7f)
            canvas.drawText("HIST", dp(7f), dp(10f), paint)
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density
    }

    private class AudioMeterView(context: android.content.Context) : View(context) {
        var level: Float = 0f
        var peak: Float = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density
            color = 0x99FFFFFF.toInt()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), dp(8f), dp(8f), frame)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = dp(7f)
            canvas.drawText("AUDIO", dp(7f), dp(10f), paint)

            val left = dp(8f)
            val top = dp(22f)
            val barW = width - dp(16f)
            val barH = dp(8f)
            paint.color = 0x66393F46
            canvas.drawRoundRect(left, top, left + barW, top + barH, dp(3f), dp(3f), paint)
            canvas.drawRoundRect(left, top + dp(16f), left + barW, top + dp(24f), dp(3f), dp(3f), paint)

            paint.color = if (level >= 0.90f) 0xFFD95C5C.toInt() else 0xFF66C98B.toInt()
            canvas.drawRoundRect(left, top, left + barW * level, top + barH, dp(3f), dp(3f), paint)
            paint.color = if (peak >= 0.90f) 0xFFD95C5C.toInt() else 0xFFD4B85A.toInt()
            canvas.drawRoundRect(left, top + dp(16f), left + barW * peak, top + dp(24f), dp(3f), dp(3f), paint)
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density
    }

    private class ZoomRulerView(context: android.content.Context) : View(context) {
        var minZoom: Float = 1f
        var maxZoom: Float = 1f
        var zoom: Float = 1f
        var onRatioChanged: ((Float) -> Unit)? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val left = dp(18f)
            val right = width - dp(18f)
            val centerY = height * 0.58f

            paint.color = 0xDFFFFFFF.toInt()
            paint.strokeWidth = dp(1f)
            canvas.drawLine(left, centerY, right, centerY, paint)

            val range = (maxZoom - minZoom).coerceAtLeast(0.001f)
            for (i in 0..20) {
                val x = left + (right - left) * i / 20f
                val h = if (i % 5 == 0) dp(9f) else dp(5f)
                canvas.drawLine(x, centerY - h / 2f, x, centerY + h / 2f, paint)
            }

            val t = ((zoom - minZoom) / range).coerceIn(0f, 1f)
            val markerX = left + (right - left) * t
            paint.strokeWidth = dp(2.4f)
            paint.color = 0xFF7EC7FF.toInt()
            canvas.drawLine(markerX, centerY - dp(14f), markerX, centerY + dp(14f), paint)

            paint.style = Paint.Style.FILL
            paint.textSize = dp(8f)
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.color = Color.WHITE
            val zoomText = String.format(Locale.US, "%.1fx", zoom)
            canvas.drawText(zoomText, width / 2f - paint.measureText(zoomText) / 2f, dp(11f), paint)

            paint.textSize = dp(6.5f)
            val minText = String.format(Locale.US, "%.1fx", minZoom)
            val maxText = String.format(Locale.US, "%.1fx", maxZoom)
            canvas.drawText(minText, left, height - dp(3f), paint)
            canvas.drawText(maxText, right - paint.measureText(maxText), height - dp(3f), paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked != MotionEvent.ACTION_DOWN && event.actionMasked != MotionEvent.ACTION_MOVE && event.actionMasked != MotionEvent.ACTION_UP) {
                return true
            }
            val left = dp(18f)
            val right = width - dp(18f)
            val t = ((event.x - left) / (right - left).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val ratio = minZoom + (maxZoom - minZoom) * t
            onRatioChanged?.invoke(ratio)
            return true
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density
    }
}
