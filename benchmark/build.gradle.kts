plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "co.sorsby.debugtoolkit.benchmark"
    compileSdk {
        version = release(37)
    }

    androidComponents {
        beforeVariants(selector().withBuildType("debug")) {
            it.enable = false
        }
    }

    defaultConfig {
        minSdk = 29
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
}

dependencies {
    constraints {
        implementation(libs.wire.runtime) {
            because("Wire 6.4.5 fixes the ByteArrayProtoReader32 length overflow")
        }
    }
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
}
