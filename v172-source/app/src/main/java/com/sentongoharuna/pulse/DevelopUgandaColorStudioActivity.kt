package com.sentongoharuna.pulse

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class DevelopUgandaColorStudioActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCOPE = "v230_color_scope"
        const val EXTRA_HINT = "v230_color_hint"
    }

    private val ink = 0xFF031829.toInt()
    private val panel = 0xFF082236.toInt()
    private val white = 0xFFF1F3F8.toInt()
    private val muted = 0xFFAEB7C7.toInt()
    private val green = 0xFF91B6A0.toInt()
    private val cyan = 0xFF73B7D9.toInt()
    private val violet = 0xFFAEBDEB.toInt()
    private val amber = 0xFFD0B06F.toInt()

    private lateinit var activeView: TextView
    private lateinit var strengthView: TextView
    private lateinit var monitorCheck: CheckBox
    private lateinit var strengthSeek: SeekBar
    private lateinit var profileHost: LinearLayout
    private var loading = false

    private val scope: String by lazy {
        intent.getStringExtra(EXTRA_SCOPE)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "GLOBAL"
    }

    private val hint: String by lazy {
        intent.getStringExtra(EXTRA_HINT)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "GENERAL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
            setPadding(dp(12), dp(18), dp(12), dp(12))
        }

        root.addView(label("develop.uganda • V230", 20f, violet, true))
        root.addView(
            label(
                "CINEMA COLOR ENGINE 2.0 • SIGNATURE LUT RECIPES • REAL 17³ MASTER",
                8f,
                green,
                true
            ).apply { setPadding(0, dp(3), 0, dp(8)) }
        )

        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(24))
        }

        page.addView(card().apply {
            addView(label("COLOR SCOPE", 11f, white, true))
            addView(label(scope, 9f, cyan, true))
            addView(label("AUTO HINT • $hint", 8f, muted, false))
            activeView = label("", 11f, violet, true).apply {
                setPadding(0, dp(8), 0, 0)
            }
            addView(activeView)
        })

        page.addView(card().apply {
            addView(label("OUTPUT ARCHITECTURE", 11f, white, true))
            addView(
                label(
                    "ORIGINAL MP4 • NEVER REPLACED\nCOLOR_MASTER.mp4 • Media3 SingleColorLut 17³ + H.264/AAC re-encode\nHDR ORIGINAL • retained; delivery master tone-maps to SDR where Media3 supports it\nMONITOR • optional approximation only",
                    8f,
                    muted,
                    false
                ).apply { setPadding(0, dp(5), 0, 0) }
            )
        })

        page.addView(card().apply {
            addView(label("CINEMA COLOR ENGINE 2.0", 11f, white, true))
            addView(
                label(
                    "V230 deliberately makes the creative looks more distinct: split-toned shadows/midtones/highlights, selective teal/amber/green color shaping, stronger film density and profile-specific highlight shoulders. The ORIGINAL file is still preserved.",
                    8f,
                    muted,
                    false
                ).apply { setPadding(0, dp(5), 0, 0) }
            )
        })

        page.addView(section("PROFILE"))

        profileHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        page.addView(profileHost)

        page.addView(section("STRENGTH"))

        strengthView = label("", 10f, white, true)
        page.addView(strengthView)

        strengthSeek = SeekBar(this).apply {
            max = 75
            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        val strength = 25 + progress
                        strengthView.text = "COLOR STRENGTH • $strength%"
                        if (fromUser) {
                            DevelopUgandaColorEngine.setStrength(
                                this@DevelopUgandaColorStudioActivity,
                                scope,
                                strength
                            )
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) { }
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        refresh()
                    }
                }
            )
        }
        page.addView(strengthSeek)

        monitorCheck = CheckBox(this).apply {
            text = "OPTIONAL COLOR MONITOR APPROXIMATION"
            textSize = 9f
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(green)
            setOnCheckedChangeListener { _, checked ->
                if (loading) {
                    return@setOnCheckedChangeListener
                }

                DevelopUgandaColorEngine.setMonitorEnabled(
                    this@DevelopUgandaColorStudioActivity,
                    checked
                )
                toast(
                    if (checked) {
                        "Color monitor approximation ON • saved Color Master still uses the real 3D LUT"
                    } else {
                        "Color monitor approximation OFF • camera preview protected"
                    }
                )
            }
        }
        page.addView(monitorCheck)

        page.addView(
            label(
                "V230 uses original develop.uganda color recipes. The reference palettes guide split-toning and selective color; they are not copied proprietary camera LUT files.",
                8f,
                amber,
                true
            ).apply { setPadding(0, dp(9), 0, 0) }
        )

        scroll.addView(page)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        setContentView(root)
    }

    private fun refresh() {
        loading = true

        val resolved = DevelopUgandaColorEngine.resolve(this, scope, hint)
        activeView.text =
            "ACTIVE • ${resolved.statusLabel()} • ${resolved.strength}%"

        strengthSeek.progress =
            (resolved.strength.coerceIn(25, 100) - 25)
        strengthView.text = "COLOR STRENGTH • ${resolved.strength}%"
        monitorCheck.isChecked = DevelopUgandaColorEngine.monitorEnabled(this)

        profileHost.removeAllViews()

        addProfileButton(
            "AUTO • CAMERA / SCENE",
            "Uses the camera / scene / mode hint to choose a DU profile automatically.",
            0
        )
        addProfileButton(
            "ORIGINAL • NO COLOR MASTER",
            "No V230 grade is exported. The original CameraX recording remains the only master.",
            1
        )

        var lastFamily = ""
        DevelopUgandaColorEngine.profiles.forEachIndexed { index, profile ->
            if (profile.family != lastFamily) {
                lastFamily = profile.family
                profileHost.addView(
                    label(profile.family, 9f, amber, true).apply {
                        setPadding(dp(2), dp(12), dp(2), dp(2))
                    }
                )
            }

            addProfileButton(
                profile.label,
                profile.purpose,
                index + 2,
                profile.palette
            )
        }

        loading = false
    }

    private fun addProfileButton(
        name: String,
        purpose: String,
        menuIndex: Int,
        palette: String = ""
    ) {
        val selected =
            DevelopUgandaColorEngine.selectedMenuIndex(this, scope) == menuIndex

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(panel, if (selected) green else 0xFF456983.toInt())
        }

        val button = Button(this).apply {
            text = name
            textSize = 9f
            isAllCaps = false
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(0xFF092236.toInt(), if (selected) green else cyan)
            setOnClickListener {
                DevelopUgandaColorEngine.setSelectedMenuIndex(
                    this@DevelopUgandaColorStudioActivity,
                    scope,
                    menuIndex
                )
                refresh()
                toast("V230 CINEMA COLOR • $name")
            }
        }

        box.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(43)
            )
        )
        box.addView(
            label(purpose, 7.5f, muted, false).apply {
                setPadding(dp(2), dp(5), dp(2), 0)
            }
        )

        if (palette.isNotBlank()) {
            box.addView(
                label("PALETTE • $palette", 7.2f, amber, true).apply {
                    setPadding(dp(2), dp(4), dp(2), 0)
                }
            )
        }

        profileHost.addView(
            box,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        )
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(11), dp(10), dp(11), dp(10))
        background = rounded(panel, 0xFF456983.toInt())
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(7) }
    }

    private fun section(value: String): TextView =
        label(value, 11f, violet, true).apply {
            setPadding(0, dp(13), 0, dp(5))
        }

    private fun label(
        value: String,
        sp: Float,
        color: Int,
        bold: Boolean
    ): TextView = TextView(this).apply {
        text = value
        textSize = sp
        setTextColor(color)
        typeface = Typeface.create(
            Typeface.DEFAULT,
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
    }

    private fun rounded(fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(15).toFloat()
            setColor(fill)
            setStroke(dp(1), stroke)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun toast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    }
}
