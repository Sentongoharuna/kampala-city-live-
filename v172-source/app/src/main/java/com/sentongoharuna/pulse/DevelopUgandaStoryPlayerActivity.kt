package com.sentongoharuna.pulse

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlin.math.roundToInt

class DevelopUgandaStoryPlayerActivity :
    AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_ID =
            "story_package_id"

        const val EXTRA_DIRECT_URI =
            "direct_video_uri"

        const val EXTRA_DIRECT_LABEL =
            "direct_video_label"
    }

    private var player: ExoPlayer? =
        null

    private lateinit var playerView: PlayerView
    private lateinit var statusView: TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        val root =
            FrameLayout(
                this
            ).apply {
                setBackgroundColor(
                    Color.BLACK
                )
            }

        playerView =
            PlayerView(
                this
            ).apply {
                useController =
                    true

                resizeMode =
                    AspectRatioFrameLayout.RESIZE_MODE_FIT

                setShowBuffering(
                    PlayerView.SHOW_BUFFERING_WHEN_PLAYING
                )

                setBackgroundColor(
                    Color.BLACK
                )
            }

        root.addView(
            playerView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        statusView =
            TextView(
                this
            ).apply {
                text =
                    "develop.uganda • STORY PLAYER"

                textSize =
                    10f

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    0x99031829.toInt()
                )

                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8)
                )
            }

        root.addView(
            statusView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity =
                    Gravity.TOP
            }
        )

        setContentView(
            root
        )

        val directUri =
            intent.getStringExtra(
                EXTRA_DIRECT_URI
            )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    Uri.parse(
                        it
                    )
                }

        if (
            directUri !=
                null
        ) {
            playUri(
                directUri,
                intent.getStringExtra(
                    EXTRA_DIRECT_LABEL
                ) ?: "RECORDED VIDEO"
            )

            return
        }

        val packageId =
            intent.getStringExtra(
                EXTRA_PACKAGE_ID
            )
                ?.trim()
                .orEmpty()

        if (
            packageId.isBlank()
        ) {
            finish()
            return
        }

        val resolved =
            DevelopUgandaStoryPackager
                .resolvePlayableVideo(
                    this,
                    packageId
                )

        if (
            resolved ==
                null
        ) {
            statusView.text =
                "VIDEO NOT FOUND • package $packageId"

            return
        }

        playUri(
            resolved.uri,
            resolved.label
        )
    }

    private fun playUri(
        uri: Uri,
        label: String
    ) {
        val exo =
            ExoPlayer.Builder(
                this
            )
                .build()

        player =
            exo

        playerView.player =
            exo

        exo.addListener(
            object :
                Player.Listener {

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {
                    statusView.text =
                        when (
                            playbackState
                        ) {
                            Player.STATE_BUFFERING ->
                                "BUFFERING • $label"

                            Player.STATE_READY ->
                                "PLAYING • $label"

                            Player.STATE_ENDED ->
                                "ENDED • $label"

                            else ->
                                "develop.uganda • $label"
                        }
                }

                override fun onPlayerError(
                    error: androidx.media3.common.PlaybackException
                ) {
                    statusView.text =
                        "PLAYBACK ERROR • ${error.errorCodeName}"
                }
            }
        )

        exo.setMediaItem(
            MediaItem.fromUri(
                uri
            )
        )

        exo.prepare()
        exo.playWhenReady =
            true
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        try {
            playerView.player =
                null

            player?.release()
        } catch (_: Exception) {
        }

        player =
            null

        super.onDestroy()
    }

    private fun dp(
        value: Int
    ): Int =
        (
            value *
                resources.displayMetrics.density
            ).roundToInt()
}
