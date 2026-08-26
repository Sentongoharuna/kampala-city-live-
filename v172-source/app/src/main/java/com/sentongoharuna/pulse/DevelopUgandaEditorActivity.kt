package com.sentongoharuna.pulse

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class DevelopUgandaEditorActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var statusView: TextView
    private lateinit var sourceView: TextView
    private lateinit var startSeek: SeekBar
    private lateinit var endSeek: SeekBar
    private lateinit var startLabel: TextView
    private lateinit var endLabel: TextView
    private lateinit var previewCutButton: Button

    private var player: ExoPlayer? = null
    private var transformer: Transformer? = null

    private var sourceUri: Uri? = null
    private var exportedUri: Uri? = null
    private var socialMasterUri: Uri? = null
    private var socialMasterLabel: String = ""
    private var durationMs = 0L
    private var previewingCut = false
    private var firstFrameRendered = false

    private val galleryPicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                loadVideo(uri, "GALLERY")
            }
        }

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
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

        window.statusBarColor = 0xFF031829.toInt()
        window.navigationBarColor = 0xFF031829.toInt()

        buildUi()
        buildPlayer()

        val directUri =
            intent.getStringExtra("develop_uganda_edit_uri")
                ?.takeIf { it.isNotBlank() }
                ?.let { Uri.parse(it) }

        if (directUri != null) {
            loadVideo(directUri, "RECENT CLIP")
        } else {
            statusView.text =
                "Choose a clip • edit it or create a TikTok / Reels master • V221"
        }
    }

    override fun onDestroy() {
        transformer?.cancel()
        transformer = null

        playerView.player = null
        player?.release()
        player = null

        super.onDestroy()
    }

    private fun buildUi() {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFF031829.toInt())
            }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                dp(12),
                bars.top + dp(8),
                dp(12),
                bars.bottom + dp(8)
            )

            insets
        }

        root.addView(
            label(
                "develop.uganda  EDITOR V221",
                21f,
                0xFFAEBDEB.toInt(),
                true
            )
        )

        root.addView(
            label(
                "MEDIA3 EDITOR + SOCIAL UPLOAD MASTER • ORIGINAL STAYS UNTOUCHED",
                9.5f,
                0xFF91B6A0.toInt(),
                true
            ).apply {
                setPadding(0, dp(2), 0, dp(7))
            }
        )

        val pickerRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

        pickerRow.addView(
            action("GALLERY", 0xFF73B7D9.toInt()) {
                galleryPicker.launch("video/*")
            },
            weight()
        )

        pickerRow.addView(
            action("FILES", 0xFFA793D8.toInt()) {
                filePicker.launch(arrayOf("video/*"))
            },
            weight()
        )

        pickerRow.addView(
            action("RECENT", 0xFF91B6A0.toInt()) {
                showRecentDevelopUgandaClips()
            },
            weight()
        )

        root.addView(
            pickerRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val secondPickerRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

        secondPickerRow.addView(
            action("LAST CLIP", 0xFF91B6A0.toInt()) {
                loadLatestDevelopUgandaClip()
            },
            weight()
        )

        secondPickerRow.addView(
            action("PLAY / PAUSE", 0xFF73B7D9.toInt()) {
                toggleSourcePlayback()
            },
            weight()
        )

        secondPickerRow.addView(
            action("RELOAD", 0xFFAEB7C7.toInt()) {
                sourceUri?.let {
                    loadVideo(it, "RELOAD")
                } ?: toast("Choose a video first")
            },
            weight()
        )

        root.addView(
            secondPickerRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            )
        )

        sourceView =
            label(
                "NO CLIP LOADED",
                9.5f,
                0xFFAEB7C7.toInt(),
                true
            ).apply {
                maxLines = 2
                setPadding(0, dp(5), 0, dp(5))
            }

        root.addView(sourceView)

        playerView =
            PlayerView(this).apply {
                setBackgroundColor(Color.BLACK)
                useController = true
                controllerAutoShow = true
                controllerShowTimeoutMs = 0
                resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
            }

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(4)
            }
        )

        statusView =
            label(
                "Editor ready",
                9.5f,
                0xFFB7C3C9.toInt(),
                false
            ).apply {
                maxLines = 3
                setPadding(0, dp(5), 0, dp(5))
            }

        root.addView(statusView)

        startLabel =
            label(
                "IN 00:00",
                10f,
                0xFF73B7D9.toInt(),
                true
            )
        root.addView(startLabel)

        startSeek =
            SeekBar(this).apply {
                max = 1000
                progress = 0
            }
        root.addView(startSeek)

        endLabel =
            label(
                "OUT 00:00",
                10f,
                0xFF91B6A0.toInt(),
                true
            )
        root.addView(endLabel)

        endSeek =
            SeekBar(this).apply {
                max = 1000
                progress = 1000
            }
        root.addView(endSeek)

        startSeek.setOnSeekBarChangeListener(
            seekListener { progress ->
                if (progress >= endSeek.progress) {
                    startSeek.progress =
                        (endSeek.progress - 1).coerceAtLeast(0)
                }
                updateTrimLabels()
                seekPreview(currentStartMs())
            }
        )

        endSeek.setOnSeekBarChangeListener(
            seekListener { progress ->
                if (progress <= startSeek.progress) {
                    endSeek.progress =
                        (startSeek.progress + 1).coerceAtMost(1000)
                }
                updateTrimLabels()
                seekPreview(currentEndMs())
            }
        )

        val markRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        markRow.addView(
            action("SET IN", 0xFF73B7D9.toInt()) {
                markIn()
            },
            weight()
        )

        markRow.addView(
            action("SET OUT", 0xFF91B6A0.toInt()) {
                markOut()
            },
            weight()
        )

        markRow.addView(
            action("RESET CUT", 0xFFAEB7C7.toInt()) {
                resetCut()
            },
            weight()
        )

        root.addView(
            markRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            )
        )

        val editRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        previewCutButton =
            action(
                "PREVIEW CUT",
                0xFFAEBDEB.toInt()
            ) {
                previewCut()
            }

        editRow.addView(previewCutButton, weight())

        editRow.addView(
            action("SAVE CUT", 0xFF91B6A0.toInt()) {
                exportWithMedia3(includeAudio = true)
            },
            weight()
        )

        editRow.addView(
            action("MUTE + SAVE", 0xFFD0B06F.toInt()) {
                exportWithMedia3(includeAudio = false)
            },
            weight()
        )

        root.addView(
            editRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        val outputRow =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        outputRow.addView(
            action("PLAY SAVED", 0xFF73B7D9.toInt()) {
                playSaved()
            },
            weight()
        )

        outputRow.addView(
            action("SHARE", 0xFFC76D73.toInt()) {
                shareBestAvailable()
            },
            weight()
        )

        outputRow.addView(
            action("LOAD SAVED", 0xFFA793D8.toInt()) {
                val uri = exportedUri
                if (uri == null) {
                    toast("Save a cut first")
                } else {
                    loadVideo(
                        uri,
                        "SAVED EDIT",
                        keepExport = true
                    )
                }
            },
            weight()
        )

        root.addView(
            outputRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            )
        )

        root.addView(
            label(
                "SOCIAL UPLOAD MASTER • 1080×1920 • H.264 • AAC • 30 FPS MAX • 2s KEYFRAMES",
                8.5f,
                0xFF91B6A0.toInt(),
                true
            ).apply {
                setPadding(
                    0,
                    dp(7),
                    0,
                    dp(3)
                )
            }
        )

        val socialRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        socialRow.addView(
            action(
                "TIKTOK MASTER",
                0xFF73B7D9.toInt()
            ) {
                exportSocialMaster(
                    platform =
                        "TIKTOK",
                    bitrate =
                        16_000_000
                )
            },
            weight()
        )

        socialRow.addView(
            action(
                "REELS MASTER",
                0xFFA793D8.toInt()
            ) {
                exportSocialMaster(
                    platform =
                        "REELS",
                    bitrate =
                        14_000_000
                )
            },
            weight()
        )

        socialRow.addView(
            action(
                "SHARE SOCIAL",
                0xFFC76D73.toInt()
            ) {
                shareSocialMaster()
            },
            weight()
        )

        root.addView(
            socialRow,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        setContentView(root)
    }

    private fun buildPlayer() {
        player =
            ExoPlayer.Builder(this)
                .build()
                .also { exo ->
                    playerView.player = exo

                    exo.addListener(
                        object : Player.Listener {
                            override fun onPlaybackStateChanged(
                                playbackState: Int
                            ) {
                                if (
                                    playbackState ==
                                    Player.STATE_READY
                                ) {
                                    val d = exo.duration

                                    durationMs =
                                        if (
                                            d != C.TIME_UNSET &&
                                            d > 0L
                                        ) {
                                            d
                                        } else {
                                            0L
                                        }

                                    updateTrimLabels()

                                    statusView.text =
                                        "READY • ${formatTime(durationMs)} • tap PLAY or set IN/OUT"
                                }
                            }

                            override fun onRenderedFirstFrame() {
                                firstFrameRendered = true

                                statusView.text =
                                    "VIDEO VISIBLE • READY • ${formatTime(durationMs)} • set IN/OUT"
                            }

                            override fun onPlayerError(
                                error: PlaybackException
                            ) {
                                statusView.text =
                                    "PLAYER ERROR • ${error.errorCodeName} • try FILES or another clip"

                                toast("Could not play this video")
                            }
                        }
                    )
                }
    }

    private fun loadVideo(
        uri: Uri,
        source: String,
        keepExport: Boolean = false
    ) {
        stopCutPreview()

        sourceUri = uri

        if (!keepExport) {
            exportedUri = null
        }

        durationMs = 0L
        firstFrameRendered = false
        startSeek.progress = 0
        endSeek.progress = 1000

        sourceView.text =
            "LOADING • $source • ${displayName(uri)}"

        statusView.text =
            "Opening with Media3…"

        val exo = player ?: return

        exo.stop()
        exo.clearMediaItems()
        exo.setMediaItem(
            MediaItem.fromUri(uri)
        )
        exo.prepare()
        exo.playWhenReady = false

        sourceView.text =
            "READY • $source • ${displayName(uri)}"

        playerView.showController()

        playerView.postDelayed(
            {
                if (
                    !firstFrameRendered &&
                    sourceUri == uri
                ) {
                    statusView.text =
                        "READY • tap PLAY • if picture stays black use FILES or RECENT"
                }
            },
            1200L
        )
    }

    private fun toggleSourcePlayback() {
        val exo = player

        if (
            sourceUri == null ||
            exo == null
        ) {
            toast("Choose a video first")
            return
        }

        if (exo.isPlaying) {
            exo.pause()
        } else {
            exo.play()
        }

        playerView.showController()
    }

    private fun showRecentDevelopUgandaClips() {
        val collection =
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val names =
            mutableListOf<String>()

        val uris =
            mutableListOf<Uri>()

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
                val idIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media._ID
                    )

                val nameIndex =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media.DISPLAY_NAME
                    )

                while (
                    cursor.moveToNext() &&
                    names.size < 12
                ) {
                    names.add(
                        cursor.getString(nameIndex)
                    )

                    uris.add(
                        ContentUris.withAppendedId(
                            collection,
                            cursor.getLong(idIndex)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }

        if (names.isEmpty()) {
            toast(
                "No accessible develop.uganda clips • use GALLERY"
            )
            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "RECENT develop.uganda VIDEOS"
            )
            .setItems(
                names.toTypedArray()
            ) { _, which ->
                loadVideo(
                    uris[which],
                    "RECENT"
                )
            }
            .setNegativeButton(
                "CANCEL",
                null
            )
            .show()
    }

    private fun loadLatestDevelopUgandaClip() {
        val collection =
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        try {
            contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.Video.Media._ID
                ),
                "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("DEVELOP_UGANDA_%"),
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    loadVideo(
                        ContentUris.withAppendedId(
                            collection,
                            cursor.getLong(0)
                        ),
                        "LAST CLIP"
                    )
                    return
                }
            }

            toast(
                "No accessible develop.uganda clip • use GALLERY"
            )
        } catch (_: Exception) {
            toast(
                "Use GALLERY or FILES to choose the video"
            )
        }
    }

    private fun markIn() {
        val exo = player ?: return

        if (!readyForEdit()) {
            return
        }

        startSeek.progress =
            positionToProgress(
                exo.currentPosition
            )
                .coerceAtMost(
                    endSeek.progress - 1
                )
                .coerceAtLeast(0)

        updateTrimLabels()
    }

    private fun markOut() {
        val exo = player ?: return

        if (!readyForEdit()) {
            return
        }

        endSeek.progress =
            positionToProgress(
                exo.currentPosition
            )
                .coerceAtLeast(
                    startSeek.progress + 1
                )
                .coerceAtMost(1000)

        updateTrimLabels()
    }

    private fun resetCut() {
        stopCutPreview()
        startSeek.progress = 0
        endSeek.progress = 1000
        updateTrimLabels()
        seekPreview(0L)
        statusView.text =
            "CUT RESET • full clip selected"
    }

    private fun previewCut() {
        val exo = player ?: return

        if (!readyForEdit()) {
            return
        }

        if (previewingCut) {
            stopCutPreview()
            exo.pause()
            statusView.text =
                "Cut preview stopped"
            return
        }

        val startMs = currentStartMs()
        val endMs = currentEndMs()

        if (endMs <= startMs) {
            toast("Choose a valid cut")
            return
        }

        previewingCut = true
        previewCutButton.text =
            "STOP PREVIEW"

        exo.seekTo(startMs)
        exo.play()

        statusView.text =
            "PREVIEW CUT • ${formatTime(startMs)} → ${formatTime(endMs)}"

        val guard =
            object : Runnable {
                override fun run() {
                    if (!previewingCut) {
                        return
                    }

                    if (
                        !exo.isPlaying ||
                        exo.currentPosition >= endMs
                    ) {
                        exo.pause()
                        exo.seekTo(startMs)
                        stopCutPreview()

                        statusView.text =
                            "CUT PREVIEW FINISHED • ready to save"
                        return
                    }

                    playerView.postDelayed(
                        this,
                        60L
                    )
                }
            }

        playerView.postDelayed(
            guard,
            60L
        )
    }

    private fun stopCutPreview() {
        previewingCut = false

        if (::previewCutButton.isInitialized) {
            previewCutButton.text =
                "PREVIEW CUT"
        }
    }

    private fun seekPreview(
        positionMs: Long
    ) {
        val exo = player ?: return

        if (durationMs <= 0L) {
            return
        }

        exo.seekTo(
            positionMs.coerceIn(
                0L,
                durationMs
            )
        )
    }

    private fun exportWithMedia3(
        includeAudio: Boolean
    ) {
        val input =
            sourceUri ?: run {
                toast("Choose a video first")
                return
            }

        if (!readyForEdit()) {
            return
        }

        val startMs = currentStartMs()
        val endMs = currentEndMs()

        if (endMs <= startMs) {
            toast("Choose a longer cut")
            return
        }

        transformer?.cancel()

        val exportDir =
            File(
                cacheDir,
                "v220_media3_exports"
            ).apply {
                mkdirs()
            }

        val temp =
            File(
                exportDir,
                "export_${System.currentTimeMillis()}.mp4"
            )

        if (temp.exists()) {
            temp.delete()
        }

        val clipped =
            MediaItem.Builder()
                .setUri(input)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()

        val edited =
            EditedMediaItem.Builder(clipped)
                .setRemoveAudio(!includeAudio)
                .build()

        statusView.text =
            if (includeAudio) {
                "MEDIA3 EXPORT • cutting video…"
            } else {
                "MEDIA3 EXPORT • cutting + removing audio…"
            }

        val listener =
            object : Transformer.Listener {
                override fun onCompleted(
                    composition: Composition,
                    result: ExportResult
                ) {
                    transformer = null

                    Thread {
                        try {
                            val uri =
                                publishTempVideo(
                                    temp,
                                    muted = !includeAudio
                                )

                            temp.delete()
                            exportedUri = uri

                            runOnUiThread {
                                statusView.text =
                                    "SAVED • Media3 MP4 • PLAY SAVED / LOAD SAVED / SHARE"

                                toast(
                                    "Edited video saved"
                                )
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                statusView.text =
                                    "PUBLISH FAILED • ${e.message ?: "unknown error"}"

                                toast(
                                    "Could not publish edited video"
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
                    transformer = null
                    temp.delete()

                    statusView.text =
                        "MEDIA3 EXPORT FAILED • ${exception.errorCodeName}"

                    toast("Edit failed")
                }
            }

        transformer =
            Transformer.Builder(this)
                .addListener(listener)
                .build()
                .also {
                    it.start(
                        edited,
                        temp.absolutePath
                    )
                }
    }


    private fun exportSocialMaster(
        platform: String,
        bitrate: Int
    ) {
        val input =
            sourceUri ?: run {
                toast(
                    "Choose a video first"
                )
                return
            }

        if (
            !readyForEdit()
        ) {
            return
        }

        val startMs =
            currentStartMs()

        val endMs =
            currentEndMs()

        if (
            endMs <=
                startMs
        ) {
            toast(
                "Choose a longer cut"
            )
            return
        }

        transformer?.cancel()

        val exportDir =
            File(
                cacheDir,
                "v221_social_exports"
            ).apply {
                mkdirs()
            }

        val temp =
            File(
                exportDir,
                "social_${platform.lowercase(Locale.US)}_${System.currentTimeMillis()}.mp4"
            )

        if (
            temp.exists()
        ) {
            temp.delete()
        }

        val clipped =
            MediaItem.Builder()
                .setUri(
                    input
                )
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(
                            startMs
                        )
                        .setEndPositionMs(
                            endMs
                        )
                        .build()
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
                    )
                )
            )

        val edited =
            EditedMediaItem.Builder(
                clipped
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
                    bitrate
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

        statusView.text =
            "$platform MASTER • preparing 1080×1920 H.264/AAC • ${bitrate / 1_000_000} Mbps target"

        val listener =
            object :
                Transformer.Listener {

                override fun onCompleted(
                    completedComposition: Composition,
                    result: ExportResult
                ) {
                    transformer =
                        null

                    Thread {
                        try {
                            val uri =
                                publishSocialMaster(
                                    temp =
                                        temp,
                                    platform =
                                        platform
                                )

                            temp.delete()

                            socialMasterUri =
                                uri

                            socialMasterLabel =
                                platform

                            runOnUiThread {
                                statusView.text =
                                    "$platform MASTER SAVED • 1080×1920 • H.264 • AAC • 30 FPS MAX • original preserved"

                                toast(
                                    "$platform social master saved"
                                )
                            }
                        } catch (
                            e: Exception
                        ) {
                            runOnUiThread {
                                statusView.text =
                                    "$platform PUBLISH FAILED • ${e.message ?: "unknown error"}"

                                toast(
                                    "Could not publish social master"
                                )
                            }
                        }
                    }.start()
                }

                override fun onError(
                    failedComposition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    transformer =
                        null

                    temp.delete()

                    statusView.text =
                        "$platform MASTER FAILED • ${exception.errorCodeName}"

                    toast(
                        "Social master export failed"
                    )
                }
            }

        transformer =
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

    private fun publishSocialMaster(
        temp: File,
        platform: String
    ): Uri {
        if (
            !temp.exists() ||
            temp.length() <=
                0L
        ) {
            error(
                "Social master output is empty"
            )
        }

        val stamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(
                Date()
            )

        val name =
            "DEVELOP_UGANDA_V221_${platform}_MASTER_" +
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
                        "Movies/develop.uganda/Social"
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
                "Could not create social master in Gallery"
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
                "Could not open social master output stream"
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

    private fun shareSocialMaster() {
        val uri =
            socialMasterUri ?: run {
                toast(
                    "Create a TikTok or Reels master first"
                )
                return
            }

        startActivity(
            Intent.createChooser(
                Intent(
                    Intent.ACTION_SEND
                ).apply {
                    type =
                        "video/mp4"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_TEXT,
                        "develop.uganda ${socialMasterLabel} master"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                },
                "Share ${socialMasterLabel} master"
            )
        )
    }

    private fun publishTempVideo(
        temp: File,
        muted: Boolean
    ): Uri {
        if (
            !temp.exists() ||
            temp.length() <= 0L
        ) {
            error(
                "Transformer output is empty"
            )
        }

        val stamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date())

        val name =
            "DEVELOP_UGANDA_EDIT_V220_" +
                (if (muted) "MUTED_" else "") +
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
                        "Movies/develop.uganda/Edited"
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
                "Could not create Gallery output"
            )

        try {
            contentResolver.openOutputStream(
                uri,
                "w"
            )?.use { output ->
                FileInputStream(temp).use { input ->
                    input.copyTo(
                        output,
                        1024 * 1024
                    )
                }
            } ?: error(
                "Could not open Gallery output stream"
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
        } catch (e: Exception) {
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

    private fun playSaved() {
        val uri =
            exportedUri ?: run {
                toast("Save a cut first")
                return
            }

        loadVideo(
            uri,
            "SAVED EDIT",
            keepExport = true
        )

        player?.play()
    }

    private fun shareBestAvailable() {
        val uri =
            exportedUri ?: sourceUri ?: run {
                toast(
                    "Choose or save a video first"
                )
                return
            }

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                },
                "Share develop.uganda video"
            )
        )
    }

    private fun readyForEdit(): Boolean {
        if (sourceUri == null) {
            toast("Choose a video first")
            return false
        }

        if (durationMs <= 0L) {
            toast(
                "Wait for video to finish loading"
            )
            return false
        }

        return true
    }

    private fun currentStartMs(): Long =
        durationMs *
            startSeek.progress /
            1000L

    private fun currentEndMs(): Long =
        durationMs *
            endSeek.progress /
            1000L

    private fun positionToProgress(
        positionMs: Long
    ): Int {
        if (durationMs <= 0L) {
            return 0
        }

        return (
            positionMs *
                1000L /
                durationMs
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

    private fun displayName(
        uri: Uri
    ): String =
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            } ?: (
                uri.lastPathSegment
                    ?: "video"
                )
        } catch (_: Exception) {
            uri.lastPathSegment
                ?: "video"
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

            override fun onStartTrackingTouch(
                seekBar: SeekBar?
            ) {
            }

            override fun onStopTrackingTouch(
                seekBar: SeekBar?
            ) {
            }
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
                    shape =
                        GradientDrawable.RECTANGLE

                    cornerRadius =
                        dp(15).toFloat()

                    setColor(
                        0xFF092236.toInt()
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
                    if (bold) {
                        Typeface.BOLD
                    } else {
                        Typeface.NORMAL
                    }
                )
        }

    private fun weight():
        LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.MATCH_PARENT,
            1f
        ).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }

    private fun formatTime(
        ms: Long
    ): String {
        val total =
            (ms / 1000L)
                .coerceAtLeast(0L)

        val hours =
            total / 3600L

        val minutes =
            (total % 3600L) / 60L

        val seconds =
            total % 60L

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

    private fun dp(
        value: Int
    ): Int =
        (
            value *
                resources.displayMetrics.density
            )
            .roundToInt()

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
