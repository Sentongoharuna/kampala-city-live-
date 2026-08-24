package com.sentongoharuna.pulse

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class DevelopUgandaNewsroomActivity : AppCompatActivity() {

    private lateinit var contentHost: FrameLayout
    private lateinit var navHome: Button
    private lateinit var navCamera: Button
    private lateinit var navLive: Button
    private lateinit var navEdit: Button
    private lateinit var navNewsroom: Button

    private val gold = 0xFFFFC21A.toInt()
    private val cyan = 0xFF7FE8FF.toInt()
    private val green = 0xFF76E39A.toInt()
    private val red = 0xFFFF4D42.toInt()
    private val white = Color.WHITE
    private val ink = 0xFF070B0E.toInt()
    private val card = 0xFF11181D.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        showHome()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(10))
            setBackgroundColor(0xFF0A1014.toInt())
        }

        top.addView(
            label("develop.uganda", 23f, gold, true),
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )
        top.addView(
            label("MOBILE NEWSROOM", 9f, white, true).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            },
            LinearLayout.LayoutParams(dp(140), dp(48))
        )

        root.addView(
            top,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            )
        )

        contentHost = FrameLayout(this).apply {
            setBackgroundColor(ink)
        }

        root.addView(
            contentHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(10))
            setBackgroundColor(0xFF091015.toInt())
        }

        navHome = navButton("HOME", white)
        navCamera = navButton("CAMERA", gold)
        navLive = navButton("LIVE", red)
        navEdit = navButton("EDIT", cyan)
        navNewsroom = navButton("NEWSROOM", green)

        listOf(
            navHome,
            navCamera,
            navLive,
            navEdit,
            navNewsroom
        ).forEach {
            nav.addView(
                it,
                LinearLayout.LayoutParams(
                    0,
                    dp(54),
                    1f
                )
            )
        }

        root.addView(
            nav,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            )
        )

        setContentView(root)

        navHome.setOnClickListener { showHome() }
        navCamera.setOnClickListener { openCamera("FIELD REPORT") }
        navLive.setOnClickListener { showLiveStudio() }
        navEdit.setOnClickListener { openEditor() }
        navNewsroom.setOnClickListener { showNewsroom() }
    }

    private fun showHome() {
        val scroll = ScrollView(this)
        val page = pageColumn()

        page.addView(
            hero(
                "REPORT. EDIT. PUBLISH.",
                "A mobile newsroom built around the develop.uganda field camera."
            )
        )

        page.addView(
            sectionTitle("QUICK ACTIONS")
        )

        val actions = horizontalButtons(
            listOf(
                Triple("FIELD CAMERA", gold) {
                    openCamera("FIELD REPORT")
                },
                Triple("LIVE EFFECT", red) {
                    openCamera("LIVE EFFECT")
                },
                Triple("EDIT VIDEO", cyan) {
                    openEditor()
                }
            )
        )
        page.addView(actions)

        page.addView(sectionTitle("TODAY'S WORKFLOW"))
        page.addView(
            infoCard(
                "1  CAPTURE",
                "Record a field report with Report ID, GPS, compass, weather and live telemetry."
            )
        )
        page.addView(
            infoCard(
                "2  EDIT",
                "Open a video, preview it, choose trim points and create a clean shareable cut."
            )
        )
        page.addView(
            infoCard(
                "3  PACKAGE",
                "Save Reporter, Story ID and headline in the Newsroom before distribution."
            )
        )
        page.addView(
            infoCard(
                "4  PUBLISH",
                "Share the finished report to TikTok, X, WhatsApp, Instagram, YouTube or another app."
            )
        )

        page.addView(sectionTitle("REPORTER LIVE EFFECT"))
        page.addView(
            infoCard(
                "ON LOCATION",
                "Launches the same professional camera with a red LIVE EFFECT broadcast label. " +
                    "This is a recorded/live-style effect; a true internet livestream still requires a streaming service endpoint."
            )
        )

        scroll.addView(page)
        setPage(scroll)
    }

    private fun showLiveStudio() {
        val scroll = ScrollView(this)
        val page = pageColumn()

        page.addView(
            hero(
                "REPORTER LIVE",
                "Broadcast-style capture for breaking news, interviews and on-location reporting."
            )
        )

        page.addView(
            bigAction(
                "● OPEN LIVE EFFECT CAMERA",
                red
            ) {
                openCamera("LIVE EFFECT")
            }
        )

        page.addView(sectionTitle("LIVE EFFECT INCLUDES"))
        page.addView(
            infoCard(
                "BROADCAST IDENTITY",
                "develop.uganda + LIVE EFFECT, Reporter, Story ID, Report ID, timecode and location."
            )
        )
        page.addView(
            infoCard(
                "FIELD INSTRUMENTS",
                "Compass, GPS, satellites, speed, distance, horizon, audio level, weather, battery and network."
            )
        )
        page.addView(
            infoCard(
                "STREAMING STATUS",
                "This build prepares the live-report visual mode but does not pretend to stream to the internet. " +
                    "A real LIVE button needs an RTMP/SRT/WebRTC destination or develop.uganda streaming backend."
            )
        )

        scroll.addView(page)
        setPage(scroll)
    }

    private fun showNewsroom() {
        val scroll = ScrollView(this)
        val page = pageColumn()

        page.addView(
            hero(
                "NEWSROOM",
                "Prepare reporter identity and story details before opening the camera."
            )
        )

        val prefs =
            getSharedPreferences(
                "develop_uganda_reporter",
                Context.MODE_PRIVATE
            )

        val reporter = editorField(
            "Reporter / citizen name",
            prefs.getString("reporter_name", "") ?: ""
        )
        val story = editorField(
            "Story ID / assignment",
            prefs.getString("story_id", "") ?: ""
        )

        val newsroomPrefs =
            getSharedPreferences(
                "develop_uganda_newsroom",
                Context.MODE_PRIVATE
            )

        val headline = editorField(
            "Headline",
            newsroomPrefs.getString("headline", "") ?: ""
        )

        val description = EditText(this).apply {
            hint = "Story summary / caption"
            setHintTextColor(0xFF73808A.toInt())
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            minLines = 4
            setText(
                newsroomPrefs.getString(
                    "description",
                    ""
                ) ?: ""
            )
            background = rounded(
                card,
                0xFF33414B.toInt(),
                14
            )
        }

        page.addView(sectionTitle("ASSIGNMENT"))
        page.addView(reporter, full(dp(54), 0, 6))
        page.addView(story, full(dp(54), 0, 6))
        page.addView(headline, full(dp(54), 0, 6))
        page.addView(description, full(dp(110), 0, 12))

        page.addView(
            bigAction(
                "SAVE ASSIGNMENT",
                green
            ) {
                val reporterValue =
                    reporter.text.toString().trim()
                        .ifBlank { "CITIZEN" }

                prefs.edit()
                    .putString(
                        "reporter_name",
                        reporterValue
                    )
                    .putString(
                        "story_id",
                        story.text.toString().trim()
                    )
                    .apply()

                newsroomPrefs.edit()
                    .putString(
                        "headline",
                        headline.text.toString().trim()
                    )
                    .putString(
                        "description",
                        description.text.toString().trim()
                    )
                    .apply()

                toast("Assignment saved")
            }
        )

        page.addView(
            bigAction(
                "SAVE & OPEN FIELD CAMERA",
                gold
            ) {
                navNewsroom.performClick()
                val reporterValue =
                    reporter.text.toString().trim()
                        .ifBlank { "CITIZEN" }

                prefs.edit()
                    .putString(
                        "reporter_name",
                        reporterValue
                    )
                    .putString(
                        "story_id",
                        story.text.toString().trim()
                    )
                    .apply()

                newsroomPrefs.edit()
                    .putString(
                        "headline",
                        headline.text.toString().trim()
                    )
                    .putString(
                        "description",
                        description.text.toString().trim()
                    )
                    .apply()

                openCamera("FIELD REPORT")
            }
        )

        page.addView(
            bigAction(
                "SHARE STORY TEXT",
                cyan
            ) {
                val shareText = buildString {
                    val h =
                        headline.text.toString().trim()
                    if (h.isNotBlank()) {
                        append(h)
                        append("\n\n")
                    }
                    append(
                        description.text.toString().trim()
                    )
                    val id =
                        story.text.toString().trim()
                    if (id.isNotBlank()) {
                        append("\n\nStory ID: ")
                        append(id)
                    }
                    append("\n\n#developUganda")
                }

                val send =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareText
                        )
                    }

                startActivity(
                    Intent.createChooser(
                        send,
                        "Publish / share story"
                    )
                )
            }
        )

        scroll.addView(page)
        setPage(scroll)
    }

    private fun openCamera(mode: String) {
        startActivity(
            Intent(
                this,
                DevelopUgandaCameraActivity::class.java
            ).apply {
                putExtra(
                    "develop_uganda_mode",
                    mode
                )
            }
        )
    }

    private fun openEditor() {
        startActivity(
            Intent(
                this,
                DevelopUgandaEditorActivity::class.java
            )
        )
    }

    private fun setPage(view: View) {
        contentHost.removeAllViews()
        contentHost.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun pageColumn(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
    }

    private fun hero(
        title: String,
        subtitle: String
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = rounded(
                0xFF10181D.toInt(),
                gold,
                22
            )

            addView(
                label(
                    title,
                    23f,
                    gold,
                    true
                )
            )
            addView(
                label(
                    subtitle,
                    14f,
                    0xFFD3DDE2.toInt(),
                    false
                ).apply {
                    setPadding(0, dp(7), 0, 0)
                }
            )
        }.apply {
            layoutParams = full(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                14
            )
        }
    }

    private fun infoCard(
        title: String,
        body: String
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(
                card,
                0xFF27343D.toInt(),
                16
            )
            addView(
                label(
                    title,
                    13f,
                    white,
                    true
                )
            )
            addView(
                label(
                    body,
                    13f,
                    0xFFB8C5CC.toInt(),
                    false
                ).apply {
                    setPadding(0, dp(5), 0, 0)
                }
            )
        }.apply {
            layoutParams = full(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                8
            )
        }
    }

    private fun sectionTitle(
        value: String
    ): TextView {
        return label(
            value,
            11f,
            0xFF81919B.toInt(),
            true
        ).apply {
            setPadding(
                dp(2),
                dp(16),
                dp(2),
                dp(8)
            )
        }
    }

    private fun horizontalButtons(
        items: List<Triple<String, Int, () -> Unit>>
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        items.forEachIndexed { index, item ->
            val button =
                navButton(
                    item.first,
                    item.second
                ).apply {
                    setOnClickListener {
                        item.third.invoke()
                    }
                }

            row.addView(
                button,
                LinearLayout.LayoutParams(
                    0,
                    dp(64),
                    1f
                ).apply {
                    if (index > 0) {
                        marginStart = dp(7)
                    }
                }
            )
        }

        return row
    }

    private fun bigAction(
        title: String,
        accent: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = title
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(
                0xFF121A1F.toInt(),
                accent,
                18
            )
            setOnClickListener {
                action.invoke()
            }
            layoutParams = full(
                dp(58),
                0,
                9
            )
        }
    }

    private fun editorField(
        hintValue: String,
        initial: String
    ): EditText {
        return EditText(this).apply {
            hint = hintValue
            setHintTextColor(0xFF73808A.toInt())
            setTextColor(Color.WHITE)
            textSize = 15f
            setText(initial)
            setPadding(dp(14), 0, dp(14), 0)
            isSingleLine = true
            background = rounded(
                card,
                0xFF33414B.toInt(),
                14
            )
        }
    }

    private fun navButton(
        value: String,
        accent: Int
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 9f
            isAllCaps = false
            setTextColor(accent)
            background = rounded(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                12
            )
            setPadding(dp(2), 0, dp(2), 0)
        }
    }

    private fun label(
        value: String,
        sp: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = sp
            setTextColor(color)
            typeface = Typeface.create(
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
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(fill)
            if (stroke != Color.TRANSPARENT) {
                setStroke(dp(1), stroke)
            }
        }
    }

    private fun full(
        height: Int,
        top: Int,
        bottom: Int
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        ).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }
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
}
