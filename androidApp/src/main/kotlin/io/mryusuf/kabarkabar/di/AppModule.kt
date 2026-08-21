package io.mryusuf.kabarkabar.di

import io.mryusuf.kabarkabar.BuildConfig
import io.mryusuf.kabarkabar.data.remote.api.NewsApiConfig
import io.mryusuf.kabarkabar.di.androidSharedModule
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.presentation.articlelist.ArticleListViewModel
import io.mryusuf.kabarkabar.presentation.articledetail.ArticleDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Main DI module for the Android application.
 *
 * It bridges the Android-specific presentation layer with the shared domain/data layers.
 */
val appModule = module {
    includes(androidSharedModule)

    single<NewsApiConfig> {
        object : NewsApiConfig {
            override val apiKey: String = BuildConfig.NEWS_API_KEY
        }
    }

    viewModel { ArticleListViewModel(get()) }
    viewModel { (articleId: ArticleId, country: NewsCountry) ->
        ArticleDetailViewModel(get(), articleId, country)
    }
}
