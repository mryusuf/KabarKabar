package io.mryusuf.kabarkabar.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Interface representing an active subscription that can be cancelled from Swift.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Wrapper for Kotlin [Flow] to allow Swift to subscribe to updates easily.
 */
class FlowWrapper<T : Any>(private val flow: Flow<T>) {
    fun subscribe(
        onEvent: (T) -> Unit
    ): Cancellable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        flow
            .onEach { onEvent(it) }
            .launchIn(scope)

        return object : Cancellable {
            override fun cancel() {
                scope.cancel()
            }
        }
    }
}

/**
 * Extension to wrap any [Flow] into a [FlowWrapper].
 */
fun <T : Any> Flow<T>.asWrapper(): FlowWrapper<T> = FlowWrapper(this)
