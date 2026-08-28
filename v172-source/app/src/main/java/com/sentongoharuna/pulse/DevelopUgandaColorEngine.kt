package com.sentongoharuna.pulse

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import androidx.camera.view.PreviewView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.SingleColorLut
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
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * V229 Professional Color Engine.
 *
 * ORIGINAL is always preserved. The operator monitor is an optional matrix
 * approximation. COLOR_MASTER.mp4 is the real 17^3 Media3 SingleColorLut
 * render with an actual H.264/AAC re-encode.
 *
 * These are develop.uganda looks, not copies of proprietary ARRI / Sony / RED
 * LUTs. The design goals are professional highlight, contrast, saturation and
 * shadow/highlight separation while remaining safe for phone-originated video.
 */
@OptIn(UnstableApi::class)
object DevelopUgandaColorEngine {

    data class Profile(
        val id: String,
        val label: String,
        val purpose: String,
        val contrast: Float,
        val saturation: Float,
        val gamma: Float,
        val blackLift: Float,
        val redGain: Float,
        val greenGain: Float,
        val blueGain: Float,
        val shadowRed: Float,
        val shadowGreen: Float,
        val shadowBlue: Float,
        val highlightRed: Float,
        val highlightGreen: Float,
        val highlightBlue: Float,
        val defaultStrength: Int
    )

    data class ResolvedSelection(
        val requestedId: String,
        val profile: Profile?,
        val strength: Int,
        val autoResolved: Boolean
    ) {
        val enabled: Boolean
            get() = profile != null

        val label: String
            get() = profile?.label ?: "ORIGINAL"

        fun statusLabel(): String =
            if (autoResolved && profile != null) {
                "AUTO → ${profile.label}"
            } else {
                label
            }
    }

    data class ExportOutcome(
        val success: Boolean,
        val uri: Uri?,
        val profileLabel: String,
        val strength: Int,
        val width: Int?,
        val height: Int?,
        val durationMs: Long?,
        val bitrate: Long?,
        val message: String
    )

    private data class VideoInfo(
        val width: Int?,
        val height: Int?,
        val durationMs: Long?,
        val bitrate: Long?
    )

    const val ID_AUTO = "AUTO"
    const val ID_ORIGINAL = "ORIGINAL"

    private const val PREFS = "develop_uganda_v229_color_engine"
    private const val KEY_MONITOR = "monitor_enabled"

    private val main = Handler(Looper.getMainLooper())
    private val activeTransformers = mutableSetOf<Transformer>()

    val profiles = listOf(
        Profile(
            "CINEMA_NATURAL",
            "DU CINEMA NATURAL",
            "Natural cinema • gentle highlight roll-off • restrained saturation",
            1.04f, 0.97f, 1.02f, 0.010f,
            1.018f, 1.000f, 0.988f,
            -0.004f, 0.000f, 0.008f,
            0.010f, 0.004f, -0.006f,
            88
        ),
        Profile(
            "COOL_CINEMA",
            "DU COOL CINEMA",
            "Cool cinematic separation • clean highlights • controlled cyan",
            1.055f, 0.96f, 1.01f, 0.008f,
            0.995f, 1.004f, 1.026f,
            -0.010f, 0.002f, 0.018f,
            0.008f, 0.002f, -0.004f,
            82
        ),
        Profile(
            "FILM_BIAS",
            "DU FILM BIAS",
            "Warm highlights • cooler shadows • film-density style contrast",
            1.085f, 0.93f, 1.03f, 0.012f,
            1.015f, 0.998f, 0.985f,
            -0.006f, 0.001f, 0.014f,
            0.018f, 0.006f, -0.010f,
            82
        ),
        Profile(
            "EXTENDED_VIDEO",
            "DU EXTENDED VIDEO",
            "Professional default • open shadows • gentle contrast",
            1.015f, 0.985f, 1.00f, 0.016f,
            1.004f, 1.002f, 0.998f,
            0.000f, 0.002f, 0.006f,
            0.004f, 0.002f, -0.002f,
            92
        ),
        Profile(
            "SOFT_FILM",
            "DU SOFT FILM",
            "Documentary-style low contrast • muted chroma • soft roll-off",
            0.965f, 0.86f, 1.025f, 0.022f,
            1.004f, 1.000f, 0.996f,
            -0.002f, 0.002f, 0.006f,
            0.006f, 0.002f, -0.004f,
            88
        ),
        Profile(
            "WARM_709",
            "DU WARM 709",
            "Clean newsroom rendering with modest warmth",
            1.055f, 0.98f, 1.00f, 0.008f,
            1.026f, 1.004f, 0.978f,
            0.000f, 0.002f, 0.004f,
            0.016f, 0.005f, -0.008f,
            78
        ),
        Profile(
            "NIGHT_CINEMA",
            "DU NIGHT CINEMA",
            "Lifted dark detail • restrained chroma • cool shadow separation",
            0.97f, 0.82f, 1.05f, 0.030f,
            0.994f, 1.000f, 1.018f,
            -0.008f, 0.000f, 0.016f,
            0.004f, 0.002f, -0.002f,
            92
        ),
        Profile(
            "BLEACH_DRAMA",
            "DU BLEACH DRAMA",
            "Hard dramatic contrast • substantially reduced saturation",
            1.14f, 0.58f, 0.98f, 0.004f,
            1.008f, 1.000f, 0.992f,
            -0.004f, 0.000f, 0.005f,
            0.008f, 0.004f, -0.004f,
            72
        ),
        Profile(
            "GOLDEN_HOUR",
            "DU GOLDEN HOUR",
            "Warm travel/commercial rendering without crushed shadows",
            1.045f, 1.02f, 1.00f, 0.010f,
            1.036f, 1.008f, 0.966f,
            0.000f, 0.002f, 0.004f,
            0.024f, 0.010f, -0.012f,
            74
        ),
        Profile(
            "CLEAN_SOCIAL",
            "DU CLEAN SOCIAL",
            "Bright clean social delivery • moderate contrast • restrained color",
            1.035f, 0.96f, 0.99f, 0.014f,
            1.010f, 1.008f, 1.002f,
            0.002f, 0.003f, 0.005f,
            0.008f, 0.006f, 0.000f,
            94
        ),
        Profile(
            "CONSTRUCTION",
            "DU CONSTRUCTION",
            "Neutral concrete/steel • controlled orange/yellow • structural clarity",
            1.075f, 0.92f, 0.99f, 0.006f,
            1.004f, 1.006f, 1.008f,
            -0.002f, 0.002f, 0.006f,
            0.006f, 0.004f, 0.000f,
            86
        ),
        Profile(
            "PEOPLE",
            "DU PEOPLE",
            "Skin-first neutral rendering • restrained reds • modest contrast",
            1.015f, 0.91f, 1.015f, 0.016f,
            1.016f, 1.004f, 0.994f,
            0.002f, 0.003f, 0.004f,
            0.014f, 0.006f, -0.004f,
            88
        ),
        Profile(
            "MONO_CINEMA",
            "DU MONO CINEMA",
            "Luminance-based monochrome • lifted blacks • cinema contrast",
            1.085f, 0.0f, 1.00f, 0.012f,
            1.000f, 1.000f, 1.000f,
            0.000f, 0.000f, 0.000f,
            0.000f, 0.000f, 0.000f,
            90
        )
    )

    fun menuLabels(): Array<String> =
        buildList {
            add("AUTO • CAMERA / SCENE")
            add("ORIGINAL • NO COLOR MASTER")
            profiles.forEach { add(it.label) }
        }.toTypedArray()

    fun selectedMenuIndex(
        context: Context,
        scope: String
    ): Int {
        val id = selectedId(context, scope)

        return when (id) {
            ID_AUTO -> 0
            ID_ORIGINAL -> 1
            else -> profiles.indexOfFirst { it.id == id }
                .takeIf { it >= 0 }
                ?.plus(2)
                ?: 0
        }
    }

    fun setSelectedMenuIndex(
        context: Context,
        scope: String,
        index: Int
    ) {
        val id = when (index) {
            0 -> ID_AUTO
            1 -> ID_ORIGINAL
            else -> profiles.getOrNull(index - 2)?.id ?: ID_AUTO
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(profileKey(scope), id)
            .apply()
    }

    fun selectedStrength(
        context: Context,
        scope: String,
        resolvedProfile: Profile? = null
    ): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = strengthKey(scope)

        if (prefs.contains(key)) {
            return prefs.getInt(key, 85).coerceIn(25, 100)
        }

        return resolvedProfile?.defaultStrength ?: 85
    }

    fun setStrength(
        context: Context,
        scope: String,
        strength: Int
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(strengthKey(scope), strength.coerceIn(25, 100))
            .apply()
    }

    fun monitorEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONITOR, false)

    fun setMonitorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MONITOR, enabled)
            .apply()
    }

    fun resolve(
        context: Context,
        scope: String,
        hint: String
    ): ResolvedSelection {
        val requested = selectedId(context, scope)

        if (requested == ID_ORIGINAL) {
            return ResolvedSelection(requested, null, 0, false)
        }

        val profile = if (requested == ID_AUTO) {
            autoProfile(hint)
        } else {
            profiles.firstOrNull { it.id == requested } ?: autoProfile(hint)
        }

        return ResolvedSelection(
            requestedId = requested,
            profile = profile,
            strength = selectedStrength(context, scope, profile),
            autoResolved = requested == ID_AUTO
        )
    }

    /**
     * Optional operator monitor approximation only. It is deliberately OFF by
     * default so V229 cannot destabilize an already proven CameraX preview.
     */
    fun applyPreviewMonitor(
        previewView: PreviewView,
        selection: ResolvedSelection
    ) {
        if (!monitorEnabled(previewView.context) || selection.profile == null) {
            previewView.setLayerType(View.LAYER_TYPE_NONE, null)
            return
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                previewMatrix(
                    selection.profile,
                    selection.strength / 100f
                )
            )
        }

        previewView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }

    fun createLutEffect(selection: ResolvedSelection): SingleColorLut? {
        val profile = selection.profile ?: return null
        return SingleColorLut.createFromCube(
            buildCube(
                profile,
                selection.strength,
                17
            )
        )
    }

    fun exportVideoMaster(
        context: Context,
        inputUri: Uri,
        packageId: String,
        scope: String,
        hint: String,
        callback: (ExportOutcome) -> Unit
    ) {
        val selection = resolve(context, scope, hint)
        val profile = selection.profile

        if (profile == null) {
            callback(
                ExportOutcome(
                    false,
                    null,
                    "ORIGINAL",
                    0,
                    null,
                    null,
                    null,
                    null,
                    "Original-only color mode selected"
                )
            )
            return
        }

        val inputInfo = readVideoInfo(context, inputUri)
        val tempDir = File(context.cacheDir, "v229_color_exports").apply { mkdirs() }
        val temp = File(tempDir, "color_${System.currentTimeMillis()}.mp4")
        if (temp.exists()) temp.delete()

        val lut = createLutEffect(selection)
        if (lut == null) {
            callback(
                ExportOutcome(
                    false,
                    null,
                    profile.label,
                    selection.strength,
                    inputInfo.width,
                    inputInfo.height,
                    inputInfo.durationMs,
                    inputInfo.bitrate,
                    "Could not create 3D LUT"
                )
            )
            return
        }

        val source = MediaItem.Builder().setUri(inputUri).build()
        val effects = Effects(emptyList(), listOf(lut))
        val edited = EditedMediaItem.Builder(source)
            .setEffects(effects)
            .build()
        val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(listOf(edited))
        val compositionBuilder = Composition.Builder(listOf(sequence))

        // COLOR_MASTER is a delivery copy. The original HDR source is never
        // replaced. For API 29+ Media3's OpenGL tone mapper makes an SDR master.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            compositionBuilder.setHdrMode(
                Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL
            )
        }

        val composition = compositionBuilder.build()
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(targetVideoBitrate(inputInfo))
                    .setiFrameIntervalSeconds(2f)
                    .build()
            )
            .setRequestedAudioEncoderSettings(
                AudioEncoderSettings.Builder()
                    .setBitrate(256_000)
                    .build()
            )
            .build()

        var transformer: Transformer? = null

        val listener = object : Transformer.Listener {
            override fun onCompleted(
                composition: Composition,
                result: ExportResult
            ) {
                try { lut.release() } catch (_: Exception) { }

                transformer?.let {
                    synchronized(activeTransformers) {
                        activeTransformers.remove(it)
                    }
                }

                Thread {
                    try {
                        val outputInfo = readVideoInfo(temp.absolutePath)
                        val published = publishColorMaster(
                            context,
                            temp,
                            packageId,
                            profile.label
                        )
                        temp.delete()

                        main.post {
                            callback(
                                ExportOutcome(
                                    true,
                                    published,
                                    profile.label,
                                    selection.strength,
                                    outputInfo.width,
                                    outputInfo.height,
                                    outputInfo.durationMs,
                                    outputInfo.bitrate,
                                    "17³ 3D LUT color master ready"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        temp.delete()
                        main.post {
                            callback(
                                ExportOutcome(
                                    false,
                                    null,
                                    profile.label,
                                    selection.strength,
                                    inputInfo.width,
                                    inputInfo.height,
                                    inputInfo.durationMs,
                                    inputInfo.bitrate,
                                    "Color export failed • original is safe • ${e.javaClass.simpleName}"
                                )
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
                try { lut.release() } catch (_: Exception) { }

                transformer?.let {
                    synchronized(activeTransformers) {
                        activeTransformers.remove(it)
                    }
                }

                temp.delete()
                callback(
                    ExportOutcome(
                        false,
                        null,
                        profile.label,
                        selection.strength,
                        inputInfo.width,
                        inputInfo.height,
                        inputInfo.durationMs,
                        inputInfo.bitrate,
                        "Media3 LUT export failed • original is safe"
                    )
                )
            }
        }

        val builtTransformer = Transformer.Builder(context)
            .setEncoderFactory(encoderFactory)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        transformer = builtTransformer

        synchronized(activeTransformers) {
            activeTransformers.add(builtTransformer)
        }

        builtTransformer.start(composition, temp.absolutePath)
    }

    fun profileJson(
        context: Context,
        scope: String,
        hint: String
    ): org.json.JSONObject {
        val value = resolve(context, scope, hint)

        return org.json.JSONObject()
            .put("requested_profile", value.requestedId)
            .put("resolved_profile", value.profile?.id ?: ID_ORIGINAL)
            .put("resolved_label", value.label)
            .put("strength_percent", value.strength)
            .put("auto_resolved", value.autoResolved)
            .put("monitor_enabled", monitorEnabled(context))
            .put(
                "master_pipeline",
                if (value.enabled) {
                    "Media3 SingleColorLut 17^3 + H.264/AAC re-encode"
                } else {
                    "Original only"
                }
            )
            .put(
                "preview_note",
                "Optional monitor is a matrix approximation; COLOR_MASTER.mp4 uses the real 3D LUT."
            )
            .put("generated_utc", Instant.now().toString())
    }

    private fun readVideoInfo(context: Context, uri: Uri): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            VideoInfo(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            )
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }

    private fun readVideoInfo(path: String): VideoInfo {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            VideoInfo(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            )
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }

    private fun targetVideoBitrate(info: VideoInfo): Int {
        val pixels = (info.width ?: 0) * (info.height ?: 0)
        val baseline = when {
            pixels >= 8_000_000 -> 48_000_000
            pixels >= 2_000_000 -> 20_000_000
            pixels >= 900_000 -> 12_000_000
            else -> 8_000_000
        }

        val source = info.bitrate
            ?.coerceAtMost(72_000_000L)
            ?.toInt()
            ?: baseline

        return maxOf(baseline, source)
    }

    private fun publishColorMaster(
        context: Context,
        temp: File,
        packageId: String,
        profileLabel: String
    ): Uri {
        val safeProfile = profileLabel
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
            .take(28)
            .ifBlank { "COLOR" }

        val safePackage = packageId
            .uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_-]+"), "_")
            .take(42)

        val name = "DEVELOP_UGANDA_V229_${safeProfile}_${safePackage}.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "Movies/develop.uganda/Color Masters"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Could not create Color Master")

        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                FileInputStream(temp).use { input ->
                    input.copyTo(output, 1024 * 1024)
                }
            } ?: error("Could not write Color Master")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    },
                    null,
                    null
                )
            }

            return uri
        } catch (e: Exception) {
            try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) { }
            throw e
        }
    }

    private fun selectedId(context: Context, scope: String): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val scoped = prefs.getString(profileKey(scope), null)

        if (!scoped.isNullOrBlank()) return scoped

        return prefs.getString("profile_GLOBAL", ID_AUTO) ?: ID_AUTO
    }

    private fun autoProfile(hint: String): Profile {
        val value = hint.uppercase(Locale.US)
        val id = when {
            "SOCIAL" in value -> "CLEAN_SOCIAL"
            "NIGHT" in value || "LOW LIGHT" in value -> "NIGHT_CINEMA"
            "INTERVIEW" in value || "PEOPLE" in value || "V205" in value || "V211" in value -> "PEOPLE"
            "CONSTRUCTION" in value || "BUILDING" in value -> "CONSTRUCTION"
            "CINEMA" in value || "MOVIE" in value -> "CINEMA_NATURAL"
            "OUTDOOR" in value || "TRAVEL" in value -> "GOLDEN_HOUR"
            "DOCUMENTARY" in value -> "SOFT_FILM"
            "NEWS" in value || "REPORTER" in value || "BREAKING" in value -> "EXTENDED_VIDEO"
            else -> "CINEMA_NATURAL"
        }

        return profiles.first { it.id == id }
    }

    private fun previewMatrix(profile: Profile, strength: Float): ColorMatrix {
        val sat = 1f + (profile.saturation - 1f) * strength
        val contrast = 1f + (profile.contrast - 1f) * strength
        val redGain = 1f + (profile.redGain - 1f) * strength
        val greenGain = 1f + (profile.greenGain - 1f) * strength
        val blueGain = 1f + (profile.blueGain - 1f) * strength

        val inv = 1f - sat
        val rw = 0.2126f
        val gw = 0.7152f
        val bw = 0.0722f
        val offset = (profile.blackLift * 255f * strength) - ((contrast - 1f) * 127.5f)

        return ColorMatrix(
            floatArrayOf(
                (inv * rw + sat) * contrast * redGain,
                inv * gw * contrast * redGain,
                inv * bw * contrast * redGain,
                0f,
                offset,

                inv * rw * contrast * greenGain,
                (inv * gw + sat) * contrast * greenGain,
                inv * bw * contrast * greenGain,
                0f,
                offset,

                inv * rw * contrast * blueGain,
                inv * gw * contrast * blueGain,
                (inv * bw + sat) * contrast * blueGain,
                0f,
                offset,

                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun buildCube(
        profile: Profile,
        strengthPercent: Int,
        size: Int
    ): Array<Array<IntArray>> {
        val strength = strengthPercent.coerceIn(0, 100) / 100f

        return Array(size) { rIndex ->
            Array(size) { gIndex ->
                IntArray(size) { bIndex ->
                    val r = rIndex.toFloat() / (size - 1).toFloat()
                    val g = gIndex.toFloat() / (size - 1).toFloat()
                    val b = bIndex.toFloat() / (size - 1).toFloat()
                    val transformed = transform(profile, r, g, b, strength)

                    Color.argb(
                        255,
                        (transformed[0] * 255f).roundToInt().coerceIn(0, 255),
                        (transformed[1] * 255f).roundToInt().coerceIn(0, 255),
                        (transformed[2] * 255f).roundToInt().coerceIn(0, 255)
                    )
                }
            }
        }
    }

    private fun transform(
        profile: Profile,
        inputR: Float,
        inputG: Float,
        inputB: Float,
        strength: Float
    ): FloatArray {
        val original = floatArrayOf(inputR, inputG, inputB)

        var r = tone(inputR, profile)
        var g = tone(inputG, profile)
        var b = tone(inputB, profile)

        var luma = (r * 0.2126f + g * 0.7152f + b * 0.0722f).coerceIn(0f, 1f)

        r = luma + (r - luma) * profile.saturation
        g = luma + (g - luma) * profile.saturation
        b = luma + (b - luma) * profile.saturation

        r *= profile.redGain
        g *= profile.greenGain
        b *= profile.blueGain

        luma = (r * 0.2126f + g * 0.7152f + b * 0.0722f).coerceIn(0f, 1f)
        val shadowWeight = (1f - luma).coerceIn(0f, 1f).pow(2f)
        val highlightWeight = luma.coerceIn(0f, 1f).pow(2f)

        r += profile.shadowRed * shadowWeight + profile.highlightRed * highlightWeight
        g += profile.shadowGreen * shadowWeight + profile.highlightGreen * highlightWeight
        b += profile.shadowBlue * shadowWeight + profile.highlightBlue * highlightWeight

        val graded = floatArrayOf(
            r.coerceIn(0f, 1f),
            g.coerceIn(0f, 1f),
            b.coerceIn(0f, 1f)
        )

        return floatArrayOf(
            mix(original[0], graded[0], strength),
            mix(original[1], graded[1], strength),
            mix(original[2], graded[2], strength)
        )
    }

    private fun tone(value: Float, profile: Profile): Float {
        var x = value.coerceIn(0f, 1f)
        x = x.pow(1f / profile.gamma.coerceAtLeast(0.2f))
        x = 0.5f + (x - 0.5f) * profile.contrast
        x = profile.blackLift + x * (1f - profile.blackLift)

        if (x > 0.72f) {
            val over = x - 0.72f
            x = 0.72f + over / (1f + over * 1.8f)
        }

        return x.coerceIn(0f, 1f)
    }

    private fun mix(a: Float, b: Float, t: Float): Float =
        (a + (b - a) * t).coerceIn(0f, 1f)

    private fun profileKey(scope: String): String = "profile_${safeScope(scope)}"
    private fun strengthKey(scope: String): String = "strength_${safeScope(scope)}"

    private fun safeScope(value: String): String =
        value.uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_]+"), "_")
            .take(48)
            .ifBlank { "GLOBAL" }
}
