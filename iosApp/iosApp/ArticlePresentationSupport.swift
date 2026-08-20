import Foundation
import SharedLogic

func userFacingMessage(for error: SyncError) -> String {
    if error is SyncErrorNetwork {
        return "Network unavailable. Check your connection and try again."
    }
    if error is SyncErrorRemoteApi {
        return "The news service is unavailable right now."
    }
    if error is SyncErrorMalformedData {
        return "The news response was unusable."
    }
    if error is SyncErrorPersistence {
        return "Saved news could not be read."
    }
    return "Something went wrong. Try again."
}

let unexpectedErrorMessage = "Something went wrong. Try again."

func publishedDate(for article: Article) -> Date {
    Date(timeIntervalSince1970: TimeInterval(article.publishedAt.value) / 1_000)
}
