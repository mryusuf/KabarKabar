package io.mryusuf.kabarkabar.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.mryusuf.kabarkabar.data.local.dao.ArticleDao
import io.mryusuf.kabarkabar.data.local.db.AppDatabase
import io.mryusuf.kabarkabar.data.local.db.LocalArticleDataSource
import io.mryusuf.kabarkabar.data.local.db.RoomLocalArticleDataSource
import io.mryusuf.kabarkabar.data.remote.api.KtorRemoteArticleDataSource
import io.mryusuf.kabarkabar.data.remote.api.NewsApiConfig
import io.mryusuf.kabarkabar.data.remote.api.RemoteArticleDataSource
import io.mryusuf.kabarkabar.data.remote.api.createNewsApiClient
import io.mryusuf.kabarkabar.data.repository.OfflineFirstArticleRepository
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import io.mryusuf.kabarkabar.getDatabaseBuilder
import io.mryusuf.kabarkabar.getHttpClientEngine
import org.koin.dsl.module

/**
 * Shared Koin module containing cross-platform data and domain components.
 */
internal val sharedModule = module {
    single<AppDatabase> { getDatabaseBuilder().build() }
    single<ArticleDao> { get<AppDatabase>().articleDao() }
    single<LocalArticleDataSource> { RoomLocalArticleDataSource(get()) }

    single<HttpClientEngine> { getHttpClientEngine() }
    single<HttpClient> { createNewsApiClient(get(), get()) }
    single<RemoteArticleDataSource> { KtorRemoteArticleDataSource(get()) }

    single<ArticleRepository> {
        OfflineFirstArticleRepository(
            remoteDataSource = get(),
            localDataSource = get(),
        )
    }
}
