import Foundation
import SharedLogic

@MainActor
final class ArticleListViewModel: ObservableObject {
    private let repository: ArticleRepository
    private let allowsRemoteSync: Bool

    @Published private(set) var articles: [Article] = []
    @Published private(set) var isInitialLoading = true
    @Published private(set) var isRefreshing = false
    @Published private(set) var isLoadingMore = false
    @Published private(set) var canLoadMore = false
    @Published private(set) var blockingErrorMessage: String?
    @Published private(set) var refreshErrorMessage: String?
    @Published private(set) var paginationErrorMessage: String?
    @Published private(set) var country: NewsCountry

    private var articleObservation: SharedLogic.Cancellable?
    private var initialSyncTask: Task<Void, Never>?
    private var loadMoreTask: Task<Void, Never>?
    private var operationGeneration = 0
    private var hasCompletedInitialSync = false
    private var hasStarted = false

    init(repository: ArticleRepository, country: NewsCountry = .us, allowsRemoteSync: Bool = true) {
        self.repository = repository
        self.country = country
        self.allowsRemoteSync = allowsRemoteSync
    }

    func start() {
        guard !hasStarted else { return }
        hasStarted = true
        observeArticles()
        if allowsRemoteSync {
            scheduleInitialSync()
        } else {
            hasCompletedInitialSync = true
            isInitialLoading = false
        }
    }

    func selectCountry(_ newCountry: NewsCountry) {
        guard newCountry.code != country.code else { return }

        operationGeneration += 1
        articleObservation?.cancel()
        initialSyncTask?.cancel()
        loadMoreTask?.cancel()

        country = newCountry
        articles = []
        canLoadMore = false
        blockingErrorMessage = nil
        refreshErrorMessage = nil
        paginationErrorMessage = nil
        isInitialLoading = true
        isRefreshing = false
        isLoadingMore = false
        hasCompletedInitialSync = false

        observeArticles()
        if allowsRemoteSync {
            scheduleInitialSync()
        } else {
            hasCompletedInitialSync = true
            isInitialLoading = false
        }
    }

    func refresh() async {
        guard allowsRemoteSync, !isRefreshing else { return }
        await runRefresh()
    }

    func loadMoreIfNeeded(current article: Article) {
        guard let currentIndex = articles.firstIndex(where: {
            $0.id.value == article.id.value
        }) else { return }
        let triggerIndex = max(articles.count - 4, 0)
        guard currentIndex >= triggerIndex else { return }
        guard canLoadMore, !isLoadingMore, !isRefreshing else { return }

        let generation = operationGeneration
        let repository = repository
        let country = country
        isLoadingMore = true
        paginationErrorMessage = nil

        loadMoreTask = Task { [weak self] in
            do {
                let result = try await repository.loadMoreArticles(country: country)
                guard !Task.isCancelled else { return }
                self?.finishLoadMore(result: result, generation: generation)
            } catch {
                guard !Task.isCancelled else { return }
                self?.finishLoadMore(with: unexpectedErrorMessage, generation: generation)
            }
        }
    }

    func retryLoadMore() {
        guard let last = articles.last else { return }
        loadMoreIfNeeded(current: last)
    }

    private func observeArticles() {
        articleObservation?.cancel()
        let generation = operationGeneration
        let flow = repository.observeArticles(country: country)
        articleObservation = FlowWrapper<AnyObject>(flow: flow).subscribe { [weak self] observation in
            guard let self, generation == self.operationGeneration else { return }

            if let data = observation as? ArticleObservationData<NSArray> {
                self.articles = data.value as? [Article] ?? []
                self.canLoadMore = self.repository.canLoadMore(country: self.country)
                self.isInitialLoading = self.articles.isEmpty && !self.hasCompletedInitialSync
                self.blockingErrorMessage = nil
            } else if let failure = observation as? ArticleObservationFailure {
                self.handleObservationFailure(failure.error)
            }
        }
    }

    private func scheduleInitialSync() {
        let generation = operationGeneration
        let repository = repository
        let country = country
        isRefreshing = true
        initialSyncTask = Task { [weak self] in
            do {
                let result = try await repository.refreshArticles(country: country)
                guard !Task.isCancelled else { return }
                self?.finishInitialSync(result: result, generation: generation)
            } catch {
                guard !Task.isCancelled else { return }
                self?.finishInitialSync(with: unexpectedErrorMessage, generation: generation)
            }
        }
    }

    private func runRefresh() async {
        guard !isRefreshing else { return }

        isRefreshing = true
        let generation = operationGeneration
        defer {
            if generation == operationGeneration {
                isRefreshing = false
            }
        }

        do {
            let result = try await repository.refreshArticles(country: country)
            guard !Task.isCancelled, generation == operationGeneration else { return }
            apply(result: result, isInitialSync: false, isPagination: false)
        } catch {
            guard !Task.isCancelled, generation == operationGeneration else { return }
            if articles.isEmpty {
                blockingErrorMessage = unexpectedErrorMessage
            } else {
                refreshErrorMessage = unexpectedErrorMessage
            }
        }
    }

    private func finishInitialSync(result: RefreshResult?, generation: Int) {
        guard generation == operationGeneration else { return }
        apply(result: result, isInitialSync: true, isPagination: false)
        isRefreshing = false
    }

    private func finishInitialSync(with message: String, generation: Int) {
        guard generation == operationGeneration else { return }
        hasCompletedInitialSync = true
        handleInitialFailure(message)
        isRefreshing = false
    }

    private func finishLoadMore(result: RefreshResult?, generation: Int) {
        guard generation == operationGeneration else { return }
        apply(result: result, isInitialSync: false, isPagination: true)
        isLoadingMore = false
    }

    private func finishLoadMore(with message: String, generation: Int) {
        guard generation == operationGeneration else { return }
        paginationErrorMessage = message
        isLoadingMore = false
    }

    private func apply(result: RefreshResult?, isInitialSync: Bool, isPagination: Bool) {
        if let failure = result as? RefreshResultFailure {
            let message = userFacingMessage(for: failure.error)
            if isInitialSync {
                hasCompletedInitialSync = true
                handleInitialFailure(message)
            } else if isPagination {
                paginationErrorMessage = message
            } else if articles.isEmpty {
                blockingErrorMessage = message
            } else {
                refreshErrorMessage = message
            }
            return
        }

        if isInitialSync {
            hasCompletedInitialSync = true
            isInitialLoading = false
            blockingErrorMessage = nil
        } else if isPagination {
            paginationErrorMessage = nil
        } else {
            refreshErrorMessage = nil
            blockingErrorMessage = nil
        }
        canLoadMore = repository.canLoadMore(country: country)
    }

    private func handleInitialFailure(_ message: String) {
        isInitialLoading = false
        if articles.isEmpty {
            blockingErrorMessage = message
        } else {
            blockingErrorMessage = nil
            refreshErrorMessage = message
        }
    }

    private func handleObservationFailure(_ error: SyncError) {
        let message = userFacingMessage(for: error)
        if articles.isEmpty {
            isInitialLoading = false
            blockingErrorMessage = message
        } else {
            refreshErrorMessage = message
        }
    }

    deinit {
        articleObservation?.cancel()
        initialSyncTask?.cancel()
        loadMoreTask?.cancel()
    }
}
