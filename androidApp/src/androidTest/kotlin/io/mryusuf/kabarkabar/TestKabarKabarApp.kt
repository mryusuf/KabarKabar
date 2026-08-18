package io.mryusuf.kabarkabar

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * A test-only application class that avoids production Koin initialization.
 */
class TestKabarKabarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@TestKabarKabarApp)
                modules(emptyList())
            }
        }
    }
}
