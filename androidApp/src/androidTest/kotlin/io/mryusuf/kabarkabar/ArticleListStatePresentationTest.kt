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
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest

@RunWith(AndroidJUnit4::class)
class LoadingStatePresentationTest : KoinTest {

    private val refreshGate = CompletableDeferred<Unit>()
    private val fakeRepository = FakeArticleRepository(refreshGate = refreshGate)
    private val testModule = module {
        single<ArticleRepository> { fakeRepository }
        viewModel { ArticleListViewModel(get()) }
    }

    @get:Rule(order = 0)
    val koinModuleRule = KoinModuleRule(testModule)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loading_state_has_a_readable_progress_label() {
        composeTestRule.onNodeWithText("Loading articles…").assertIsDisplayed()
        refreshGate.complete(Unit)
    }
}

@RunWith(AndroidJUnit4::class)
class EmptyStatePresentationTest : KoinTest {

    private val fakeRepository = FakeArticleRepository(
        initialRefreshResult = RefreshResult.Success,
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
    fun empty_state_is_distinct_from_error_and_has_no_retry_action() {
        composeTestRule.onNodeWithText("No headlines found").assertIsDisplayed()
        composeTestRule.onNodeWithText("No articles found.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }
}

@RunWith(AndroidJUnit4::class)
class BlockingErrorStatePresentationTest : KoinTest {

    private val fakeRepository = FakeArticleRepository(
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
    fun blocking_error_has_readable_message_and_retry_action() {
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Network unavailable. Check your connection.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
