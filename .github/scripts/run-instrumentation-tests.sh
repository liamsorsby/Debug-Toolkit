#!/usr/bin/env bash

set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
deadline=$((SECONDS + 180))
results_directory="app/build/outputs/androidTest-results/connected/debug"

adb -s "$serial" wait-for-device

until [[ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]] &&
    adb -s "$serial" shell service check package 2>/dev/null | grep -q "found" &&
    adb -s "$serial" shell service check activity 2>/dev/null | grep -q "found"; do
    if ((SECONDS >= deadline)); then
        echo "Android framework services did not become ready within 180 seconds." >&2
        adb -s "$serial" shell getprop >&2 || true
        exit 1
    fi
    sleep 5
done

rm -rf "$results_directory"
./gradlew :app:connectedDebugAndroidTest \
    --rerun \
    --stacktrace \
    --console=plain

result_file="$(find "$results_directory" -maxdepth 1 -type f -name "TEST-*.xml" -print -quit)"

if [[ -z "$result_file" ]]; then
    echo "Instrumentation completed without producing test results." >&2
    exit 1
fi

if ! grep -Eq '<testsuites tests="[1-9][0-9]*" failures="0" errors="0"' "$result_file"; then
    echo "Instrumentation results do not contain a successful test run: $result_file" >&2
    exit 1
fi
