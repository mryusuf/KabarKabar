package io.mryusuf.kabarkabar.presentation.articledetail

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.mryusuf.kabarkabar.R
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.presentation.util.DateFormatter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: ArticleId,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    viewModel: ArticleDetailViewModel = koinViewModel(
        parameters = { parametersOf(articleId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.article_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                ArticleDetailUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.loading_article),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is ArticleDetailUiState.Data -> {
                    val formattedDate = remember(state.article.publishedAt) {
                        DateFormatter.format(state.article.publishedAt)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        usableImageUrl(state.article.imageUrl)?.let { imageUrl ->
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = stringResource(
                                        R.string.article_image_content_description
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 10f)
                                        .clickable { onImageClick(imageUrl) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.article.title,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            state.article.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 28.sp
                                )
                            }
                        }
                    }
                }

                ArticleDetailUiState.NotFound -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.article_not_found_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.article_not_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                is ArticleDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.error_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(detailErrorMessageRes(state.error)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun detailErrorMessageRes(error: ArticleDetailUiError): Int = when (error) {
    ArticleDetailUiError.NetworkUnavailable -> R.string.error_network
    ArticleDetailUiError.RemoteUnavailable -> R.string.error_remote
    ArticleDetailUiError.PersistenceUnavailable -> R.string.error_persistence
    ArticleDetailUiError.MalformedData -> R.string.error_malformed
    ArticleDetailUiError.Unknown -> R.string.error_unknown
}

private fun usableImageUrl(rawImageUrl: String?): String? {
    val imageUrl = rawImageUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = Uri.parse(imageUrl)
    val hasHttpScheme = uri.scheme == "http" || uri.scheme == "https"
    return imageUrl.takeIf { hasHttpScheme && !uri.host.isNullOrBlank() }
}
