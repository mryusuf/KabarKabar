package io.mryusuf.kabarkabar.di

import io.mryusuf.kabarkabar.data.remote.api.NewsApiConfig
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

/**
 * iOS-specific entry point for Koin.
 */
fun initKoin(newsApiConfig: NewsApiConfig) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            modules(
                sharedModule,
                module {
                    single { newsApiConfig }
                }
            )
        }
    }
}

/**
 * Ergonomic bridge for Swift to access Koin-managed dependencies.
 */
class IOSDependencyContainer {
    fun getArticleRepository(): ArticleRepository =
        KoinPlatformTools.defaultContext().get().get()
}
