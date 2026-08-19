package io.mryusuf.kabarkabar.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.mryusuf.kabarkabar.BuildConfig
import io.mryusuf.kabarkabar.getDatabaseBuilder
import io.mryusuf.kabarkabar.getHttpClientEngine
import io.mryusuf.kabarkabar.data.local.dao.ArticleDao
import io.mryusuf.kabarkabar.data.local.db.AppDatabase
import io.mryusuf.kabarkabar.data.local.db.LocalArticleDataSource
import io.mryusuf.kabarkabar.data.local.db.RoomLocalArticleDataSource
import io.mryusuf.kabarkabar.data.remote.api.KtorRemoteArticleDataSource
import io.mryusuf.kabarkabar.data.remote.api.NewsApiConfig
import io.mryusuf.kabarkabar.data.remote.api.RemoteArticleDataSource
import io.mryusuf.kabarkabar.data.remote.api.createNewsApiClient
import io.mryusuf.kabarkabar.data.repository.OfflineFirstArticleRepository
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
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
    single<AppDatabase> { getDatabaseBuilder().build() }
    single<ArticleDao> { get<AppDatabase>().articleDao() }
    single<LocalArticleDataSource> { RoomLocalArticleDataSource(get()) }

    single<NewsApiConfig> {
        object : NewsApiConfig {
            override val apiKey: String = BuildConfig.NEWS_API_KEY
        }
    }
    single<HttpClientEngine> { getHttpClientEngine() }
    single<HttpClient> { createNewsApiClient(get(), get()) }
    single<RemoteArticleDataSource> { KtorRemoteArticleDataSource(get()) }

    single<ArticleRepository> {
        OfflineFirstArticleRepository(
            remoteDataSource = get(),
            localDataSource = get(),
        )
    }

    viewModel { ArticleListViewModel(get()) }
    viewModel { (articleId: ArticleId, country: NewsCountry) ->
        ArticleDetailViewModel(get(), articleId, country)
    }
}
