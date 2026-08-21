package io.mryusuf.kabarkabar.ui.navigation

import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavRoutesTest {

    @Test
    fun image_url_with_encoded_components_is_not_decoded_twice() {
        val imageUrl = "https://example.com/hero.jpg?part=one%2Ftwo&token=a+b"
        val route = NavRoutes.imageViewer(imageUrl)
        val encodedArgument = route.substringAfter("image_viewer/")
        val navigationDecodedArgument = URLDecoder.decode(
            encodedArgument,
            StandardCharsets.UTF_8.toString(),
        )

        assertEquals(imageUrl, NavRoutes.imageUrlFromArgument(navigationDecodedArgument))
    }

    @Test
    fun stable_id_with_percent_encoding_survives_navigation_decode() {
        val articleId = ArticleId.fromCanonicalUrl("https://example.com/story%2Fpart")
        val route = NavRoutes.articleDetail(articleId, NewsCountry.US)
        val encodedArgument = route.substringAfter("article_detail/").substringBefore("/")
        val navigationDecodedArgument = URLDecoder.decode(
            encodedArgument,
            StandardCharsets.UTF_8.toString(),
        )

        assertEquals(articleId, NavRoutes.articleIdFromArgument(navigationDecodedArgument))
    }

    @Test
    fun country_route_argument_accepts_only_supported_country_codes() {
        assertEquals(NewsCountry.US, NavRoutes.countryFromArgument("us"))
        assertEquals(NewsCountry.ID, NavRoutes.countryFromArgument("id"))
        assertNull(NavRoutes.countryFromArgument("gb"))
    }
}
