package io.mryusuf.kabarkabar

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner to provide [TestKabarKabarApp].
 */
class KabarKabarTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestKabarKabarApp::class.java.name, context)
    }
}
