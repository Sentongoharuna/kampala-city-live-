develop.uganda dfv238

BASE
- Exact V238 Operator Experience Pro.
- dfv238 is intentionally NOT based on V239 or V240.

CAM REPORTER CAMERA
- LUTS is permanent at the top-left and cannot be hidden by V238 CLEAN VIEW.
- ASPECT is permanent at the top-right.
- LUTS keeps the real V235 Uganda LUT Mixer function and styling.
- ASPECT keeps the real V236 format/safe-frame function.
- LENS / RECORD / LIGHT remain available for shooting.
- Other V238 camera controls are moved, not deleted, into a right-side SETTINGS drawer.
- Existing button listeners/functions are preserved when their rows move into the drawer.
- The duplicate legacy LOOK control is hidden so LUTS remains the single primary look control.

PROTECTED COLOR FUNCTION
- DevelopUgandaColorEngine.kt unchanged.
- DevelopUgandaColorTuner.kt unchanged.
- DevelopUgandaEverydayColorMixer.kt unchanged.
- DevelopUgandaNavySheet.kt unchanged.
- CameraX recording/output code is not replaced.
- Only the visible V235 GRADE chip wording is renamed to LUTS.

HOME
- V238 home structure is not redesigned by this patch.

VERSION
- versionCode 23801
- versionName dfv238
