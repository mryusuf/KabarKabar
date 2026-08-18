package io.mryusuf.kabarkabar.ui.navigation

import io.mryusuf.kabarkabar.domain.model.ArticleId
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

class NavRoutesTest {

    @Test
    fun stable_id_with_percent_encoding_survives_navigation_decode() {
        val articleId = ArticleId.fromCanonicalUrl("https://example.com/story%2Fpart")
        val route = NavRoutes.articleDetail(articleId)
        val encodedArgument = route.substringAfter("article_detail/")
        val navigationDecodedArgument = URLDecoder.decode(
            encodedArgument,
            StandardCharsets.UTF_8.toString(),
        )

        assertEquals(articleId, NavRoutes.articleIdFromArgument(navigationDecodedArgument))
    }
}
