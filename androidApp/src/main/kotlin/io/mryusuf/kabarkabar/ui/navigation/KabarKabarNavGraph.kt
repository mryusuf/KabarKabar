package io.mryusuf.kabarkabar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.presentation.articledetail.ArticleDetailScreen
import io.mryusuf.kabarkabar.presentation.articlelist.ArticleListScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NavRoutes {
    const val ARTICLE_LIST = "article_list"
    const val ARTICLE_DETAIL = "article_detail/{articleId}"

    fun articleDetail(id: ArticleId): String {
        val encodedId = URLEncoder.encode(id.value, StandardCharsets.UTF_8.toString())
        return "article_detail/$encodedId"
    }

    fun articleIdFromArgument(value: String?): ArticleId? =
        value?.let { runCatching { ArticleId.fromValue(it) }.getOrNull() }
}

@Composable
fun KabarKabarNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.ARTICLE_LIST
    ) {
        composable(NavRoutes.ARTICLE_LIST) {
            ArticleListScreen(
                onArticleClick = { articleId ->
                    navController.navigate(NavRoutes.articleDetail(articleId))
                }
            )
        }
        composable(
            route = NavRoutes.ARTICLE_DETAIL,
            arguments = listOf(
                navArgument("articleId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val articleId = NavRoutes.articleIdFromArgument(
                backStackEntry.arguments?.getString("articleId")
            )
            if (articleId != null) {
                ArticleDetailScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
