package io.mryusuf.kabarkabar.ui.theme

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeTest {

    @Test
    fun light_theme_uses_dark_system_bar_icons() {
        assertTrue(shouldUseLightSystemBarIcons(darkTheme = false))
    }

    @Test
    fun dark_theme_uses_light_system_bar_icons() {
        assertFalse(shouldUseLightSystemBarIcons(darkTheme = true))
    }
}
