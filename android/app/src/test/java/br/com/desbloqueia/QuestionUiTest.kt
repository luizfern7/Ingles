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
        return PackCodec.decode(TestQuestions.text()).first { it.fonte.questao==40 }
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
    private fun aiQuestion(): Question=checkNotNull(javaClass.classLoader!!.getResourceAsStream("autoral_ia_fixture.json")).bufferedReader().use {PackCodec.decode(it.readText()).single()}
    @Test fun hintRevealsOnlyHintBeforeAnswerWithoutGrading() {
        val q=aiQuestion();var answerCount=0
        compose.setContent {MaterialTheme {QuestionScreen(QuestionUi(Session("hint",q)),Settings(animations=false),{answerCount++},{},{},{})}}
        compose.onNodeWithText(q.dica!!).assertDoesNotExist()
        scrollTo("Ver dica");compose.onNodeWithText("Ver dica").performClick()
        scrollTo(q.dica!!);compose.onNodeWithText(q.dica!!).assertIsDisplayed()
        compose.onNodeWithText("Entenda a resposta").assertDoesNotExist()
        compose.runOnIdle {assertEquals(0,answerCount)}
        scrollTo("Ocultar dica");compose.onNodeWithText("Ocultar dica").performClick()
        compose.onNodeWithText(q.dica!!).assertDoesNotExist()
    }
    private fun noHint(hint: String?) {
        val q=aiQuestion().copy(dica=hint)
        compose.setContent {MaterialTheme {QuestionScreen(QuestionUi(Session("no-hint",q)),Settings(animations=false),{},{},{},{})}}
        compose.onNodeWithText(q.enunciado).assertExists()
        compose.onNodeWithText("Ver dica").assertDoesNotExist()
        compose.onNodeWithText("Ocultar dica").assertDoesNotExist()
    }
    @Test fun noHintButtonWhenFieldIsAbsentOrNull() {noHint(null)}
    @Test fun noHintButtonWhenFieldIsEmpty() {noHint("")}
    @Test fun noHintButtonWhenFieldIsWhitespace() {noHint("   ")}
    @Test fun provenanceAndTechnicalIdsDoNotClutterStudy() {
        val q=aiQuestion().copy(assunto_id="topic_internal_id")
        compose.setContent {MaterialTheme {QuestionScreen(QuestionUi(Session("review",q)),Settings(animations=false),{},{},{},{})}}
        compose.onNodeWithText(q.reviewNotice()).assertDoesNotExist()
        compose.onNodeWithText(q.originSummary()).assertDoesNotExist()
        compose.onNodeWithText("Vínculo: topic_internal_id").assertDoesNotExist()
        compose.onNodeWithText(q.enunciado).assertExists()
    }

    @Test fun absentAlternativeAnalysisIsSilentAndExplanationRemains() {
        val q=aiQuestion().copy(explicacoes_alternativas=emptyMap())
        compose.setContent {MaterialTheme {QuestionScreen(QuestionUi(Session("clean",q),selected=q.resposta_correta),Settings(animations=false),{},{},{},{})}}
        scrollTo("Entenda a resposta");compose.onNodeWithText("Entenda a resposta").assertIsDisplayed()
        scrollTo("Próximo item")
        compose.onNodeWithText("Analisar alternativas ↓").assertDoesNotExist()
        compose.onNodeWithText("Este pacote não forneceu comentários individuais das alternativas.").assertDoesNotExist()
        compose.onNodeWithText(q.autoria_explicacao).assertDoesNotExist()
        compose.onNodeWithText(q.originDetails()).assertDoesNotExist()
    }

}
