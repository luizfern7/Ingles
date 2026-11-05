package br.com.desbloqueia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.desbloqueia.data.*
import br.com.desbloqueia.domain.Selection
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class StudyTest {
    private lateinit var db: StudyDatabase
    private lateinit var repo: Repository
    private lateinit var context: Context
    private lateinit var questions: List<Question>
    @Before fun prepare() = runBlocking {
        context=ApplicationProvider.getApplicationContext()
        db=Room.inMemoryDatabaseBuilder(context,StudyDatabase::class.java).allowMainThreadQueries().build()
        repo=Repository(db,context)
        repo.initialize()
        questions=db.dao().questions().map { PackCodec.question(it.payload) }
    }
    @After fun close() { db.close() }
    @Test fun bundledPackHas25TraceableQuestions() {
        assertEquals(25,questions.size)
        assertEquals(setOf("Língua Portuguesa","Matemática","Conhecimentos Bancários"),questions.map { it.disciplina }.toSet())
        questions.forEach { assertEquals(16,it.fonte.pagina_gabarito);assertEquals(5,it.explicacoes_alternativas.size);assertTrue(it.explicacao.length>80) }
    }
    @Test fun officialKeyIsCorrectlyMatched() {
        val expected=mapOf(10 to "D",12 to "A",13 to "B",16 to "B",17 to "D",35 to "B",36 to "C",37 to "A",39 to "C",40 to "E",41 to "C",42 to "B",43 to "A",44 to "D",45 to "B",46 to "A",49 to "D",50 to "B",51 to "D",52 to "C",54 to "B",55 to "E",56 to "D",58 to "A",59 to "B")
        questions.forEach { assertEquals(expected[it.fonte.questao],it.resposta_correta) }
    }
    @Test fun malformedImportIsAtomic() = runBlocking {
        val bad=QuestionPack(1,listOf(questions[0],questions[1].copy(resposta_correta="Z")))
        try {repo.importPack(PackCodec.json.encodeToString(QuestionPack.serializer(),bad));fail("Expected validation error")} catch(_: IllegalArgumentException) {}
        assertEquals(25,db.dao().questions().size)
    }
    @Test fun duplicateIdsAreRejected() {
        try {PackCodec.decode(PackCodec.json.encodeToString(QuestionPack.serializer(),QuestionPack(1,listOf(questions[0],questions[0]))));fail()} catch(_: IllegalArgumentException) {}
    }
    @Test fun unverifiedKeyAndMissingExplanationsAreRejected() {
        for(q in listOf(questions[0].copy(status_validacao="pendente"),questions[0].copy(explicacoes_alternativas=emptyMap()))) {
            try { PackCodec.decode(PackCodec.json.encodeToString(QuestionPack.serializer(),QuestionPack(1,listOf(q))));fail() } catch(_: IllegalArgumentException) {}
        }
    }
    @Test fun repeatImportPreservesAnswers() = runBlocking {
        val s=repo.next(Settings())!!;repo.answer(s,s.question.resposta_correta)
        val text=context.assets.open("questions.json").bufferedReader().use { it.readText() }
        assertEquals(0,repo.importPack(text));repo.initialize()
        assertEquals(1,db.dao().attempts().size)
    }
    @Test fun newPackageIsAddedWithoutReplacingIds() = runBlocking {
        val q=questions[0].copy(id="new-id")
        assertEquals(1,repo.importPack(PackCodec.json.encodeToString(QuestionPack.serializer(),QuestionPack(1,listOf(q)))))
        assertEquals(26,db.dao().questions().size)
    }
    @Test fun gradingAndDuplicateTapArePersistent() = runBlocking {
        val s=repo.next(Settings())!!
        assertTrue(repo.answer(s,s.question.resposta_correta))
        assertFalse(repo.answer(s,s.question.alternativas.keys.first { it!=s.question.resposta_correta }))
        val attempts=db.dao().attempts();assertEquals(1,attempts.size);assertTrue(attempts.single().correct)
        val s2=repo.next(Settings())!!
        repo.answer(s2,s2.question.alternativas.keys.first { it!=s2.question.resposta_correta })
        assertFalse(db.dao().attempts().first {it.presentationId==s2.id}.correct)
    }
    @Test fun unvisitedQuestionsComeBeforeReviewedOnes() {
        val q=questions[0]
        val a=AttemptEntity("a",q.id,q.disciplina,"A",true,1)
        assertNotEquals(q.id,Selection.choose(questions,listOf(a),emptyList(),emptySet(),1000)!!.id)
    }
    @Test fun reviewMistakesBeforeKnownCorrectWhenDue() {
        val q=questions.take(2)
        val attempts=q.mapIndexed { i,it -> AttemptEntity("$i",it.id,it.disciplina,"A",i==0,1) }
        assertEquals(q[1].id,Selection.choose(q,attempts,emptyList(),emptySet(),2_000_000)!!.id)
    }
    @Test fun disciplineAndImmediateRepeatRules() {
        val math=questions.first { it.disciplina=="Matemática" }
        val shown=listOf(PresentationEntity("p",math.id,1))
        val next=Selection.choose(questions,emptyList(),shown,setOf("Matemática"),1000)!!
        assertEquals("Matemática",next.disciplina);assertNotEquals(math.id,next.id)
        assertNull(Selection.choose(questions,emptyList(),emptyList(),setOf("Inexistente"),1000))
    }
    @Test fun unlockFrequencyPauseAndOptIn() {
        val s=UnlockState(cycle=2,handledCycle=1,lastPrompt=1_000_000)
        assertFalse(Selection.due(s,Settings(),10_000_000))
        assertFalse(Selection.due(s,Settings(unlock=true),1_100_000))
        assertFalse(Selection.due(s,Settings(unlock=true,pausedUntil=20_000_000),10_000_000))
        assertTrue(Selection.due(s,Settings(unlock=true),10_000_000))
        assertFalse(Selection.due(s.copy(handledCycle=2),Settings(unlock=true),10_000_000))
    }
    @Test fun unlockEventsAreDeduplicatedAcrossRepositoryRestart() = runBlocking {
        repo.screenOff();val session=repo.unlockPrompt(Settings(unlock=true),10_000_000)!!
        val restarted=Repository(db,context)
        assertNull(restarted.unlockPrompt(Settings(unlock=true),20_000_000))
        assertEquals(session.id,restarted.pending(session.id)!!.id)
        assertNull(restarted.pending(session.id))
        restarted.screenOff()
        assertNotNull(restarted.unlockPrompt(Settings(unlock=true),20_000_000))
    }
    @Test fun diskDatabaseSurvivesReopen(): Unit = runBlocking {
        val name="durable-test.db";context.deleteDatabase(name)
        val disk=Room.databaseBuilder(context,StudyDatabase::class.java,name).allowMainThreadQueries().build()
        val r=Repository(disk,context);r.initialize();val s=r.next(Settings())!!;r.answer(s,s.question.resposta_correta);r.screenOff();disk.close()
        val reopened=Room.databaseBuilder(context,StudyDatabase::class.java,name).allowMainThreadQueries().build()
        assertEquals(1,reopened.dao().attempts().size);assertEquals(1L,reopened.dao().unlock()!!.cycle)
        reopened.close();context.deleteDatabase(name)
    }
}
