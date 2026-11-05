package br.com.desbloqueia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=StudyApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppStartupTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun realActivityLoadsDatabaseAndStartsStudy() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Estudar agora →").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Estudar agora →").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Redação oficial").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Redação oficial").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Fechar e usar o celular"))
        compose.onNodeWithText("Fechar e usar o celular").assertIsDisplayed()
    }
}
