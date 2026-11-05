package br.com.desbloqueia.data

import androidx.room.withTransaction
import br.com.desbloqueia.domain.Selection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class Session(val id: String, val question: Question)
class Repository(private val db: StudyDatabase) {
    val dao=db.dao()
    private val lock=Mutex()
    // Nenhum conteúdo é pré-carregado. Apenas abre/valida o banco existente.
    suspend fun initialize() { dao.countQuestions() }
    suspend fun importPack(text: String): Int {
        val questions=PackCodec.decode(text)
        return lock.withLock { db.withTransaction {
            // IDs existentes são preservados para não alterar o significado do histórico.
            dao.insertQuestions(questions.map { QuestionEntity(it.id,it.disciplina,PackCodec.encode(it)) }).count { it!=-1L }
        } }
    }
    private suspend fun select(s: Settings, now: Long): Question? = Selection.choose(
        dao.questions().map { PackCodec.question(it.payload) },dao.attempts(),dao.presentations(),s.disciplines,now)
    suspend fun next(s: Settings, now: Long=System.currentTimeMillis()): Session? = lock.withLock { db.withTransaction {
        val q=select(s,now)?:return@withTransaction null
        val id=UUID.randomUUID().toString();dao.present(PresentationEntity(id,q.id,now));Session(id,q)
    } }
    suspend fun answer(session: Session, letter: String, now: Long=System.currentTimeMillis()): Boolean = lock.withLock { db.withTransaction {
        require(letter in session.question.alternativas)
        // Ordenação estrita mesmo se o relógio retroceder ou duas respostas ocorrerem no mesmo ms.
        val answeredAt=maxOf(now,(dao.lastAnswerAt()?:0L)+1L)
        dao.answer(AttemptEntity(session.id,session.question.id,session.question.disciplina,letter,letter==session.question.resposta_correta,answeredAt))!=-1L
    } }
    suspend fun screenOff() = lock.withLock { db.withTransaction {
        val s=dao.unlock()?:UnlockState();dao.saveUnlock(s.copy(cycle=s.cycle+1))
    } }
    suspend fun unlockPrompt(settings: Settings, now: Long=System.currentTimeMillis()): Session? = lock.withLock { db.withTransaction {
        val state=dao.unlock()?:UnlockState()
        if(!Selection.due(state,settings,now)) return@withTransaction null
        val q=select(settings,now)?:return@withTransaction null
        val id=UUID.randomUUID().toString()
        dao.present(PresentationEntity(id,q.id,now))
        dao.saveUnlock(state.copy(handledCycle=state.cycle,lastPrompt=now,pendingId=id,pendingQuestion=q.id))
        Session(id,q)
    } }
    suspend fun pending(id: String): Session? = lock.withLock { db.withTransaction {
        val state=dao.unlock()?:return@withTransaction null
        if(state.pendingId!=id) return@withTransaction null
        val q=state.pendingQuestion?.let { dao.question(it) }?:return@withTransaction null
        dao.saveUnlock(state.copy(pendingId=null,pendingQuestion=null))
        Session(id,PackCodec.question(q.payload))
    } }
}
