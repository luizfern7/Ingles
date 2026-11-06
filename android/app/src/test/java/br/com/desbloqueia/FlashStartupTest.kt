package br.com.desbloqueia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=StudyApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlashStartupTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun flashOnlyBankCanStudyFlipAndPersistRatingWithoutQuestions() {
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Importar meu primeiro JSON").fetchSemanticsNodes().isNotEmpty()}
        val app=compose.activity.application as StudyApplication
        runBlocking {app.repository.importStudyPack("""{"schema_version":2,"flashcards":[{"id":"f","disciplina":"Banco","assunto_id":"sfn_cmn","frente":"Qual função?","verso":"Definir diretrizes."}]}""")}
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Estudar agora →").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Estudar agora →").performScrollTo().performClick()
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Virar cartão").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Definir diretrizes.").assertDoesNotExist()
        compose.onNodeWithText("Virar cartão").performScrollTo().performClick()
        compose.waitUntil(15_000) {compose.onAllNodesWithText("VERSO").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Voltar à frente").performScrollTo().performClick()
        compose.onNodeWithText("Qual função?").performScrollTo().assertIsDisplayed()
        runBlocking {assertTrue(app.repository.dao.flashReviews().isEmpty())}
        compose.onNodeWithText("Virar cartão").performScrollTo().performClick()
        compose.waitUntil(15_000) {compose.onAllNodesWithText("VERSO").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Sei").performScrollTo().performClick()
        compose.waitUntil(15_000) {compose.onAllNodesWithText("✓ Conhecimento registrado!").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Voltar à frente").performScrollTo().performClick()
        compose.onNodeWithText("Qual função?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Virar cartão").performScrollTo().performClick()
        compose.waitUntil(15_000) {compose.onAllNodesWithText("VERSO").fetchSemanticsNodes().isNotEmpty()}
        runBlocking {
            assertEquals(0,app.repository.dao.countQuestions())
            assertEquals(1,app.repository.dao.flashReviews().size)
            assertTrue(app.repository.dao.flashReviews().single().known)
        }
    }
}
