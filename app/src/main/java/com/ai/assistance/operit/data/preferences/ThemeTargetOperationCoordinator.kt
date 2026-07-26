package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.ActivePrompt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ThemeTargetOperationCoordinator(
    private val getActivePrompt: suspend () -> ActivePrompt,
) {
    private val mutex = Mutex()

    suspend fun <T> runTransition(action: suspend () -> T): T {
        return mutex.withLock {
            action()
        }
    }

    suspend fun runForTarget(
        target: ActivePrompt,
        action: suspend () -> Unit,
    ): Boolean {
        return mutex.withLock {
            if (getActivePrompt() != target) {
                false
            } else {
                action()
                true
            }
        }
    }
}
