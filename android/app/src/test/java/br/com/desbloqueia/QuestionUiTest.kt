package br.com.desbloqueia

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
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
class QuestionUiTest {
    @get:Rule val compose=createComposeRule()
    private fun question(): Question {
        val context=ApplicationProvider.getApplicationContext<Context>()
        return context.assets.open("questions.json").bufferedReader().use { PackCodec.decode(it.readText()).first { it.fonte.questao==40 } }
    }
    private fun scrollTo(text: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }
    @Test fun choosingAnAlternativeInvokesAnswer() {
        val q=question();var selected: String?=null
        compose.setContent {MaterialTheme {QuestionScreen(QuestionUi(Session("ui",q)),Settings(animations=false),{selected=it},{},{},{})}}
        compose.onNodeWithText(q.enunciado).assertExists()
        scrollTo("4");compose.onNodeWithText("4").performClick()
        compose.runOnIdle {assertEquals("E",selected)}
    }
    @Test fun wrongAnswerRevealsExplanationAndAllAlternatives() {
        val q=question()
        compose.setContent {MaterialTheme {QuestionScreen(QuestionUi(Session("ui",q),selected="A"),Settings(animations=false),{fail("Answer must remain locked")},{},{},{})}}
        scrollTo("Resposta incorreta · Correta: E");compose.onNodeWithText("Resposta incorreta · Correta: E").assertIsDisplayed()
        scrollTo("Entenda a resposta");compose.onNodeWithText("Entenda a resposta").assertIsDisplayed()
        scrollTo("Analisar alternativas ↓");compose.onNodeWithText("Analisar alternativas ↓").performClick()
        q.explicacoes_alternativas.forEach { (letter,text) ->
            val full="$letter · ${if(letter==q.resposta_correta) "Correta" else "Incorreta"}\n$text"
            scrollTo(full);compose.onNodeWithText(full).assertIsDisplayed()
        }
        scrollTo("Concluir");compose.onNodeWithText("Concluir").assertIsDisplayed()
    }
}
