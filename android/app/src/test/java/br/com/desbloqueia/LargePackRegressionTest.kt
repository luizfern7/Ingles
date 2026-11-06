package br.com.desbloqueia

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.desbloqueia.data.*
import br.com.desbloqueia.domain.SpacedReview
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35],application=android.app.Application::class)
class LargePackRegressionTest {
    private fun input(): String {
        val external=System.getProperty("regression.pack.path")
        if(external!=null) return File(external).readText()
        // Public synthetic fallback; the user's private uploaded pack is never bundled or published.
        val base=PackCodec.decode(TestQuestions.text()).first()
        val questions=(1..500).map {base.copy(id="q$it",assunto_id="topic_${it%25}")}
        val cards=(1..500).map {Flashcard("f$it","Banco","topic_${it%25}","Pergunta $it","Resposta $it")}
        return PackCodec.json.encodeToString(StudyPack.serializer(),StudyPack(questions=questions,flashcards=cards))
    }
    @Test fun largeBankKeepsSelectingAcrossUnratedReviewedAndWrongTopicCards(): Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),StudyDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo=Repository(db);val result=repo.importStudyPack(input())
            assertEquals(500,result.questions);assertEquals(500,result.flashcards)
            var now=1_800_000_000_000L
            // First leave several cards unanswered: reviewed/presented timestamps must coexist with missing ones.
            repeat(12) { assertNotNull(repo.nextStudy(Settings(studyMode="flashcards"),now++)!!.flash) }
            repeat(120) {i ->
                val s=repo.nextStudy(Settings(studyMode="flashcards"),now)!!.flash!!
                if(i%3!=0) {repo.reveal(s);repo.rate(s,i%2==0,now)}
                if(i%4==0) {
                    val q=repo.next(Settings(),now)!!
                    repo.answer(q,q.question.alternativas.keys.first {it!=q.question.resposta_correta},now)
                }
                now+=SpacedReview.MINUTE
            }
            val before=repo.dao.flashReviews().size;assertTrue(before>0)
            // Returning after a day mixes scheduled old cards and remaining new cards, including new topic boosts.
            now+=SpacedReview.DAY
            val reopened=Repository(db)
            repeat(20) {
                reopened.screenOff()
                assertNotNull(reopened.unlockStudy(Settings(unlock=true),now))
                assertNull(reopened.unlockStudy(Settings(unlock=true),now+1))
                now+=SpacedReview.MINUTE
            }
            assertNotNull(reopened.nextStudy(Settings(),now,reviewOnly=true)!!.flash)
            assertEquals(before,repo.dao.flashReviews().size)
            assertEquals(500,repo.dao.countQuestions());assertEquals(500,repo.dao.flashcards().size)
            println("Large pack verified: 500 questions, 500 flashcards; repeated sessions, ratings, topic errors, next-day review and unlocks.")
        } finally {db.close()}
    }
}
