package com.ai.assistance.operit.data.preferences

import com.ai.assistance.operit.data.model.ActivePrompt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeTargetOperationCoordinatorTest {
    @Test
    fun targetSaveFinishesBeforeQueuedTransition() = runTest {
        val firstTarget = ActivePrompt.CharacterCard("first")
        val secondTarget = ActivePrompt.CharacterCard("second")
        var activeTarget = firstTarget
        val coordinator = ThemeTargetOperationCoordinator { activeTarget }
        val events = mutableListOf<String>()
        val saveStarted = CompletableDeferred<Unit>()
        val releaseSave = CompletableDeferred<Unit>()

        val save = async {
            coordinator.runForTarget(firstTarget) {
                events += "save-start"
                saveStarted.complete(Unit)
                releaseSave.await()
                events += "save-end"
            }
        }
        runCurrent()
        saveStarted.await()

        val transition = async {
            coordinator.runTransition {
                activeTarget = secondTarget
                events += "transition"
            }
        }
        runCurrent()
        assertEquals(listOf("save-start"), events)

        releaseSave.complete(Unit)
        advanceUntilIdle()

        assertTrue(save.await())
        transition.await()
        assertEquals(listOf("save-start", "save-end", "transition"), events)
    }

    @Test
    fun staleTargetDoesNotWriteTheme() = runTest {
        val activeTarget = ActivePrompt.CharacterCard("current")
        val staleTarget = ActivePrompt.CharacterCard("stale")
        val coordinator = ThemeTargetOperationCoordinator { activeTarget }
        var wroteTheme = false

        val saved = coordinator.runForTarget(staleTarget) {
            wroteTheme = true
        }

        assertFalse(saved)
        assertFalse(wroteTheme)
    }
}
