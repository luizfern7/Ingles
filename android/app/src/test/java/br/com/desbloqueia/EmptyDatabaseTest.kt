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
class EmptyDatabaseTest {
    private lateinit var db: StudyDatabase
    private lateinit var repo: Repository
    @Before fun prepare() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        db=Room.inMemoryDatabaseBuilder(context,StudyDatabase::class.java).allowMainThreadQueries().build()
        repo=Repository(db)
    }
    @After fun close() {db.close()}
    @Test fun firstAndRepeatedStartupHaveNoSeedAndNoSession(): Unit=runBlocking {
        repo.initialize();repo.initialize()
        assertEquals(0,repo.dao.countQuestions())
        assertNull(repo.next(Settings()))
        repo.screenOff()
        assertNull(repo.unlockPrompt(Settings(unlock=true,frequency=0),1000))
        assertEquals(0,repo.dao.presentations().size)
        val context=ApplicationProvider.getApplicationContext<Context>()
        assertFalse(context.assets.list("")!!.contains("questions.json"))
    }
    @Test fun onlyExplicitImportAddsQuestionsAndRestartDoesNotResetProgress(): Unit=runBlocking {
        repo.initialize();assertEquals(0,repo.dao.countQuestions())
        assertEquals(25,repo.importPack(TestQuestions.text()))
        val session=repo.next(Settings())!!
        repo.answer(session,session.question.resposta_correta)
        val reopened=Repository(db);reopened.initialize()
        assertEquals(25,reopened.dao.countQuestions())
        assertEquals(1,reopened.dao.attempts().size)
        assertEquals(0,reopened.importPack(TestQuestions.text()))
    }
    @Test fun invalidPackLeavesEmptyDatabaseUntouched(): Unit=runBlocking {
        repo.initialize()
        try {repo.importPack("{\"schema_version\":1,\"questions\":[]}");fail("Empty import must be rejected")} catch(_: IllegalArgumentException) {}
        assertEquals(0,repo.dao.countQuestions())
    }
}
