package br.com.desbloqueia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.desbloqueia.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35],application=android.app.Application::class)
class AiPackTest {
    private fun text()=checkNotNull(javaClass.classLoader!!.getResourceAsStream("autoral_ia_fixture.json")).bufferedReader().use {it.readText()}
    private fun q()=PackCodec.decode(text()).single()
    private fun encode(question: Question)=PackCodec.json.encodeToString(QuestionPack.serializer(),QuestionPack(1,listOf(question)))
    @Test fun aiSourceNeedsNoPdfHashOrPages() {
        val q=q();assertEquals("autoral_ia",q.fonte.tipo);assertEquals("",q.fonte.sha256);assertEquals(0,q.fonte.pagina)
        assertEquals("revisao_pendente",q.status_validacao);assertTrue(q.reviewNotice().contains("podem conter erros"))
        assertFalse(q.originSummary().contains("2013"));assertTrue(q.originDetails().contains("não é gabarito oficial"))
    }
    @Test fun oldOfficialPackagesRemainReadable() {
        val official=PackCodec.decode(TestQuestions.text())
        assertEquals(25,official.size);assertTrue(official.all {it.fonte.tipo=="oficial" && it.dica==null})
    }
    @Test fun hintIsOptionalNullEmptyOrWhitespace() {
        assertNull(PackCodec.decode(encode(q().copy(dica=null)).replace("\"fonte\":","\"dica\":null,\"fonte\":" )).single().dica)
        for(hint in listOf(null,"","   ","Pense no percentual do valor original.")) {
            val restored=PackCodec.decode(encode(q().copy(dica=hint))).single()
            assertEquals(hint,restored.dica)
        }
    }
    @Test fun aiCanDeclareReviewedWithoutPretendingOfficialKey() {
        val q=PackCodec.decode(encode(q().copy(status_validacao="revisado"))).single()
        assertTrue(q.reviewNotice().contains("declarada"))
        try {PackCodec.decode(encode(q.copy(status_validacao="gabarito_conferido")));fail()} catch(_: IllegalArgumentException) {}
    }
    @Test fun missingAiDescriptionUnknownSourceAndInvalidAnswerAreRejected() {
        for(q in listOf(q().copy(fonte=Source(tipo="autoral_ia")),q().copy(fonte=Source(tipo="desconhecido")),q().copy(resposta_correta="E"),q().copy(explicacoes_alternativas=emptyMap()))) {
            try {PackCodec.decode(encode(q));fail("Invalid question accepted")} catch(_: IllegalArgumentException) {}
        }
    }
    @Test fun aiImportPersistsHintAndProgressAfterReopen(): Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="ai-pack-test.db";context.deleteDatabase(name)
        val db=Room.databaseBuilder(context,StudyDatabase::class.java,name).allowMainThreadQueries().build()
        val repo=Repository(db);repo.initialize();assertEquals(0,repo.dao.countQuestions())
        assertEquals(1,repo.importPack(text()))
        val session=repo.next(Settings())!!;assertEquals(q().dica,session.question.dica)
        repo.answer(session,"C");db.close()
        val opened=Room.databaseBuilder(context,StudyDatabase::class.java,name).allowMainThreadQueries().build()
        val restored=PackCodec.question(opened.dao().questions().single().payload)
        assertEquals(q().dica,restored.dica);assertEquals("autoral_ia",restored.fonte.tipo)
        assertTrue(opened.dao().attempts().single().correct)
        opened.close();context.deleteDatabase(name)
    }
}
