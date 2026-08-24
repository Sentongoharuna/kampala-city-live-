package com.sentongoharuna.pulse

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class DevelopUgandaEditorActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var statusView: TextView
    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar
    private lateinit var startLabel: TextView
    private lateinit var endLabel: TextView

    private var sourceUri: Uri? = null
    private var durationMs: Long = 0L
    private var exportedUri: Uri? = null

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                } catch (_: Exception) {
                }
                loadVideo(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        intent.getStringExtra(
            "develop_uganda_edit_uri"
        )
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                loadVideo(
                    Uri.parse(
                        it
                    )
                )
            }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(18))
            setBackgroundColor(0xFF070B0E.toInt())
        }

        root.addView(
            text(
                "develop.uganda  EDIT DESK",
                22f,
                0xFFFFC21A.toInt(),
                true
            )
        )

        root.addView(
            text(
                "SOCIAL CUT • REPORT PACKAGE • SHARE",
                12f,
                0xFFB7C3C9.toInt(),
                false
            ).apply {
                setPadding(0, dp(4), 0, dp(12))
            }
        )

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        root.addView(
            videoView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        statusView =
            text(
                "Choose a clip • preview • trim • save • publish",
                12f,
                0xFFB7C3C9.toInt(),
                false
            ).apply {
                setPadding(0, dp(8), 0, dp(8))
            }
        root.addView(statusView)

        startLabel = text(
            "START 00:00",
            11f,
            0xFF7FE8FF.toInt(),
            true
        )
        root.addView(startLabel)

        startSeek = SeekBar(this).apply {
            max = 1000
            progress = 0
        }
        root.addView(startSeek)

        endLabel = text(
            "END 00:00",
            11f,
            0xFF76E39A.toInt(),
            true
        )
        root.addView(endLabel)

        endSeek = SeekBar(this).apply {
            max = 1000
            progress = 1000
        }
        root.addView(endSeek)

        startSeek.setOnSeekBarChangeListener(
            seekListener {
                if (it >= endSeek.progress) {
                    startSeek.progress =
                        (endSeek.progress - 1)
                            .coerceAtLeast(0)
                }
                updateTrimLabels()
                previewAtStart()
            }
        )

        endSeek.setOnSeekBarChangeListener(
            seekListener {
                if (it <= startSeek.progress) {
                    endSeek.progress =
                        (startSeek.progress + 1)
                            .coerceAtMost(1000)
                }
                updateTrimLabels()
            }
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        controls.addView(
            action("OPEN CLIP", 0xFFFFC21A.toInt()) {
                picker.launch(
                    arrayOf("video/*")
                )
            },
            weight()
        )

        controls.addView(
            action("PREVIEW", 0xFF7FE8FF.toInt()) {
                if (sourceUri == null) {
                    toast("Choose a video first")
                } else {
                    if (videoView.isPlaying) {
                        videoView.pause()
                    } else {
                        videoView.start()
                    }
                }
            },
            weight()
        )

        controls.addView(
            action("TRIM + SAVE", 0xFF76E39A.toInt()) {
                exportTrim()
            },
            weight()
        )

        root.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            )
        )

        val editTools =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER
            }

        editTools.addView(
            action(
                "MUTE + SAVE",
                0xFFFFC21A.toInt()
            ) {
                exportMutedTrim()
            },
            weight()
        )

        editTools.addView(
            action(
                "PUBLISH / SHARE",
                0xFFFF5A52.toInt()
            ) {
                shareExport()
            },
            weight()
        )

        root.addView(
            editTools,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        setContentView(root)
    }

    private fun loadVideo(uri: Uri) {
        sourceUri = uri
        exportedUri = null

        val retriever =
            MediaMetadataRetriever()

        try {
            retriever.setDataSource(
                this,
                uri
            )
            durationMs =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )
                    ?.toLongOrNull()
                    ?: 0L
        } catch (e: Exception) {
            durationMs = 0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener {
            durationMs =
                if (durationMs > 0L) {
                    durationMs
                } else {
                    it.duration.toLong()
                }
            updateTrimLabels()
            statusView.text =
                "Ready • ${formatTime(durationMs)} • lossless MP4 trim"
        }
        videoView.seekTo(1)
    }

    private fun updateTrimLabels() {
        val startMs =
            durationMs *
                startSeek.progress /
                1000L
        val endMs =
            durationMs *
                endSeek.progress /
                1000L

        startLabel.text =
            "START ${formatTime(startMs)}"
        endLabel.text =
            "END ${formatTime(endMs)}"
    }

    private fun previewAtStart() {
        val uri = sourceUri ?: return
        val startMs =
            durationMs *
                startSeek.progress /
                1000L
        if (videoView.tag != uri) {
            videoView.tag = uri
        }
        videoView.seekTo(
            startMs.toInt()
        )
    }

    private fun exportTrim() {
        val input = sourceUri ?: run {
            toast("Choose a video first")
            return
        }

        if (durationMs <= 0L) {
            toast("Video duration is unavailable")
            return
        }

        val startUs =
            durationMs *
                startSeek.progress *
                1000L /
                1000L

        val endUs =
            durationMs *
                endSeek.progress *
                1000L /
                1000L

        if (endUs <= startUs) {
            toast("Choose a longer trim range")
            return
        }

        statusView.text =
            "Saving trimmed report…"

        Thread {
            try {
                val output =
                    createOutputVideo()

                trimMp4(
                    input,
                    output,
                    startUs,
                    endUs,
                    includeAudio = true
                )

                exportedUri = output

                runOnUiThread {
                    statusView.text =
                        "Saved • ${formatTime((endUs - startUs) / 1000L)} • develop.uganda"
                    toast("Trimmed video saved")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusView.text =
                        "Trim failed: ${e.message ?: "unknown error"}"
                    toast("Trim failed")
                }
            }
        }.start()
    }

    private fun exportMutedTrim() {
        val input =
            sourceUri ?: run {
                toast(
                    "Choose a video first"
                )
                return
            }

        if (
            durationMs <=
            0L
        ) {
            toast(
                "Video duration is unavailable"
            )
            return
        }

        val startUs =
            durationMs *
                startSeek.progress *
                1000L /
                1000L

        val endUs =
            durationMs *
                endSeek.progress *
                1000L /
                1000L

        if (
            endUs <=
            startUs
        ) {
            toast(
                "Choose a longer trim range"
            )
            return
        }

        statusView.text =
            "Saving muted social cut…"

        Thread {
            try {
                val output =
                    createOutputVideo()

                trimMp4(
                    input,
                    output,
                    startUs,
                    endUs,
                    includeAudio = false
                )

                exportedUri =
                    output

                runOnUiThread {
                    statusView.text =
                        "Muted cut saved • ${formatTime((endUs - startUs) / 1000L)}"

                    toast(
                        "Muted video saved"
                    )
                }
            } catch (
                e: Exception
            ) {
                runOnUiThread {
                    statusView.text =
                        "Mute export failed: ${e.message ?: "unknown error"}"

                    toast(
                        "Mute export failed"
                    )
                }
            }
        }.start()
    }

    private fun createOutputVideo(): Uri {
        val name =
            "DEVELOP_UGANDA_EDIT_" +
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date()) +
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
                if (Build.VERSION.SDK_INT >= 29) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/develop.uganda/Edited"
                    )
                }
            }

        return contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error(
            "Could not create output video"
        )
    }

    private fun trimMp4(
        input: Uri,
        output: Uri,
        startUs: Long,
        endUs: Long,
        includeAudio: Boolean
    ) {
        val extractor =
            MediaExtractor()

        extractor.setDataSource(
            this,
            input,
            null
        )

        val pfd =
            contentResolver
                .openFileDescriptor(
                    output,
                    "rw"
                )
                ?: error(
                    "Could not open output file"
                )

        val muxer =
            MediaMuxer(
                pfd.fileDescriptor,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

        val trackMap =
            HashMap<Int, Int>()

        var maxInputSize = 1024 * 1024

        for (i in 0 until extractor.trackCount) {
            val format =
                extractor.getTrackFormat(i)

            val mime =
                format.getString(
                    MediaFormat.KEY_MIME
                ) ?: ""

            if (
                mime.startsWith(
                    "video/"
                ) ||
                (
                    includeAudio &&
                    mime.startsWith(
                        "audio/"
                    )
                    )
            ) {
                extractor.selectTrack(i)
                trackMap[i] =
                    muxer.addTrack(format)

                if (
                    format.containsKey(
                        MediaFormat.KEY_MAX_INPUT_SIZE
                    )
                ) {
                    maxInputSize =
                        maxOf(
                            maxInputSize,
                            format.getInteger(
                                MediaFormat.KEY_MAX_INPUT_SIZE
                            )
                        )
                }
            }
        }

        val rotation =
            readRotation(input)

        if (rotation != 0) {
            muxer.setOrientationHint(
                rotation
            )
        }

        muxer.start()

        val buffer =
            ByteBuffer.allocateDirect(
                maxInputSize.coerceAtMost(
                    8 * 1024 * 1024
                )
            )

        val info =
            MediaCodec.BufferInfo()

        extractor.seekTo(
            startUs,
            MediaExtractor.SEEK_TO_PREVIOUS_SYNC
        )

        var firstWrittenUs = -1L

        while (true) {
            val track =
                extractor.sampleTrackIndex

            if (track < 0) {
                break
            }

            val sampleUs =
                extractor.sampleTime

            if (sampleUs < 0L || sampleUs > endUs) {
                break
            }

            val outputTrack =
                trackMap[track]

            if (
                outputTrack != null &&
                sampleUs >= startUs
            ) {
                buffer.clear()

                val size =
                    extractor.readSampleData(
                        buffer,
                        0
                    )

                if (size < 0) {
                    break
                }

                if (firstWrittenUs < 0L) {
                    firstWrittenUs = sampleUs
                }

                info.offset = 0
                info.size = size
                info.presentationTimeUs =
                    sampleUs -
                        firstWrittenUs
                info.flags =
                    extractor.sampleFlags

                muxer.writeSampleData(
                    outputTrack,
                    buffer,
                    info
                )
            }

            if (!extractor.advance()) {
                break
            }
        }

        try {
            muxer.stop()
        } finally {
            muxer.release()
            extractor.release()
            pfd.close()
        }
    }

    private fun readRotation(
        uri: Uri
    ): Int {
        val r =
            MediaMetadataRetriever()

        return try {
            r.setDataSource(
                this,
                uri
            )
            r.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )
                ?.toIntOrNull()
                ?: 0
        } catch (_: Exception) {
            0
        } finally {
            try {
                r.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun shareExport() {
        val uri =
            exportedUri ?: run {
                toast("Trim and save a video first")
                return
            }

        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                send,
                "Publish develop.uganda report"
            )
        )
    }

    private fun seekListener(
        change: (Int) -> Unit
    ): SeekBar.OnSeekBarChangeListener {
        return object :
            SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    change(progress)
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
    }

    private fun action(
        value: String,
        accent: Int,
        click: () -> Unit
    ): Button {
        return Button(this).apply {
            text = value
            textSize = 10f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background =
                GradientDrawable().apply {
                    shape =
                        GradientDrawable.RECTANGLE
                    cornerRadius =
                        dp(16).toFloat()
                    setColor(
                        0xFF121A1F.toInt()
                    )
                    setStroke(
                        dp(1),
                        accent
                    )
                }
            setOnClickListener {
                click.invoke()
            }
        }
    }

    private fun text(
        value: String,
        sp: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = sp
            setTextColor(color)
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

    private fun weight():
        LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        ).apply {
            marginStart = dp(4)
            marginEnd = dp(4)
        }
    }

    private fun formatTime(
        ms: Long
    ): String {
        val total =
            (ms / 1000L)
                .coerceAtLeast(0L)

        return String.format(
            Locale.US,
            "%02d:%02d",
            total / 60L,
            total % 60L
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
}
