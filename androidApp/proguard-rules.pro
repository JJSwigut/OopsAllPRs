# Oops All PRs release keep rules.
#
# Keep this file conservative until release smoke tests prove tighter rules are
# safe. Android Gradle Plugin default rules still apply through
# proguard-android-optimize.txt.

# Manifest/runtime entry points. Manifest classes are normally retained by AGP,
# but keeping them here makes the release contract explicit.
-keep class com.jjswigut.oopsallprs.MainActivity { *; }
-keep class com.jjswigut.oopsallprs.platform.RestTimerReceiver { *; }

# SQLDelight generated database APIs are used across shared repositories and
# platform drivers. Keep generated names stable for release diagnostics and any
# runtime adapter lookups.
-keep class com.jjswigut.oopsallprs.db.** { *; }

# Compose Multiplatform resource accessors and collectors back packaged seed
# data and shared resources, including exercises.csv.
-keep class oopsallprs.shared.generated.resources.** { *; }
