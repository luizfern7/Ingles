package br.com.desbloqueia

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.desbloqueia.data.*
import br.com.desbloqueia.domain.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35],application=android.app.Application::class)
class FlashRepositoryTest {
    private lateinit var db: StudyDatabase;private lateinit var repo: Repository
    private val f=Flashcard("f1","Conhecimentos Bancários","sfn_cmn","Qual função?","Definir diretrizes.")
    private val f2=f.copy(id="f2",assunto_id="outro",frente="Outro conceito?",verso="Outra resposta.")
    private fun questions()=PackCodec.decode(TestQuestions.text()).take(2).map {it.copy(assunto_id="sfn_cmn")}
    private fun encode(pack: StudyPack)=PackCodec.json.encodeToString(StudyPack.serializer(),pack)
    @Before fun prepare() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),StudyDatabase::class.java).allowMainThreadQueries().build();repo=Repository(db)
    }
    @After fun close() {db.close()}
    @Test fun flashOnlyImportsIndependentlyAndRatingRequiresReveal(): Unit=runBlocking {
        assertEquals(1,repo.importPack(encode(StudyPack(flashcards=listOf(f)))))
        assertEquals(0,repo.dao.countQuestions());assertEquals(1,repo.dao.observeContentCount().first())
        val s=repo.nextStudy(Settings(),1000)!!.flash!!
        try {repo.rate(s,true,1000);fail()} catch(_: IllegalArgumentException) {}
        assertTrue(repo.dao.flashReviews().isEmpty())
        repo.reveal(s);val result=repo.rate(s,true,1000)
        assertTrue(result.second);assertEquals(1,result.first.correct)
        assertFalse(repo.rate(s,false,1001).second);assertEquals(1,repo.dao.flashReviews().size)
        assertNull(repo.nextStudy(Settings(studyMode="flashcards"),2000))
        val later=repo.nextStudy(Settings(studyMode="flashcards"),result.first.nextReviewAt+1)!!.flash!!
        repo.reveal(later);val wrong=repo.rate(later,false,result.first.nextReviewAt+1).first
        assertEquals("aprendendo",wrong.state);assertEquals(10*SpacedReview.MINUTE,wrong.nextReviewAt-wrong.lastReviewAt)
    }
    @Test fun wrongQuestionBoostsTopicButKeepsMixedQuota(): Unit=runBlocking {
        repo.importPack(encode(StudyPack(questions=questions(),flashcards=listOf(f,f2))))
        val kinds=mutableListOf<String>()
        repeat(10) {i ->
            val s=repo.nextStudy(Settings(),10000+i.toLong())!!;kinds+=s.kind
            s.question?.let {q ->repo.answer(q,q.question.alternativas.keys.first {it!=q.question.resposta_correta},10000+i.toLong())}
        }
        assertEquals(List(5) {listOf("question","flashcard")}.flatten(),kinds)
        assertEquals(5,repo.dao.attempts().size)
        assertEquals(5,repo.dao.topicStats().first().single().answered)
        assertEquals(0,repo.dao.topicStats().first().single().correct)
    }
    @Test fun errorsBringRelatedFutureCardDueAndNoMissingAssociationFailure(): Unit=runBlocking {
        repo.importPack(encode(StudyPack(questions=questions(),flashcards=listOf(f,f2))))
        repo.dao.saveProgress(FlashProgress(f.id,correct=3,streak=3,lastReviewAt=100,nextReviewAt=999999,state="memorizado"))
        val q=repo.next(Settings(),1000)!!;repo.answer(q,q.question.alternativas.keys.first {it!=q.question.resposta_correta},1000)
        assertEquals(1000,repo.dao.progress(f.id)!!.nextReviewAt.toInt())
        assertEquals(f.id,repo.nextStudy(Settings(studyMode="flashcards"),1001)!!.flash!!.card.id)
        val unmatched=questions()[0].copy(id="unmatched",assunto_id="no_cards")
        repo.importPack(encode(StudyPack(questions=listOf(unmatched))))
        val session=Session("manual-unmatched",unmatched)
        assertTrue(repo.answer(session,unmatched.alternativas.keys.first {it!=unmatched.resposta_correta},2000))
    }
    @Test fun mixAndPendingFlashPersistAcrossRepositoryRecreation(): Unit=runBlocking {
        repo.importPack(encode(StudyPack(questions=questions(),flashcards=listOf(f,f2))))
        repo.screenOff();assertNotNull(repo.unlockStudy(Settings(unlock=true),1000)!!.question)
        repo.screenOff()
        val restarted=Repository(db);val s=restarted.unlockStudy(Settings(unlock=true),2000)!!
        assertNotNull(s.flash)
        assertNull(restarted.unlockStudy(Settings(unlock=true),2001))
        assertEquals(s.id,Repository(db).pendingStudy(s.id)!!.id)
        assertNull(restarted.pendingStudy(s.id))
    }
    @Test fun classificationLinksOldErrorsWithoutChangingAnswersOrPayloadMeaning(): Unit=runBlocking {
        val q=questions()[0].copy(assunto_id=null)
        repo.importPack(encode(StudyPack(questions=listOf(q),flashcards=listOf(f))))
        repo.dao.saveProgress(FlashProgress(f.id,lastReviewAt=100,nextReviewAt=999999))
        val s=repo.next(Settings(),1000)!!;repo.answer(s,q.alternativas.keys.first {it!=q.resposta_correta},1000)
        assertNull(repo.dao.attempts().single().topicId)
        val result=repo.importStudyPack(encode(StudyPack(questions=listOf(q.copy(assunto_id="sfn_cmn")))))
        assertEquals(1,result.linked);assertEquals("sfn_cmn",repo.dao.attempts().single().topicId)
        assertEquals(1000L,repo.dao.progress(f.id)!!.nextReviewAt)
        assertFalse(repo.dao.attempts().single().correct)
        assertEquals(q.resposta_correta,PackCodec.question(repo.dao.question(q.id)!!.payload).resposta_correta)
    }
    @Test fun suggestedCardsNeedExplicitSaveStayPendingAndDeduplicate(): Unit=runBlocking {
        val q=questions()[0];val draft=ExplanationSuggestionProvider().suggest(q)!!
        assertTrue(draft.front.contains(q.enunciado));assertTrue(draft.front.contains(q.alternativas.values.first()))
        assertTrue(repo.dao.flashcards().isEmpty())
        assertEquals(1,repo.saveDraft(draft,"sfn_cmn",draft.front,draft.back,draft.explanation,false).flashcards)
        assertEquals(0,repo.saveDraft(draft,"sfn_cmn",draft.front,draft.back,draft.explanation,false).flashcards)
        val card=PackCodec.card(repo.dao.flashcards().single().payload)
        assertEquals("revisao_pendente",card.status_validacao);assertEquals(q.id,card.derivado_de)
    }
    @Test fun flashProgressSurvivesDiskReopen(): Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="flash-disk-test.db";context.deleteDatabase(name)
        val disk=Room.databaseBuilder(context,StudyDatabase::class.java,name).allowMainThreadQueries().build()
        val r=Repository(disk);r.importPack(encode(StudyPack(flashcards=listOf(f))))
        val s=r.nextStudy(Settings(studyMode="flashcards"),1000)!!.flash!!;r.reveal(s);r.rate(s,true,1000);disk.close()
        val restored=Room.databaseBuilder(context,StudyDatabase::class.java,name).allowMainThreadQueries().build()
        assertEquals(1,restored.dao().progress(f.id)!!.correct);assertEquals(1,restored.dao().flashReviews().size)
        restored.close();context.deleteDatabase(name)
    }
}
