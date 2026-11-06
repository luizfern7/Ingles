package br.com.desbloqueia.domain

import br.com.desbloqueia.data.*

object StudyMixer {
    data class Decision(val kind: String,val state: MixState)
    fun choose(s: Settings,stored: MixState?,channel: String,questions: Boolean,cards: Boolean): Decision? {
        val percent=when(s.studyMode) {"questions"->0;"flashcards"->100;else->s.flashPercent.coerceIn(0,100)}
        var state=stored?:MixState(channel)
        if(state.percent!=percent || state.mode!=s.studyMode) state=MixState(channel,percent=percent,mode=s.studyMode)
        if(percent==0) return if(questions) Decision("question",state) else null
        if(percent==100) return if(cards) Decision("flashcard",state) else null
        if(!questions && !cards) return null
        // If one pool has no eligible items, do not accumulate quota debt.
        if(!questions) return Decision("flashcard",state)
        if(!cards) return Decision("question",state)
        val credit=state.credit+percent
        return if(credit>=100) Decision("flashcard",state.copy(credit=credit-100))
        else Decision("question",state.copy(credit=credit))
    }
}
object SpacedReview {
    const val MINUTE=60_000L
    const val DAY=24*60*MINUTE
    private val days=listOf(1,3,7,14,30)
    fun review(previous: FlashProgress,known: Boolean,now: Long): FlashProgress {
        val streak=if(known) (previous.streak+1).coerceAtMost(5) else 0
        val interval=if(known) days[streak-1]*DAY else 10*MINUTE
        return previous.copy(correct=previous.correct+if(known)1 else 0,wrong=previous.wrong+if(known)0 else 1,
            streak=streak,lastReviewAt=now,nextReviewAt=now+interval,state=if(!known)"aprendendo" else if(streak>=3)"memorizado" else "revisando")
    }
    fun choose(cards: List<Flashcard>,progress: List<FlashProgress>,shown: List<FlashPresentation>,attempts: List<AttemptEntity>,disciplines: Set<String>,now: Long): Flashcard? {
        val p=progress.associateBy {it.cardId}
        val errors=attempts.filter {!it.correct && it.topicId!=null}.groupBy {it.topicId!!}.mapValues {it.value.maxOf {a->a.answeredAt}}
        val last=shown.maxByOrNull {it.shownAt}
        val seen=shown.groupBy {it.cardId}.mapValues {it.value.maxOf {s->s.shownAt}}
        fun boosted(f: Flashcard)=(errors[f.assunto_id]?:0L)>(p[f.id]?.lastReviewAt?:0L)
        var candidates=cards.filter {(disciplines.isEmpty() || it.disciplina in disciplines) && ((p[it.id]?.nextReviewAt?:0L)<=now || boosted(it))}
        if(last!=null) {
            val other=candidates.filter {it.id!=last.cardId}
            candidates=if(other.isNotEmpty()) other else candidates.filter {it.id!=last.cardId || now-last.shownAt>=MINUTE}
        }
        return candidates.minWithOrNull(compareBy<Flashcard> {if(boosted(it))0 else if(p[it.id]?.state=="aprendendo")1 else if(it.id in p)2 else 3}.thenBy {p[it.id]?.nextReviewAt?:0L}.thenBy {seen[it.id]?:0L}.thenBy {it.id})
    }
}

data class FlashcardDraft(val sourceQuestion: Question,val topic: String,val front: String,val back: String,val explanation: String)
interface FlashcardSuggestionProvider { fun suggest(question: Question): FlashcardDraft? }
/** Local extraction only: preserve the whole prompt/options, never invent a new answer. */
class ExplanationSuggestionProvider: FlashcardSuggestionProvider {
    override fun suggest(question: Question): FlashcardDraft? {
        val answer=question.alternativas[question.resposta_correta]?:return null
        if(question.explicacao.isBlank()) return null
        val front=question.enunciado+"\n\n"+question.alternativas.toSortedMap().entries.joinToString("\n") {"${it.key}) ${it.value}"}
        return FlashcardDraft(question,question.assunto_id.orEmpty(),front,answer,question.explicacao)
    }
}
