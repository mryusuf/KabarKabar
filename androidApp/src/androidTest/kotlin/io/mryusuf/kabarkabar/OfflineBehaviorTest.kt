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
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class OfflineBehaviorTest : KoinTest {

    private val fakeRepository = FakeArticleRepository(
        initialArticles = listOf(ArticleTestData.article1),
        initialRefreshResult = RefreshResult.Failure(SyncError.Network),
    )

    private val testModule = module {
        single<ArticleRepository> { fakeRepository }
        viewModel { ArticleListViewModel(get()) }
    }

    @get:Rule(order = 0)
    val koinModuleRule = KoinModuleRule(testModule)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun t5_cached_articles_remain_visible_when_initial_refresh_fails() {
        val cachedArticle = ArticleTestData.article1

        // MainActivity starts the real NavHost and ArticleListViewModel. The fake
        // repository already contains the local snapshot and returns a classified
        // failure from the ViewModel's automatic synchronization.
        composeTestRule.onNodeWithText(cachedArticle.title).assertIsDisplayed()

        assertEquals(1, fakeRepository.refreshCallCount)
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }
}
