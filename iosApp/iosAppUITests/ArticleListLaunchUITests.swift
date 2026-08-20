import XCTest

final class ArticleListLaunchUITests: XCTestCase {
    func testLaunchShowsNativeListShell() {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-test-no-network"]
        app.launch()

        XCTAssertTrue(
            app.staticTexts["KabarKabar"].waitForExistence(timeout: 10),
            "The native SwiftUI list shell should be visible after launch."
        )
        XCTAssertFalse(
            app.staticTexts["The news service is unavailable right now."].exists,
            "A no-network launch must not manufacture a remote-service error."
        )
    }

    func testSelectingIndonesiaDoesNotShowApiKeyError() {
        let app = XCUIApplication()
        app.launchArguments = ["--ui-test-no-network"]
        app.launch()

        XCTAssertTrue(app.buttons["Country"].waitForExistence(timeout: 10))
        app.buttons["Country"].tap()

        let indonesia = app.buttons["Indonesia"]
        XCTAssertTrue(indonesia.waitForExistence(timeout: 5))
        indonesia.tap()

        XCTAssertFalse(
            app.staticTexts["Add NEWS_API_KEY to Config.local.xcconfig to fetch fresh news."].exists,
            "Changing country must not expose an API-key error as list content."
        )
    }
}
