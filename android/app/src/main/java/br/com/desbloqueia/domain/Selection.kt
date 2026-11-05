package br.com.desbloqueia.domain

import br.com.desbloqueia.data.*
object Selection {
    fun choose(questions: List<Question>, attempts: List<AttemptEntity>, shown: List<PresentationEntity>, disciplines: Set<String>, now: Long): Question? {
        val latest=attempts.groupBy { it.questionId }.mapValues { it.value.maxBy { a->a.answeredAt } }
        val seen=shown.groupBy { it.questionId }.mapValues { it.value.maxOf { a->a.shownAt } }
        val eligible=questions.filter { disciplines.isEmpty() || it.disciplina in disciplines }
        // Evita repetição imediata sempre que outra questão elegível existir.
        val last=shown.maxByOrNull { it.shownAt }?.questionId
        val candidates=eligible.filter { it.id!=last }.ifEmpty { eligible }
        return candidates.minWithOrNull(compareBy<Question> {
            val a=latest[it.id]
            when { a==null && it.id !in seen -> 0; a!=null && !a.correct && now-a.answeredAt>=15*60_000 -> 1; a==null -> 2; !a.correct -> 3; else -> 4 }
        }.thenBy { seen[it.id]?:0L }.thenBy { it.id })
    }
    fun due(state: UnlockState, settings: Settings, now: Long): Boolean =
        settings.unlock && now>=settings.pausedUntil && state.cycle>state.handledCycle &&
        (state.lastPrompt==0L || now-state.lastPrompt>=settings.frequency*60_000L)
}
