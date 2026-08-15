package io.mryusuf.kabarkabar.data.remote.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewsApiClientTest {

    @Test
    fun redactApiKey_masksTheKeyAnywhereInAFormattedMessage() {
        val apiKey = "test-key-not-secret"

        val redacted = redactApiKey("request header contains $apiKey", apiKey)

        assertFalse(redacted.contains(apiKey))
        assertTrue(redacted.contains("[REDACTED]"))
    }

    @Test
    fun redactApiKey_leavesMessagesUnchangedWhenConfigurationIsBlank() {
        val message = "request contains no configured key"

        assertTrue(redactApiKey(message, "").contains(message))
    }
}
