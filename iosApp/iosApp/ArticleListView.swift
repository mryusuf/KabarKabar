import SwiftUI
import SharedLogic

struct ArticleListView: View {
    private let repository: ArticleRepository
    private let allowsRemoteSync: Bool
    @StateObject private var viewModel: ArticleListViewModel

    init(repository: ArticleRepository, country: NewsCountry = .us, allowsRemoteSync: Bool = true) {
        self.repository = repository
        self.allowsRemoteSync = allowsRemoteSync
        _viewModel = StateObject(wrappedValue: ArticleListViewModel(
            repository: repository,
            country: country,
            allowsRemoteSync: allowsRemoteSync
        ))
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("KabarKabar")
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        countryMenu
                    }
                }
                .onAppear { viewModel.start() }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isInitialLoading && viewModel.articles.isEmpty && viewModel.blockingErrorMessage == nil {
            ProgressView("Loading saved news…")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .accessibilityLabel("Loading saved news")
        } else if let error = viewModel.blockingErrorMessage, viewModel.articles.isEmpty {
            ContentUnavailableView {
                Label("Unable to load news", systemImage: "wifi.exclamationmark")
            } description: {
                Text(error)
            } actions: {
                if allowsRemoteSync {
                    Button("Try Again") {
                        Task { await viewModel.refresh() }
                    }
                }
            }
        } else if viewModel.articles.isEmpty {
            ContentUnavailableView {
                Label("No news yet", systemImage: "newspaper")
            } description: {
                Text(allowsRemoteSync
                     ? "Pull to refresh when you are online."
                     : "No saved news for this country yet.")
            } actions: {
                if allowsRemoteSync {
                    Button("Refresh") {
                        Task { await viewModel.refresh() }
                    }
                }
            }
        } else {
            articleList
        }
    }

    private var articleList: some View {
        List {
            if let error = viewModel.refreshErrorMessage {
                Section {
                    Label(error, systemImage: "exclamationmark.triangle")
                        .foregroundStyle(.orange)
                        .accessibilityElement(children: .combine)
                }
            }

            ForEach(Array(viewModel.articles.enumerated()), id: \.element.id.value) { index, article in
                NavigationLink {
                    ArticleDetailView(
                        repository: repository,
                        articleId: article.id,
                        country: viewModel.country
                    )
                } label: {
                    ArticleRowView(article: article, isProminent: index == 0)
                }
                .onAppear {
                    viewModel.loadMoreIfNeeded(current: article)
                }
            }

            if viewModel.canLoadMore, let lastArticle = viewModel.articles.last {
                Color.clear
                    .frame(height: 1)
                    .onAppear {
                        viewModel.loadMoreIfNeeded(current: lastArticle)
                    }
                    .accessibilityHidden(true)
            }

            if viewModel.isLoadingMore {
                HStack {
                    Spacer()
                    ProgressView("Loading more")
                    Spacer()
                }
            } else if let error = viewModel.paginationErrorMessage {
                VStack(alignment: .leading, spacing: 8) {
                    Text(error)
                        .foregroundStyle(.secondary)
                    Button("Retry") {
                        viewModel.retryLoadMore()
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            await viewModel.refresh()
        }
    }

    private var countryMenu: some View {
        Menu {
            Button {
                viewModel.selectCountry(.us)
            } label: {
                Label("United States", systemImage: viewModel.country.code == "us" ? "checkmark" : "")
            }
            Button {
                viewModel.selectCountry(.id)
            } label: {
                Label("Indonesia", systemImage: viewModel.country.code == "id" ? "checkmark" : "")
            }
        } label: {
            Label(viewModel.country.code.uppercased(), systemImage: "globe")
        }
        .accessibilityLabel("Country")
        .accessibilityValue(viewModel.country.code.uppercased())
    }
}

private struct ArticleRowView: View {
    let article: Article
    let isProminent: Bool

    var body: some View {
        Group {
            if isProminent {
                VStack(alignment: .leading, spacing: 10) {
                    if let url = imageURL {
                        ArticleImage(url: url, height: 180)
                    }

                    articleText
                }
            } else {
                HStack(alignment: .top, spacing: 12) {
                    if let url = imageURL {
                        ArticleImage(url: url, width: 112, height: 84)
                    }

                    articleText
                }
            }
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens article details")
    }

    private var articleText: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(article.title)
                .font(isProminent ? .title3 : .headline)
                .fontWeight(isProminent ? .semibold : .regular)
                .lineLimit(isProminent ? 4 : 3)

            if let description = article.description_, !description.isEmpty {
                Text(description)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(isProminent ? 4 : 3)
            }

            Text(publishedDate(for: article), style: .date)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var imageURL: URL? {
        guard let imageUrl = article.imageUrl else { return nil }
        return URL(string: imageUrl)
    }
}

struct ArticleImage: View {
    let url: URL
    let width: CGFloat?
    let height: CGFloat

    init(url: URL, width: CGFloat? = nil, height: CGFloat) {
        self.url = url
        self.width = width
        self.height = height
    }

    var body: some View {
        AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image):
                image
                    .resizable()
                    .scaledToFill()
            case .failure:
                placeholder
            case .empty:
                ZStack {
                    placeholder
                    ProgressView()
                }
            @unknown default:
                placeholder
            }
        }
        .frame(width: width, height: height)
        .frame(maxWidth: width ?? .infinity)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityHidden(true)
    }

    private var placeholder: some View {
        RoundedRectangle(cornerRadius: 10)
            .fill(Color.secondary.opacity(0.15))
            .overlay {
                Image(systemName: "photo")
                    .foregroundStyle(.secondary)
            }
    }
}
