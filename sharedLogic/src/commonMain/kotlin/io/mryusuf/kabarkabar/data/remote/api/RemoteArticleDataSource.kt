package io.mryusuf.kabarkabar.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.mryusuf.kabarkabar.data.remote.dto.NewsApiResponseDto
import io.mryusuf.kabarkabar.domain.model.NewsCountry

/**
 * Data source for fetching articles from a remote API.
 */
interface RemoteArticleDataSource {
    suspend fun fetchTopHeadlines(country: NewsCountry): NewsApiResponseDto
}

class KtorRemoteArticleDataSource(
    private val httpClient: HttpClient
) : RemoteArticleDataSource {
    override suspend fun fetchTopHeadlines(country: NewsCountry): NewsApiResponseDto {
        return httpClient.get("/v2/top-headlines") {
            parameter("country", country.code)
        }.body()
    }
}
