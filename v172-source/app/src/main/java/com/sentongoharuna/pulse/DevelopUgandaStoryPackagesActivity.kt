package com.sentongoharuna.pulse

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class DevelopUgandaStoryPackagesActivity : AppCompatActivity() {

    private lateinit var host: LinearLayout

    private val ink = 0xFF031829.toInt()
    private val panel = 0xFF082236.toInt()
    private val white = 0xFFF1F3F8.toInt()
    private val muted = 0xFFAEB7C7.toInt()
    private val green = 0xFF91B6A0.toInt()
    private val cyan = 0xFF73B7D9.toInt()
    private val gold = 0xFFAEBDEB.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshPackages()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ink)
            setPadding(dp(12), dp(16), dp(12), dp(12))
        }

        root.addView(
            label(
                "develop.uganda • STORY PACKAGES V226",
                18f,
                gold,
                true
            )
        )

        root.addView(
            label(
                "ORIGINAL • SOCIAL COPY WHEN AVAILABLE • THUMBNAIL • MANIFEST • INTEGRITY • CAPTION • TRANSCRIPT DRAFT",
                8f,
                muted,
                true
            ).apply {
                setPadding(0, dp(3), 0, dp(8))
            }
        )

        val refresh = actionButton(
            "REFRESH PACKAGES",
            cyan
        ) {
            refreshPackages()
        }

        root.addView(
            refresh,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            )
        )

        val scroll = ScrollView(this)
        host = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(20))
        }
        scroll.addView(host)

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

    private fun refreshPackages() {
        host.removeAllViews()

        val packages = DevelopUgandaStoryPackager.listRegistry(this)

        if (packages.isEmpty()) {
            host.addView(
                cardText(
                    "NO STORY PACKAGES YET",
                    "Record a successful REPORT or LIVE clip. V226 will create a package automatically after CameraX Finalize."
                )
            )
            return
        }

        packages.forEach { entry ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = rounded(panel, cyan)
            }

            card.addView(
                label(
                    entry.title.ifBlank { entry.packageId },
                    12f,
                    white,
                    true
                )
            )

            card.addView(
                label(
                    "${entry.camera}\nPACKAGE • ${entry.packageId}\n${entry.state}\n${entry.createdUtc}",
                    8f,
                    muted,
                    false
                ).apply {
                    setPadding(0, dp(3), 0, dp(7))
                }
            )

            val row1 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            row1.addView(
                smallButton("PLAY ORIGINAL", green) {
                    openPackageFile(entry.packageId, "ORIGINAL_VIDEO.mp4", "video/mp4")
                },
                weight()
            )

            row1.addView(
                smallButton("SHARE ORIGINAL", cyan) {
                    sharePackageFile(entry.packageId, "ORIGINAL_VIDEO.mp4", "video/mp4")
                },
                weight().apply { marginStart = dp(5) }
            )

            card.addView(row1)

            val row2 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            row2.addView(
                smallButton("CAPTION", gold) {
                    sharePackageFile(entry.packageId, "CAPTION_DRAFT.txt", "text/plain")
                },
                weight()
            )

            row2.addView(
                smallButton("MANIFEST", cyan) {
                    sharePackageFile(entry.packageId, "MANIFEST.json", "application/json")
                },
                weight().apply { marginStart = dp(5) }
            )

            card.addView(
                row2,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
                ).apply {
                    topMargin = dp(5)
                }
            )

            val transcribe = actionButton(
                if (Build.VERSION.SDK_INT >= 33) {
                    "TRANSCRIBE SAVED VIDEO • ON-DEVICE IF AVAILABLE"
                } else {
                    "TRANSCRIPT REQUIRES ANDROID 13+"
                },
                green
            ) {
                if (Build.VERSION.SDK_INT < 33) {
                    toast("Android 13 / API 33+ required")
                } else {
                    DevelopUgandaStoryPackager.requestTranscript(
                        this,
                        entry.packageId
                    )
                    toast("Transcript request started • review the draft when ready")
                }
            }

            transcribe.isEnabled = Build.VERSION.SDK_INT >= 33

            card.addView(
                transcribe,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(44)
                ).apply {
                    topMargin = dp(5)
                }
            )

            val transcriptStatus = DevelopUgandaStoryPackager.findPackageFile(
                this,
                entry.packageId,
                "TRANSCRIPT_STATUS.txt"
            )?.let {
                DevelopUgandaStoryPackager.readText(this, it)
            }

            if (!transcriptStatus.isNullOrBlank()) {
                card.addView(
                    label(
                        transcriptStatus,
                        7.5f,
                        muted,
                        false
                    ).apply {
                        setPadding(0, dp(6), 0, 0)
                    }
                )
            }

            host.addView(
                card,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            )
        }
    }

    private fun openPackageFile(
        packageId: String,
        displayName: String,
        mime: String
    ) {
        val uri = DevelopUgandaStoryPackager.findPackageFile(
            this,
            packageId,
            displayName
        ) ?: run {
            toast("File not available in this package")
            return
        }

        if (uri.scheme == "file") {
            toast("Open/share from Story Packages requires Android 10+; the package still exists in the app external Downloads folder")
            return
        }

        try {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        } catch (_: Exception) {
            toast("No app is available to open this file")
        }
    }

    private fun sharePackageFile(
        packageId: String,
        displayName: String,
        mime: String
    ) {
        val uri = DevelopUgandaStoryPackager.findPackageFile(
            this,
            packageId,
            displayName
        ) ?: run {
            toast("File not available in this package")
            return
        }

        if (uri.scheme == "file") {
            toast("Open/share from Story Packages requires Android 10+; the package still exists in the app external Downloads folder")
            return
        }

        try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = mime
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share $displayName"
                )
            )
        } catch (_: Exception) {
            toast("Share is not available")
        }
    }

    private fun cardText(title: String, body: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(panel, cyan)
            addView(label(title, 12f, white, true))
            addView(
                label(body, 8.5f, muted, false).apply {
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }

    private fun actionButton(
        value: String,
        accent: Int,
        action: () -> Unit
    ): Button =
        Button(this).apply {
            text = value
            textSize = 8f
            isAllCaps = false
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(0xFF092236.toInt(), accent)
            setOnClickListener { action.invoke() }
        }

    private fun smallButton(
        value: String,
        accent: Int,
        action: () -> Unit
    ): Button = actionButton(value, accent, action)

    private fun label(
        value: String,
        sp: Float,
        color: Int,
        bold: Boolean
    ): TextView =
        TextView(this).apply {
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

    private fun weight(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(42), 1f)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun toast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    }
}
