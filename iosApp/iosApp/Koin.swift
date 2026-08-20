import Foundation
import SharedLogic

func initKoin() -> Bool {
    let config = NewsApiConfigImpl()
    KoinIOSKt.doInitKoin(newsApiConfig: config)
    return config.isConfigured
}

final class NewsApiConfigImpl: NewsApiConfig {
    let apiKey: String
    let baseUrl: String = "https://newsapi.org"

    var isConfigured: Bool {
        !apiKey.isEmpty
    }

    init(bundle: Bundle = .main) {
        apiKey = (bundle.object(forInfoDictionaryKey: "NEWS_API_KEY") as? String ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
