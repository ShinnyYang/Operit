package com.ai.assistance.operit.data.api

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class CodexUsageClientTest {
    private val client = CodexUsageClient(OkHttpClient())

    @Test
    fun parsesPlanAndRollingWindows() {
        val usage = client.parseUsage(
            """
            {
              "plan_type": "plus",
              "rate_limit": {
                "primary_window": {
                  "used_percent": 18,
                  "limit_window_seconds": 18000,
                  "reset_at": 1700000123
                },
                "secondary_window": {
                  "used_percent": 52,
                  "limit_window_seconds": 604800,
                  "reset_at": 1700400123
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals("plus", usage.planType)
        assertEquals(82, usage.primaryWindow?.remainingPercent)
        assertEquals(48, usage.secondaryWindow?.remainingPercent)
        assertEquals(18000L, usage.primaryWindow?.windowDurationSeconds)
        assertEquals(1700400123L, usage.secondaryWindow?.resetsAtEpochSeconds)
    }

    @Test
    fun keepsSecondaryWindowWhenPrimaryWindowIsAbsent() {
        val usage = client.parseUsage(
            """
            {
              "plan_type": "plus",
              "rate_limit": {
                "secondary_window": {
                  "used_percent": 52,
                  "limit_window_seconds": 604800,
                  "reset_at": 1700400123
                }
              }
            }
            """.trimIndent(),
        )

        assertNull(usage.primaryWindow)
        assertNotNull(usage.secondaryWindow)
        assertEquals(48, usage.secondaryWindow?.remainingPercent)
    }
}
