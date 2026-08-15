package io.mryusuf.kabarkabar.data.remote.api

/**
 * Configuration for the NewsAPI client.
 */
interface NewsApiConfig {
    val apiKey: String
    val baseUrl: String get() = "https://newsapi.org"
}
