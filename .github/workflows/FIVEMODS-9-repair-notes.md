# FIVEMODS 9 — Interview and workflow home repair

This is a focused repair of the existing `build-fivemods-9-status-policy-fix.yml`.
The YAML preparation and content checks pass locally. **An APK has not been compiled or tested on a phone here.** Android SDK and Gradle are unavailable in this workspace. The workflow builds the APK on GitHub Actions and uploads it only after preparation, verification, and compilation succeed.

## Use the YAML

Replace `.github/workflows/build-fivemods-9-status-policy-fix.yml` with the attached file. Keep the existing `develop_uganda_five_mode_source.zip` at the repository root and `.github/workflows/build-fivemods-8.yml` in place. Run **FIVEMODS 9 - Interview and workflow home repair APK** in Actions.

The download artifact is named `FIVEMODS-9-Interview-Home-Repair-<run number>`. Inside it, the APK is `FIVEMODS-9-Interview-Home-Repair.apk`, accompanied by its SHA-256 checksum and source verification. The app package remains `com.sentongoharuna.pulse`; its existing label remains `develop.uganda`. No external download site is involved.

## What caused the missing pages

The General Hub and original Newsroom home are present in the current F9 source and registered in the manifest. The General Hub remains the launcher, with 36 `routeCard(` occurrences and its existing Newsroom destination.

The failure is in `DevelopUgandaV28012ProWorkflowConsole.statusRibbon()`. The old function interpolates battery/readiness percentages and user text into a format string, then calls `.format(Locale.US, ...)`. The literal `%` characters are interpreted as format instructions. Running the old format with ordinary values reproduces `UnknownFormatConversionException` on Java 17.

`buildHome()` calls the console refresh while constructing the page. That exception reaches the existing catch block, which substitutes Safe Home. The normal page construction stops before the workflow cards are added. This establishes a reproducible source-level cause consistent with the missing-page report; device screenshots/logs of this repaired APK are still required to confirm the installed result.

The repair extracts and applies the **exact console file diff** from the real `FIVEMODS_8_PATCH` in `build-fivemods-8.yml`. It does not write replacement pages. Patch dry-run and application both require zero fuzz; missing/ambiguous payload blocks or conflicts stop preparation.

This restores the specific F8 dependency repair needed by the existing home pages. **It is not a complete rebase of F9 onto all real F8 feature changes.** The previously identified broader F8/F9 integration remains separate; this delivery preserves the current F9 application and fixes the two reported areas.

## Interview darkness repairs and limits

1. The six named backing sites retain their original resource-derived alpha: focus reticle 38; motion, light, audio and thermal readouts 66; experience banner 82. The colour preparer no longer replaces the first N matching calls with alpha 204. Each view is identified by unique surrounding code and checked explicitly. The XML palette and the named `#CC031829` token remain unchanged for their other existing uses.
2. Interview already requests 30 fps in its existing mode profile, but its `VideoCapture.Builder` never received that request. The repaired code selects a device-reported range whose upper limit matches the existing request, passes that range to CameraX video setup, and uses the same selection for Camera2 auto-exposure. It prefers the lowest available lower limit at that upper limit so the device has its supported low-light frame-rate flexibility. If no matching range exists, no new range is invented and CameraX keeps automatic negotiation. Other modes keep their existing selection code.
3. The existing F9 exposure helper may run before the first ambient-light sample arrives and return without applying its low-light setting. The first real sensor sample now completes that setup before recording. The helper's existing EV thresholds, manual ISO/shutter checks, operator lock, focus lock, and recording-start call are unchanged. The new sensor callback does not change a running take.

The uploaded video is itself dark and contains no HUD covering the frame. It is approximately one second of 4K H.264 footage at about 60 fps. That clip predates the later F9 changes; it does **not** prove the current F9 APK still records Interview at 60 fps. No actual ISO/shutter capture telemetry accompanies the clip, so its exact exposure cause cannot be established from the file alone.

These changes repair concrete setup defects and the opacity regression. They do not guarantee sufficient exposure in every scene. In particular, the existing extra EV assistance still depends on an available light sensor and intentionally respects manual settings and locks. A new Interview clip and its real ISO/shutter/EV readouts are needed to confirm brightness. The separate device-dependent completely black preview has not been diagnosed or declared fixed.

Android documents that CameraX's target frame rate is a negotiation request, not a guaranteed delivered rate: [VideoCapture.Builder](https://developer.android.com/reference/androidx/camera/video/VideoCapture.Builder#setTargetFrameRate(android.util.Range)). Supported ranges and their limitations are described in [CameraInfo](https://developer.android.com/reference/androidx/camera/core/CameraInfo#getSupportedFrameRateRanges()).

## File-by-file changes

| File | Status | Reason |
| --- | --- | --- |
| `.github/workflows/build-fivemods-9-status-policy-fix.yml` | Edited delivery | Retains the original F9 compressed payload and build command; adds strict preparation, exact F8 console repair, content checks, and correctly named APK artifacts. |
| `.fivemods9hotfix/repair_existing_f9.py` | New, generated at build time | Contains the readable focused repair and verification code embedded in the YAML; no extra repository upload is needed. |
| `.fivemods9hotfix/prepare_fivemods9_hotfix.py` | Edited at build time | Removes the opacity-changing first-N replacements; checks uniquely named backing sites; counts AGP/import matches before substitution. |
| `.fivemods9hotfix/apply_fivemods9.py` | Edited at build time | Fixes only `regex()` to count all matches before substitution; the resulting F9 feature code is preserved. |
| `.github/workflows/build-fivemods-8.yml` | Untouched input | Supplies the existing console repair from its real F8 patch; missing input fails loudly. |
| `develop_uganda_five_mode_source.zip` | Untouched input | Retains the original source baseline used by the accepted current F9 build. |
| `f9-repair-audit/*` | New build evidence | Records the restored patch, exact source diff, file hashes, and APK checksum. |

The following inventory compares the final app source with a fresh replay of the original F9 Status Policy workflow. No app source files were added or deleted. Only the two listed Kotlin files changed; every other app/build resource below was compared byte for byte.

| Source file | Status | Reason |
| --- | --- | --- |
| `FIVE_MODE_BUILD_NOTES.md` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V273-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V274-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V275-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V277-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V278-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V279-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-11-FULL-PRESERVATION-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-12-FIX1-RUNTIME-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-12-FIX2-PHONE-CRASH-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-12-FIX3-FULL-FUNCTION-BUTTON-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-12-PRO-WORKFLOW-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-13-PRO-STABLE-MASTER-REFINEMENT-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-18-LIVE-OPERATOR-INTELLIGENCE-CONTROL-POLISH-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-19-FIELD-RELIABILITY-PRODUCTION-QA-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-19-FIX3-RECORDER-RECOVERY-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-19-FIX4-ADAPTIVE-CORE-ENGINE-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-19-FIX6-FHD-PRO-CLEAR-COMPILE-SAFE-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-19-FIX7-FHD-PRO-CLEAR-REPAIR-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-19-FIX8-PRO-IMAGE-ENGINE-AUDIT.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `V280-README.txt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/build.gradle.kts` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/AndroidManifest.xml` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaAdaptiveFormatUi.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaAdditiveCameraModes.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaBrandMetadataActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaBrandMetadataStore.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaCameraActivity.kt` | Edited | Restores six original alphas and repairs Interview FPS negotiation and late initial light-sensor setup. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaCameraHealthActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaCameraModeNavigator.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaClipQc.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaColorEngine.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaColorStudioActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaColorTuner.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaContinuityMemory.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaDirectorOverlayView.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaEditorActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaEverydayColorMixer.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaFieldIntelligencePanel.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaFivemods8Theme.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaFivemods9DualOutputExporter.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaGeneralHubActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaIndependentCameras.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaInstantReviewDialog.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaInterviewAnnotatedExporter.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaInterviewHudView.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaInterviewObservationEngine.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaLensIntelligence.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaLiveActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaLiveGradePanel.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaMediaInbox.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaModeProfiles.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaNavySheet.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaNewsroomActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaOperatorExperience.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaPhotoSuiteActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaShotAssistView.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaStatusPolicy.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaStoryPackager.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaStoryPackagesActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaStoryPlayerActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaTranscriptEngine.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaUnifiedControlDeck.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV242QualityMatchPro.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV244CinemaControl.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV255ProMonitorView.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV262RemoteDirector.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV263MultiCamDirector.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV264LiveCutDirector.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV265ProxySyncReview.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV269SmartStoryDesk.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV270Guidance.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV271LiveCoach.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV272FieldSoundContinuity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV273MotionShotControl.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV274MediaVault.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV275ControlSurface.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV276RecordingSafety.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV277LightingExposure.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV278LiveWorkflow.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV279ActiveShooting.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV28012ProWorkflowConsole.kt` | Edited | Restores the exact percent-safe statusRibbon fix from the real F8 patch so the full home can finish constructing. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV28012RuntimeGuard.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV28012SafeActions.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV28013StableMasterRefinement.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV28018LiveOperatorIntelligence.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV28019FieldReliability.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/DevelopUgandaV280SmartDirector.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/MainActivity.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/ReporterOverlayState.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/TelemetryRecorder.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/TelemetrySample.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/java/com/sentongoharuna/pulse/WeatherRepository.kt` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/res/values/develop_uganda_theme.xml` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `app/src/main/res/values/styles.xml` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `build.gradle.kts` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `gradle.properties` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |
| `settings.gradle.kts` | Untouched | Byte-identical to the current F9 build; existing behaviour retained. |

## Verification performed

- Parsed the delivered YAML and checked every shell block with `bash -n`.
- Replayed the full preparation from a fresh copy of the repository source ZIP and real F8 workflow.
- Confirmed the original embedded F9 compressed payload is byte-for-byte unchanged.
- Reproduced the old home formatting exception on Java 17; ran the restored format successfully with normal names, literal percentages, and `%s%n` in project names.
- Checked all six original-alpha sites; deleting or duplicating each named anchor is rejected.
- Confirmed the repaired regex helper rejects missing and duplicate matches before replacement.
- Confirmed a missing real F8 workflow stops before source extraction.
- Verified the launcher, Newsroom registration, full-home sections, route-card inventory and registered home destinations.
- Compared all source files against the unmodified F9 workflow output: two changed, 96 identical, none added/deleted.
- Confirmed the F9 HUD and mandatory dual exporter remain identical to the original payload; the Status setter repair, Kotlin 2.2.0 and AGP 8.11.1 remain present.
- Did not run Android compilation, installation, camera recording, gallery verification, or device-specific black-preview diagnostics.

After the GitHub build succeeds, check that launching the app opens the full workflow home, the existing Newsroom page can be reached, and Interview preview plus a newly recorded clip are visible. The APK's `DU_INTERVIEW_REPAIR` log tag records the negotiated frame-rate request for diagnosis. A successful compile alone does not establish correct exposure on the phone.

YAML SHA-256: `cff37b10c5316b41cf5c4bc2b25285a56594e94ccb446657790f5e362f20df5d`
