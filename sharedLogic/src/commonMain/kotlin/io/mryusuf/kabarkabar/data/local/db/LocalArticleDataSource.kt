package io.mryusuf.kabarkabar.data.local.db

import io.mryusuf.kabarkabar.data.local.dao.ArticleDao
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import kotlinx.coroutines.flow.Flow

/**
 * Data source for interacting with locally persisted articles.
 */
interface LocalArticleDataSource {
    fun observeAll(country: NewsCountry): Flow<List<ArticleEntity>>
    fun observeById(id: String, country: NewsCountry): Flow<ArticleEntity?>

    /** Replaces the committed snapshot atomically; a failed replacement must preserve it. */
    suspend fun replaceSnapshot(articles: List<ArticleEntity>, country: NewsCountry)

    /** Appends new article IDs while preserving the committed row on duplicates. */
    suspend fun appendArticles(articles: List<ArticleEntity>, country: NewsCountry)
}

class RoomLocalArticleDataSource(
    private val articleDao: ArticleDao
) : LocalArticleDataSource {
    override fun observeAll(country: NewsCountry): Flow<List<ArticleEntity>> =
        articleDao.observeAll(country.code)

    override fun observeById(id: String, country: NewsCountry): Flow<ArticleEntity?> =
        articleDao.observeById(id, country.code)

    override suspend fun replaceSnapshot(articles: List<ArticleEntity>, country: NewsCountry) {
        require(articles.all { it.countryCode == country.code }) {
            "All replacement articles must belong to ${country.code}"
        }
        articleDao.replaceAll(articles, country.code)
    }

    override suspend fun appendArticles(articles: List<ArticleEntity>, country: NewsCountry) {
        require(articles.all { it.countryCode == country.code }) {
            "All articles to append must belong to ${country.code}"
        }
        articleDao.appendAll(articles, country.code)
    }
}
