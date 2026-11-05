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
    val explicacao: String, val explicacoes_alternativas: Map<String,String>,
    val fonte: Source, val status_validacao: String,
    val autoria_explicacao: String = "Explicação fornecida pelo autor do pacote.",
    val dica: String? = null
)
@Serializable data class QuestionPack(val schema_version: Int, val questions: List<Question>)
object PackCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    val json = Json { ignoreUnknownKeys = false }
    fun decode(text: String): List<Question> {
        require(text.toByteArray().size <= MAX_BYTES) { "Pacote excede 5 MB" }
        val pack = json.decodeFromString<QuestionPack>(text)
        require(pack.schema_version == 1) { "Versão de pacote não suportada" }
        require(pack.questions.isNotEmpty() && pack.questions.size <= 5000) { "Pacote vazio ou excessivo" }
        require(pack.questions.map { it.id }.distinct().size == pack.questions.size) { "IDs duplicados" }
        pack.questions.forEach { q ->
            require(q.id.isNotBlank() && q.id.length <= 160)
            require(listOf(q.disciplina,q.assunto,q.enunciado,q.explicacao,q.autoria_explicacao).all { it.isNotBlank() }) { "Conteúdo incompleto: ${q.id}" }
            require(q.alternativas.size in 2..5 && q.alternativas.keys.all { it in listOf("A","B","C","D","E") })
            require(q.resposta_correta in q.alternativas)
            require(q.alternativas.values.all { it.isNotBlank() })
            require(q.explicacoes_alternativas.keys == q.alternativas.keys && q.explicacoes_alternativas.values.all { it.isNotBlank() }) { "Justificativas incompletas" }
            when(q.fonte.tipo) {
                "autoral_ia" -> {
                    require(q.fonte.descricao.isNotBlank()) {"Descreva a origem autoral por IA: ${q.id}"}
                    require(q.status_validacao in setOf("revisao_pendente","revisado")) {"Status autoral deve ser revisao_pendente ou revisado: ${q.id}"}
                }
                "oficial" -> {
                    require(q.status_validacao == "gabarito_conferido") { "Questão sem gabarito conferido: ${q.id}" }
                    require(listOf(q.fonte.concurso,q.fonte.banca,q.fonte.cargo,q.fonte.arquivo,q.fonte.versao).all {it.isNotBlank()} && q.fonte.ano>0) {"Fonte oficial incompleta: ${q.id}"}
                    require(q.fonte.pagina > 0 && q.fonte.pagina_gabarito > 0 && q.fonte.questao > 0)
                    require(q.fonte.sha256.matches(Regex("[0-9a-f]{64}")))
                }
                else -> throw IllegalArgumentException("Tipo de fonte desconhecido: ${q.fonte.tipo}")
            }
        }
        return pack.questions
    }
    fun encode(q: Question) = json.encodeToString(Question.serializer(),q)
    fun question(text: String) = json.decodeFromString<Question>(text)
}
@Entity(tableName="questions") data class QuestionEntity(@PrimaryKey val id: String, val discipline: String, val payload: String)
@Entity(tableName="attempts", indices=[Index("questionId")]) data class AttemptEntity(
    @PrimaryKey val presentationId: String, val questionId: String, val discipline: String,
    val selected: String, val correct: Boolean, val answeredAt: Long
)
@Entity(tableName="presentations", indices=[Index("questionId")]) data class PresentationEntity(
    @PrimaryKey val id: String, val questionId: String, val shownAt: Long
)
@Entity(tableName="unlock_state") data class UnlockState(
    @PrimaryKey val singleton: Int = 1, val cycle: Long = 0,
    val handledCycle: Long = 0, val lastPrompt: Long = 0,
    val pendingId: String? = null, val pendingQuestion: String? = null
)
data class Stats(val discipline: String, val answered: Int, val correct: Int)
@Dao interface StudyDao {
    @Query("SELECT COUNT(*) FROM questions") suspend fun countQuestions(): Int
    @Query("SELECT COUNT(*) FROM questions") fun observeQuestionCount(): kotlinx.coroutines.flow.Flow<Int>
    @Query("SELECT DISTINCT discipline FROM questions ORDER BY discipline") fun observeDisciplines(): kotlinx.coroutines.flow.Flow<List<String>>
    @Query("SELECT * FROM questions ORDER BY id") suspend fun questions(): List<QuestionEntity>
    @Query("SELECT * FROM questions WHERE id=:id") suspend fun question(id: String): QuestionEntity?
    @Query("SELECT * FROM attempts WHERE presentationId=:id") suspend fun attempt(id: String): AttemptEntity?
    @Query("SELECT * FROM attempts") suspend fun attempts(): List<AttemptEntity>
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
@Database(entities=[QuestionEntity::class,AttemptEntity::class,PresentationEntity::class,UnlockState::class],version=1,exportSchema=false)
abstract class StudyDatabase: RoomDatabase() { abstract fun dao(): StudyDao }
