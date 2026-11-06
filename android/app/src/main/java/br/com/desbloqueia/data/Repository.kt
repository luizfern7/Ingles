package br.com.desbloqueia.data

import androidx.room.withTransaction
import br.com.desbloqueia.domain.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class Session(val id: String,val question: Question)
class Repository(private val db: StudyDatabase) {
    val dao=db.dao()
    private val lock=Mutex()
    suspend fun initialize() {dao.countQuestions()}
    suspend fun importPack(text: String): Int=importStudyPack(text).total
    suspend fun importStudyPack(text: String): ImportResult {
        val pack=PackCodec.decodeStudy(text)
        return lock.withLock {db.withTransaction {
            var added=0;var linked=0;var duplicates=0
            for(q in pack.questions) {
                val old=dao.question(q.id)
                if(old==null) {
                    dao.insertQuestions(listOf(QuestionEntity(q.id,q.disciplina,PackCodec.encode(q),q.assunto_id)));added++
                } else {
                    val original=PackCodec.question(old.payload)
                    if(old.topicId==null && q.assunto_id!=null && original.enunciado==q.enunciado && original.alternativas==q.alternativas && original.resposta_correta==q.resposta_correta) {
                        dao.updateQuestion(old.copy(payload=PackCodec.encode(original.copy(assunto_id=q.assunto_id)),topicId=q.assunto_id))
                        dao.classifyAttempts(q.id,q.assunto_id)
                        dao.lastTopicError(q.assunto_id)?.let {dao.boostTopic(q.assunto_id,it)}
                        linked++
                    } else duplicates++
                }
            }
            val inserted=dao.insertFlashcards(pack.flashcards.map {FlashcardEntity(it.id,it.disciplina,it.assunto_id,PackCodec.fingerprint(it),PackCodec.encodeCard(it))})
            val cards=inserted.count {it!=-1L};duplicates+=pack.flashcards.size-cards
            ImportResult(added,cards,linked,duplicates)
        }}
    }
    private suspend fun select(s: Settings,now: Long)=Selection.choose(dao.questions().map {PackCodec.question(it.payload)},dao.attempts(),dao.presentations(),s.disciplines,now)
    private suspend fun createQuestion(q: Question,now: Long): Session {
        val id=UUID.randomUUID().toString();dao.present(PresentationEntity(id,q.id,maxOf(now,(dao.lastQuestionShown()?:0L)+1)));return Session(id,q)
    }
    private suspend fun createFlash(f: Flashcard,now: Long): FlashSession {
        val id=UUID.randomUUID().toString();dao.presentFlash(FlashPresentation(id,f.id,maxOf(now,(dao.lastFlashShown()?:0L)+1)));return FlashSession(id,f)
    }
    private suspend fun selectStudy(s: Settings,now: Long,channel: String): StudySession? {
        val q=if(s.studyMode=="flashcards" || (s.studyMode=="mixed" && s.flashPercent==100)) null else select(s,now)
        val f=if(s.studyMode=="questions" || (s.studyMode=="mixed" && s.flashPercent==0)) null else SpacedReview.choose(
            dao.flashcards().map {PackCodec.card(it.payload)},dao.flashProgress(),dao.flashPresentations(),dao.attempts(),s.disciplines,now)
        val decision=StudyMixer.choose(s,dao.mix(channel),channel,q!=null,f!=null)?:return null
        val session=if(decision.kind=="flashcard") StudySession(flash=createFlash(f!!,now)) else StudySession(question=createQuestion(q!!,now))
        dao.saveMix(decision.state);return session
    }
    suspend fun next(s: Settings,now: Long=System.currentTimeMillis()): Session?=lock.withLock {db.withTransaction {select(s,now)?.let {createQuestion(it,now)}}}
    suspend fun nextStudy(s: Settings,now: Long=System.currentTimeMillis(),reviewOnly: Boolean=false): StudySession?=lock.withLock {db.withTransaction {
        selectStudy(if(reviewOnly) s.copy(studyMode="flashcards") else s,now,if(reviewOnly)"review" else "manual")
    }}
    suspend fun answer(session: Session,letter: String,now: Long=System.currentTimeMillis()): Boolean=lock.withLock {db.withTransaction {
        require(letter in session.question.alternativas)
        val answeredAt=maxOf(now,(dao.lastLearningAt()?:0L)+1L)
        val topic=dao.question(session.question.id)?.topicId?:session.question.assunto_id
        val correct=letter==session.question.resposta_correta
        val added=dao.answer(AttemptEntity(session.id,session.question.id,session.question.disciplina,letter,correct,answeredAt,topic))!=-1L
        if(added && !correct && topic!=null) dao.boostTopic(topic,answeredAt)
        added
    }}
    suspend fun reveal(session: FlashSession): Boolean=lock.withLock {db.withTransaction {
        val p=dao.flashPresentation(session.id)?:return@withTransaction false
        require(p.cardId==session.card.id)
        dao.revealFlash(session.id);true
    }}
    suspend fun rate(session: FlashSession,known: Boolean,now: Long=System.currentTimeMillis()): Pair<FlashProgress,Boolean> = lock.withLock {db.withTransaction {
        val presentation=dao.flashPresentation(session.id)?:throw IllegalArgumentException("Sessão inexistente")
        require(presentation.cardId==session.card.id && presentation.revealed) {"Vire o cartão antes de avaliar"}
        val old=dao.progress(session.card.id)?:FlashProgress(session.card.id)
        if(dao.flashReview(session.id)!=null) return@withTransaction old to false
        val time=maxOf(now,(dao.lastLearningAt()?:0L)+1)
        val next=SpacedReview.review(old,known,time)
        val added=dao.insertReview(FlashReview(session.id,session.card.id,session.card.assunto_id,known,time,next.nextReviewAt))!=-1L
        if(added) dao.saveProgress(next)
        next to added
    }}
    suspend fun saveDraft(draft: FlashcardDraft,topicText: String,front: String,back: String,explanation: String,reviewed: Boolean): ImportResult {
        val topic=PackCodec.topic(topicText)?:throw IllegalArgumentException("Informe um assunto_id estável")
        val existing=dao.question(draft.sourceQuestion.id)
        val classified=existing?.topicId?:draft.sourceQuestion.assunto_id
        require(classified==null || classified==topic) {"Mantenha o assunto_id já classificado desta questão"}
        val provisional=Flashcard("draft",draft.sourceQuestion.disciplina,topic,front,back,explanation,
            draft.sourceQuestion.assunto,draft.sourceQuestion.subassunto,if(reviewed)"revisado" else "revisao_pendente",
            Source(tipo="autoral",descricao="Sugestão local baseada no gabarito e na explicação do pacote; não validada automaticamente."),draft.sourceQuestion.id)
        val card=provisional.copy(id="derived-"+PackCodec.fingerprint(provisional).take(24))
        val classification=if(existing!=null && existing.topicId==null) listOf(draft.sourceQuestion.copy(assunto_id=topic)) else emptyList()
        return importStudyPack(PackCodec.json.encodeToString(StudyPack.serializer(),StudyPack(questions=classification,flashcards=listOf(card))))
    }
    suspend fun screenOff()=lock.withLock {db.withTransaction {
        val s=dao.unlock()?:UnlockState();dao.saveUnlock(s.copy(cycle=s.cycle+1))
    }}
    suspend fun unlockStudy(s: Settings,now: Long=System.currentTimeMillis()): StudySession?=lock.withLock {db.withTransaction {
        val state=dao.unlock()?:UnlockState()
        if(!Selection.due(state,s,now)) return@withTransaction null
        val session=selectStudy(s,now,"unlock")?:return@withTransaction null
        dao.saveUnlock(state.copy(handledCycle=state.cycle,lastPrompt=now,pendingId=session.id,pendingQuestion=session.contentId,pendingKind=session.kind));session
    }}
    // Compatibility APIs retained for existing tests and callers.
    suspend fun unlockPrompt(s: Settings,now: Long=System.currentTimeMillis()): Session?=lock.withLock {db.withTransaction {
        val state=dao.unlock()?:UnlockState()
        if(!Selection.due(state,s,now)) return@withTransaction null
        val q=select(s,now)?:return@withTransaction null
        val session=createQuestion(q,now)
        dao.saveUnlock(state.copy(handledCycle=state.cycle,lastPrompt=now,pendingId=session.id,pendingQuestion=q.id,pendingKind="question"));session
    }}
    suspend fun pendingStudy(id: String): StudySession?=lock.withLock {db.withTransaction {
        val state=dao.unlock()?:return@withTransaction null
        if(state.pendingId!=id) return@withTransaction null
        val item=state.pendingQuestion?:return@withTransaction null
        val session=if(state.pendingKind=="flashcard") {
            val f=dao.flashcard(item)?:return@withTransaction null
            StudySession(flash=FlashSession(id,PackCodec.card(f.payload)))
        } else {
            val q=dao.question(item)?:return@withTransaction null
            StudySession(question=Session(id,PackCodec.question(q.payload)))
        }
        dao.saveUnlock(state.copy(pendingId=null,pendingQuestion=null));session
    }}
    suspend fun pending(id: String)=pendingStudy(id)?.question
}
