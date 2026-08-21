package io.mryusuf.kabarkabar.domain

import io.mryusuf.kabarkabar.domain.model.NewsCountry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewsCountryTest {

    @Test
    fun supports_exactly_the_two_product_countries_and_request_codes() {
        assertEquals(listOf(NewsCountry.US, NewsCountry.ID), NewsCountry.values().toList())
        assertEquals("us", NewsCountry.US.code)
        assertEquals("id", NewsCountry.ID.code)
    }

    @Test
    fun rejects_unknown_request_codes() {
        assertNull(NewsCountry.fromCode("gb"))
        assertNull(NewsCountry.fromCode("US"))
    }
}
