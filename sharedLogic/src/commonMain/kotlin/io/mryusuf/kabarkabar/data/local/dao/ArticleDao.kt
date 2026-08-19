package io.mryusuf.kabarkabar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.mryusuf.kabarkabar.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE countryCode = :countryCode ORDER BY publishedAt DESC, id ASC")
    fun observeAll(countryCode: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id AND countryCode = :countryCode")
    fun observeById(id: String, countryCode: String): Flow<ArticleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE countryCode = :countryCode")
    suspend fun deleteAll(countryCode: String)

    @Transaction
    suspend fun replaceAll(articles: List<ArticleEntity>, countryCode: String) {
        require(articles.all { it.countryCode == countryCode }) {
            "All replacement articles must belong to $countryCode"
        }
        deleteAll(countryCode)
        insertAll(articles)
    }
}
