import com.google.firebase.perf.plugin.FirebasePerfExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
    jacoco
}

// Local, gitignored overrides for secrets that are normally supplied as environment variables
// in CI. Lets a developer set values once on their machine instead of exporting shell env vars.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
fun secret(name: String): String = System.getenv(name) ?: localProperties.getProperty(name).orEmpty()

val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
}
val newRelicDebugToken = secret("NEW_RELIC_DEBUG_TOKEN")
val newRelicReleaseToken = secret("NEW_RELIC_RELEASE_TOKEN")
val newRelicConfigured = newRelicDebugToken.isNotBlank() || newRelicReleaseToken.isNotBlank()
if (newRelicConfigured) {
    apply(plugin = "newrelic")
}
val releaseStoreFile = secret("ANDROID_KEYSTORE_PATH").ifBlank { null }
val releaseStorePassword = secret("ANDROID_KEYSTORE_PASSWORD").ifBlank { null }
val releaseKeyAlias = secret("ANDROID_KEY_ALIAS").ifBlank { null }
val releaseKeyPassword = secret("ANDROID_KEY_PASSWORD").ifBlank { null }
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

jacoco {
    toolVersion = "0.8.13"
}

android {
    namespace = "co.sorsby.debugtoolkit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "co.sorsby.debugtoolkit"
        minSdk = 29
        targetSdk = 37
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "0.1.0-local"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            isPseudoLocalesEnabled = true
            buildConfigField("String", "NEW_RELIC_TOKEN", "\"$newRelicDebugToken\"")
            if (firebaseConfigured) {
                configure<FirebasePerfExtension> {
                    // Local JVM unit tests compile against the debug variant and run on a
                    // stubbed Android runtime, which is incompatible with the Performance
                    // Monitoring plugin's OkHttp bytecode instrumentation. Automatic network,
                    // screen, and app-start traces remain enabled for the release build.
                    setInstrumentationEnabled(false)
                }
            }
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "NEW_RELIC_TOKEN", "\"$newRelicReleaseToken\"")
            optimization {
                enable = true
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    constraints {
        implementation(libs.guava) {
            because("Versions before 32.0.0 contain insecure temporary-file APIs")
        }
    }
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.androidx.profileinstaller)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.performance)
    implementation(libs.newrelic.android.agent)
    baselineProfile(project(":baselineprofile"))
    testImplementation(libs.junit)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.tls)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.accessibility)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

baselineProfile {
    automaticGenerationDuringBuild = false
}

val coverageClasses = fileTree(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
) {
    include("co/sorsby/debugtoolkit/domain/**")
    include("co/sorsby/debugtoolkit/core/network/**")
    include("co/sorsby/debugtoolkit/data/dns/**")
    include("co/sorsby/debugtoolkit/data/http/**")
    include("co/sorsby/debugtoolkit/data/ping/**")
    include("co/sorsby/debugtoolkit/data/portscan/**")
    include("co/sorsby/debugtoolkit/data/whois/**")
    include("co/sorsby/debugtoolkit/data/publicip/**")
    include("co/sorsby/debugtoolkit/data/lan/**")
    include("co/sorsby/debugtoolkit/data/speed/**")
    include("co/sorsby/debugtoolkit/data/tls/**")
    include("co/sorsby/debugtoolkit/feature/**")
    exclude("**/*\$\$serializer*")
    exclude("**/*\$Companion*")
    exclude("**/*UiState*")
    exclude("**/DnsPayload*")
    exclude("**/DnsAnswer*")
    exclude("**/AndroidLinkAddress*")
}
val coverageExecutionData = layout.buildDirectory.file(
    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(coverageExecutionData)
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("testDebugUnitTest")
    classDirectories.setFrom(coverageClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(coverageExecutionData)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.register("qualityCheck") {
    dependsOn("testDebugUnitTest", "lintDebug", "jacocoTestCoverageVerification")
}