#!/usr/bin/env bash

set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
deadline=$((SECONDS + 180))
results_directory="app/build/outputs/androidTest-results/connected/debug"

adb -s "$serial" wait-for-device

until [[ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] &&
    adb -s "$serial" shell service check package 2>/dev/null | grep -q "found" &&
    adb -s "$serial" shell service check activity 2>/dev/null | grep -q "found" &&
    [[ "$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')" =~ ^[0-9]+$ ]]; do
    if ((SECONDS >= deadline)); then
        echo "Android framework services did not become ready within 180 seconds." >&2
        adb -s "$serial" shell getprop >&2 || true
        exit 1
    fi
    sleep 5
done

# Restarting the adb server forces Gradle's own ddmlib connection to read the
# device's properties fresh, rather than reusing a stale pre-boot snapshot.
# Without this, AGP can intermittently see an empty API level for an
# otherwise fully booted device and skip it with "Unknown API Level".
adb kill-server
adb start-server
adb -s "$serial" wait-for-device

# Espresso refuses to interact with a view hierarchy whose window lacks focus, which is how a
# headless emulator sits once the display sleeps or the keyguard is showing. Waking the device,
# dismissing the keyguard and pinning the screen on keeps every activity focusable for the whole
# run, rather than only until the first idle timeout.
prepare_display() {
    adb -s "$serial" shell input keyevent KEYCODE_WAKEUP || true
    adb -s "$serial" shell wm dismiss-keyguard || true
    adb -s "$serial" shell svc power stayon true || true
    adb -s "$serial" shell settings put system screen_off_timeout 1800000 || true
}

prepare_display

rm -rf "$results_directory"

attempt=1
max_attempts=2
until ./gradlew :app:connectedDebugAndroidTest --stacktrace --console=plain; do
    if ((attempt >= max_attempts)); then
        echo "connectedDebugAndroidTest failed after ${attempt} attempts." >&2
        exit 1
    fi
    echo "connectedDebugAndroidTest failed, retrying (attempt $((attempt + 1))/${max_attempts})..." >&2
    adb kill-server
    adb start-server
    adb -s "$serial" wait-for-device
    prepare_display
    attempt=$((attempt + 1))
done

result_file="$(find "$results_directory" -maxdepth 1 -type f -name "TEST-*.xml" -print -quit)"

if [[ -z "$result_file" ]]; then
    echo "Instrumentation completed without producing test results." >&2
    exit 1
fi

if ! grep -Eq '<testsuites tests="[1-9][0-9]*" failures="0" errors="0"' "$result_file"; then
    echo "Instrumentation results do not contain a successful test run: $result_file" >&2
    exit 1
fi
