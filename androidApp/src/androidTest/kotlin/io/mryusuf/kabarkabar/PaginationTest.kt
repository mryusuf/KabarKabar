package io.mryusuf.kabarkabar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import io.mryusuf.kabarkabar.fakes.FakeArticleRepository
import io.mryusuf.kabarkabar.presentation.articlelist.ArticleListViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PaginationTest : KoinTest {

    private val fakeRepository = FakeArticleRepository(
        initialArticles = listOf(ArticleTestData.article1),
        initialHasMore = true,
    ).also { repository ->
        repository.configureLoadMore(
            result = RefreshResult.Success,
            articles = listOf(ArticleTestData.article2),
            hasMoreAfter = false,
        )
    }

    private val testModule = module {
        single<ArticleRepository> { fakeRepository }
        viewModel { ArticleListViewModel(get()) }
    }

    @get:Rule(order = 0)
    val koinModuleRule = KoinModuleRule(testModule)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun successful_load_more_appends_persisted_like_observation() {
        composeTestRule.onNodeWithText(ArticleTestData.article1.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(ArticleTestData.article2.title).assertIsDisplayed()
        composeTestRule.onNodeWithText("Load more").assertDoesNotExist()
        assertEquals(1, fakeRepository.loadMoreCallCount)
    }
}

@RunWith(AndroidJUnit4::class)
class PaginationFailureTest : KoinTest {

    private val fakeRepository = FakeArticleRepository(
        initialArticles = listOf(ArticleTestData.article1),
        initialHasMore = true,
    ).also { repository ->
        repository.configureLoadMore(
            result = RefreshResult.Failure(SyncError.Network),
            hasMoreAfter = true,
        )
    }

    private val testModule = module {
        single<ArticleRepository> { fakeRepository }
        viewModel { ArticleListViewModel(get()) }
    }

    @get:Rule(order = 0)
    val koinModuleRule = KoinModuleRule(testModule)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun failed_load_more_preserves_rows_and_exposes_retry_button() {
        composeTestRule.onNodeWithText(ArticleTestData.article1.title).assertIsDisplayed()
        composeTestRule.onNodeWithText("Load more").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
        assertEquals(1, fakeRepository.loadMoreCallCount)
    }
}
