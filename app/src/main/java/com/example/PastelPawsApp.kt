package com.example

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.AppDatabase
import com.example.data.network.FeedNetworkClient
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.PastelPawsRepository
import com.example.notifications.NotificationHelper
import com.example.ui.components.SoundEffectPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val preferencesRepository: AppPreferencesRepository = AppPreferencesRepository(context)
    val networkClient: FeedNetworkClient = FeedNetworkClient()
    val repository: PastelPawsRepository = PastelPawsRepository(
        context = context,
        database = database,
        preferences = preferencesRepository,
        network = networkClient,
    )
    val soundPlayer: SoundEffectPlayer = SoundEffectPlayer()
}

class PastelPawsApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        NotificationHelper.scheduleAnnualBirthdayAlarm(this)
        appScope.launch {
            container.repository.ensureSeeded()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_meme_cache"))
                    .maxSizeBytes(60L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
