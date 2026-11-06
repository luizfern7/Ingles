package br.com.desbloqueia.data

import androidx.room.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

@Serializable data class Flashcard(
    val id: String, val disciplina: String, val assunto_id: String, val frente: String, val verso: String,
    val explicacao: String?=null, val assunto: String?=null, val subassunto: String?=null,
    val status_validacao: String="revisao_pendente", val fonte: Source?=null, val derivado_de: String?=null
)
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class StudyPack(@EncodeDefault val schema_version: Int=2,val questions: List<Question> = emptyList(),val flashcards: List<Flashcard> = emptyList())
@Entity(tableName="flashcards",indices=[Index("topicId"),Index(value=["fingerprint"],unique=true)])
data class FlashcardEntity(@PrimaryKey val id: String,val discipline: String,val topicId: String,val fingerprint: String,val payload: String)
@Entity(tableName="flash_progress") data class FlashProgress(
    @PrimaryKey val cardId: String,val correct: Int=0,val wrong: Int=0,val streak: Int=0,
    val lastReviewAt: Long=0,val nextReviewAt: Long=0,val state: String="novo"
)
@Entity(tableName="flash_reviews",indices=[Index("cardId"),Index("topicId")]) data class FlashReview(
    @PrimaryKey val presentationId: String,val cardId: String,val topicId: String,val known: Boolean,val reviewedAt: Long,val dueAt: Long
)
@Entity(tableName="flash_presentations",indices=[Index("cardId")]) data class FlashPresentation(
    @PrimaryKey val id: String,val cardId: String,val shownAt: Long,val revealed: Boolean=false
)
@Entity(tableName="study_mix") data class MixState(@PrimaryKey val channel: String,val credit: Int=0,val percent: Int=-1,val mode: String="mixed")
data class TopicStats(val topicId: String,val answered: Int,val correct: Int,val flashReviews: Int=0,val flashUnknown: Int=0)
data class DailyStats(val day: String,val questions: Int,val questionCorrect: Int,val flashcards: Int,val flashKnown: Int)
data class FlashSummary(val studied: Int=0,val memorized: Int=0,val due: Int=0,val known: Int=0,val unknown: Int=0)
data class ImportResult(val questions: Int,val flashcards: Int,val linked: Int,val duplicates: Int) {val total get()=questions+flashcards}
data class FlashSession(val id: String,val card: Flashcard)
data class StudySession(val question: Session?=null,val flash: FlashSession?=null) {
    init {require((question!=null) xor (flash!=null))}
    val id get()=question?.id?:flash!!.id
    val kind get()=if(question!=null) "question" else "flashcard"
    val contentId get()=question?.question?.id?:flash!!.card.id
}
