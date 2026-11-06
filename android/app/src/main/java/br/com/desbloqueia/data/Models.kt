package br.com.desbloqueia.data

import androidx.room.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Source(
    val concurso: String="", val banca: String="", val cargo: String="", val ano: Int=0,
    val arquivo: String="", val sha256: String="", val pagina: Int=0, val questao: Int=0,
    val pagina_gabarito: Int=0, val versao: String="",
    val tipo: String="oficial", val descricao: String=""
)
@Serializable data class Question(
    val id: String, val disciplina: String, val assunto: String, val enunciado: String,
    val alternativas: Map<String,String>, val resposta_correta: String,
    val explicacao: String, val explicacoes_alternativas: Map<String,String> = emptyMap(),
    val fonte: Source, val status_validacao: String,
    val autoria_explicacao: String = "Explicação fornecida pelo autor do pacote.",
    val dica: String? = null,
    val assunto_id: String? = null, val subassunto: String? = null
)
@Serializable data class QuestionPack(val schema_version: Int, val questions: List<Question>)
@Entity(tableName="questions",indices=[Index("topicId")]) data class QuestionEntity(@PrimaryKey val id: String, val discipline: String, val payload: String, val topicId: String?=null)
@Entity(tableName="attempts", indices=[Index("questionId"),Index("topicId")]) data class AttemptEntity(
    @PrimaryKey val presentationId: String, val questionId: String, val discipline: String,
    val selected: String, val correct: Boolean, val answeredAt: Long, val topicId: String?=null
)
@Entity(tableName="presentations", indices=[Index("questionId")]) data class PresentationEntity(
    @PrimaryKey val id: String, val questionId: String, val shownAt: Long
)
@Entity(tableName="unlock_state") data class UnlockState(
    @PrimaryKey val singleton: Int = 1, val cycle: Long = 0,
    val handledCycle: Long = 0, val lastPrompt: Long = 0,
    val pendingId: String? = null, val pendingQuestion: String? = null,
    @ColumnInfo(defaultValue="'question'") val pendingKind: String="question"
)
data class Stats(val discipline: String, val answered: Int, val correct: Int)
@Dao interface StudyDao {
    @Query("SELECT (SELECT COUNT(*) FROM questions)+(SELECT COUNT(*) FROM flashcards)") fun observeContentCount(): kotlinx.coroutines.flow.Flow<Int>
    @Query("SELECT discipline FROM questions UNION SELECT discipline FROM flashcards ORDER BY discipline") fun observeAllDisciplines(): kotlinx.coroutines.flow.Flow<List<String>>
    @Update suspend fun updateQuestion(q: QuestionEntity)
    @Query("UPDATE attempts SET topicId=:topic WHERE questionId=:question AND topicId IS NULL") suspend fun classifyAttempts(question: String, topic: String)
    @Query("SELECT MAX(answeredAt) FROM attempts WHERE topicId=:topic AND correct=0") suspend fun lastTopicError(topic: String): Long?
    @Query("UPDATE flash_progress SET nextReviewAt=MIN(nextReviewAt,:now) WHERE cardId IN (SELECT id FROM flashcards WHERE topicId=:topic) AND lastReviewAt<:now") suspend fun boostTopic(topic: String,now: Long)
    @Query("SELECT * FROM flashcards ORDER BY id") suspend fun flashcards(): List<FlashcardEntity>
    @Query("SELECT * FROM flashcards WHERE id=:id") suspend fun flashcard(id: String): FlashcardEntity?
    @Query("SELECT COUNT(*) FROM flashcards") fun observeFlashcardCount(): kotlinx.coroutines.flow.Flow<Int>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertFlashcards(cards: List<FlashcardEntity>): List<Long>
    @Query("SELECT * FROM flash_progress") suspend fun flashProgress(): List<FlashProgress>
    @Query("SELECT * FROM flash_progress") fun observeFlashProgress(): kotlinx.coroutines.flow.Flow<List<FlashProgress>>
    @Query("SELECT * FROM flash_progress WHERE cardId=:id") suspend fun progress(id: String): FlashProgress?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveProgress(p: FlashProgress)
    @Query("SELECT * FROM flash_presentations") suspend fun flashPresentations(): List<FlashPresentation>
    @Query("SELECT * FROM flash_presentations WHERE id=:id") suspend fun flashPresentation(id: String): FlashPresentation?
    @Insert suspend fun presentFlash(p: FlashPresentation)
    @Query("UPDATE flash_presentations SET revealed=1 WHERE id=:id") suspend fun revealFlash(id: String)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertReview(r: FlashReview): Long
    @Query("SELECT * FROM flash_reviews WHERE presentationId=:id") suspend fun flashReview(id: String): FlashReview?
    @Query("SELECT * FROM flash_reviews") suspend fun flashReviews(): List<FlashReview>
    @Query("SELECT * FROM flash_reviews") fun observeFlashReviews(): kotlinx.coroutines.flow.Flow<List<FlashReview>>
    @Query("SELECT * FROM study_mix WHERE channel=:channel") suspend fun mix(channel: String): MixState?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveMix(state: MixState)
    @Query("SELECT topicId,SUM(q) AS answered,SUM(qc) AS correct,SUM(f) AS flashReviews,SUM(fw) AS flashUnknown FROM (SELECT topicId,1 AS q,CASE WHEN correct THEN 1 ELSE 0 END AS qc,0 AS f,0 AS fw FROM attempts WHERE topicId IS NOT NULL UNION ALL SELECT topicId,0 AS q,0 AS qc,1 AS f,CASE WHEN known THEN 0 ELSE 1 END AS fw FROM flash_reviews) GROUP BY topicId ORDER BY (SUM(q)-SUM(qc)+SUM(fw)) DESC") fun topicStats(): kotlinx.coroutines.flow.Flow<List<TopicStats>>
    @Query("SELECT strftime('%Y-%m-%d', moment/1000,'unixepoch','localtime') AS day,SUM(q) AS questions,SUM(qc) AS questionCorrect,SUM(f) AS flashcards,SUM(fc) AS flashKnown FROM (SELECT answeredAt AS moment,1 AS q,CASE WHEN correct THEN 1 ELSE 0 END AS qc,0 AS f,0 AS fc FROM attempts UNION ALL SELECT reviewedAt AS moment,0 AS q,0 AS qc,1 AS f,CASE WHEN known THEN 1 ELSE 0 END AS fc FROM flash_reviews) GROUP BY day ORDER BY day DESC LIMIT 7") fun dailyStats(): kotlinx.coroutines.flow.Flow<List<DailyStats>>
    @Query("SELECT COUNT(*) FROM questions") suspend fun countQuestions(): Int
    @Query("SELECT COUNT(*) FROM questions") fun observeQuestionCount(): kotlinx.coroutines.flow.Flow<Int>
    @Query("SELECT DISTINCT discipline FROM questions ORDER BY discipline") fun observeDisciplines(): kotlinx.coroutines.flow.Flow<List<String>>
    @Query("SELECT * FROM questions ORDER BY id") suspend fun questions(): List<QuestionEntity>
    @Query("SELECT * FROM questions WHERE id=:id") suspend fun question(id: String): QuestionEntity?
    @Query("SELECT * FROM attempts WHERE presentationId=:id") suspend fun attempt(id: String): AttemptEntity?
    @Query("SELECT * FROM attempts") suspend fun attempts(): List<AttemptEntity>
    @Query("SELECT MAX(moment) FROM (SELECT answeredAt AS moment FROM attempts UNION ALL SELECT reviewedAt AS moment FROM flash_reviews)") suspend fun lastLearningAt(): Long?
    @Query("SELECT MAX(shownAt) FROM presentations") suspend fun lastQuestionShown(): Long?
    @Query("SELECT MAX(shownAt) FROM flash_presentations") suspend fun lastFlashShown(): Long?
    @Query("SELECT MAX(answeredAt) FROM attempts") suspend fun lastAnswerAt(): Long?
    @Query("SELECT * FROM presentations") suspend fun presentations(): List<PresentationEntity>
    @Query("SELECT discipline, COUNT(*) AS answered, SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct FROM attempts GROUP BY discipline") fun stats(): kotlinx.coroutines.flow.Flow<List<Stats>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertQuestions(q: List<QuestionEntity>): List<Long>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun answer(a: AttemptEntity): Long
    @Insert suspend fun present(p: PresentationEntity)
    @Query("SELECT * FROM unlock_state WHERE singleton=1") suspend fun unlock(): UnlockState?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveUnlock(s: UnlockState)
    @Query("SELECT COUNT(*) FROM (SELECT questionId FROM attempts GROUP BY questionId HAVING MAX(CASE WHEN correct THEN answeredAt ELSE 0 END) < MAX(CASE WHEN NOT correct THEN answeredAt ELSE 0 END))") fun reviewCount(): kotlinx.coroutines.flow.Flow<Int>
}
@Database(entities=[QuestionEntity::class,AttemptEntity::class,PresentationEntity::class,UnlockState::class,FlashcardEntity::class,FlashProgress::class,FlashReview::class,FlashPresentation::class,MixState::class],version=2,exportSchema=false)
abstract class StudyDatabase: RoomDatabase() { abstract fun dao(): StudyDao }
