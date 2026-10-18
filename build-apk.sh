#!/bin/sh
# Builds the debug APK on any Linux host with a JDK 17+ and the Android SDK:
#
#   ./build-apk.sh
#
# The Android SDK comes from ANDROID_HOME (or ANDROID_SDK_ROOT, or sdk.dir in
# local.properties). Gradle is the wrapper's 9.6.0, the version CI uses
# (.github/workflows/android.yml); its download is checked against the
# SHA-256 in gradle/wrapper/gradle-wrapper.properties. Prints the APK path.
set -eu

cd "$(dirname "$0")"
if [ -z "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ] && ! grep -q '^sdk\.dir=' local.properties 2>/dev/null; then
  echo "✗ no Android SDK: set ANDROID_HOME or sdk.dir in local.properties"
  exit 2
fi

sh ./gradlew --console=plain :app:assembleDebug
ls "$PWD"/app/build/outputs/apk/debug/*.apk
