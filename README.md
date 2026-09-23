# Debug Toolkit

Debug Toolkit is a native Android application for inspecting the active network and
running focused diagnostics. It uses Kotlin, Jetpack Compose, Material 3, coroutines,
Koin, OkHttp, and Preferences DataStore.

## Tools

- Network status, transport, metering, local addresses, link estimates, and Wi-Fi RSSI.
- Cloudflare latency, jitter, download, and upload measurements.
- TLS protocol, cipher suite, and peer certificate-chain inspection.
- Cloudflare DNS-over-HTTPS queries for A, AAAA, CNAME, MX, NS, TXT, SOA, CAA, SRV,
  and PTR records.
- HTTPS GET and HEAD inspection with status, redirects, headers, timing, and a bounded
  text preview.

Wi-Fi dBm is a radio-signal measurement, not a physical-distance measurement. Speed
results are point-in-time estimates affected by the device, radio conditions, route,
and Cloudflare location.

## Architecture

Production features remain in the `app` module and are separated into `core`, `data`,
`domain`, `feature`, and `ui` packages. External services and Android platform APIs
sit behind interfaces. Koin modules construct repositories and ViewModels while tests
can replace those bindings with deterministic fakes.

Composable functions render state but do not call network or platform services.
ViewModels own asynchronous work and expose immutable state flows.

## Firebase and privacy

Firebase Crashlytics is enabled when Firebase is configured so production crashes,
ANRs, and sanitized diagnostic failures can be investigated. Firebase Analytics and
Performance Monitoring remain disabled until analytics consent is explicitly granted;
unset consent has the same effective behavior as denied. Revoking consent disables
both services again.

Journey events contain only screen route names, diagnostic tool types, success or
failure outcomes, categorized errors, and durations. Entered domains, URLs, DNS
answers, certificate data, HTTP headers and bodies, local addresses, and Wi-Fi details
are never attached to telemetry. Crashlytics breadcrumbs use the same restricted event
set and do not assign a user identifier.

To connect a Firebase project, register Android application
`co.sorsby.debugtoolkit`, download its `google-services.json`, and place it at
`app/google-services.json`. That file is ignored by Git. The Google Services and
Crashlytics plugins are applied automatically when the file exists; without it the app
remains buildable and telemetry safely operates as a no-op. Performance uses only
explicit sanitized diagnostic traces. Automatic network instrumentation is deliberately
disabled so inspected URLs and DNS query parameters cannot enter telemetry.

The app stores only theme, analytics-consent, and Cloudflare-disclosure preferences.

Network diagnostics run only after user action and results are not persisted. DNS and
speed tests connect to Cloudflare. TLS and HTTP tools connect to the endpoint entered
by the user. HTTP body previews are restricted to textual content and 1 MiB.

Cloudflare documents its DNS-over-HTTPS service for third-party use. The
`speed.cloudflare.com` transfer endpoints are used by Cloudflare's open-source speed
test engine but are not published as a versioned public API; that dependency is isolated
behind `SpeedTestRepository` so it can be replaced if availability or terms change.

`ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` provide connection information.
`ACCESS_FINE_LOCATION` is requested through Android 12 and `NEARBY_WIFI_DEVICES` on
Android 13 or later solely to expose Wi-Fi RSSI. No background location is requested.

## Quality gates

Use Java 25 as declared in `.tool-versions`.

- `./gradlew qualityCheck` runs JVM tests, Android lint, and coverage verification.
- `./gradlew connectedDebugAndroidTest` runs Compose and platform integration tests on
  a connected emulator or device.
- `./gradlew assembleRelease` confirms the release variant builds.
- `./gradlew :app:generateBaselineProfile` regenerates startup profiles.
- `./gradlew :benchmark:connectedBenchmarkAndroidTest` measures cold startup on a
  connected physical device. AndroidX Benchmark intentionally rejects emulator results.

Coverage is enforced at 90% lines and 85% branches across domain logic, ViewModels,
network clients, TLS inspection, and coroutine call handling. Generated serialization
classes, declarative models, Compose UI, DataStore, and Android framework entry
points/adapters are excluded because they are covered by instrumentation tests or do
not contain independently testable business decisions.

GitHub Actions runs these gates for pushes to `main` and all pull requests.
