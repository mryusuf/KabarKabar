package io.mryusuf.kabarkabar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.mryusuf.kabarkabar.domain.model.ArticleId

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val publishedAt: Long,
) {
    init {
        require(url == url.trim()) { "Article URL must be canonical" }
        require(id == ArticleId.fromCanonicalUrl(url).value) {
            "Article ID must be derived from the canonical URL"
        }
        require(title.isNotBlank()) { "Article title must not be blank" }
    }
}
