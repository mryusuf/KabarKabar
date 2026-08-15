package io.mryusuf.kabarkabar.data.local.db

import io.mryusuf.kabarkabar.data.local.dao.ArticleDao
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data source for interacting with locally persisted articles.
 */
interface LocalArticleDataSource {
    fun observeAll(): Flow<List<ArticleEntity>>
    fun observeById(id: String): Flow<ArticleEntity?>

    /** Replaces the committed snapshot atomically; a failed replacement must preserve it. */
    suspend fun replaceSnapshot(articles: List<ArticleEntity>)
}

class RoomLocalArticleDataSource(
    private val articleDao: ArticleDao
) : LocalArticleDataSource {
    override fun observeAll(): Flow<List<ArticleEntity>> = articleDao.observeAll()

    override fun observeById(id: String): Flow<ArticleEntity?> = articleDao.observeById(id)

    override suspend fun replaceSnapshot(articles: List<ArticleEntity>) {
        articleDao.replaceAll(articles)
    }
}
