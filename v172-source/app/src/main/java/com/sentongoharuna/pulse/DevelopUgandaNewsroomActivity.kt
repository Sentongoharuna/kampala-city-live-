package com.sentongoharuna.pulse

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

    private val gold = 0xFFAEBDEB.toInt()
    private val cyan = 0xFF8FA8E8.toInt()
    private val green = 0xFF91B6A0.toInt()
    private val red = 0xFFC76D73.toInt()
    private val white = 0xFFF1F3F8.toInt()
    private val ink = 0xFF031829.toInt()
    private val card = 0xFF092236.toInt()
    private val muted = 0xFFAEB7C7.toInt()

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
                    0xFF061D2E.toInt()
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
                "DIRECTOR & QC PRO • V227\nV226 FIX2 NEWSROOM + V225 PHOTO RETAINED",
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
                    0xFF061D2E.toInt()
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
                "REPORT\nFIELD",
                gold
            ) {
                openReportCamera()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "LIVE\nSTUDIO",
                red
            ) {
                showLivePage()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "EDIT\nVIDEO",
                cyan
            ) {
                openEditor()
            },
            navWeight()
        )

        nav.addView(
            navButton(
                "DESK\nSTORY",
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
                "develop.uganda NEWSROOM",
                "REPORT • LIVE • EDIT • NEWSROOM"
            )
        )

        page.addView(
            compactStatus(
                "READY TO REPORT",
                "FIELD • LIVE • EDIT • STORY DESK",
                green
            )
        )

        page.addView(
            compactStatus(
                "CAMERA MEMORY",
                "REPORT + LIVE remember your last HUD and key operating settings",
                gold
            )
        )

        page.addView(
            compactStatus(
                "QUICK PRESETS",
                "REPORT: FIELD / OUTDOOR / NIGHT / INTERVIEW / CINEMA   •   LIVE: BREAKING / INTERVIEW / EVENT / COMMUNITY",
                cyan
            )
        )

        page.addView(
            compactStatus(
                "SAVED VIDEO VISIBILITY",
                "SHORT TELEMETRY RAIL • WIDER READABLE COLUMN • LIVE BUILD TAG AND BLINKING REC BADGE USE SEPARATE SAFE LANES",
                green
            )
        )


        page.addView(
            compactStatus(
                "AUTO VIEW • ON-DEVICE SCENE DESCRIPTION",
                "MAIN REPORT + LIVE PREVIEW SHOW A BRIEF ML KIT DESCRIPTION OF THE CURRENT VIEW • SCREEN-ONLY SO A WRONG AI LABEL IS NOT PERMANENTLY BURNED INTO EVIDENCE FOOTAGE",
                0xFF62D8C9.toInt()
            )
        )

        page.addView(
            compactStatus(
                "SHOT QUALITY GUARD • REAL SIGNALS",
                "TOO DARK • MIC CLIPPING • SHAKE HIGH • HORIZON OFF • THERMAL RISK • STORAGE LOW • GPS WEAK • FOCUS NOT CONFIRMED • FIELD PREFLIGHT • RECOVERY JOURNAL • SCREEN-ONLY PEAK/ZEBRA",
                0xFF62D8C9.toInt()
            )
        )

        page.addView(
            compactStatus(
                "CREATOR CAMERA ENGINE",
                "V205→V222 INDEPENDENT CAMERAS RETAINED • V217 ADDS FULL-SCREEN CAMERA PREVIEW + POLISHED SAVED-VIDEO HUD • NOTHING DROPPED",
                cyan
            )
        )

        page.addView(
            compactStatus(
                "V227 • DIRECTOR & QC PRO",
                "SCREEN-ONLY REAL FACE COMPOSITION FOR PEOPLE/INTERVIEW • PREVIEW LUMA HISTOGRAM • ESTIMATED RECORD TIME • REAL CAMERA DEVICE MAP • SHOT CONTINUITY • INSTANT MP4 QC + REVIEW • V226 FIX2 NEWSROOM INTAKE RETAINED",
                0xFF91B6A0.toInt()
            )
        )

        page.addView(
            launchCard(
                "V227 • CAMERA HEALTH",
                "See what this phone actually exposes",
                "REAL CameraX/Camera2 device IDs + focal lengths • UHD • HLG HDR • stabilization • hardware FPS ranges • JPEG / Ultra HDR / RAW / RAW+JPEG • on-device transcription • thermal/location/storage • H.264/AAC encoder presence",
                0xFF73B7D9.toInt(),
                "OPEN CAMERA HEALTH"
            ) {
                openIndependentCamera(
                    DevelopUgandaCameraHealthActivity::class.java
                )
            }
        )

        page.addView(
            sectionTitle(
                "INDEPENDENT PRO CAMERAS"
            )
        )

        page.addView(
            compactStatus(
                "SHOT FINDER • ALL CAMERAS ARE INDEPENDENT • PICK BY WHAT YOU ARE FILMING",
                "PEOPLE → V205   •   BACKLIGHT → V206   •   BUILDINGS → V207   •   WALK/ACTION → V208   •   NIGHT → V209   •   EVERYDAY → V210   •   INTERVIEW AUDIO → V211   •   VERIFIED → V212   •   LONG RECORD → V213   •   CINEMATIC → V214   •   UNSURE → V215   •   SOCIAL POST → V222",
                cyan
            )
        )

        page.addView(
            compactStatus(
                "V217 FULL FRAME CAMERA",
                "EVERY CAMERA STILL OPENS DIRECTLY • CAMERA PREVIEW NOW FILLS THE SCREEN BEHIND CONTROLS • RECORDING FORCES FULL FRAME • SAVED HUD IS WIDER AND CLEANER",
                green
            )
        )

        page.addView(
            launchCard(
                "V205 • PEOPLE / PORTRAIT FOCUS",
                "People, portraits and interviews",
                "Tap subject for AF • long-press persistent AF lock • INTERVIEW + SOCIAL FHD default • focus reticle emphasized • all shared recording/telemetry tools remain",
                gold,
                "OPEN V205 FOCUS CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaFocusAssistCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V206 • SUBJECT METERING",
                "Backlit faces, windows and mixed light",
                "Long-press metering region • visible reticle • NATURAL default • independent saved settings • exact camera identity burned into V216 output",
                0xFFD0B06F.toInt(),
                "OPEN V206 METER CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaMeteringLockCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V207 • BUILDINGS / LEVEL",
                "Architecture, rooms and straight horizons",
                "Rotation-vector horizon guide emphasized • LEVEL LOCK / LEVEL NEAR / ADJUST • OUTDOOR default • other modules retained but visually secondary",
                green,
                "OPEN V207 HORIZON CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaHorizonCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V208 • WALK / ACTION STEADY",
                "Walking, vehicles and moving subjects",
                "ACTION STAB default • real STEADY / MOVING / SHAKE guidance emphasized • DOCUMENTARY default • device stabilization remains real CameraX capability",
                0xFF71B9A7.toInt(),
                "OPEN V208 STEADYSHOT CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaSteadyShotCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V209 • NIGHT / LOW LIGHT",
                "Night streets and dark interiors",
                "LOW LIGHT + NIGHT defaults • real Android lux sensor emphasized • dark/dim/normal/bright guidance • 30fps advice preserved",
                0xFF8A86B8.toInt(),
                "OPEN V209 NIGHT CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaNightIntelligenceCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V210 • EVERYDAY PRO",
                "Best general-purpose professional camera",
                "All V204→V215 tools visible together • Social Master capture engine • focus/meter/horizon/motion/lux/audio/thermal/verified-state controls • REPORTER default",
                cyan,
                "OPEN V210 ALL-PRO CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaAllProCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V211 • INTERVIEW / AUDIO",
                "Speech, interviews and events",
                "CameraX microphone amplitude + peak emphasized • LOW / GOOD / HOT / CLIP RISK • INTERVIEW default • audio track is still recorded normally",
                green,
                "OPEN V211 AUDIO CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaAudioGuardCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V212 • VERIFIED REPORT",
                "Site reports, incidents and evidence capture",
                "Live CameraX + GPS + sensor + audio state emphasized • NEWS default • V216 filename and SHA-256 integrity metadata identify this exact camera",
                0xFF73B7D9.toInt(),
                "OPEN V212 VERIFIED CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaVerifiedCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V213 • LONG RECORD / HEAT SAFE",
                "Long takes and hot conditions",
                "Android PowerManager thermal state emphasized • severe+ safe fallback retained • SOCIAL FHD default • thermal state recorded in output",
                red,
                "OPEN V213 THERMAL CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaThermalSafeCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V214 • CINEMATIC LOOKS",
                "Cinematic people, travel and creative shots",
                "SOCIAL HDR + WARM first-run defaults • subtle look-matched preview • mode accent/purpose • exact quality/scene/look recorded",
                0xFFA793D8.toInt(),
                "OPEN V214 SIGNATURE CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaModeSignatureCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V215 • SMART AUTO",
                "Fast shooting when you do not want to choose settings",
                "AUTO DIRECTOR enabled on first launch • real lux + shake + thermal choose actual Social FHD / Social 60 / Action Stab / Low Light • never changes mid-recording",
                0xFF73B7D9.toInt(),
                "OPEN V215 AUTO CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaAutoDirectorCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V222 • SOCIAL MEDIA CAMERA",
                "TikTok • Instagram Reels • YouTube Shorts • social posts",
                "Direct 9:16 SOCIAL FHD camera • compact social HUD • records the normal high-quality original first • then automatically forces a separate H.264/AAC social re-encode • 1080×1920 • 30fps max • 16 Mbps target • 2s keyframes • saves separately in Movies/develop.uganda/SM Posts",
                0xFF62D8C9.toInt(),
                "OPEN V222 SM CAMERA"
            ) {
                openIndependentCamera(
                    DevelopUgandaSocialMediaCameraActivity::class.java
                )
            }
        )

        page.addView(
            compactStatus(
                "SM CAMERA WORKFLOW",
                "TAP V222 → RECORD → STOP → ORIGINAL SAVES NORMALLY → SM OPTIMIZING → SM READY → PICK THE SEPARATE SM POSTS VIDEO IN TIKTOK / REELS",
                0xFF62D8C9.toInt()
            )
        )

        page.addView(
            compactStatus(
                "NOTHING DROPPED",
                "V205 • V206 • V207 • V208 • V209 • V210 • V211 • V212 • V213 • V214 • V215 • V222 SOCIAL MEDIA CAMERA ALL REMAIN DIRECT-LAUNCH OPTIONS • V223 ADDS AUTO VIEW TO MAIN REPORT/LIVE",
                gold
            )
        )

        val quickLaunch =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(8)
                )
            }

        quickLaunch.addView(
            smallClipButton(
                "REPORT",
                gold
            ) {
                openReportCamera()
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        quickLaunch.addView(
            smallClipButton(
                "LIVE",
                red
            ) {
                openLiveCamera()
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                marginStart =
                    dp(6)
            }
        )

        quickLaunch.addView(
            smallClipButton(
                "EDIT",
                cyan
            ) {
                openEditor()
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                marginStart =
                    dp(6)
            }
        )

        quickLaunch.addView(
            smallClipButton(
                "DESK",
                green
            ) {
                showNewsroom()
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                marginStart =
                    dp(6)
            }
        )

        page.addView(
            quickLaunch
        )

        page.addView(
            sectionTitle(
                "AUTO STORY PACKAGE • V227"
            )
        )

        page.addView(
            compactStatus(
                "AFTER A SUCCESSFUL REPORT / LIVE RECORDING",
                "ORIGINAL PACKAGE COPY • THUMBNAIL • MANIFEST • METADATA • SHA-256 COPY CHECK • CAPTION DRAFT • SOCIAL MASTER WHEN AVAILABLE • OPTIONAL ON-DEVICE TRANSCRIPT / SRT DRAFT",
                0xFF62D8C9.toInt()
            )
        )

        page.addView(
            launchCard(
                "V227 • STORY PACKAGES",
                "Open, share and transcribe completed report packages",
                "Each package is stored under Download/develop.uganda/Story Packages/<Package ID> • original Gallery video stays untouched • Interview/V211 can request on-device transcript automatically • any package can request transcript manually on Android 13+ when an on-device recognizer exists",
                0xFF62D8C9.toInt(),
                "OPEN STORY PACKAGES"
            ) {
                openIndependentCamera(
                    DevelopUgandaStoryPackagesActivity::class.java
                )
            }
        )

        page.addView(
            sectionTitle(
                "PROFESSIONAL PHOTO CAMERAS • V225"
            )
        )

        page.addView(
            compactStatus(
                "CAPABILITY-AWARE STILL PHOTOGRAPHY",
                "ONLY FORMATS THE SELECTED LENS REPORTS AS SUPPORTED ARE SHOWN • JPEG • ULTRA HDR JPEG_R • RAW DNG • RAW+JPEG • EDGE PEAK / ZEBRA REMAIN SCREEN-ONLY",
                0xFFD0B06F.toInt()
            )
        )

        page.addView(
            launchCard(
                "V225 • PHOTO PRO",
                "General professional still photography",
                "Maximum-quality CameraX ImageCapture • tap focus • capability-aware JPEG / Ultra HDR / RAW DNG / RAW+JPEG selector • level guide • edge peak / zebra operator assist",
                0xFFD0B06F.toInt(),
                "OPEN PHOTO PRO"
            ) {
                openIndependentCamera(
                    DevelopUgandaPhotoProCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V225 • BUILDING PHOTO",
                "Architecture • rooms • property • straight lines",
                "Level guide emphasized • Ultra HDR preferred only when this lens reports support • otherwise JPEG fallback • maximum-quality capture • RAW options remain selectable when supported",
                green,
                "OPEN BUILDING PHOTO"
            ) {
                openIndependentCamera(
                    DevelopUgandaBuildingPhotoCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V225 • PEOPLE PHOTO",
                "People • portraits • interview stills",
                "Tap-to-focus with CameraX focus confirmation • maximum-quality JPEG default • RAW/HDR choices appear only if the device supports them • peaking optional",
                gold,
                "OPEN PEOPLE PHOTO"
            ) {
                openIndependentCamera(
                    DevelopUgandaPeoplePhotoCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V225 • NIGHT PHOTO",
                "Night • dark rooms • low-light stills",
                "CameraX MAXIMIZE_QUALITY • flash OFF by default • no fake Nightography claim • device-supported RAW/HDR formats remain selectable • zebra/peaking available",
                0xFF8A86B8.toInt(),
                "OPEN NIGHT PHOTO"
            ) {
                openIndependentCamera(
                    DevelopUgandaNightPhotoCameraActivity::class.java
                )
            }
        )

        page.addView(
            launchCard(
                "V225 • VERIFIED PHOTO",
                "Inspections • site records • evidence-style stills",
                "JPEG maximum quality • filename identifies VERIFIED PHOTO • metadata JSON includes capture time/GPS/camera • SHA-256 integrity sidecar detects later file changes without claiming authorship",
                0xFF73B7D9.toInt(),
                "OPEN VERIFIED PHOTO"
            ) {
                openIndependentCamera(
                    DevelopUgandaVerifiedPhotoCameraActivity::class.java
                )
            }
        )

        page.addView(
            sectionTitle(
                "CAPTURE MODES"
            )
        )

        page.addView(
            launchCard(
                "FIELD REPORT CAMERA",
                "FIELD REPORT • V216 INDEPENDENT CAMERA SUITE • V205→V222 DIRECT HOME LAUNCHERS • SEPARATE CAMERA ACTIVITIES + PREFERENCES • NOTHING DROPPED • telemetry",
                "Report ID • reporter/story • GPS/GNSS • compass • weather • audio • camera health",
                gold,
                "OPEN REPORT CAMERA"
            ) {
                openReportCamera()
            }
        )

        page.addView(
            launchCard(
                "LIVE STUDIO",
                "LIVE STUDIO • SOCIAL 30/60 • SOCIAL HDR • UHD 30/60 • ACTION 30/60 • low-light profile • LIVE graphics",
                "Signals • reporter identity • output setup • broadcast profiles • pulsing red record ring",
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
                "EDIT + SOCIAL MASTER • OPTIONAL",
                "Edit normally, then create TikTok or Reels upload masters without touching the original",
                "GALLERY • FILES • RECENT • LAST CLIP • Media3 preview/edit • TIKTOK MASTER 16 Mbps • REELS MASTER 14 Mbps • original preserved",
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
            compactStatus(
                "REPORTING WORKFLOW",
                "ASSIGN → CAPTURE → VERIFY → EDIT → SHARE",
                cyan
            )
        )

        page.addView(
            sectionTitle(
                "RECENT CLIPS"
            )
        )

        addRecentClips(
            page
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
                "V218 • GALLERY / FILES / LAST CLIP • PREVIEW CUT • KEYFRAME-SAFE MP4 REMUX • MUTE • SHARE",
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

    private fun addRecentClips(
        page: LinearLayout
    ) {
        val projection =
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED
            )

        val uri =
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        var shown =
            0

        try {
            contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media._ID
                    )

                val nameCol =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media.DISPLAY_NAME
                    )

                val durationCol =
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media.DURATION
                    )

                while (
                    cursor.moveToNext() &&
                    shown <
                    4
                ) {
                    val name =
                        cursor.getString(
                            nameCol
                        ) ?: continue

                    if (
                        !name.startsWith(
                            "DEVELOP_UGANDA_",
                            ignoreCase = true
                        )
                    ) {
                        continue
                    }

                    val id =
                        cursor.getLong(
                            idCol
                        )

                    val duration =
                        cursor.getLong(
                            durationCol
                        )

                    val clipUri =
                        ContentUris.withAppendedId(
                            uri,
                            id
                        )

                    page.addView(
                        recentClipCard(
                            name,
                            duration,
                            clipUri
                        )
                    )

                    shown +=
                        1
                }
            }
        } catch (
            _: Exception
        ) {
        }

        if (
            shown ==
            0
        ) {
            page.addView(
                infoCard(
                    "NO RECENT CLIPS",
                    "Record with REPORT or LIVE, then return here. Clips created by develop.uganda will appear here when Android MediaStore permits access."
                )
            )
        }
    }

    private fun recentClipCard(
        name: String,
        durationMs: Long,
        uri: Uri
    ): View {
        val cardView =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )

                background =
                    rounded(
                        card,
                        0xFF2A3940.toInt(),
                        15
                    )
            }

        val badge =
            when {
                name.contains(
                    "_LIVE_",
                    ignoreCase = true
                ) ->
                    "LIVE"

                else ->
                    "REPORT"
            }

        cardView.addView(
            label(
                "$badge • ${formatDuration(durationMs)}",
                10f,
                if (
                    badge ==
                    "LIVE"
                ) {
                    red
                } else {
                    gold
                },
                true
            )
        )

        cardView.addView(
            label(
                name.removeSuffix(
                    ".mp4"
                ),
                11f,
                white,
                true
            ).apply {
                maxLines =
                    1
            }
        )

        val actions =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                setPadding(
                    0,
                    dp(7),
                    0,
                    0
                )
            }

        actions.addView(
            smallClipButton(
                "PLAY",
                cyan
            ) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW
                        ).apply {
                            setDataAndType(
                                uri,
                                "video/mp4"
                            )

                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    )
                } catch (
                    _: Exception
                ) {
                    toast(
                        "No video player available"
                    )
                }
            },
            LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            )
        )

        actions.addView(
            smallClipButton(
                "EDIT",
                green
            ) {
                startActivity(
                    Intent(
                        this,
                        DevelopUgandaEditorActivity::class.java
                    ).apply {
                        putExtra(
                            "develop_uganda_edit_uri",
                            uri.toString()
                        )
                    }
                )
            },
            LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            ).apply {
                marginStart =
                    dp(7)
            }
        )

        actions.addView(
            smallClipButton(
                "SHARE",
                gold
            ) {
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

                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        },
                        "Share develop.uganda clip"
                    )
                )
            },
            LinearLayout.LayoutParams(
                0,
                dp(40),
                1f
            ).apply {
                marginStart =
                    dp(7)
            }
        )

        cardView.addView(
            actions
        )

        cardView.layoutParams =
            full(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                7
            )

        return cardView
    }

    private fun smallClipButton(
        value: String,
        accent: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text =
                value

            textSize =
                8f

            isAllCaps =
                false

            setTextColor(
                white
            )

            background =
                rounded(
                    0xFF082033.toInt(),
                    accent,
                    13
                )

            setOnClickListener {
                action.invoke()
            }
        }
    }

    private fun formatDuration(
        durationMs: Long
    ): String {
        val seconds =
            (
                durationMs /
                    1000L
                )
                .coerceAtLeast(
                    0L
                )

        return String.format(
            java.util.Locale.US,
            "%02d:%02d",
            seconds /
                60L,
            seconds %
                60L
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

    private fun openIndependentCamera(
        cameraClass: Class<*>
    ) {
        startActivity(
            Intent(
                this,
                cameraClass
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
