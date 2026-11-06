package br.com.desbloqueia.data

import java.security.MessageDigest
import java.util.Locale
import kotlinx.serialization.json.*

object PackCodec {
    const val MAX_BYTES=5*1024*1024
    val json=Json {ignoreUnknownKeys=false}
    fun topic(value: String?): String? {
        if(value.isNullOrBlank()) return null
        val normalized=value.trim().lowercase(Locale.ROOT).replace(Regex("[\\s-]+"),"_").replace(Regex("_+"),"_")
        require(normalized.length<=120 && normalized.matches(Regex("[a-z0-9]+(?:_[a-z0-9]+)*"))) {"assunto_id inválido: use letras a-z, números e underscore"}
        return normalized
    }
    fun decode(text: String)=decodeStudy(text).questions
    fun decodeStudy(text: String): StudyPack {
        require(text.toByteArray(Charsets.UTF_8).size<=MAX_BYTES) {"Pacote excede 5 MB"}
        val root=json.parseToJsonElement(text).jsonObject
        require(root.keys.all {it in setOf("schema_version","questions","flashcards")}) {"Campo desconhecido no pacote"}
        val version=root["schema_version"]?.jsonPrimitive?.intOrNull
        require(version in setOf(1,2)) {"Versão de pacote não suportada"}
        val questions=(root["questions"]?.jsonArray?:JsonArray(emptyList())).map { element ->
            val raw=element.jsonObject
            val compact=raw["alternativas"] is JsonArray
            val obj=raw.toMutableMap()
            if(compact) {
                val choices=raw.getValue("alternativas").jsonArray
                require(choices.size in 2..5 && choices.all {it is JsonPrimitive && it.isString && it.content.isNotBlank()})
                obj["alternativas"]=JsonObject(choices.mapIndexed {i,v -> ('A'+i).toString() to v}.toMap())
                val key=raw["resposta_correta"]?.jsonPrimitive?:throw IllegalArgumentException("Resposta ausente")
                if(!key.isString) {
                    val index=key.intOrNull;require(index!=null && index in choices.indices) {"Índice da resposta inválido (base zero)"}
                    obj["resposta_correta"]=JsonPrimitive(('A'+index).toString())
                }
                if("fonte" !in obj) obj["fonte"]=json.encodeToJsonElement(Source.serializer(),Source(tipo="autoral",descricao="Conteúdo importado pelo usuário; origem não informada."))
                if("status_validacao" !in obj) obj["status_validacao"]=JsonPrimitive("revisao_pendente")
            }
            val id=topic(raw["assunto_id"]?.let {if(it is JsonNull) null else it.jsonPrimitive.content})
            if("assunto" !in obj && id!=null) obj["assunto"]=JsonPrimitive(id)
            val q=json.decodeFromJsonElement(Question.serializer(),JsonObject(obj)).copy(assunto_id=id)
            validateQuestion(q,compact && "explicacoes_alternativas" !in raw)
            q
        }
        val cards=(root["flashcards"]?.jsonArray?:JsonArray(emptyList())).map {
            val f=json.decodeFromJsonElement(Flashcard.serializer(),it)
            val normalized=f.copy(assunto_id=topic(f.assunto_id)?:throw IllegalArgumentException("Flashcard precisa de assunto_id"))
            require(normalized.id.isNotBlank() && normalized.id.length<=160)
            require(listOf(normalized.disciplina,normalized.frente,normalized.verso).all {s->s.isNotBlank()}) {"Flashcard incompleto: ${f.id}"}
            require(f.status_validacao in setOf("revisao_pendente","revisado","gabarito_conferido")) {"Status do flashcard inválido"}
            if(f.fonte!=null) validateSource(f.fonte,f.status_validacao,f.id)
            else require(f.status_validacao!="gabarito_conferido") {"Fonte oficial ausente"}
            normalized
        }
        require(questions.size+cards.size in 1..5000) {"Pacote vazio ou excessivo"}
        require(questions.map {it.id}.distinct().size==questions.size && cards.map {it.id}.distinct().size==cards.size) {"IDs duplicados no pacote"}
        require(cards.map {fingerprint(it)}.distinct().size==cards.size) {"Conteúdo de flashcard duplicado"}
        return StudyPack(version!!,questions,cards)
    }
    private fun validateQuestion(q: Question,missingAnalysis: Boolean) {
        require(q.id.isNotBlank() && q.id.length<=160)
        require(listOf(q.disciplina,q.assunto,q.enunciado,q.explicacao,q.autoria_explicacao).all {it.isNotBlank()}) {"Conteúdo incompleto: ${q.id}"}
        require(q.alternativas.size in 2..5 && q.alternativas.keys.all {it in listOf("A","B","C","D","E")} && q.alternativas.values.all {it.isNotBlank()})
        require(q.resposta_correta in q.alternativas)
        require((missingAnalysis && q.explicacoes_alternativas.isEmpty()) || (q.explicacoes_alternativas.keys==q.alternativas.keys && q.explicacoes_alternativas.values.all {it.isNotBlank()})) {"Justificativas incompletas"}
        validateSource(q.fonte,q.status_validacao,q.id)
    }
    private fun validateSource(source: Source,status: String,id: String) {
        when(source.tipo) {
            "autoral_ia","autoral" -> {
                require(source.descricao.isNotBlank()) {"Descreva a origem autoral: $id"}
                require(status in setOf("revisao_pendente","revisado")) {"Status autoral deve ser revisao_pendente ou revisado: $id"}
            }
            "oficial" -> {
                require(status=="gabarito_conferido") {"Questão sem gabarito conferido: $id"}
                require(listOf(source.concurso,source.banca,source.cargo,source.arquivo,source.versao).all {it.isNotBlank()} && source.ano>0) {"Fonte oficial incompleta"}
                require(source.pagina>0 && source.pagina_gabarito>0 && source.questao>0)
                require(source.sha256.matches(Regex("[0-9a-f]{64}")))
            }
            else -> throw IllegalArgumentException("Tipo de fonte desconhecido: ${source.tipo}")
        }
    }
    fun encode(q: Question)=json.encodeToString(Question.serializer(),q)
    fun question(text: String)=json.decodeFromString<Question>(text)
    fun encodeCard(f: Flashcard)=json.encodeToString(Flashcard.serializer(),f)
    fun card(text: String)=json.decodeFromString<Flashcard>(text)
    fun fingerprint(f: Flashcard): String {
        fun normalized(s: String)=s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+")," ")
        val bytes=listOf(f.assunto_id,normalized(f.frente),normalized(f.verso)).joinToString("\u0000").toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {"%02x".format(it)}
    }
}
