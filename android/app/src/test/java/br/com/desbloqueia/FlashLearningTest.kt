package br.com.desbloqueia

import br.com.desbloqueia.data.*
import br.com.desbloqueia.domain.*
import org.junit.Test
import org.junit.Assert.*

class FlashLearningTest {
    @Test fun everyPercentageHasExactLongRunDistribution() {
        for(percent in 0..100 step 10) {
            var state: MixState?=null;var flash=0
            repeat(100) {
                val d=StudyMixer.choose(Settings(flashPercent=percent),state,"unlock",true,true)!!
                if(d.kind=="flashcard")flash++;state=d.state
            }
            assertEquals("percent=$percent",percent,flash)
        }
    }
    @Test fun defaultIsStrictAlternationAndStateResumes() {
        var state: MixState?=null
        val sequence=(1..8).map {val d=StudyMixer.choose(Settings(),state,"unlock",true,true)!!;state=d.state;d.kind}
        assertEquals(listOf("question","flashcard","question","flashcard","question","flashcard","question","flashcard"),sequence)
        val first=StudyMixer.choose(Settings(),null,"unlock",true,true)!!
        assertEquals("flashcard",StudyMixer.choose(Settings(),first.state.copy(),"unlock",true,true)!!.kind)
    }
    @Test fun modesAndMissingPoolsAreHandledWithoutQuotaDebt() {
        assertNull(StudyMixer.choose(Settings(studyMode="questions"),null,"unlock",false,true))
        assertNull(StudyMixer.choose(Settings(studyMode="flashcards"),null,"unlock",true,false))
        assertNull(StudyMixer.choose(Settings(),null,"unlock",false,false))
        val q=StudyMixer.choose(Settings(),null,"unlock",true,false)!!
        assertEquals("question",q.kind);assertEquals(0,q.state.credit)
        assertEquals("flashcard",StudyMixer.choose(Settings(),null,"unlock",false,true)!!.kind)
    }
    @Test fun deterministicSpacingIncreasesAndFailureResets() {
        var p=FlashProgress("f")
        for(days in listOf(1,3,7,14,30,30)) {
            val now=maxOf(1000,p.nextReviewAt)
            p=SpacedReview.review(p,true,now)
            assertEquals(days*SpacedReview.DAY,p.nextReviewAt-now)
        }
        assertEquals("memorizado",p.state)
        p=SpacedReview.review(p,false,p.nextReviewAt)
        assertEquals(10*SpacedReview.MINUTE,p.nextReviewAt-p.lastReviewAt)
        assertEquals(0,p.streak);assertEquals("aprendendo",p.state);assertEquals(6,p.correct);assertEquals(1,p.wrong)
    }
    @Test fun topicErrorBoostsOnlyRelatedCardsAndAvoidsRepeats() {
        val a=Flashcard("a","Banco","sfn_cmn","Frente A","Verso A")
        val b=Flashcard("b","Banco","outro","Frente B","Verso B")
        val error=AttemptEntity("q","q","Banco","A",false,1000,"sfn_cmn")
        val p=listOf(FlashProgress("a",lastReviewAt=100,nextReviewAt=99999))
        assertEquals("a",SpacedReview.choose(listOf(b,a),p,emptyList(),listOf(error),emptySet(),2000)!!.id)
        assertEquals("b",SpacedReview.choose(listOf(b,a),p,listOf(FlashPresentation("s","a",1999)),listOf(error),emptySet(),2000)!!.id)
    }
    @Test fun unknownCardReturnsBeforeNewWhenDueAndFutureCardsWait() {
        val a=Flashcard("a","Banco","t","A","B");val b=a.copy(id="b",frente="C")
        val p=listOf(FlashProgress("a",wrong=1,nextReviewAt=1000,state="aprendendo"))
        assertEquals("a",SpacedReview.choose(listOf(b,a),p,emptyList(),emptyList(),emptySet(),2000)!!.id)
        assertNull(SpacedReview.choose(listOf(a),p,emptyList(),emptyList(),emptySet(),500))
        assertNull(SpacedReview.choose(listOf(a),emptyList(),listOf(FlashPresentation("s","a",1000)),emptyList(),emptySet(),1001))
    }
    @Test fun compactAndIndependentPacksHaveNormalizedStableLinks() {
        val pack=PackCodec.decodeStudy("""{"schema_version":2,"questions":[{"id":"q101","disciplina":"Banco","assunto_id":" SFN-CMN ","enunciado":"Qual órgão?","alternativas":["BC","CMN","CVM","BB"],"resposta_correta":1,"explicacao":"O CMN define diretrizes."}],"flashcards":[{"id":"f201","disciplina":"Banco","assunto_id":"sfn_cmn","frente":"Qual função?","verso":"Definir diretrizes."}]}""")
        assertEquals("B",pack.questions.single().resposta_correta)
        assertEquals("sfn_cmn",pack.questions.single().assunto_id)
        assertEquals(pack.questions.single().assunto_id,pack.flashcards.single().assunto_id)
        assertEquals("autoral",pack.questions.single().fonte.tipo)
        assertTrue(pack.questions.single().explicacoes_alternativas.isEmpty())
        val only=PackCodec.decodeStudy("""{"schema_version":2,"flashcards":[{"id":"f","disciplina":"Banco","assunto_id":"sfn_cmn","frente":"Função?","verso":"Diretrizes."}]}""")
        assertTrue(only.questions.isEmpty());assertEquals(1,only.flashcards.size)
    }
    @Test fun invalidTopicsIndexesAndDuplicateCardsAreRejected() {
        try {PackCodec.topic("sfn.cmn");fail()} catch(_: IllegalArgumentException) {}
        val f=Flashcard("f","Banco","t","Frente","Verso")
        val duplicate=StudyPack(flashcards=listOf(f,f.copy(id="other")))
        try {PackCodec.decodeStudy(PackCodec.json.encodeToString(StudyPack.serializer(),duplicate));fail()} catch(_: IllegalArgumentException) {}
        val invalid="""{"schema_version":2,"questions":[{"id":"q","disciplina":"Banco","assunto_id":"t","enunciado":"Qual?","alternativas":["A","B"],"resposta_correta":2,"explicacao":"Motivo."}]}"""
        try {PackCodec.decodeStudy(invalid);fail()} catch(_: IllegalArgumentException) {}
    }
    @Test fun seenAndUnseenUnratedCardsUseSameTimestampType() {
        val a=Flashcard("a","Banco","t","A","Verso A")
        val b=a.copy(id="b",frente="B");val c=a.copy(id="c",frente="C")
        val shown=listOf(FlashPresentation("p1","a",1000),FlashPresentation("p2","c",2000))
        assertEquals("b",SpacedReview.choose(listOf(a,b,c),emptyList(),shown,emptyList(),emptySet(),3000)!!.id)
    }
    @Test fun boostedNewAndReviewedCardsUseSameDueTimestampType() {
        val a=Flashcard("a","Banco","t","A","Verso A")
        val b=a.copy(id="b",frente="B");val c=a.copy(id="c",assunto_id="other",frente="C")
        val progress=listOf(FlashProgress("a",lastReviewAt=100,nextReviewAt=1000))
        val shown=listOf(FlashPresentation("p","c",2000))
        val error=AttemptEntity("err","q","Banco","A",false,2000,"t")
        assertEquals("b",SpacedReview.choose(listOf(a,b,c),progress,shown,listOf(error),emptySet(),3000)!!.id)
    }

}
