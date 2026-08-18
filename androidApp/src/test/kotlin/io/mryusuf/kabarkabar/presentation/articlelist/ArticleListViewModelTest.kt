package io.mryusuf.kabarkabar.presentation.articlelist

import io.mryusuf.kabarkabar.domain.error.SyncError
import io.mryusuf.kabarkabar.domain.model.Article
import io.mryusuf.kabarkabar.domain.model.ArticleId
import io.mryusuf.kabarkabar.domain.model.ArticleObservation
import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds
import io.mryusuf.kabarkabar.domain.model.RefreshResult
import io.mryusuf.kabarkabar.domain.repository.ArticleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        repository.emitObservation(ArticleObservation.Data(articles))

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
        repository.emitObservation(ArticleObservation.Data(emptyList()))
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
        repository.emitObservation(ArticleObservation.Data(emptyList()))
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
        repository.emitObservation(ArticleObservation.Data(articles))
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
        repository.emitObservation(ArticleObservation.Data(articles))
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
        repository.emitObservation(ArticleObservation.Data(articles))
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
        repository.emitObservation(ArticleObservation.Data(articles))
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

        repository.emitObservation(ArticleObservation.Failure(SyncError.Persistence))
        runCurrent()

        val state = viewModel.uiState.value
        assertIs<ArticleListContent.Data>(state.content)
        assertEquals(articles, state.content.articles)
        assertIs<ArticleListUiEvent.ShowObservationError>(events.single())
    }

    @Test
    fun `local observation failure without cache is blocking`() = runTest {
        repository.emitObservation(ArticleObservation.Failure(SyncError.Persistence))
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
        repository.emitObservation(ArticleObservation.Data(articles))
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
    private val observations = MutableSharedFlow<ArticleObservation<List<Article>>>(replay = 1)
    private var refreshResult: RefreshResult = RefreshResult.Success
    private val refreshSignal = MutableSharedFlow<Unit>()
    var refreshCallCount: Int = 0
    var refreshException: CancellationException? = null

    fun emitObservation(observation: ArticleObservation<List<Article>>) {
        observations.tryEmit(observation)
    }

    fun setRefreshResult(result: RefreshResult) {
        refreshResult = result
    }

    suspend fun completeRefresh() {
        refreshSignal.emit(Unit)
    }

    override fun observeArticles(): Flow<ArticleObservation<List<Article>>> = observations

    override fun observeArticle(id: ArticleId): Flow<ArticleObservation<Article?>> {
        throw NotImplementedError()
    }

    override suspend fun refreshArticles(): RefreshResult {
        refreshCallCount += 1
        refreshException?.let { throw it }
        refreshSignal.first()
        return refreshResult
    }
}
