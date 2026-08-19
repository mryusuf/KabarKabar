package io.mryusuf.kabarkabar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import io.mryusuf.kabarkabar.fakes.FakeArticleRepository
import io.mryusuf.kabarkabar.presentation.articledetail.ArticleDetailViewModel
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ArticleNavigationTest : KoinTest {

    private val fakeRepository = FakeArticleRepository(
        initialArticles = listOf(ArticleTestData.article1, ArticleTestData.article2),
    )

    private val testModule = module {
        single<ArticleRepository> { fakeRepository }
        viewModel { ArticleListViewModel(get()) }
        viewModel { (articleId: ArticleId) -> ArticleDetailViewModel(get(), articleId) }
    }

    @get:Rule(order = 0)
    val koinModuleRule = KoinModuleRule(testModule)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun t4_list_to_detail_shows_the_selected_article() {
        val selectedArticle = ArticleTestData.article2

        // The list is rendered by MainActivity's real NavHost and the click
        // travels through ArticleListScreen using the stable ArticleId.
        composeTestRule.onNodeWithText(ArticleTestData.article1.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedArticle.title).assertIsDisplayed()
        composeTestRule.onNodeWithText("Featured").assertDoesNotExist()

        val rootBounds = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot
        val firstTitleBounds = composeTestRule
            .onNodeWithText(ArticleTestData.article1.title)
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            firstTitleBounds.top < rootBounds.height * 0.35f,
            "A missing first image must not leave a giant blank hero area",
        )

        composeTestRule.onNodeWithText(selectedArticle.title).performClick()

        // Detail must resolve the article selected from the list, not merely
        // show any detail destination.
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedArticle.title).assertIsDisplayed()
        val detailTitleBounds = composeTestRule
            .onNodeWithText(selectedArticle.title)
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            detailTitleBounds.top < rootBounds.height * 0.35f,
            "A missing detail image must not leave a giant blank hero area",
        )
        selectedArticle.description?.let {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText(ArticleTestData.article1.title).assertDoesNotExist()

        // App-bar back remains a small, useful assertion of the same route.
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        composeTestRule.onNodeWithText("KabarKabar").assertIsDisplayed()
        composeTestRule.onNodeWithText(selectedArticle.title).assertIsDisplayed()
    }
}

class KoinModuleRule(private val module: Module) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            loadKoinModules(module)
            try {
                base.evaluate()
            } finally {
                unloadKoinModules(module)
            }
        }
    }
}
