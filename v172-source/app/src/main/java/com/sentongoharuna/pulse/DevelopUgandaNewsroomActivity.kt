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

    private val gold = 0xFFFFC21A.toInt()
    private val cyan = 0xFF77E9FF.toInt()
    private val green = 0xFF62E889.toInt()
    private val red = 0xFFFF3B32.toInt()
    private val white = Color.WHITE
    private val ink = 0xFF05090C.toInt()
    private val card = 0xFF10171C.toInt()
    private val muted = 0xFF9BABAF.toInt()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )
        buildShell()
        showHome()
    }

    private fun buildShell() {
        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(
                    ink
                )
            }

        val top =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER_VERTICAL
                setPadding(
                    dp(18),
                    dp(11),
                    dp(18),
                    dp(8)
                )
                setBackgroundColor(
                    0xFF080E12.toInt()
                )
            }

        top.addView(
            label(
                "develop.uganda",
                22f,
                gold,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            )
        )

        top.addView(
            label(
                "CONTROL ROOM PRO • V188",
                9f,
                white,
                true
            ).apply {
                gravity =
                    Gravity.CENTER_VERTICAL or
                        Gravity.END
            },
            LinearLayout.LayoutParams(
                dp(165),
                dp(46)
            )
        )

        root.addView(
            top,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(66)
            )
        )

        contentHost =
            FrameLayout(this).apply {
                setBackgroundColor(
                    ink
                )
            }

        root.addView(
            contentHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val nav =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    Gravity.CENTER
                setPadding(
                    dp(8),
                    dp(5),
                    dp(8),
                    dp(9)
                )
                setBackgroundColor(
                    0xFF080E12.toInt()
                )
            }

        nav.addView(
            navButton(
                "HOME",
                white
            ) {
                showHome()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "REPORT",
                gold
            ) {
                openReportCamera()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "LIVE",
                red
            ) {
                showLivePage()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "EDIT",
                cyan
            ) {
                openEditor()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "DESK",
                green
            ) {
                showNewsroom()
            },
            navWeight()
        )

        root.addView(
            nav,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(68)
            )
        )

        setContentView(
            root
        )
    }

    private fun showHome() {
        val scroll =
            ScrollView(this)

        val page =
            pageColumn()

        page.addView(
            hero(
                "CONTROL ROOM PRO",
                "REPORT • LIVE • EDIT • DESK • PUBLISH"
            )
        )

        page.addView(
            sectionTitle(
                "CAPTURE MODES"
            )
        )

        page.addView(
            launchCard(
                "FIELD REPORT CAMERA",
                "FIELD REPORT • safe preview • AUTO UI • control lock • camera capabilities • SHA-256 integrity record",
                "Reporter ID • GPS • compass • weather • telemetry • scenes • looks",
                gold,
                "OPEN REPORT CAMERA"
            ) {
                openReportCamera()
            }
        )

        page.addView(
            launchCard(
                "LIVE STUDIO",
                "LIVE STUDIO • 3-2-1 countdown • MARK timestamps • lower-third styles • live control lock",
                "Blinking LIVE • signal lamps • smaller glowing setting buttons • Reporter ID • lower third • safe recorded graphics • pulsing red record ring",
                red,
                "OPEN LIVE STUDIO"
            ) {
                openLiveCamera()
            }
        )

        page.addView(
            sectionTitle(
                "POST PRODUCTION"
            )
        )

        page.addView(
            launchCard(
                "EDIT DESK",
                "Fast social cut",
                "Open clip • preview • trim • save • share",
                cyan,
                "OPEN EDIT DESK"
            ) {
                openEditor()
            }
        )

        page.addView(
            launchCard(
                "NEWSROOM DESK",
                "Prepare the story before capture",
                "Reporter • Story ID • headline • description • assignment",
                green,
                "OPEN NEWSROOM"
            ) {
                showNewsroom()
            }
        )

        page.addView(
            sectionTitle(
                "LIVE STATUS"
            )
        )

        page.addView(
            compactStatus(
                "REPORT CAMERA",
                "FIELD REPORT",
                gold
            )
        )
        page.addView(
            compactStatus(
                "LIVE CAMERA",
                "LOCAL LIVE CAPTURE • INTERNET STREAM BACKEND NOT CONNECTED",
                red
            )
        )
        page.addView(
            compactStatus(
                "EDITOR",
                "LOSSLESS MP4 TRIM + SHARE",
                cyan
            )
        )

        scroll.addView(
            page
        )

        setPage(
            scroll
        )
    }

    private fun showLivePage() {
        val scroll =
            ScrollView(this)
        val page =
            pageColumn()

        page.addView(
            hero(
                "LIVE CONTROL ROOM",
                "A separate broadcast deck for breaking news, interviews, events and community live-style coverage."
            )
        )

        page.addView(
            bigAction(
                "● ENTER LIVE STUDIO",
                red
            ) {
                openLiveCamera()
            }
        )

        page.addView(
            sectionTitle(
                "LIVE-ONLY CONTROLS"
            )
        )

        page.addView(
            infoCard(
                "QUALITY",
                "Switch FHD / HD for the LIVE camera."
            )
        )
        page.addView(
            infoCard(
                "AUDIO",
                "Enable or disable recorded microphone audio."
            )
        )
        page.addView(
            infoCard(
                "GRAPHICS",
                "Show or hide the LIVE camera's broadcast reticle and live-feed graphics."
            )
        )
        page.addView(
            infoCard(
                "LENS / LIGHT / OUTPUT",
                "Live-specific lens, torch and output status controls."
            )
        )

        page.addView(
            sectionTitle(
                "SIGNAL SYSTEM"
            )
        )

        page.addView(
            infoCard(
                "GREEN SIGNALS",
                "NET, GPS, MIC, CAM and battery lamps report readiness. REC turns red during capture."
            )
        )

        page.addView(
            infoCard(
                "LIVE INDICATOR",
                "The LIVE REC logo blinks during recording and the circular record control gains a pulsing red glow ring."
            )
        )

        page.addView(
            infoCard(
                "IMPORTANT",
                "This version records the dedicated LIVE STUDIO feed locally. A true public livestream still needs a real RTMP, SRT or WebRTC destination."
            )
        )

        scroll.addView(
            page
        )
        setPage(
            scroll
        )
    }

    private fun showNewsroom() {
        val scroll =
            ScrollView(this)
        val page =
            pageColumn()

        page.addView(
            hero(
                "NEWSROOM DESK",
                "Prepare identity, headline and assignment before recording."
            )
        )

        val prefs =
            getSharedPreferences(
                "develop_uganda_reporter",
                Context.MODE_PRIVATE
            )

        val newsroomPrefs =
            getSharedPreferences(
                "develop_uganda_newsroom",
                Context.MODE_PRIVATE
            )

        val reporter =
            editorField(
                "Reporter / citizen name",
                prefs.getString(
                    "reporter_name",
                    ""
                ) ?: ""
            )

        val story =
            editorField(
                "Story ID / assignment",
                prefs.getString(
                    "story_id",
                    ""
                ) ?: ""
            )

        val headline =
            editorField(
                "Headline",
                newsroomPrefs.getString(
                    "headline",
                    ""
                ) ?: ""
            )

        val description =
            EditText(this).apply {
                hint =
                    "Story summary / caption"
                setHintTextColor(
                    0xFF73808A.toInt()
                )
                setTextColor(
                    white
                )
                textSize =
                    15f
                gravity =
                    Gravity.TOP
                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(12)
                )
                minLines =
                    4
                setText(
                    newsroomPrefs.getString(
                        "description",
                        ""
                    ) ?: ""
                )
                background =
                    rounded(
                        card,
                        0xFF34434B.toInt(),
                        14
                    )
            }

        page.addView(
            sectionTitle(
                "ASSIGNMENT"
            )
        )

        page.addView(
            reporter,
            full(
                dp(54),
                0,
                6
            )
        )
        page.addView(
            story,
            full(
                dp(54),
                0,
                6
            )
        )
        page.addView(
            headline,
            full(
                dp(54),
                0,
                6
            )
        )
        page.addView(
            description,
            full(
                dp(110),
                0,
                12
            )
        )

        page.addView(
            bigAction(
                "SAVE ASSIGNMENT",
                green
            ) {
                saveAssignment(
                    reporter,
                    story,
                    headline,
                    description
                )
            }
        )

        page.addView(
            bigAction(
                "SAVE + OPEN FIELD REPORT CAMERA",
                gold
            ) {
                saveAssignment(
                    reporter,
                    story,
                    headline,
                    description
                )
                openReportCamera()
            }
        )

        page.addView(
            bigAction(
                "SAVE + OPEN LIVE STUDIO",
                red
            ) {
                saveAssignment(
                    reporter,
                    story,
                    headline,
                    description
                )
                openLiveCamera()
            }
        )

        page.addView(
            bigAction(
                "SHARE STORY TEXT",
                cyan
            ) {
                saveAssignment(
                    reporter,
                    story,
                    headline,
                    description
                )
                shareStoryText(
                    story,
                    headline,
                    description
                )
            }
        )

        scroll.addView(
            page
        )

        setPage(
            scroll
        )
    }

    private fun saveAssignment(
        reporter: EditText,
        story: EditText,
        headline: EditText,
        description: EditText
    ) {
        val reporterValue =
            reporter.text
                .toString()
                .trim()
                .ifBlank {
                    "CITIZEN"
                }

        val prefs =
            getSharedPreferences(
                "develop_uganda_reporter",
                Context.MODE_PRIVATE
            )

        val newsroomPrefs =
            getSharedPreferences(
                "develop_uganda_newsroom",
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putString(
                "reporter_name",
                reporterValue
            )
            .putString(
                "story_id",
                story.text
                    .toString()
                    .trim()
            )
            .apply()

        newsroomPrefs.edit()
            .putString(
                "headline",
                headline.text
                    .toString()
                    .trim()
            )
            .putString(
                "description",
                description.text
                    .toString()
                    .trim()
            )
            .apply()

        toast(
            "Assignment saved"
        )
    }

    private fun shareStoryText(
        story: EditText,
        headline: EditText,
        description: EditText
    ) {
        val text =
            buildString {
                val h =
                    headline.text
                        .toString()
                        .trim()

                if (
                    h.isNotBlank()
                ) {
                    append(h)
                    append("\n\n")
                }

                append(
                    description.text
                        .toString()
                        .trim()
                )

                val id =
                    story.text
                        .toString()
                        .trim()

                if (
                    id.isNotBlank()
                ) {
                    append(
                        "\n\nStory ID: "
                    )
                    append(id)
                }

                append(
                    "\n\n#developUganda"
                )
            }

        val send =
            Intent(
                Intent.ACTION_SEND
            ).apply {
                type =
                    "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    text
                )
            }

        startActivity(
            Intent.createChooser(
                send,
                "Publish / share story"
            )
        )
    }

    private fun openReportCamera() {
        startActivity(
            Intent(
                this,
                DevelopUgandaCameraActivity::class.java
            ).apply {
                putExtra(
                    "develop_uganda_mode",
                    "FIELD REPORT"
                )
            }
        )
    }

    private fun openLiveCamera() {
        startActivity(
            Intent(
                this,
                DevelopUgandaLiveActivity::class.java
            )
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

    private fun setPage(
        view: View
    ) {
        contentHost.removeAllViews()

        contentHost.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun pageColumn():
        LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(26)
            )
        }
    }

    private fun hero(
        title: String,
        subtitle: String
    ): View {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
            )
            background =
                rounded(
                    0xFF0E1519.toInt(),
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
                    13f,
                    0xFFD1D9DD.toInt(),
                    false
                ).apply {
                    setPadding(
                        0,
                        dp(6),
                        0,
                        0
                    )
                }
            )
        }.apply {
            layoutParams =
                full(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0,
                    13
                )
        }
    }

    private fun launchCard(
        title: String,
        kicker: String,
        body: String,
        accent: Int,
        actionText: String,
        action: () -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(15),
                dp(14),
                dp(15),
                dp(14)
            )
            background =
                rounded(
                    card,
                    accent,
                    18
                )

            addView(
                label(
                    title,
                    17f,
                    accent,
                    true
                )
            )

            addView(
                label(
                    kicker,
                    12f,
                    white,
                    true
                ).apply {
                    setPadding(
                        0,
                        dp(5),
                        0,
                        0
                    )
                }
            )

            addView(
                label(
                    body,
                    12f,
                    muted,
                    false
                ).apply {
                    setPadding(
                        0,
                        dp(5),
                        0,
                        dp(10)
                    )
                }
            )

            addView(
                bigAction(
                    actionText,
                    accent,
                    action
                )
            )
        }.apply {
            layoutParams =
                full(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0,
                    9
                )
        }
    }

    private fun compactStatus(
        title: String,
        body: String,
        accent: Int
    ): View {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.HORIZONTAL
            gravity =
                Gravity.CENTER_VERTICAL
            setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
            )
            background =
                rounded(
                    0xFF0D1317.toInt(),
                    0xFF253139.toInt(),
                    14
                )

            addView(
                label(
                    "●",
                    16f,
                    accent,
                    true
                ),
                LinearLayout.LayoutParams(
                    dp(24),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                LinearLayout(this@DevelopUgandaNewsroomActivity).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    addView(
                        label(
                            title,
                            11f,
                            white,
                            true
                        )
                    )

                    addView(
                        label(
                            body,
                            10f,
                            muted,
                            false
                        )
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }.apply {
            layoutParams =
                full(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0,
                    6
                )
        }
    }

    private fun infoCard(
        title: String,
        body: String
    ): View {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
            background =
                rounded(
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
                    12f,
                    muted,
                    false
                ).apply {
                    setPadding(
                        0,
                        dp(5),
                        0,
                        0
                    )
                }
            )
        }.apply {
            layoutParams =
                full(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0,
                    7
                )
        }
    }

    private fun sectionTitle(
        value: String
    ): TextView {
        return label(
            value,
            10f,
            0xFF81919B.toInt(),
            true
        ).apply {
            setPadding(
                dp(2),
                dp(14),
                dp(2),
                dp(7)
            )
        }
    }

    private fun bigAction(
        title: String,
        accent: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text =
                title
            textSize =
                11f
            isAllCaps =
                false
            setTextColor(
                white
            )
            background =
                rounded(
                    0xFF111A1F.toInt(),
                    accent,
                    17
                )
            setOnClickListener {
                action.invoke()
            }
            layoutParams =
                full(
                    dp(54),
                    0,
                    7
                )
        }
    }

    private fun navButton(
        title: String,
        accent: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text =
                title
            textSize =
                9f
            isAllCaps =
                false
            setTextColor(
                accent
            )
            background =
                rounded(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                    12
                )
            setOnClickListener {
                action.invoke()
            }
            setPadding(
                dp(1),
                0,
                dp(1),
                0
            )
        }
    }

    private fun editorField(
        hintValue: String,
        initial: String
    ): EditText {
        return EditText(this).apply {
            hint =
                hintValue
            setHintTextColor(
                0xFF73808A.toInt()
            )
            setTextColor(
                white
            )
            textSize =
                15f
            setText(
                initial
            )
            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )
            isSingleLine =
                true
            background =
                rounded(
                    card,
                    0xFF34434B.toInt(),
                    14
                )
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

    private fun full(
        height: Int,
        top: Int,
        bottom: Int
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        ).apply {
            topMargin =
                dp(top)
            bottomMargin =
                dp(bottom)
        }
    }

    private fun navWeight():
        LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            dp(52),
            1f
        )
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
}
