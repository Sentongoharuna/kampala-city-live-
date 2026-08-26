package com.sentongoharuna.pulse

import android.content.ContentUris
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
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.PickVisualMediaRequest
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
    private lateinit var sourceView: TextView
    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar
    private lateinit var startLabel: TextView
    private lateinit var endLabel: TextView
    private lateinit var previewCutButton: Button

    private var sourceUri: Uri? = null
    private var exportedUri: Uri? = null
    private var durationMs = 0L
    private var prepared = false
    private var previewingCut = false

    private val galleryPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) loadVideo(uri, "GALLERY")
        }

    private val filePicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                loadVideo(uri, "FILES")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        val directUri =
            intent.getStringExtra("develop_uganda_edit_uri")
                ?.takeIf { it.isNotBlank() }
                ?.let { Uri.parse(it) }

        if (directUri != null) {
            loadVideo(directUri, "RECENT CLIP")
        } else {
            statusView.text =
                "Choose GALLERY, FILES or LAST CLIP • selected video will appear here"
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundColor(0xFF031829.toInt())
        }

        root.addView(label("develop.uganda  EDITOR V218", 21f, 0xFFAEBDEB.toInt(), true))
        root.addView(
            label(
                "EDITOR PICKUP PRO • CAMERA CORE V217 UNCHANGED",
                9.5f,
                0xFF91B6A0.toInt(),
                true
            )
        )

        val pickRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        pickRow.addView(action("GALLERY", 0xFF73B7D9.toInt()) {
            galleryPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        }, weight())

        pickRow.addView(action("FILES", 0xFFA793D8.toInt()) {
            filePicker.launch(arrayOf("video/*"))
        }, weight())

        pickRow.addView(action("LAST CLIP", 0xFF91B6A0.toInt()) {
            loadLatestDevelopUgandaClip()
        }, weight())

        root.addView(pickRow, LinearLayout.LayoutParams(-1, dp(48)))

        sourceView = label("NO CLIP LOADED", 9.5f, 0xFFAEB7C7.toInt(), true)
        sourceView.setPadding(0, dp(5), 0, dp(5))
        root.addView(sourceView)

        videoView = VideoView(this).apply {
            setBackgroundColor(Color.BLACK)
            val controller = MediaController(this@DevelopUgandaEditorActivity)
            controller.setAnchorView(this)
            setMediaController(controller)
        }

        root.addView(
            videoView,
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
        )

        statusView = label(
            "Choose a clip • preview • set IN/OUT • save cut",
            9.5f,
            0xFFB7C3C9.toInt(),
            false
        )
        root.addView(statusView)

        startLabel = label("IN 00:00", 10f, 0xFF73B7D9.toInt(), true)
        root.addView(startLabel)
        startSeek = SeekBar(this).apply { max = 1000; progress = 0 }
        root.addView(startSeek)

        endLabel = label("OUT 00:00", 10f, 0xFF91B6A0.toInt(), true)
        root.addView(endLabel)
        endSeek = SeekBar(this).apply { max = 1000; progress = 1000 }
        root.addView(endSeek)

        startSeek.setOnSeekBarChangeListener(seekListener { p ->
            if (p >= endSeek.progress) {
                startSeek.progress = (endSeek.progress - 1).coerceAtLeast(0)
            }
            updateTrimLabels()
            seekPreview(currentStartMs())
        })

        endSeek.setOnSeekBarChangeListener(seekListener { p ->
            if (p <= startSeek.progress) {
                endSeek.progress = (startSeek.progress + 1).coerceAtMost(1000)
            }
            updateTrimLabels()
            seekPreview(currentEndMs())
        })

        val markRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        markRow.addView(action("SET IN", 0xFF73B7D9.toInt()) { markIn() }, weight())
        markRow.addView(action("SET OUT", 0xFF91B6A0.toInt()) { markOut() }, weight())
        markRow.addView(action("RESET", 0xFFAEB7C7.toInt()) { resetCut() }, weight())
        root.addView(markRow, LinearLayout.LayoutParams(-1, dp(46)))

        val editRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        previewCutButton = action("PREVIEW CUT", 0xFFAEBDEB.toInt()) { previewCut() }
        editRow.addView(previewCutButton, weight())

        editRow.addView(action("SAVE CUT", 0xFF91B6A0.toInt()) {
            exportCut(includeAudio = true)
        }, weight())

        editRow.addView(action("MUTE + SAVE", 0xFFD0B06F.toInt()) {
            exportCut(includeAudio = false)
        }, weight())

        root.addView(editRow, LinearLayout.LayoutParams(-1, dp(50)))

        val outputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        outputRow.addView(action("PLAY SAVED", 0xFF73B7D9.toInt()) { playSaved() }, weight())
        outputRow.addView(action("SHARE", 0xFFC76D73.toInt()) { shareBestAvailable() }, weight())
        outputRow.addView(action("LOAD SAVED", 0xFFA793D8.toInt()) {
            val uri = exportedUri
            if (uri == null) toast("Save a cut first") else loadVideo(uri, "SAVED EDIT")
        }, weight())

        root.addView(outputRow, LinearLayout.LayoutParams(-1, dp(46)))
        setContentView(root)
    }

    private fun loadVideo(uri: Uri, source: String) {
        stopCutPreview()
        sourceUri = uri
        exportedUri = null
        prepared = false
        durationMs = 0L
        startSeek.progress = 0
        endSeek.progress = 1000
        sourceView.text = "LOADING • $source • ${displayName(uri)}"
        statusView.text = "Opening video…"

        videoView.setOnPreparedListener { player ->
            prepared = true
            durationMs = player.duration.toLong().coerceAtLeast(0L)
            if (durationMs <= 0L) durationMs = metadataDuration(uri)
            updateTrimLabels()
            sourceView.text = "READY • $source • ${displayName(uri)}"
            val details = videoDetails(uri)
            statusView.text =
                "READY • ${formatTime(durationMs)}" +
                    (if (details.isNotBlank()) " • $details" else "") +
                    " • set IN/OUT then SAVE CUT"
            try { videoView.seekTo(1) } catch (_: Exception) {}
        }

        videoView.setOnErrorListener { _, what, extra ->
            prepared = false
            statusView.text =
                "Preview failed • code $what/$extra • try FILES or another clip"
            toast("Video preview failed")
            true
        }

        try {
            videoView.setVideoURI(uri)
            videoView.requestFocus()
        } catch (e: Exception) {
            prepared = false
            statusView.text = "Open failed • ${e.message ?: "unsupported video"}"
            toast("Could not open video")
        }
    }

    private fun loadLatestDevelopUgandaClip() {
        statusView.text = "Looking for latest develop.uganda clip…"

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED
                ),
                "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("DEVELOP_UGANDA_%"),
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id =
                        cursor.getLong(
                            cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                        )
                    loadVideo(
                        ContentUris.withAppendedId(collection, id),
                        "LAST CLIP"
                    )
                    return
                }
            }
            statusView.text =
                "No accessible develop.uganda clip found • use GALLERY"
        } catch (_: Exception) {
            statusView.text =
                "Automatic clip lookup blocked by Android • use GALLERY or FILES"
        }
    }

    private fun markIn() {
        if (!requirePrepared()) return
        startSeek.progress =
            positionToProgress(videoView.currentPosition.toLong())
                .coerceAtMost(endSeek.progress - 1)
                .coerceAtLeast(0)
        updateTrimLabels()
    }

    private fun markOut() {
        if (!requirePrepared()) return
        endSeek.progress =
            positionToProgress(videoView.currentPosition.toLong())
                .coerceAtLeast(startSeek.progress + 1)
                .coerceAtMost(1000)
        updateTrimLabels()
    }

    private fun resetCut() {
        stopCutPreview()
        startSeek.progress = 0
        endSeek.progress = 1000
        updateTrimLabels()
        seekPreview(0L)
        statusView.text = "Cut reset • full clip selected"
    }

    private fun previewCut() {
        if (!requirePrepared()) return

        if (previewingCut) {
            stopCutPreview()
            videoView.pause()
            statusView.text = "Cut preview stopped"
            return
        }

        val startMs = currentStartMs()
        val endMs = currentEndMs()
        if (endMs <= startMs) {
            toast("Choose a valid cut")
            return
        }

        previewingCut = true
        previewCutButton.text = "STOP PREVIEW"
        videoView.seekTo(startMs.toInt())
        videoView.start()
        statusView.text =
            "Previewing cut • ${formatTime(startMs)} → ${formatTime(endMs)}"

        val guard = object : Runnable {
            override fun run() {
                if (!previewingCut) return

                if (
                    !videoView.isPlaying ||
                    videoView.currentPosition.toLong() >= endMs
                ) {
                    videoView.pause()
                    videoView.seekTo(startMs.toInt())
                    stopCutPreview()
                    statusView.text = "Cut preview finished • ready to save"
                    return
                }

                videoView.postDelayed(this, 60L)
            }
        }

        videoView.postDelayed(guard, 60L)
    }

    private fun stopCutPreview() {
        previewingCut = false
        if (::previewCutButton.isInitialized) {
            previewCutButton.text = "PREVIEW CUT"
        }
    }

    private fun seekPreview(positionMs: Long) {
        if (!prepared) return
        try {
            videoView.seekTo(
                positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toInt()
            )
        } catch (_: Exception) {
        }
    }

    private fun exportCut(includeAudio: Boolean) {
        val input = sourceUri ?: run {
            toast("Choose a video first")
            return
        }

        if (!requirePrepared()) return

        val requestedStartUs = currentStartMs() * 1000L
        val requestedEndUs = currentEndMs() * 1000L

        if (requestedEndUs <= requestedStartUs) {
            toast("Choose a longer cut")
            return
        }

        stopCutPreview()

        statusView.text =
            if (includeAudio) "Saving playable cut…" else "Saving muted cut…"

        Thread {
            var output: Uri? = null

            try {
                output = createOutputVideo(muted = !includeAudio)

                remuxTrim(
                    input,
                    output,
                    requestedStartUs,
                    requestedEndUs,
                    includeAudio
                )

                finalizeOutput(output)
                exportedUri = output

                runOnUiThread {
                    statusView.text =
                        "SAVED • tap PLAY SAVED, LOAD SAVED or SHARE"
                    toast("Edited video saved")
                }
            } catch (e: Exception) {
                if (output != null) {
                    try {
                        contentResolver.delete(output, null, null)
                    } catch (_: Exception) {
                    }
                }

                runOnUiThread {
                    statusView.text =
                        "Edit failed • ${e.message ?: "unknown error"}"
                    toast("Edit failed")
                }
            }
        }.start()
    }

    private fun remuxTrim(
        input: Uri,
        output: Uri,
        requestedStartUs: Long,
        requestedEndUs: Long,
        includeAudio: Boolean
    ) {
        val safeStartUs = safeVideoStartUs(input, requestedStartUs)

        val extractor = MediaExtractor()
        extractor.setDataSource(this, input, null)

        val pfd =
            contentResolver.openFileDescriptor(output, "rw")
                ?: error("Could not open output file")

        val muxer =
            MediaMuxer(
                pfd.fileDescriptor,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )

        var muxerStarted = false

        try {
            val trackMap = HashMap<Int, Int>()
            var maxInputSize = 4 * 1024 * 1024

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime =
                    format.getString(MediaFormat.KEY_MIME) ?: ""

                val include =
                    mime.startsWith("video/") ||
                        (includeAudio && mime.startsWith("audio/"))

                if (!include) continue

                extractor.selectTrack(i)
                trackMap[i] = muxer.addTrack(format)

                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    maxInputSize =
                        maxOf(
                            maxInputSize,
                            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        )
                }
            }

            if (trackMap.isEmpty()) {
                error("No supported video/audio tracks")
            }

            val rotation = readRotation(input)
            if (rotation != 0) muxer.setOrientationHint(rotation)

            muxer.start()
            muxerStarted = true

            val buffer =
                ByteBuffer.allocateDirect(
                    maxInputSize.coerceIn(
                        1024 * 1024,
                        32 * 1024 * 1024
                    )
                )

            val info = MediaCodec.BufferInfo()

            extractor.seekTo(
                safeStartUs,
                MediaExtractor.SEEK_TO_PREVIOUS_SYNC
            )

            var writtenSamples = 0

            while (true) {
                val track = extractor.sampleTrackIndex
                if (track < 0) break

                val sampleUs = extractor.sampleTime
                if (sampleUs < 0L || sampleUs > requestedEndUs) break

                val outputTrack = trackMap[track]

                if (outputTrack != null && sampleUs >= safeStartUs) {
                    buffer.clear()

                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break

                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs =
                        (sampleUs - safeStartUs).coerceAtLeast(0L)
                    info.flags = extractor.sampleFlags

                    muxer.writeSampleData(
                        outputTrack,
                        buffer,
                        info
                    )

                    writtenSamples++
                }

                if (!extractor.advance()) break
            }

            if (writtenSamples <= 0) {
                error("No samples were written")
            }
        } finally {
            try {
                if (muxerStarted) muxer.stop()
            } catch (_: Exception) {
            }
            try { muxer.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            try { pfd.close() } catch (_: Exception) {}
        }
    }

    private fun safeVideoStartUs(
        uri: Uri,
        requestedStartUs: Long
    ): Long {
        val extractor = MediaExtractor()

        return try {
            extractor.setDataSource(this, uri, null)

            var videoTrack = -1

            for (i in 0 until extractor.trackCount) {
                val mime =
                    extractor.getTrackFormat(i)
                        .getString(MediaFormat.KEY_MIME)
                        ?: ""

                if (mime.startsWith("video/")) {
                    videoTrack = i
                    break
                }
            }

            if (videoTrack < 0) {
                requestedStartUs
            } else {
                extractor.selectTrack(videoTrack)

                extractor.seekTo(
                    requestedStartUs,
                    MediaExtractor.SEEK_TO_PREVIOUS_SYNC
                )

                extractor.sampleTime
                    .takeIf { it >= 0L }
                    ?: requestedStartUs
            }
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    private fun createOutputVideo(muted: Boolean): Uri {
        val stamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date())

        val name =
            "DEVELOP_UGANDA_EDIT_V218_" +
                (if (muted) "MUTED_" else "") +
                stamp +
                ".mp4"

        val values =
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "Movies/develop.uganda/Edited"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

        return contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Could not create output video")
    }

    private fun finalizeOutput(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                },
                null,
                null
            )
        }
    }

    private fun playSaved() {
        val uri = exportedUri ?: run {
            toast("Save a cut first")
            return
        }

        try {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        } catch (_: Exception) {
            loadVideo(uri, "SAVED EDIT")
        }
    }

    private fun shareBestAvailable() {
        val uri = exportedUri ?: sourceUri ?: run {
            toast("Choose or save a video first")
            return
        }

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Share develop.uganda video"
            )
        )
    }

    private fun currentStartMs(): Long =
        durationMs * startSeek.progress / 1000L

    private fun currentEndMs(): Long =
        durationMs * endSeek.progress / 1000L

    private fun positionToProgress(positionMs: Long): Int {
        if (durationMs <= 0L) return 0

        return (
            positionMs * 1000L / durationMs
            )
            .toInt()
            .coerceIn(0, 1000)
    }

    private fun updateTrimLabels() {
        startLabel.text =
            "IN ${formatTime(currentStartMs())}"
        endLabel.text =
            "OUT ${formatTime(currentEndMs())}"
    }

    private fun requirePrepared(): Boolean {
        if (sourceUri == null) {
            toast("Choose a video first")
            return false
        }

        if (!prepared || durationMs <= 0L) {
            toast("Wait for video to finish loading")
            return false
        }

        return true
    }

    private fun metadataDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
                ?.toLongOrNull()
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun videoDetails(uri: Uri): String {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(this, uri)

            val width =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )
            val height =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )
            val rotation =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                )

            buildString {
                if (!width.isNullOrBlank() && !height.isNullOrBlank()) {
                    append("$width×$height")
                }

                if (!rotation.isNullOrBlank() && rotation != "0") {
                    if (isNotEmpty()) append(" • ")
                    append("ROT ${rotation}°")
                }

                val mime = contentResolver.getType(uri)

                if (!mime.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(mime)
                }
            }
        } catch (_: Exception) {
            ""
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun readRotation(uri: Uri): Int {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(this, uri)
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )
                ?.toIntOrNull()
                ?: 0
        } catch (_: Exception) {
            0
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun displayName(uri: Uri): String =
        try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: (uri.lastPathSegment ?: "video")
        } catch (_: Exception) {
            uri.lastPathSegment ?: "video"
        }

    private fun seekListener(
        change: (Int) -> Unit
    ): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    stopCutPreview()
                    change(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    private fun action(
        value: String,
        accent: Int,
        click: () -> Unit
    ): Button =
        Button(this).apply {
            text = value
            textSize = 9f
            isAllCaps = false
            setTextColor(Color.WHITE)

            background =
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(15).toFloat()
                    setColor(0xFF092236.toInt())
                    setStroke(dp(1), accent)
                }

            setOnClickListener { click.invoke() }
        }

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
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    if (bold) Typeface.BOLD else Typeface.NORMAL
                )
        }

    private fun weight(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        ).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

    private fun formatTime(ms: Long): String {
        val totalSeconds =
            (ms / 1000L)
                .coerceAtLeast(0L)

        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0L) {
            String.format(
                Locale.US,
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        } else {
            String.format(
                Locale.US,
                "%02d:%02d",
                minutes,
                seconds
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density)
            .roundToInt()

    private fun toast(value: String) {
        Toast.makeText(
            this,
            value,
            Toast.LENGTH_SHORT
        ).show()
    }
}
