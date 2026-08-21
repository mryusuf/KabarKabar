import Foundation
import SharedLogic

@MainActor
final class ArticleDetailViewModel: ObservableObject {
    private let repository: ArticleRepository
    private let articleId: ArticleId
    private let country: NewsCountry

    @Published private(set) var article: Article?
    @Published private(set) var isLoading = true
    @Published private(set) var errorMessage: String?

    private var articleObservation: SharedLogic.Cancellable?

    init(repository: ArticleRepository, articleId: ArticleId, country: NewsCountry) {
        self.repository = repository
        self.articleId = articleId
        self.country = country
        observeArticle()
    }

    func retry() {
        isLoading = true
        errorMessage = nil
        observeArticle()
    }

    private func observeArticle() {
        articleObservation?.cancel()
        let flow = repository.observeArticle(id: articleId, country: country)
        articleObservation = FlowWrapper<AnyObject>(flow: flow).subscribe { [weak self] observation in
            guard let self else { return }

            if let data = observation as? ArticleObservationData<Article> {
                self.article = data.value
                self.isLoading = false
                self.errorMessage = data.value == nil ? "This article is no longer available." : nil
            } else if let failure = observation as? ArticleObservationFailure {
                self.isLoading = false
                self.errorMessage = userFacingMessage(for: failure.error)
            }
        }
    }

    deinit {
        articleObservation?.cancel()
    }
}
