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

## CI and releases

Pull requests and `main` use separate GitHub Actions workflows. Pull requests must use
a Conventional Commit title because the repository is expected to use squash merges.
Examples include `fix(network): clarify capacity values`, `feat(dns): add HTTPS
records`, and `feat!: redesign diagnostic storage`. The title determines whether
semantic-release proposes a patch, minor, or major version. The first tagged release
is `1.0.0`; subsequent releases increment from the latest `v*` tag.

The pull-request workflow runs JVM tests, coverage enforcement, Android lint, an
optimized release build, profile and benchmark module builds, emulator integration
tests, and SonarCloud analysis. The emulator job verifies Android framework readiness
and requires successful instrumentation result files so infrastructure failures cannot
be reported as passing tests. It builds the APKs before starting a minimum-supported
API 29 emulator so compilation does not compete with the running device. After every
required check passes, same-repository pull
requests distribute a debug APK through Firebase App Distribution. Fork pull requests
never receive deployment credentials and are not distributed.

Pushes to `main` repeat the deterministic release gates and then run semantic-release.
Emulator tests are not repeated because protected `main` accepts only pull requests
that passed the required integration check. Fastlane builds
a signed release APK and AAB during release preparation. After semantic-release creates
the Git tag, its publish phase distributes the APK through Firebase App Distribution,
uploads the AAB to Google Play's internal track, and creates the GitHub release. Measured
Macrobenchmark regression tests remain a managed-physical-device responsibility;
GitHub-hosted emulators only validate instrumentation and profile compatibility.

Configure these GitHub environments:

- `pr-distribution` for trusted pull-request Firebase uploads.
- `production-release` for Firebase and Google Play delivery. Repository maintainers
  can require approval for this environment.

Configure these GitHub Actions secrets:

- `FIREBASE_GOOGLE_SERVICES_JSON`: contents of `app/google-services.json`.
- `FIREBASE_APP_ID`: Firebase Android application ID.
- `FIREBASE_SERVICE_ACCOUNT_JSON`: service account JSON with App Distribution access.
- `PLAY_SERVICE_ACCOUNT_JSON`: Play Console service account JSON.
- `ANDROID_KEYSTORE_BASE64`: base64-encoded Google Play upload keystore.
- `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
- `SONAR_TOKEN`: SonarCloud analysis token.

Configure these repository or environment variables:

- `SONAR_PROJECT_KEY` and `SONAR_ORGANIZATION`.
- `FIREBASE_TESTER_GROUPS`, containing comma-separated App Distribution groups.
- `VERSION_CODE_OFFSET`, if the Play listing already has a version code greater than
  the `main` workflow run number. The published version code is this offset plus the
  run number.

Protect `main`, allow squash merging only, and require the Conventional PR title,
Tests/lint/coverage/profiles, Emulator integration tests, and SonarCloud checks. Add
`app/google-services.json` only through CI or local ignored configuration; never commit
service accounts or signing material.
