# PULSE V172 — Native Android Broadcast Reporter Camera

Target: **Android / Samsung**.

## What V172 implements now

- Native CameraX 1.6.1 viewfinder and recording.
- CameraX OverlayEffect applied to Preview + VideoCapture, so selected newsroom graphics are burned into the saved video.
- PULSE LIVE / BREAKING / REC graphics.
- Reporter, station and headline lower thirds.
- Automatic high-accuracy phone location updates every second.
- Latitude, longitude, altitude/elevation, accuracy, compass heading and speed.
- Reverse-geocoded place name.
- Weather cache with 10-minute refresh and retention during network loss.
- Date/time, timezone and running timecode.
- Network transport + Android link-upstream estimate.
- Battery and free storage.
- Gallery export to Movies/PULSE.
- 1-second synchronized JSONL telemetry sidecar saved to Documents/PULSE.
- Native zoom, exposure compensation, lens flip and torch when supported.
- UI prepared for professional manual controls, scopes, audio monitoring, Live and Edit tabs.

## Important implementation truth

Some requested controls require hardware/vendor support. V172 must query Samsung/Android capabilities rather than display fake values:
- ISO / sensor exposure time / manual focus / Kelvin/tint: Camera2 interop where the camera advertises MANUAL_SENSOR and related capabilities.
- 4K60, 1080p120/240, HLG10/HDR and stabilization combinations: use CameraX SessionConfig feature groups and high-speed capabilities.
- RAW: use ImageCaptureCapabilities before offering DNG.
- H.265: use hardware codec capability checks.
- "ProRes-style": export profile/bitrate target only; Android devices generally do not expose Apple ProRes capture.
- ND is a simulation unless a device has a physical variable-ND system.
- Headphone monitoring depends on the active audio route and latency.

## Live mode

Recommended library: RootEncoder 2.8.0. It supports RTMP and SRT and exposes stream metrics. Wire `GenericStream`/`RtmpStream`/`SrtStream` into a foreground service with:
- low-latency / quality profiles,
- reconnect,
- adaptive bitrate,
- simultaneous local backup recording,
- stream health and dropped-frame metrics.

Do not silently continue camera/microphone capture in the background. Android background capture should run as a visible foreground service with an ongoing notification.

## Editor

Recommended: AndroidX Media3 Transformer 1.11.0 for trim, composition, effects and export. Keep the V172 telemetry JSONL sidecar as the canonical editable metadata source. A production remux stage can additionally copy samples into an MP4 timed metadata track with Media3 Mp4Muxer.

## Project structure

app/
  src/main/
    AndroidManifest.xml
    java/com/sentongoharuna/pulse/
      MainActivity.kt
      ReporterOverlayState.kt
      TelemetrySample.kt
      TelemetryRecorder.kt
      WeatherRepository.kt
    res/values/styles.xml

## Next production modules

camera/
  CameraCapabilityRepository.kt
  Camera2ManualController.kt
  HighSpeedController.kt
  RawPhotoController.kt
  AudioMonitor.kt

live/
  StreamForegroundService.kt
  RootEncoderController.kt
  AdaptiveBitrateController.kt
  StreamHealthRepository.kt
  TeleprompterState.kt
  CommentOverlay.kt

telemetry/
  TelemetryTrackMuxer.kt
  ReverseGeocoder.kt
  WeatherRepository.kt
  NetworkHealthSampler.kt

editor/
  EditorActivity.kt
  TimelineModel.kt
  Media3ExportEngine.kt
  TelemetryOverlayEditor.kt
