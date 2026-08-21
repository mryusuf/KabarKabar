import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    private let repository: ArticleRepository
    private let allowsRemoteSync: Bool

    init() {
        let hasConfiguredRemoteSync = initKoin()
        repository = IOSDependencyContainer().getArticleRepository()
        allowsRemoteSync = hasConfiguredRemoteSync &&
            !ProcessInfo.processInfo.arguments.contains("--ui-test-no-network")
    }

    var body: some Scene {
        WindowGroup {
            ArticleListView(
                repository: repository,
                allowsRemoteSync: allowsRemoteSync
            )
        }
    }
}
