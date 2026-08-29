package com.sentongoharuna.pulse

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * dfv238 camera shell.
 *
 * Design law:
 * 1. The V235 LUT control is permanent and is the visual reference.
 * 2. ASPECT and LUTS are the only two persistent top camera controls.
 * 3. V238 camera functions are not deleted: non-shooting controls move into
 *    a right-side settings drawer and keep their original click listeners.
 * 4. The V238 LENS / RECORD / LIGHT action row stays available at the bottom.
 * 5. V235 color algorithms and the CameraX recording path are not changed.
 */
object DevelopUgandaDfv238CameraShell {

    private const val TAG = "develop_uganda_dfv238_camera_shell"
    private const val LUT_TAG = "develop_uganda_v235_live_grade_panel"
    private const val ASPECT_TAG = "develop_uganda_v236_field_intelligence"
    private const val TOOLS_TAG = "develop_uganda_v233_tools_chip"
    private const val READY_TAG = "dfv238_shot_ready_chip"

    private val settingsRowTags = listOf(
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
        TOOLS_TAG,
        READY_TAG
    )

    fun attach(
        activity: AppCompatActivity,
        root: FrameLayout
    ) {
        if (root.findViewWithTag<View>(TAG) != null) return
        Controller(activity, root).attach()
    }

    private class Controller(
        private val activity: AppCompatActivity,
        private val root: FrameLayout
    ) {
        private val handler = Handler(Looper.getMainLooper())
        private lateinit var marker: View
        private lateinit var scrim: View
        private lateinit var drawer: LinearLayout
        private lateinit var body: LinearLayout
        private lateinit var settingsTab: Button
        private var drawerOpen = false
        private val movedViews = linkedSetOf<View>()

        private val tick = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed) return
                protectTopControls()
                moveSettingsIntoDrawer()
                handler.postDelayed(this, 180L)
            }
        }

        fun attach() {
            marker = View(activity).apply {
                tag = TAG
                visibility = View.GONE
            }
            root.addView(marker, FrameLayout.LayoutParams(1, 1))

            buildScrim()
            buildDrawer()
            buildSettingsTab()

            marker.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) = Unit
                    override fun onViewDetachedFromWindow(v: View) {
                        handler.removeCallbacksAndMessages(null)
                    }
                }
            )

            root.post {
                protectTopControls()
                moveSettingsIntoDrawer()
            }
            handler.post(tick)
        }

        private fun buildScrim() {
            scrim = View(activity).apply {
                setBackgroundColor(0x66000000)
                visibility = View.GONE
                setOnClickListener { setDrawer(false) }
            }
            root.addView(
                scrim,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        private fun buildDrawer() {
            drawer = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(12), dp(10), dp(10))
                background = rounded(
                    0xFA031829.toInt(),
                    0xFF73B7D9.toInt(),
                    18
                )
                visibility = View.GONE
                elevation = dp(16).toFloat()
            }

            val head = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            head.addView(
                TextView(activity).apply {
                    text = "dfv238 • CAMERA SETTINGS"
                    textSize = 10f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            head.addView(
                lutStyleButton("CLOSE ×").apply {
                    setOnClickListener { setDrawer(false) }
                },
                LinearLayout.LayoutParams(dp(78), dp(42))
            )
            drawer.addView(head)

            drawer.addView(
                TextView(activity).apply {
                    text =
                        "The shooting view stays clean: LUTS + ASPECT on top. " +
                        "The original V238 controls below keep their real functions here."
                    textSize = 7.6f
                    setTextColor(0xFFB9C9D4.toInt())
                    setPadding(0, dp(6), 0, dp(8))
                }
            )

            body = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
            }

            val scroll = ScrollView(activity).apply {
                isFillViewport = false
                addView(
                    body,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            drawer.addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

            drawer.addView(
                TextView(activity).apply {
                    text =
                        "LUTS remains the master visual control. " +
                        "ASPECT controls the delivery frame and safe guides. " +
                        "LENS / RECORD / LIGHT remain on the shooting screen."
                    textSize = 7.2f
                    setTextColor(0xFF91B6A0.toInt())
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(7), 0, 0)
                }
            )

            val width =
                (activity.resources.displayMetrics.widthPixels * 0.82f)
                    .toInt()
                    .coerceAtMost(dp(390))

            root.addView(
                drawer,
                FrameLayout.LayoutParams(
                    width,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.END
                    topMargin = dp(4)
                    bottomMargin = dp(4)
                    rightMargin = dp(4)
                }
            )
        }

        private fun buildSettingsTab() {
            settingsTab = lutStyleButton("SETTINGS\n▸").apply {
                setOnClickListener { setDrawer(!drawerOpen) }
            }

            root.addView(
                settingsTab,
                FrameLayout.LayoutParams(
                    dp(92),
                    dp(50)
                ).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    rightMargin = dp(7)
                }
            )
        }

        private fun protectTopControls() {
            val lut = root.findViewWithTag<View>(LUT_TAG)
            val aspect = root.findViewWithTag<View>(ASPECT_TAG)

            placeTopChip(lut, true, 104, 52)
            placeTopChip(aspect, false, 122, 52)

            // V238 CLEAN VIEW auto-hide must never make these two disappear.
            lut?.visibility = View.VISIBLE
            aspect?.visibility = View.VISIBLE
            lut?.bringToFront()
            aspect?.bringToFront()

            // The original action row stays on the camera for actual shooting.
            root.findViewWithTag<View>("v237_camera_action_row")
                ?.visibility = View.VISIBLE

            // LOOK in the old five-button row duplicates the permanent LUTS control.
            root.findViewWithTag<View>("v237_look_button")
                ?.visibility = View.GONE

            settingsTab.visibility =
                if (drawerOpen) View.GONE else View.VISIBLE
        }

        private fun placeTopChip(
            view: View?,
            start: Boolean,
            widthDp: Int,
            heightDp: Int
        ) {
            if (view == null) return
            val lp =
                (view.layoutParams as? FrameLayout.LayoutParams)
                    ?: FrameLayout.LayoutParams(dp(widthDp), dp(heightDp))

            lp.width = dp(widthDp)
            lp.height = dp(heightDp)
            lp.gravity =
                Gravity.TOP or
                    (if (start) Gravity.START else Gravity.END)
            lp.topMargin = dp(10)
            lp.leftMargin = if (start) dp(8) else 0
            lp.rightMargin = if (!start) dp(8) else 0
            lp.bottomMargin = 0
            view.layoutParams = lp

            if (view is Button) {
                view.textSize = 8.2f
                view.isAllCaps = false
                view.typeface = Typeface.DEFAULT_BOLD
                view.setTextColor(Color.WHITE)
                view.setPadding(dp(7), 0, dp(7), 0)
                view.elevation = dp(7).toFloat()
            }
        }

        private fun moveSettingsIntoDrawer() {
            if (!this::body.isInitialized) return

            settingsRowTags.forEach { tag ->
                val view = root.findViewWithTag<View>(tag) ?: return@forEach
                if (view === drawer || view === settingsTab) return@forEach
                if (view.parent === body) {
                    movedViews.add(view)
                    return@forEach
                }

                val parent = view.parent as? ViewGroup ?: return@forEach
                parent.removeView(view)

                body.addView(
                    view,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(5)
                    }
                )
                movedViews.add(view)
            }

            root.findViewWithTag<View>("v237_look_button")
                ?.visibility = View.GONE

            if (drawerOpen) {
                movedViews.forEach { it.visibility = View.VISIBLE }
                root.findViewWithTag<View>("v237_look_button")
                    ?.visibility = View.GONE
            }
        }

        private fun setDrawer(open: Boolean) {
            drawerOpen = open
            drawer.visibility = if (open) View.VISIBLE else View.GONE
            scrim.visibility = if (open) View.VISIBLE else View.GONE
            settingsTab.visibility = if (open) View.GONE else View.VISIBLE

            if (open) {
                moveSettingsIntoDrawer()
                movedViews.forEach { it.visibility = View.VISIBLE }
                root.findViewWithTag<View>("v237_look_button")
                    ?.visibility = View.GONE
            }

            protectTopControls()
        }

        private fun lutStyleButton(value: String): Button =
            Button(activity).apply {
                text = value
                textSize = 8.2f
                isAllCaps = false
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(dp(7), 0, dp(7), 0)
                background = rounded(
                    0xE6031829.toInt(),
                    0xFF73B7D9.toInt(),
                    16
                )
            }

        private fun rounded(
            fill: Int,
            stroke: Int,
            radius: Int
        ): GradientDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(fill)
                cornerRadius = dp(radius).toFloat()
                setStroke(dp(1), stroke)
            }

        private fun dp(value: Int): Int =
            (value * activity.resources.displayMetrics.density).toInt()
    }
}
