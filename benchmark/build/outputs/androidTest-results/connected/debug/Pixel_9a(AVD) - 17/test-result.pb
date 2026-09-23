î"
A
StartupBenchmark co.sorsby.debugtoolkit.benchmarkcoldStartup„
§java.lang.AssertionError: ERRORS (not suppressed): EMULATOR DEBUGGABLE
WARNINGS (suppressed): 

ERROR: Running on Emulator
    Benchmark is running on an emulator, which is not representative of
    real user devices. Use a physical device to benchmark. Emulator
    benchmark improvements might not carry over to a real user's
    experience (or even regress real device performance).

ERROR: Benchmark Target is Debuggable
    Target package co.sorsby.debugtoolkit is running with debuggable=true in its manifest,
    which drastically reduces runtime performance in order to support debugging
    features. Run benchmarks with debuggable=false. Debuggable affects execution
    speed in ways that mean benchmark improvements might not carry over to a
    real user's experience (or even regress release performance).

While you can suppress these errors (turning them into warnings)
PLEASE NOTE THAT EACH SUPPRESSED ERROR COMPROMISES ACCURACY

// Sample suppression, in a benchmark module's build.gradle:
android {
    defaultConfig {
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,DEBUGGABLE"
    }
}
	at androidx.benchmark.ConfigurationErrorKt.checkAndGetSuppressionState(ConfigurationError.kt:102)
	at androidx.benchmark.macro.MacrobenchmarkKt.checkErrors(Macrobenchmark.kt:225)
	at androidx.benchmark.macro.MacrobenchmarkKt.macrobenchmark(Macrobenchmark.kt:264)
	at androidx.benchmark.macro.MacrobenchmarkKt.macrobenchmarkWithStartupMode(Macrobenchmark.kt:435)
	at androidx.benchmark.macro.junit4.MacrobenchmarkRule.measureRepeated(MacrobenchmarkRule.kt:87)
	at co.sorsby.debugtoolkit.benchmark.StartupBenchmark.coldStartup(StartupBenchmark.kt:19)
java.lang.RuntimeExceptionùjava.lang.RuntimeException: java.lang.AssertionError: ERRORS (not suppressed): EMULATOR DEBUGGABLE
WARNINGS (suppressed): 

ERROR: Running on Emulator
    Benchmark is running on an emulator, which is not representative of
    real user devices. Use a physical device to benchmark. Emulator
    benchmark improvements might not carry over to a real user's
    experience (or even regress real device performance).

ERROR: Benchmark Target is Debuggable
    Target package co.sorsby.debugtoolkit is running with debuggable=true in its manifest,
    which drastically reduces runtime performance in order to support debugging
    features. Run benchmarks with debuggable=false. Debuggable affects execution
    speed in ways that mean benchmark improvements might not carry over to a
    real user's experience (or even regress release performance).

While you can suppress these errors (turning them into warnings)
PLEASE NOTE THAT EACH SUPPRESSED ERROR COMPROMISES ACCURACY

// Sample suppression, in a benchmark module's build.gradle:
android {
    defaultConfig {
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,DEBUGGABLE"
    }
}
	at androidx.benchmark.ConfigurationErrorKt.checkAndGetSuppressionState(ConfigurationError.kt:102)
	at androidx.benchmark.macro.MacrobenchmarkKt.checkErrors(Macrobenchmark.kt:225)
	at androidx.benchmark.macro.MacrobenchmarkKt.macrobenchmark(Macrobenchmark.kt:264)
	at androidx.benchmark.macro.MacrobenchmarkKt.macrobenchmarkWithStartupMode(Macrobenchmark.kt:435)
	at androidx.benchmark.macro.junit4.MacrobenchmarkRule.measureRepeated(MacrobenchmarkRule.kt:87)
	at co.sorsby.debugtoolkit.benchmark.StartupBenchmark.coldStartup(StartupBenchmark.kt:19)

	at com.android.tools.androidtest.testengine.descriptor.AndroidDynamicTestDescriptor.execute(AndroidDynamicTestDescriptor.kt:68)
	at com.android.tools.androidtest.testengine.descriptor.AndroidDynamicTestDescriptor.execute(AndroidDynamicTestDescriptor.kt:36)
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:511)
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1450)
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:2019)
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:187)
"Ê

logcatandroid–
Õ/Users/liam.sorsby/AndroidStudioProjects/DebugToolkit/benchmark/build/outputs/androidTest-results/connected/debug/Pixel_9a(AVD) - 17/logcat-co.sorsby.debugtoolkit.benchmark.StartupBenchmark-coldStartup.txt*±

device-infoandroidñ
ì/Users/liam.sorsby/AndroidStudioProjects/DebugToolkit/benchmark/build/outputs/androidTest-results/connected/debug/Pixel_9a(AVD) - 17/device-info.pb