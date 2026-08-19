package io.mryusuf.kabarkabar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ImageViewerTest : KoinTest {

    private val articleWithImage = ArticleTestData.articleWithImage
    private val articleWithoutImage = ArticleTestData.article1
    private val articleWithBlankImageUrl = ArticleTestData.articleWithBlankImageUrl
    private val articleWithMalformedImageUrl = ArticleTestData.articleWithMalformedImageUrl

    private val fakeRepository = FakeArticleRepository(
        initialArticles = listOf(
            articleWithImage,
            articleWithoutImage,
            articleWithBlankImageUrl,
            articleWithMalformedImageUrl,
        ),
    )

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
    fun opens_image_viewer_when_hero_image_is_clicked() {
        // Go to detail
        composeTestRule.onNodeWithText(articleWithImage.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()

        // Click hero image
        composeTestRule.onNodeWithContentDescription("Article image").performClick()

        // Image viewer should be open - check for Close button
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        // Close it
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        // Should be back in detail
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()
        composeTestRule.onNodeWithText(articleWithImage.title).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()

        // The detail route is still exactly one layer above the viewer.
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("KabarKabar").assertIsDisplayed()
        composeTestRule.onNodeWithText(articleWithImage.title).assertIsDisplayed()
    }

    @Test
    fun system_back_closes_viewer_then_preserves_detail_and_list_back_stack() {
        composeTestRule.onNodeWithText(articleWithImage.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Article image").performClick()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()
        composeTestRule.onNodeWithText(articleWithImage.title).assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.onNodeWithText("KabarKabar").assertIsDisplayed()
        composeTestRule.onNodeWithText(articleWithImage.title).assertIsDisplayed()
    }

    @Test
    fun opening_viewer_does_not_refresh_article_repository() {
        composeTestRule.onNodeWithText(articleWithImage.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()
        assertEquals(1, fakeRepository.refreshCallCount)

        composeTestRule.onNodeWithContentDescription("Article image").performClick()
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()

        assertEquals(1, fakeRepository.refreshCallCount)
    }

    @Test
    fun hero_image_not_present_when_article_has_no_image_url() {
        // Go to detail of article without image
        composeTestRule.onNodeWithText(articleWithoutImage.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()

        // Hero image should NOT be present
        composeTestRule.onNodeWithContentDescription("Article image").assertDoesNotExist()
    }

    @Test
    fun hero_image_not_present_when_article_image_url_is_blank() {
        composeTestRule.onNodeWithText(articleWithBlankImageUrl.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Article image").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
    }

    @Test
    fun hero_image_not_present_when_article_image_url_is_malformed() {
        composeTestRule.onNodeWithText(articleWithMalformedImageUrl.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Article image").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
    }

    @Test
    fun failed_image_still_leaves_viewer_close_affordance_usable() {
        composeTestRule.onNodeWithText(articleWithImage.title).performClick()
        composeTestRule.onNodeWithText("Article Detail").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Article image").performClick()

        // The fixture URL is intentionally not an image resource. Viewer controls
        // must remain usable even when Coil cannot decode the requested content.
        composeTestRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    }
}
