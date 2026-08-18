package io.mryusuf.kabarkabar

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.mryusuf.kabarkabar.di.appModule
import io.mryusuf.kabarkabar.initializeAndroidPlatform
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KabarKabarApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()

        // Initialize platform-specific requirements
        initializeAndroidPlatform(this)

        startKoin {
            androidLogger()
            androidContext(this@KabarKabarApp)
            modules(appModule)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }
}
