package br.com.desbloqueia

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.desbloqueia.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35],application=android.app.Application::class)
class MigrationTest {
    @Test fun v1UpgradePreservesEveryLegacyTableAndPendingQuestion(): Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="migration-test.db";context.deleteDatabase(name)
        val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        val old=SQLiteDatabase.openOrCreateDatabase(path,null)
        old.execSQL("CREATE TABLE questions (id TEXT NOT NULL,discipline TEXT NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(id))")
        old.execSQL("CREATE TABLE attempts (presentationId TEXT NOT NULL,questionId TEXT NOT NULL,discipline TEXT NOT NULL,selected TEXT NOT NULL,correct INTEGER NOT NULL,answeredAt INTEGER NOT NULL,PRIMARY KEY(presentationId))")
        old.execSQL("CREATE INDEX index_attempts_questionId ON attempts(questionId)")
        old.execSQL("CREATE TABLE presentations (id TEXT NOT NULL,questionId TEXT NOT NULL,shownAt INTEGER NOT NULL,PRIMARY KEY(id))")
        old.execSQL("CREATE INDEX index_presentations_questionId ON presentations(questionId)")
        old.execSQL("CREATE TABLE unlock_state (singleton INTEGER NOT NULL,cycle INTEGER NOT NULL,handledCycle INTEGER NOT NULL,lastPrompt INTEGER NOT NULL,pendingId TEXT,pendingQuestion TEXT,PRIMARY KEY(singleton))")
        val q=PackCodec.decode(TestQuestions.text()).first();val payload=PackCodec.encode(q)
        old.execSQL("INSERT INTO questions VALUES(?,?,?)",arrayOf(q.id,q.disciplina,payload))
        old.execSQL("INSERT INTO attempts VALUES(?,?,?,?,?,?)",arrayOf("old-attempt",q.id,q.disciplina,q.resposta_correta,1,1000))
        old.execSQL("INSERT INTO presentations VALUES(?,?,?)",arrayOf("old-session",q.id,999))
        old.execSQL("INSERT INTO unlock_state VALUES(1,4,4,1000,?,?)",arrayOf("old-pending",q.id))
        old.version=1;old.close()
        val db=Room.databaseBuilder(context,StudyDatabase::class.java,name).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        assertEquals(1,db.dao().countQuestions())
        assertEquals(payload,db.dao().question(q.id)!!.payload)
        assertNull(db.dao().question(q.id)!!.topicId)
        assertEquals(1,db.dao().attempts().size);assertTrue(db.dao().attempts().single().correct)
        assertEquals("old-session",db.dao().presentations().single().id)
        assertEquals(4L,db.dao().unlock()!!.cycle)
        assertEquals("question",db.dao().unlock()!!.pendingKind)
        assertEquals(q.id,Repository(db).pendingStudy("old-pending")!!.question!!.question.id)
        assertTrue(db.dao().flashcards().isEmpty());assertTrue(db.dao().flashReviews().isEmpty())
        db.close();context.deleteDatabase(name)
    }
}
