package com.sentongoharuna.pulse

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView

/**
 * V233 Live Grade Monitor.
 *
 * This is an operator-only camera-screen control surface. It applies the
 * selected Uganda Scene LUT and palette tuning to the PreviewView immediately,
 * while the original CameraX recording remains untouched. Final COLOR_MASTER
 * export is still produced by DevelopUgandaColorEngine's tuned 17^3 LUT.
 */
object DevelopUgandaLiveGradePanel {

    private const val TAG = "develop_uganda_v232_live_grade_panel"

    fun attach(
        activity: AppCompatActivity,
        root: FrameLayout,
        previewView: PreviewView,
        scopeProvider: () -> String,
        hintProvider: () -> String
    ) {
        if (root.findViewWithTag<View>(TAG) != null) {
            return
        }

        Controller(
            activity,
            root,
            previewView,
            scopeProvider,
            hintProvider
        ).attach()
    }

    private class Controller(
        private val activity: AppCompatActivity,
        private val root: FrameLayout,
        private val previewView: PreviewView,
        private val scopeProvider: () -> String,
        private val hintProvider: () -> String
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private var bypass = false
        private var expanded = false
        private var loading = false
        private var lastSignature = ""

        private lateinit var chip: Button
        private lateinit var card: LinearLayout
        private lateinit var active: TextView
        private lateinit var masterLabel: TextView
        private lateinit var masterSeek: SeekBar
        private lateinit var paletteHost: LinearLayout
        private lateinit var lookButton: Button
        private lateinit var compareButton: Button

        private val refreshRunnable =
            object : Runnable {
                override fun run() {
                    if (activity.isFinishing || activity.isDestroyed) {
                        return
                    }

                    refreshIfNeeded()
                    handler.postDelayed(this, 650L)
                }
            }

        fun attach() {
            // A TextureView-compatible PreviewView allows the screen-space
            // ColorMatrix monitor to be visible over the actual camera feed.
            // The CameraX recording pipeline itself is not altered.
            previewView.implementationMode =
                PreviewView.ImplementationMode.COMPATIBLE

            DevelopUgandaColorEngine.setMonitorEnabled(
                activity,
                true
            )

            buildChip()
            buildCard()
            applyNow()
            refreshUi(force = true)
            handler.postDelayed(refreshRunnable, 650L)
        }

        private fun buildChip() {
            chip = Button(activity).apply {
                tag = TAG
                text = "LIVE GRADE\nTUNE ▸"
                textSize = 8.4f
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = rounded(
                    0xE6031829.toInt(),
                    0xFF73B7D9.toInt(),
                    16
                )
                setOnClickListener {
                    expanded = !expanded
                    card.visibility =
                        if (expanded) View.VISIBLE else View.GONE
                    refreshUi(force = true)
                }
            }

            root.addView(
                chip,
                FrameLayout.LayoutParams(
                    dp(92),
                    dp(48)
                ).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    rightMargin = dp(8)
                }
            )
        }

        private fun buildCard() {
            card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(9), dp(10), dp(9))
                background = rounded(
                    0xF2031829.toInt(),
                    0xFF73B7D9.toInt(),
                    18
                )
                visibility = View.GONE
                elevation = dp(10).toFloat()
            }

            val header = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            active = label("LIVE GRADE", 9.5f, Color.WHITE, true).apply {
                maxLines = 2
            }
            header.addView(
                active,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val close = Button(activity).apply {
                text = "×"
                textSize = 18f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = rounded(
                    0xFF102B40.toInt(),
                    0xFF566A74.toInt(),
                    12
                )
                setOnClickListener {
                    expanded = false
                    card.visibility = View.GONE
                }
            }
            header.addView(close, LinearLayout.LayoutParams(dp(42), dp(38)))
            card.addView(header)

            val quick = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(5), 0, dp(4))
            }

            lookButton = smallButton("LOOK ▾", 0xFF73B7D9.toInt()).apply {
                setOnClickListener { chooseLook() }
            }
            compareButton = smallButton("HOLD ORIGINAL", 0xFFD0B06F.toInt()).apply {
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            bypass = true
                            DevelopUgandaColorEngine.clearPreviewMonitor(previewView)
                            text = "ORIGINAL • HOLD"
                            true
                        }

                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            bypass = false
                            text = "HOLD ORIGINAL"
                            applyNow()
                            true
                        }

                        else -> true
                    }
                }
            }
            val reset = smallButton("RESET", 0xFF91B6A0.toInt()).apply {
                setOnClickListener { resetCurrent() }
            }

            quick.addView(lookButton, weight())
            quick.addView(space(dp(5)), fixed(dp(5), 1))
            quick.addView(compareButton, weight())
            quick.addView(space(dp(5)), fixed(dp(5), 1))
            quick.addView(reset, weight())
            card.addView(quick)

            masterLabel = label("MASTER • 0%", 8f, 0xFFF1F3F8.toInt(), true)
            card.addView(masterLabel)

            masterSeek = SeekBar(activity).apply {
                max = 100
                progressTintList = ColorStateList.valueOf(0xFF73B7D9.toInt())
                thumbTintList = ColorStateList.valueOf(0xFF73B7D9.toInt())
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            masterLabel.text = "MASTER LUT STRENGTH • $progress%"
                            if (fromUser && !loading) {
                                DevelopUgandaColorEngine.setStrength(
                                    activity,
                                    scope(),
                                    progress
                                )
                                applyNow()
                                updateHeader()
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                        override fun onStopTrackingTouch(seekBar: SeekBar?) { }
                    }
                )
            }
            card.addView(masterSeek)

            card.addView(
                label(
                    "LIVE PALETTE • 0–200% EACH COLOR",
                    7.4f,
                    0xFFD0B06F.toInt(),
                    true
                ).apply { setPadding(0, dp(2), 0, dp(2)) }
            )

            paletteHost = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
            }
            card.addView(paletteHost)

            card.addView(
                label(
                    "Changes above are visible on the CAMERA SCREEN now. The ORIGINAL MP4 stays clean; COLOR_MASTER.mp4 receives the final tuned 17³ LUT.",
                    7.1f,
                    0xFFAEB7C7.toInt(),
                    false
                ).apply { setPadding(0, dp(5), 0, 0) }
            )

            root.addView(
                card,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM
                    leftMargin = dp(8)
                    rightMargin = dp(8)
                    bottomMargin = dp(78)
                }
            )
        }

        private fun chooseLook() {
            val labels = DevelopUgandaColorEngine.menuLabels()
            val selected =
                DevelopUgandaColorEngine.selectedMenuIndex(
                    activity,
                    scope()
                )

            AlertDialog.Builder(activity)
                .setTitle("V233 • LIVE UGANDA LOOK")
                .setSingleChoiceItems(
                    labels,
                    selected
                ) { dialog, which ->
                    DevelopUgandaColorEngine.setSelectedMenuIndex(
                        activity,
                        scope(),
                        which
                    )
                    dialog.dismiss()
                    lastSignature = ""
                    applyNow()
                    refreshUi(force = true)
                }
                .setNegativeButton("CANCEL", null)
                .show()
        }

        private fun resetCurrent() {
            val selection = resolve()
            val profile = selection.profile ?: return

            DevelopUgandaColorTuner.reset(
                activity,
                scope(),
                profile.id
            )

            DevelopUgandaColorEngine.setStrength(
                activity,
                scope(),
                profile.defaultStrength
            )

            lastSignature = ""
            applyNow()
            refreshUi(force = true)
        }

        private fun rebuildPalette(selection: DevelopUgandaColorEngine.ResolvedSelection) {
            paletteHost.removeAllViews()
            val profile = selection.profile

            if (profile == null) {
                paletteHost.addView(
                    label(
                        "ORIGINAL • LIVE GRADE BYPASSED",
                        8f,
                        0xFFAEB7C7.toInt(),
                        true
                    )
                )
                return
            }

            val style = DevelopUgandaColorTuner.style(profile.id) ?: return
            val tuning = DevelopUgandaColorTuner.load(activity, scope(), profile.id)

            style.slots.forEachIndexed { index, slot ->
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val swatch = TextView(activity).apply {
                    text = ""
                    background = rounded(
                        Color.parseColor(slot.hex),
                        0xFFF1F3F8.toInt(),
                        8
                    )
                }
                row.addView(swatch, fixed(dp(24), dp(24)))

                val valueLabel =
                    label(
                        "${index + 1} ${slot.name} • ${tuning.value(index)}%",
                        7.2f,
                        0xFFF1F3F8.toInt(),
                        true
                    ).apply { setPadding(dp(6), 0, dp(3), 0) }
                row.addView(
                    valueLabel,
                    LinearLayout.LayoutParams(
                        dp(120),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )

                val seek = SeekBar(activity).apply {
                    max = 200
                    progress = tuning.value(index)
                    progressTintList = ColorStateList.valueOf(Color.parseColor(slot.hex))
                    thumbTintList = ColorStateList.valueOf(Color.parseColor(slot.hex))
                    setOnSeekBarChangeListener(
                        object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                valueLabel.text =
                                    "${index + 1} ${slot.name} • $progress%"

                                if (fromUser && !loading) {
                                    DevelopUgandaColorTuner.set(
                                        activity,
                                        scope(),
                                        profile.id,
                                        index,
                                        progress
                                    )
                                    applyNow()
                                    updateHeader()
                                }
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
                        }
                    )
                }
                row.addView(
                    seek,
                    LinearLayout.LayoutParams(
                        0,
                        dp(32),
                        1f
                    )
                )

                paletteHost.addView(row)
            }
        }

        private fun applyNow() {
            if (bypass) {
                return
            }

            DevelopUgandaColorEngine.setMonitorEnabled(activity, true)
            val selection = resolve()
            DevelopUgandaColorEngine.applyPreviewMonitor(
                previewView,
                selection,
                scope()
            )
        }

        private fun resolve(): DevelopUgandaColorEngine.ResolvedSelection =
            DevelopUgandaColorEngine.resolve(
                activity,
                scope(),
                hint()
            )

        private fun refreshIfNeeded() {
            val signature = signature()
            if (signature != lastSignature) {
                refreshUi(force = true)
                applyNow()
            }
        }

        private fun refreshUi(force: Boolean) {
            if (!force && signature() == lastSignature) {
                return
            }

            loading = true
            val selection = resolve()
            masterSeek.progress = selection.strength.coerceIn(0, 100)
            masterLabel.text = "MASTER LUT STRENGTH • ${selection.strength}%"
            rebuildPalette(selection)
            updateHeader()
            loading = false
            lastSignature = signature()
        }

        private fun updateHeader() {
            val selection = resolve()
            val name =
                selection.profile?.let {
                    DevelopUgandaColorTuner.displayName(it.id, it.label)
                } ?: "ORIGINAL"

            val custom =
                selection.profile?.let {
                    if (DevelopUgandaColorTuner.load(activity, scope(), it.id).isDefault()) {
                        "BASE"
                    } else {
                        "CUSTOM"
                    }
                } ?: "RAW"

            active.text =
                "● LIVE GRADE • $name\n${selection.strength}% • $custom • HOLD ORIGINAL TO COMPARE"

            chip.text =
                "GRADE • ${selection.strength}%\n${name.take(12)} ▸"

            lookButton.text = "LOOK ▾\n${name.take(16)}"
        }

        private fun signature(): String {
            val selection = resolve()
            val profile = selection.profile
            val tuning =
                if (profile == null) {
                    "RAW"
                } else {
                    DevelopUgandaColorTuner
                        .load(activity, scope(), profile.id)
                        .values
                        .joinToString(",")
                }

            return "${scope()}:${selection.requestedId}:${profile?.id}:${selection.strength}:$tuning"
        }

        private fun scope(): String =
            scopeProvider().ifBlank { "GLOBAL" }

        private fun hint(): String =
            try {
                hintProvider()
            } catch (_: Exception) {
                "REPORTER"
            }

        private fun smallButton(textValue: String, stroke: Int): Button =
            Button(activity).apply {
                text = textValue
                textSize = 7.3f
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = rounded(0xFF092236.toInt(), stroke, 12)
                setPadding(dp(3), 0, dp(3), 0)
            }

        private fun label(
            value: String,
            size: Float,
            color: Int,
            bold: Boolean
        ): TextView =
            TextView(activity).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) typeface = Typeface.DEFAULT_BOLD
            }

        private fun rounded(fill: Int, stroke: Int, radius: Int): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(fill)
                cornerRadius = dp(radius).toFloat()
                setStroke(dp(1), stroke)
            }

        private fun space(width: Int): View =
            View(activity).apply {
                layoutParams = LinearLayout.LayoutParams(width, 1)
            }

        private fun weight(): LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(0, dp(42), 1f)

        private fun fixed(width: Int, height: Int): LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(width, height)

        private fun dp(value: Int): Int =
            (value * activity.resources.displayMetrics.density).toInt()
    }
}
