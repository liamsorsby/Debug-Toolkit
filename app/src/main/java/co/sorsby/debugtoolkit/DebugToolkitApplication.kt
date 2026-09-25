package co.sorsby.debugtoolkit

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import co.sorsby.debugtoolkit.data.dns.CloudflareDnsRepository
import co.sorsby.debugtoolkit.data.dns.DirectNameserverResolver
import co.sorsby.debugtoolkit.data.dns.DnsRepository
import co.sorsby.debugtoolkit.data.dns.RawDnsResolver
import co.sorsby.debugtoolkit.data.http.HttpInspector
import co.sorsby.debugtoolkit.data.http.OkHttpInspector
import co.sorsby.debugtoolkit.data.network.AndroidNetworkMonitor
import co.sorsby.debugtoolkit.data.network.NetworkMonitor
import co.sorsby.debugtoolkit.data.settings.DataStoreSettingsRepository
import co.sorsby.debugtoolkit.data.settings.SettingsRepository
import co.sorsby.debugtoolkit.data.speed.CloudflareSpeedTestRepository
import co.sorsby.debugtoolkit.data.speed.SpeedTestRepository
import co.sorsby.debugtoolkit.data.tls.SocketTlsInspector
import co.sorsby.debugtoolkit.data.tls.TlsInspector
import co.sorsby.debugtoolkit.feature.DnsViewModel
import co.sorsby.debugtoolkit.feature.HttpViewModel
import co.sorsby.debugtoolkit.feature.NetworkViewModel
import co.sorsby.debugtoolkit.feature.SettingsViewModel
import co.sorsby.debugtoolkit.feature.SpeedViewModel
import co.sorsby.debugtoolkit.feature.TlsViewModel
import co.sorsby.debugtoolkit.telemetry.FirebaseJourneyTracker
import co.sorsby.debugtoolkit.telemetry.JourneyTracker
import com.newrelic.agent.android.NewRelic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

class DebugToolkitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // New Relic reports app-health metrics (crashes, network, and startup performance), so
        // it is started unconditionally like Crashlytics/Performance Monitoring, independent of
        // analytics consent. It only starts when a token has been supplied for this build
        // variant; local builds without one simply skip it.
        if (BuildConfig.NEW_RELIC_TOKEN.isNotBlank()) {
            NewRelic.withApplicationToken(BuildConfig.NEW_RELIC_TOKEN).start(this)
        }
        startKoin {
            androidContext(this@DebugToolkitApplication)
            modules(appModule)
        }
    }
}

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = false
        }
    }
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single<JourneyTracker>(createdAtStart = true) {
        FirebaseJourneyTracker(
            context = androidContext(),
            settingsRepository = get(),
            scope = get(),
        )
    }
    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    single {
        androidContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    single<NetworkMonitor> {
        AndroidNetworkMonitor(androidContext(), get(), get())
    }
    single<DirectNameserverResolver> { RawDnsResolver() }
    single<DnsRepository> {
        CloudflareDnsRepository(
            client = get(),
            json = get(),
            endpoint = "https://cloudflare-dns.com/dns-query".toHttpUrl(),
            nameserverResolver = get(),
        )
    }
    single<TlsInspector> { SocketTlsInspector() }
    single<HttpInspector> { OkHttpInspector(get()) }
    single<SpeedTestRepository> {
        CloudflareSpeedTestRepository(
            client = get(),
            endpoint = "https://speed.cloudflare.com".toHttpUrl(),
        )
    }
    viewModel { SettingsViewModel(get()) }
    viewModel { NetworkViewModel(get()) }
    viewModel { DnsViewModel(get(), get()) }
    viewModel { TlsViewModel(get(), get()) }
    viewModel { HttpViewModel(get(), get()) }
    viewModel { SpeedViewModel(get(), get()) }
}
