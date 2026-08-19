package io.mryusuf.kabarkabar.presentation.articlelist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.mryusuf.kabarkabar.R
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.presentation.articlelist.components.ArticleRow
import io.mryusuf.kabarkabar.presentation.articlelist.components.ProminentArticleRow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    onArticleClick: (ArticleId, NewsCountry) -> Unit,
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.article_list_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    CountrySelector(
                        selectedCountry = uiState.selectedCountry,
                        onCountrySelected = viewModel::onCountrySelected,
                    )
                }
            )
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
                            CircularProgressIndicator(strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.loading_articles),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                ArticleListContent.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.empty_articles_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.empty_articles),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                is ArticleListContent.Data -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            items = content.articles,
                            key = { _, article -> article.id.value }
                        ) { index, article ->
                            if (index == 0) {
                                ProminentArticleRow(
                                    article = article,
                                    onClick = { onArticleClick(article.id, uiState.selectedCountry) }
                                )
                            } else {
                                ArticleRow(
                                    article = article,
                                    onClick = { onArticleClick(article.id, uiState.selectedCountry) }
                                )
                            }
                            if (index < content.articles.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }

                is ArticleListContent.Error -> {
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
                                text = stringResource(articleListErrorMessageRes(content.error)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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

@Composable
private fun CountrySelector(
    selectedCountry: NewsCountry,
    onCountrySelected: (NewsCountry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val countries = remember { NewsCountry.values().toList() }
    val selectedCode = selectedCountry.code.uppercase()
    val selectedContentDescription = stringResource(
        R.string.country_selector_content_description,
        selectedCode,
    )

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = selectedContentDescription
                },
        ) {
            Text(countryDisplayLabel(selectedCountry))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            countries.forEach { country ->
                val isSelected = country == selectedCountry
                val accessibleLabel = countryAccessibilityLabel(country)
                DropdownMenuItem(
                    text = { Text(countryDisplayLabel(country)) },
                    onClick = {
                        expanded = false
                        onCountrySelected(country)
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = accessibleLabel
                        role = Role.RadioButton
                        selected = isSelected
                    },
                )
            }
        }
    }
}

private fun countryDisplayLabel(country: NewsCountry): String = when (country) {
    NewsCountry.US -> "🇺🇸 US"
    NewsCountry.ID -> "🇮🇩 ID"
}

@Composable
private fun countryAccessibilityLabel(country: NewsCountry): String = when (country) {
    NewsCountry.US -> stringResource(R.string.country_us_accessibility_label)
    NewsCountry.ID -> stringResource(R.string.country_id_accessibility_label)
}

private fun articleListErrorMessageRes(error: ArticleListUiError): Int = when (error) {
    ArticleListUiError.NetworkUnavailable -> R.string.error_network
    ArticleListUiError.RemoteUnavailable -> R.string.error_remote
    ArticleListUiError.PersistenceUnavailable -> R.string.error_persistence
    ArticleListUiError.MalformedData -> R.string.error_malformed
    ArticleListUiError.Unknown -> R.string.error_unknown
}
