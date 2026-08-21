package io.mryusuf.kabarkabar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import io.mryusuf.kabarkabar.fakes.FakeArticleRepository
import io.mryusuf.kabarkabar.presentation.articledetail.ArticleDetailViewModel
import io.mryusuf.kabarkabar.presentation.articlelist.ArticleListViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CountrySelectionTest : KoinTest {

    private val usArticle = ArticleTestData.article1
    private val idArticle = ArticleTestData.article2

    private val fakeRepository = FakeArticleRepository(
        initialArticles = listOf(usArticle),
    ).apply {
        emit(listOf(idArticle), NewsCountry.ID)
    }

    private val testModule = module {
        single<ArticleRepository> { fakeRepository }
        viewModel { ArticleListViewModel(get()) }
        viewModel { (articleId: ArticleId, country: NewsCountry) ->
            ArticleDetailViewModel(get(), articleId, country)
        }
    }

    @get:Rule(order = 0)
    val koinModuleRule = KoinModuleRule(testModule)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun country_switch_shows_isolated_caches() {
        // The compact selector exposes both its label and its current value.
        val countrySelector = composeTestRule.onNodeWithContentDescription("Country: US")
        countrySelector.assertIsDisplayed()
        val minimumTouchTargetHeight = with(composeTestRule.density) { 48.dp.toPx() }
        assertTrue(
            countrySelector.fetchSemanticsNode().boundsInRoot.height >= minimumTouchTargetHeight,
            "Country selector must expose at least a 48dp touch target",
        )
        composeTestRule.onNodeWithText(usArticle.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(idArticle.title).assertDoesNotExist()

        // Opening the selector exposes readable country names and selection semantics.
        composeTestRule.onNodeWithContentDescription("Country: US").performClick()
        composeTestRule
            .onNodeWithContentDescription("United States (US)")
            .assertIsSelected()
        composeTestRule
            .onNodeWithContentDescription("Indonesia (ID)")
            .assertIsNotSelected()
        composeTestRule.onAllNodesWithText("🇺🇸 US").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("🇮🇩 ID").assertCountEquals(1)

        // Selecting ID changes the feed without navigating to another screen.
        composeTestRule.onNodeWithContentDescription("Indonesia (ID)").performClick()

        // Then ID shows its own data
        composeTestRule.onNodeWithContentDescription("Country: ID").assertIsDisplayed()
        composeTestRule.onNodeWithText(idArticle.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(usArticle.title).assertDoesNotExist()

        // Switching back restores the US cache through the same selector.
        composeTestRule.onNodeWithContentDescription("Country: ID").performClick()
        composeTestRule.onNodeWithContentDescription("United States (US)").performClick()

        // Then US data is restored immediately
        composeTestRule.onNodeWithContentDescription("Country: US").assertIsDisplayed()
        composeTestRule.onNodeWithText(usArticle.title).assertIsDisplayed()
    }
}
