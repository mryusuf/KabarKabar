package io.mryusuf.kabarkabar.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsApiResponseDto(
    @SerialName("status") val status: String,
    @SerialName("totalResults") val totalResults: Int? = null,
    @SerialName("articles") val articles: List<NewsApiArticleDto>? = null,
)
