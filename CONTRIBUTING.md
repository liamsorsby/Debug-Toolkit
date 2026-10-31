# Contributing to Debug Toolkit

This guide is for developers working on the app, its CI, and the supporting
Cloudflare Worker. For what the app does, see the [README](README.md). To report a
security issue, follow [SECURITY.md](SECURITY.md) rather than opening a public issue.

## Architecture

Production features remain in the `app` module and are separated into `core`, `data`,
`domain`, `feature`, and `ui` packages. External services and Android platform APIs
sit behind interfaces. Koin modules construct repositories and ViewModels while tests
can replace those bindings with deterministic fakes.

Composable functions render state but do not call network or platform services.
ViewModels own asynchronous work and expose immutable state flows.

## Firebase and telemetry

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

The release workflow is started manually (Actions → Main release → Run workflow, on `main`). It repeats the deterministic release gates and then runs semantic-release.
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

### App version sunset

The app checks a public JSON policy endpoint before showing the normal app. The
`privacy-policy/` Cloudflare Worker serves `GET /version-policy?channel=play`
or `?channel=other` with this shape:

```json
{
  "minimumVersionCode": 123,
  "updateUrl": "https://play.google.com/store/apps/details?id=co.sorsby.debugtoolkit"
}
```

Deploy it with `npx wrangler deploy --config privacy-policy/wrangler.toml`. Set the
Worker variables `MIN_SUPPORTED_VERSION_CODE`, `PLAY_UPDATE_URL`, and
`OTHER_UPDATE_URL` in the Cloudflare dashboard or `privacy-policy/wrangler.toml`. Start
with a minimum of `0`; only raise it after a release with that version code and an
accessible update destination are available. The app selects `play` for Play Store
installs and `other` otherwise. Set the Play URL to the listing and the other URL to
a Firebase App Distribution or other HTTPS install link. Both default to the Play
listing until changed. The endpoint is public and must not contain secrets. It
returns `Cache-Control: no-store`.

Set `APP_UPDATE_POLICY_URL` to the full HTTPS endpoint URL ending in
`/version-policy` for Android builds. Locally, set it in `local.properties`; in
GitHub Actions, define it as a repository or environment variable. If it is unset,
the update gate is disabled. The app persists the last valid policy and uses it if
the endpoint is temporarily unavailable; if no valid policy has ever been received,
the app continues normally. An outdated Play install starts the immediate Play update
flow when available, otherwise the app opens the policy's update URL.

The Worker also serves AdMob's `/app-ads.txt` from `privacy-policy/app-ads.txt`.
Its route must cover the root of the developer website listed on Google Play.

### Landing page

The same Worker serves a multilingual landing page from `privacy-policy/site/`:

- `/` is English. Browsers whose `Accept-Language` prefers another app language get a
  302 to `/<slug>/` (for example `/de/`, `/pt-br/`, `/zh-tw/`) unless the visitor chose a
  language (`?lang=en` or the `dt_lang` cookie set by the footer language list).
- Every page has a canonical URL, `hreflang` alternates (with `x-default` → `/`), Open
  Graph/Twitter tags and `SoftwareApplication` JSON-LD. `/sitemap.xml` and `/robots.txt`
  are generated from the same locale list.
- Android visitors see a dismissible "download the app" bar (hidden again for 14 days
  after dismissal). Play links are localised with `hl` and carry a
  `utm_source=website&utm_medium=landing&utm_campaign=<locale>&utm_content=<placement>`
  install referrer.
- Images live in `privacy-policy/site/assets/` and are served from `/assets/` with a
  one-year immutable cache. Bump `ASSET_VERSION` in `site/render.js` whenever an asset
  changes. The browser script is `site/assets/site.client.js` (bundled as text and served
  as `/assets/site.js`).

Page text comes from two places, merged by `npm run site:strings` into
`privacy-policy/site/strings/<locale>.json` (committed):

- `site/copy.js`: marketing copy written for the site, one entry per locale.
- The app's `strings.xml` files: tagline, section names and the 11 tool names and
  descriptions, so the site always matches the app.

Run `npm run site:strings` after editing `copy.js` or any of those app strings, and
`npm run test:site` before pushing. CI runs `test:site`, which fails if the committed
JSON is stale. To add a language, add it to `site/locales.js` and `site/copy.js`.

Analytics are optional. Set `GA4_MEASUREMENT_ID` in `privacy-policy/wrangler.toml` (both
`[vars]` and `[previews.vars]`) to a GA4 web data stream ID (`G-…`); variables in
`wrangler.toml` replace dashboard values on deploy, so set it there rather than in the
dashboard. When it is empty the site ships no analytics code.

When set, the standard Google tag is rendered inline in `<head>` (so Google's tag
checker detects it) and runs on load with no consent banner; Google signals are off in the
GA4 property. The CSP allows `'unsafe-inline'` scripts (and so must
not use a nonce, which would disable it) so Cloudflare's injected Google tag gateway and
bot-detection scripts can run. Events: `play_store_click` (mark it
as a key event in GA4), `download_prompt_shown`, `download_prompt_dismissed` and
`language_selected`, each with `placement`/`locale` parameters.

Preview locally with `npx wrangler dev --config privacy-policy/wrangler.toml`. Landing
page changes are not part of the app, so use a `docs:` (or `ci:`) commit type to avoid
cutting an app release.

To roll back a sunset, lower `MIN_SUPPORTED_VERSION_CODE` in Worker configuration.
This is a client-side compatibility gate, not a tamper-proof security boundary or a
way to disable code already installed.

Separately, Play installs that are still supported are offered an optional
(flexible) Play update on start once Play has known about the update for at least
2 days, at most once every 3 days. When the download finishes, a "Restart"
snackbar installs it. Test this with Play internal app sharing or an internal test
track, as in-app updates only work for Play-installed builds.

### In-app reviews

The app asks Google Play for an in-app review after at least 3 successful
diagnostics, at least 2 days after first use, and at most once every 120 days. The
request fires on the next screen change after a successful run, so the dialog never
covers results. Play applies its own quota and may show nothing; the app carries on
either way. Test with Play internal app sharing or an internal test track.

Configure these repository or environment variables:

- `SONAR_PROJECT_KEY` and `SONAR_ORGANIZATION`.
- `FIREBASE_TESTER_GROUPS`, containing comma-separated App Distribution groups.
- `APP_UPDATE_POLICY_URL`, the HTTPS `privacy-policy` Worker endpoint ending in
  `/version-policy`; it can be scoped at the repository or deployment environment.
- `VERSION_CODE_OFFSET`, if the Play listing already has a version code greater than
  the `main` workflow run number. The published version code is this offset plus the
  run number.

Protect `main`, allow squash merging only, and require the Conventional PR title,
Tests/lint/coverage/profiles, Emulator integration tests, and SonarCloud checks. Add
`app/google-services.json` only through CI or local ignored configuration; never commit
service accounts or signing material.
