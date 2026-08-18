package io.mryusuf.kabarkabar.presentation.articlelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mryusuf.kabarkabar.R
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.presentation.articlelist.components.ArticleRow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (ArticleId) -> Unit,
    viewModel: ArticleListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessages = mapOf(
        ArticleListUiError.NetworkUnavailable to stringResource(R.string.error_network),
        ArticleListUiError.RemoteUnavailable to stringResource(R.string.error_remote),
        ArticleListUiError.PersistenceUnavailable to stringResource(R.string.error_persistence),
        ArticleListUiError.MalformedData to stringResource(R.string.error_malformed),
        ArticleListUiError.Unknown to stringResource(R.string.error_unknown),
    )
    val currentErrorMessages by rememberUpdatedState(errorMessages)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is ArticleListUiEvent.ShowRefreshError -> currentErrorMessages.getValue(event.error)
                is ArticleListUiEvent.ShowObservationError -> currentErrorMessages.getValue(event.error)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.article_list_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val content = uiState.content) {
                ArticleListContent.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.loading_articles),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                ArticleListContent.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.empty_articles),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is ArticleListContent.Data -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = content.articles,
                            key = { it.id.value }
                        ) { article ->
                            ArticleRow(
                                article = article,
                                onClick = { onArticleClick(article.id) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                is ArticleListContent.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(articleListErrorMessageRes(content.error)),
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = viewModel::refresh) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun articleListErrorMessageRes(error: ArticleListUiError): Int = when (error) {
    ArticleListUiError.NetworkUnavailable -> R.string.error_network
    ArticleListUiError.RemoteUnavailable -> R.string.error_remote
    ArticleListUiError.PersistenceUnavailable -> R.string.error_persistence
    ArticleListUiError.MalformedData -> R.string.error_malformed
    ArticleListUiError.Unknown -> R.string.error_unknown
}
