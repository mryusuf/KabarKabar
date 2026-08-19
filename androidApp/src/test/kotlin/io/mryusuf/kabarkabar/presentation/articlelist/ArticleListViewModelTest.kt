package io.mryusuf.kabarkabar.presentation.articlelist

import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds
import io.mryusuf.kabarkabar.domain.model.NewsCountry
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeArticleRepository
    private lateinit var viewModel: ArticleListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeArticleRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `V1 - cached content is shown immediately`() = runTest {
        // Given repository observation contains articles
        val articles = listOf(createFakeArticle("1"))
        repository.emitObservation(ArticleObservation.Data(articles), NewsCountry.US)

        viewModel = ArticleListViewModel(repository)

        val states = mutableListOf<ArticleListUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states.add(it) }
        }

        // Then ViewModel exposes Data without waiting for refresh completion
        runCurrent()

        val state = states.last()
        assertIs<ArticleListContent.Data>(state.content)
        assertEquals(articles, state.content.articles)
    }

    @Test
    fun `V2 - successful empty initial synchronization`() = runTest {
        // Given persisted content is empty
        repository.emitObservation(ArticleObservation.Data(emptyList()), NewsCountry.US)
        repository.setRefreshResult(RefreshResult.Success)

        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Empty persisted content remains Loading until the initial sync completes.
        runCurrent()
        assertIs<ArticleListContent.Loading>(viewModel.uiState.value.content)
        assertEquals(false, viewModel.uiState.value.isRefreshing)

        repository.completeRefresh()
        advanceUntilIdle()

        // Then ViewModel resolves to Empty
        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Empty>(state.content)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `V3 - failed initial synchronization with no cache`() = runTest {
        // Given persisted content is empty
        repository.emitObservation(ArticleObservation.Data(emptyList()), NewsCountry.US)
        repository.setRefreshResult(RefreshResult.Failure(SyncError.Network))

        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()

        // Then ViewModel resolves to blocking Error
        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Error>(state.content)
        assertEquals(ArticleListUiError.NetworkUnavailable, state.content.error)
    }

    @Test
    fun `V4 - refresh failure with cache`() = runTest {
        // Given Data is visible
        val articles = listOf(createFakeArticle("1"))
        repository.emitObservation(ArticleObservation.Data(articles), NewsCountry.US)
        repository.setRefreshResult(RefreshResult.Success)

        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()

        // Setup event collector
        val events = mutableListOf<ArticleListUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        // When refresh fails
        repository.setRefreshResult(RefreshResult.Failure(SyncError.RemoteApi))
        viewModel.refresh()

        runCurrent()
        // While refreshing
        assertTrue(viewModel.uiState.value.isRefreshing, "Should be refreshing")
        assertIs<ArticleListContent.Data>(viewModel.uiState.value.content)

        repository.completeRefresh()
        advanceUntilIdle()

        // Then Data remains visible, isRefreshing returns to false
        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Data>(state.content)
        assertEquals(false, state.isRefreshing)

        // And a one-shot non-blocking error event is emitted
        assertTrue(events.isNotEmpty(), "Should have emitted an event")
        val event = events.first()
        assertIs<ArticleListUiEvent.ShowRefreshError>(event)
        assertEquals(ArticleListUiError.RemoteUnavailable, event.error)
    }

    @Test
    fun `V5 - refresh does not hide cache`() = runTest {
        // Given Data is visible
        val articles = listOf(createFakeArticle("1"))
        repository.emitObservation(ArticleObservation.Data(articles), NewsCountry.US)
        repository.setRefreshResult(RefreshResult.Success)

        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()

        // While refresh is in progress
        repository.setRefreshResult(RefreshResult.Success)
        viewModel.refresh()

        runCurrent()
        // Then Data remains visible and isRefreshing is true
        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Data>(state.content)
        assertTrue(state.isRefreshing, "Should be refreshing")

        repository.completeRefresh()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `cached refresh failure remains non-blocking when UI observes after refresh`() = runTest {
        val articles = listOf(createFakeArticle("cached"))
        repository.emitObservation(ArticleObservation.Data(articles), NewsCountry.US)
        repository.setRefreshResult(RefreshResult.Failure(SyncError.RemoteApi))

        viewModel = ArticleListViewModel(repository)

        // Complete the initial refresh before any UI collector subscribes.
        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()

        val events = mutableListOf<ArticleListUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        assertIs<ArticleListContent.Data>(viewModel.uiState.value.content)
        assertEquals(1, events.size)
        assertIs<ArticleListUiEvent.ShowRefreshError>(events.single())
    }

    @Test
    fun `refresh requests made before scheduling are coalesced`() = runTest {
        repository.setRefreshResult(RefreshResult.Success)
        viewModel = ArticleListViewModel(repository)

        // The initial request and both manual calls are made before the test
        // dispatcher runs their launched coroutines.
        viewModel.refresh()
        viewModel.refresh()
        runCurrent()

        assertEquals(1, repository.refreshCallCount)

        repository.completeRefresh()
        advanceUntilIdle()
    }

    @Test
    fun `local observation failure preserves previously displayed cache`() = runTest {
        val articles = listOf(createFakeArticle("cached"))
        repository.emitObservation(ArticleObservation.Data(articles), NewsCountry.US)
        repository.setRefreshResult(RefreshResult.Success)
        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()

        val events = mutableListOf<ArticleListUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        repository.emitObservation(ArticleObservation.Failure(SyncError.Persistence), NewsCountry.US)
        runCurrent()

        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Data>(state.content)
        assertEquals(articles, state.content.articles)
        assertIs<ArticleListUiEvent.ShowObservationError>(events.single())
    }

    @Test
    fun `local observation failure without cache is blocking`() = runTest {
        repository.emitObservation(ArticleObservation.Failure(SyncError.Persistence), NewsCountry.US)
        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Error>(state.content)
        assertEquals(ArticleListUiError.PersistenceUnavailable, state.content.error)
    }

    @Test
    fun `refresh cancellation is not converted into a presentation error`() = runTest {
        val articles = listOf(createFakeArticle("cached"))
        repository.emitObservation(ArticleObservation.Data(articles), NewsCountry.US)
        repository.refreshException = CancellationException("test cancellation")
        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Data>(state.content)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `country switch clears previous cache and shows immediate new cache`() = runTest {
        viewModel = ArticleListViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        // Populate both country-scoped persisted observations before switching.
        val usArticles = listOf(createFakeArticle("us-1"))
        repository.emitObservation(ArticleObservation.Data(usArticles), NewsCountry.US)
        val idArticles = listOf(createFakeArticle("id-1"))
        repository.emitObservation(ArticleObservation.Data(idArticles), NewsCountry.ID)
        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()
        assertEquals(usArticles, (viewModel.uiState.value.content as ArticleListContent.Data).articles)
        assertEquals(NewsCountry.US, viewModel.uiState.value.selectedCountry)

        // Switching must render the already-persisted ID cache without a network result.
        viewModel.onCountrySelected(NewsCountry.ID)
        runCurrent()
        assertEquals(NewsCountry.ID, viewModel.uiState.value.selectedCountry)
        assertIs<ArticleListContent.Data>(viewModel.uiState.value.content)
        assertEquals(idArticles, (viewModel.uiState.value.content as ArticleListContent.Data).articles)

        repository.completeRefresh()
        advanceUntilIdle()
    }

    @Test
    fun `country switch without cache is loading then blocking on selected-country failure`() = runTest {
        val usArticles = listOf(createFakeArticle("us-1"))
        repository.emitObservation(ArticleObservation.Data(usArticles), NewsCountry.US)
        viewModel = ArticleListViewModel(repository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
        repository.completeRefresh()
        advanceUntilIdle()

        repository.setRefreshResult(RefreshResult.Failure(SyncError.Network))
        viewModel.onCountrySelected(NewsCountry.ID)
        runCurrent()
        assertEquals(NewsCountry.ID, viewModel.uiState.value.selectedCountry)
        assertIs<ArticleListContent.Loading>(viewModel.uiState.value.content)

        repository.completeRefresh()
        advanceUntilIdle()

        assertEquals(NewsCountry.ID, viewModel.uiState.value.selectedCountry)
        assertEquals(
            ArticleListUiError.NetworkUnavailable,
            (viewModel.uiState.value.content as ArticleListContent.Error).error,
        )
    }

    @Test
    fun `late US refresh cannot block or classify the selected ID state`() = runTest {
        val countryRepository = NonCancellableCountryRepository()
        countryRepository.emit(NewsCountry.US, ArticleObservation.Data(emptyList()))
        countryRepository.emit(NewsCountry.ID, ArticleObservation.Data(emptyList()))
        viewModel = ArticleListViewModel(countryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()

        viewModel.onCountrySelected(NewsCountry.ID)
        runCurrent()

        assertEquals(listOf(NewsCountry.US, NewsCountry.ID), countryRepository.refreshCalls)
        assertIs<ArticleListContent.Loading>(viewModel.uiState.value.content)

        countryRepository.completeNext(NewsCountry.US, RefreshResult.Failure(SyncError.Network))
        runCurrent()

        assertEquals(NewsCountry.ID, viewModel.uiState.value.selectedCountry)
        assertIs<ArticleListContent.Loading>(viewModel.uiState.value.content)

        countryRepository.completeNext(NewsCountry.ID, RefreshResult.Success)
        runCurrent()

        assertIs<ArticleListContent.Empty>(viewModel.uiState.value.content)
    }

    @Test
    fun `late ID refresh cannot classify the selected US state`() = runTest {
        val countryRepository = NonCancellableCountryRepository()
        countryRepository.emit(NewsCountry.US, ArticleObservation.Data(emptyList()))
        countryRepository.emit(NewsCountry.ID, ArticleObservation.Data(emptyList()))
        viewModel = ArticleListViewModel(countryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        runCurrent()
        countryRepository.completeNext(NewsCountry.US, RefreshResult.Success)
        runCurrent()

        viewModel.onCountrySelected(NewsCountry.ID)
        runCurrent()
        viewModel.onCountrySelected(NewsCountry.US)
        runCurrent()

        assertEquals(
            listOf(NewsCountry.US, NewsCountry.ID, NewsCountry.US),
            countryRepository.refreshCalls,
        )
        assertIs<ArticleListContent.Loading>(viewModel.uiState.value.content)

        countryRepository.completeNext(NewsCountry.ID, RefreshResult.Success)
        runCurrent()

        assertEquals(NewsCountry.US, viewModel.uiState.value.selectedCountry)
        assertIs<ArticleListContent.Loading>(viewModel.uiState.value.content)

        countryRepository.completeNext(NewsCountry.US, RefreshResult.Success)
        runCurrent()
        assertIs<ArticleListContent.Empty>(viewModel.uiState.value.content)
    }

    private fun createFakeArticle(id: String) = Article(
        id = ArticleId.fromCanonicalUrl("https://example.com/$id"),
        url = "https://example.com/$id",
        title = "Title $id",
        description = null,
        imageUrl = null,
        publishedAt = EpochMilliseconds(0L)
    )
}

class FakeArticleRepository : ArticleRepository {
    private val countryObservations =
        mutableMapOf<NewsCountry, MutableSharedFlow<ArticleObservation<List<Article>>>>()
    private var refreshResult: RefreshResult = RefreshResult.Success
    private val refreshSignal = MutableSharedFlow<Unit>()
    var refreshCallCount: Int = 0
    var refreshException: CancellationException? = null

    private fun getOrCreateObservation(country: NewsCountry) =
        countryObservations.getOrPut(country) { MutableSharedFlow(replay = 1) }

    fun emitObservation(
        observation: ArticleObservation<List<Article>>,
        country: NewsCountry = NewsCountry.US,
    ) {
        getOrCreateObservation(country).tryEmit(observation)
    }

    fun setRefreshResult(result: RefreshResult) {
        refreshResult = result
    }

    suspend fun completeRefresh() {
        refreshSignal.emit(Unit)
    }

    override fun observeArticles(country: NewsCountry): Flow<ArticleObservation<List<Article>>> =
        getOrCreateObservation(country)

    override fun observeArticle(id: ArticleId, country: NewsCountry): Flow<ArticleObservation<Article?>> {
        throw NotImplementedError()
    }

    override suspend fun refreshArticles(country: NewsCountry): RefreshResult {
        refreshCallCount += 1
        refreshException?.let { throw it }
        refreshSignal.first()
        return refreshResult
    }
}

private class NonCancellableCountryRepository : ArticleRepository {
    private val observations =
        mutableMapOf<NewsCountry, MutableSharedFlow<ArticleObservation<List<Article>>>>()
    private val pendingRefreshes = mutableMapOf<NewsCountry, ArrayDeque<PendingRefresh>>()
    val refreshCalls = mutableListOf<NewsCountry>()

    private data class PendingRefresh(
        val gate: CompletableDeferred<Unit>,
        var result: RefreshResult = RefreshResult.Success,
    )

    private fun observationFor(country: NewsCountry) =
        observations.getOrPut(country) { MutableSharedFlow(replay = 1) }

    override fun observeArticles(country: NewsCountry): Flow<ArticleObservation<List<Article>>> =
        observationFor(country)

    override fun observeArticle(
        id: ArticleId,
        country: NewsCountry,
    ): Flow<ArticleObservation<Article?>> = error("Not needed for list concurrency tests")

    override suspend fun refreshArticles(country: NewsCountry): RefreshResult {
        refreshCalls += country
        val pending = PendingRefresh(CompletableDeferred())
        pendingRefreshes.getOrPut(country) { ArrayDeque() }.addLast(pending)
        withContext(NonCancellable) {
            pending.gate.await()
        }
        return pending.result
    }

    fun emit(country: NewsCountry, observation: ArticleObservation<List<Article>>) {
        observationFor(country).tryEmit(observation)
    }

    fun completeNext(country: NewsCountry, result: RefreshResult) {
        val pending = pendingRefreshes.getValue(country).removeFirst()
        pending.result = result
        pending.gate.complete(Unit)
    }
}
