package io.mryusuf.kabarkabar.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory for the Ktor [HttpClient] configured for NewsAPI.
 */
fun createNewsApiClient(
    engine: HttpClientEngine,
    config: NewsApiConfig
): HttpClient {
    return HttpClient(engine) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    println("NewsApiClient: ${redactApiKey(message, config.apiKey)}")
                }
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 10000
        }

        defaultRequest {
            url(config.baseUrl)
            header("X-Api-Key", config.apiKey)
        }
    }
}

internal fun redactApiKey(message: String, apiKey: String): String {
    if (apiKey.isEmpty()) return message
    return message.replace(apiKey, "[REDACTED]")
}
