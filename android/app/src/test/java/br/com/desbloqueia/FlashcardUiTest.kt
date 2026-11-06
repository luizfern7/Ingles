package br.com.desbloqueia

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.com.desbloqueia.data.*
import br.com.desbloqueia.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlashcardUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun answerIsHiddenUntilFlipAndRatingRequiresSeeingBack() {
        val card=Flashcard("f","Banco","sfn_cmn","Qual função do CMN?","Definir diretrizes.","Conceito curto.")
        var state by mutableStateOf(QuestionUi(flash=FlashSession("session",card)))
        var rated: Boolean?=null
        compose.setContent {MaterialTheme {FlashcardScreen(state,Settings(animations=false),{state=state.copy(flipped=true)},{rated=it},{},{},{})}}
        compose.onNodeWithText(card.frente).assertExists()
        compose.onNodeWithText(card.verso).assertDoesNotExist()
        compose.onNodeWithText("Sei").assertDoesNotExist()
        compose.onNodeWithText("Virar cartão").performScrollTo().performClick()
        compose.onNodeWithText(card.verso).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Não sei").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(false,rated)}
    }
    @Test fun canFlipBackAndForthWithoutRating() {
        val card=Flashcard("f","Banco","sfn_cmn","Pergunta da frente","Resposta do verso")
        var state by mutableStateOf(QuestionUi(flash=FlashSession("session",card)))
        var ratings=0
        compose.setContent {MaterialTheme {FlashcardScreen(state,Settings(animations=false),{state=state.copy(flipped=!state.flipped)},{ratings++},{},{},{})}}
        compose.onNodeWithText("Virar cartão").performScrollTo().performClick()
        compose.onNodeWithText(card.verso).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Voltar à frente").performScrollTo().performClick()
        compose.onNodeWithText(card.frente).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(card.verso).assertDoesNotExist()
        compose.onNodeWithText("Virar cartão").performScrollTo().performClick()
        compose.onNodeWithText(card.verso).performScrollTo().assertIsDisplayed()
        compose.runOnIdle {assertEquals(0,ratings)}
    }

}
