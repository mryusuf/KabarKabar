package io.mryusuf.kabarkabar.data.local.entity

import androidx.room.Entity
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.NewsCountry

@Entity(
    tableName = "articles",
    primaryKeys = ["id", "countryCode"]
)
data class ArticleEntity(
    val id: String,
    val countryCode: String,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val publishedAt: Long,
) {
    init {
        require(NewsCountry.fromCode(countryCode) != null) {
            "Unsupported country code: $countryCode"
        }
        require(url == url.trim()) { "Article URL must be canonical" }
        require(id == ArticleId.fromCanonicalUrl(url).value) {
            "Article ID must be derived from the canonical URL"
        }
        require(title.isNotBlank()) { "Article title must not be blank" }
    }
}
