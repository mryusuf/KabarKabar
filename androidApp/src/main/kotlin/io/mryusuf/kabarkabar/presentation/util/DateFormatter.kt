package io.mryusuf.kabarkabar.presentation.util

import io.mryusuf.kabarkabar.domain.model.EpochMilliseconds
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility for formatting domain [EpochMilliseconds] into user-facing localized strings.
 */
object DateFormatter {
    private val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    fun format(epochMilliseconds: EpochMilliseconds): String {
        val instant = Instant.ofEpochMilli(epochMilliseconds.value)
        return formatter.format(instant)
    }
}
