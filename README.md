# Floating Charge Ring

Standalone Android app (Kotlin, no extra libraries): a round, draggable floating neon charging ring
showing the battery percentage (with decimals), measured battery power in W and a bolt while charging.

## Use
1. Open the project folder in KlencodIDE as an existing Gradle project, build and install.
2. Open the app, tap the permission button and allow "Display over other apps".
3. Tap "تشغيل النافذة العائمة". Drag the ring with one finger. Use the slider (0-1000 px) to resize.
   0 hides the ring; the ring keeps its position when resized.
4. Stop it from the app or from the notification action.

## Notes
- The W value is battery-side power (|current| x voltage). It is not the charger's rating or USB input power.
- `BatteryReader.kt` is the same reader as in ChargingLiveMeter (see its comments); the app screen shows raw diagnostics.
- Animation pauses when the screen is off.
- This source has not been built or tested on a device in this environment.
