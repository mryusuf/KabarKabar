import SwiftUI
import SharedLogic

struct ArticleDetailView: View {
    private let repository: ArticleRepository
    @StateObject private var viewModel: ArticleDetailViewModel
    @State private var isViewerPresented = false

    init(repository: ArticleRepository, articleId: ArticleId, country: NewsCountry) {
        self.repository = repository
        _viewModel = StateObject(wrappedValue: ArticleDetailViewModel(
            repository: repository,
            articleId: articleId,
            country: country
        ))
    }

    var body: some View {
        ScrollView {
            if let article = viewModel.article {
                articleContent(article)
            } else if viewModel.isLoading {
                ProgressView("Loading article…")
                    .frame(maxWidth: .infinity, minHeight: 240)
            } else {
                ContentUnavailableView {
                    Label("Article unavailable", systemImage: "doc.questionmark")
                } description: {
                    Text(viewModel.errorMessage ?? "This article is no longer available.")
                } actions: {
                    Button("Try Again") {
                        viewModel.retry()
                    }
                }
            }
        }
        .navigationTitle("Article")
        .navigationBarTitleDisplayMode(.inline)
        .fullScreenCover(isPresented: $isViewerPresented) {
            if let article = viewModel.article, let url = usableImageURL(from: article.imageUrl) {
                ImageViewer(url: url)
            }
        }
    }

    @ViewBuilder
    private func articleContent(_ article: Article) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            if let url = usableImageURL(from: article.imageUrl) {
                Button {
                    isViewerPresented = true
                } label: {
                    ArticleImage(url: url, height: 240)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Open article image")
            }

            VStack(alignment: .leading, spacing: 12) {
                Text(article.title)
                    .font(.title2)
                    .fontWeight(.semibold)
                    .fixedSize(horizontal: false, vertical: true)

                Text(publishedDate(for: article), style: .date)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Divider()

                if let description = article.description_, !description.isEmpty {
                    Text(description)
                        .font(.body)
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    Text("No summary was provided for this article.")
                        .font(.body)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
    }
}

private struct ImageViewer: View {
    let url: URL
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                    case .failure:
                        Label("Image unavailable", systemImage: "photo.badge.exclamationmark")
                            .foregroundStyle(.white)
                    case .empty:
                        ProgressView()
                            .tint(.white)
                    @unknown default:
                        EmptyView()
                    }
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                        .foregroundStyle(.white)
                }
            }
        }
        .preferredColorScheme(.dark)
    }
}
