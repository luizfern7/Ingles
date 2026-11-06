package br.com.desbloqueia.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import br.com.desbloqueia.StudyApplication
import br.com.desbloqueia.data.*
import br.com.desbloqueia.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class QuestionUi(
    val session: Session?=null,val selected: String?=null,val busy: Boolean=false,val error: String?=null,
    val flash: FlashSession?=null,val flipped: Boolean=false,val flashRating: Boolean?=null,val nextDue: Long?=null
) {
    val itemId get()=session?.id?:flash?.id
    companion object {fun from(s: StudySession?)=QuestionUi(session=s?.question,flash=s?.flash)}
}
class StudyViewModel(application: Application,private val saved: SavedStateHandle): AndroidViewModel(application) {
    private val app=application as StudyApplication
    private val repo=app.repository
    val settings=app.preferences.settings.stateIn(viewModelScope,SharingStarted.Eagerly,Settings())
    val stats=repo.dao.stats().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val reviews=repo.dao.reviewCount().stateIn(viewModelScope,SharingStarted.Eagerly,0)
    val disciplines=repo.dao.observeAllDisciplines().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val questionCount=repo.dao.observeQuestionCount().stateIn(viewModelScope,SharingStarted.Eagerly,0)
    val contentCount=repo.dao.observeContentCount().stateIn(viewModelScope,SharingStarted.Eagerly,0)
    val flashCount=repo.dao.observeFlashcardCount().stateIn(viewModelScope,SharingStarted.Eagerly,0)
    val topics=repo.dao.topicStats().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val daily=repo.dao.dailyStats().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    private val now=flow {while(true) {emit(System.currentTimeMillis());delay(60_000)}}
    val flashSummary=combine(repo.dao.observeFlashProgress(),repo.dao.observeFlashcardCount(),now) {p,total,time ->
        FlashSummary(p.size,p.count {it.state=="memorizado"},total-p.size+p.count {it.nextReviewAt<=time},p.sumOf {it.correct},p.sumOf {it.wrong})
    }.stateIn(viewModelScope,SharingStarted.Eagerly,FlashSummary())
    val ui=MutableStateFlow(QuestionUi())
    val message=MutableStateFlow<String?>(null)
    val feedback=MutableSharedFlow<Boolean>(extraBufferCapacity=1)
    val draft=MutableStateFlow<FlashcardDraft?>(null)
    val ready=MutableStateFlow(false)
    private val initialized=viewModelScope.async(Dispatchers.IO) {repo.initialize()}
    init {viewModelScope.launch {
        try {
            initialized.await()
            val q=saved.get<String>("question");val f=saved.get<String>("flash")
            if(q!=null) ui.value=QuestionUi(Session(saved["presentation"]?:UUID.randomUUID().toString(),PackCodec.question(q)),saved["selected"])
            else if(f!=null) {
                val id=saved.get<String>("presentation")?:UUID.randomUUID().toString()
                val presentation=repo.dao.flashPresentation(id);val review=repo.dao.flashReview(id)
                if(presentation!=null) ui.value=QuestionUi(flash=FlashSession(id,PackCodec.card(f)),flipped=saved.get<Boolean>("flashFaceBack")?:presentation.revealed,flashRating=review?.known,nextDue=review?.dueAt)
            }
            ready.value=true
        } catch(e: Exception) {if(e is CancellationException)throw e;message.value="Falha ao carregar banco: ${e.message}"}
    }}
    private fun persist() {
        saved["question"]=ui.value.session?.question?.let {PackCodec.encode(it)}
        saved["flash"]=ui.value.flash?.card?.let {PackCodec.encodeCard(it)}
        saved["presentation"]=ui.value.itemId;saved["selected"]=ui.value.selected;saved["flashFaceBack"]=ui.value.flipped
    }
    fun next()=load(false)
    fun reviewFlashcards()=load(true)
    private fun load(review: Boolean) {
        if(ui.value.busy)return
        ui.value=ui.value.copy(busy=true,error=null)
        viewModelScope.launch {try {
            initialized.await()
            val s=withContext(Dispatchers.IO) {repo.nextStudy(app.preferences.settings.first(),reviewOnly=review)}
            ui.value=QuestionUi.from(s).copy(error=if(s==null)"Nenhum item elegível agora. Confira o método, disciplinas e datas de revisão." else null);persist()
        } catch(e: Exception) {if(e is CancellationException)throw e;ui.value=ui.value.copy(busy=false,error="Não foi possível carregar: ${e.message}")}}
    }
    fun notification(id: String) {viewModelScope.launch {try {
        initialized.await()
        val s=withContext(Dispatchers.IO) {repo.pendingStudy(id)}
        if(s!=null) {ui.value=QuestionUi.from(s);ui.value.flash?.let {ui.value=ui.value.copy(flipped=repo.dao.flashPresentation(it.id)?.revealed?:false)};persist()}
        else if(ui.value.itemId==null)next()
    } catch(e: Exception) {if(e is CancellationException)throw e;message.value="Não foi possível abrir o lembrete."}}}
    fun answer(letter: String) {
        val state=ui.value;val session=state.session?:return
        if(state.busy || state.selected!=null)return
        ui.value=state.copy(busy=true)
        viewModelScope.launch {try {
            val result=withContext(Dispatchers.IO) {val added=repo.answer(session,letter);repo.dao.attempt(session.id)!! to added}
            ui.value=state.copy(selected=result.first.selected);persist();if(result.second)feedback.emit(result.first.correct)
        } catch(e: Exception) {if(e is CancellationException)throw e;ui.value=state.copy(error="Não foi possível salvar. Tente novamente.")}}
    }
    fun flip() {
        val state=ui.value;val session=state.flash?:return
        if(state.busy)return
        if(state.flipped) {ui.value=state.copy(flipped=false);persist();return}
        ui.value=state.copy(busy=true)
        viewModelScope.launch {try {
            val revealed=withContext(Dispatchers.IO) {repo.reveal(session)}
            ui.value=state.copy(flipped=revealed);persist()
        } catch(e: Exception) {if(e is CancellationException)throw e;ui.value=state.copy(error="Não foi possível virar o cartão.")}}
    }
    fun rate(known: Boolean) {
        val state=ui.value;val session=state.flash?:return
        if(state.busy || !state.flipped || state.flashRating!=null)return
        ui.value=state.copy(busy=true)
        viewModelScope.launch {try {
            val result=withContext(Dispatchers.IO) {repo.rate(session,known)}
            val review=repo.dao.flashReview(session.id)
            ui.value=state.copy(flashRating=review?.known,nextDue=review?.dueAt?:result.first.nextReviewAt);persist()
            if(result.second)feedback.emit(known)
        } catch(e: Exception) {if(e is CancellationException)throw e;ui.value=state.copy(error="Não foi possível salvar a revisão.")}}
    }
    fun suggest(q: Question) {draft.value=ExplanationSuggestionProvider().suggest(q)}
    fun saveDraft(topic: String,front: String,back: String,explanation: String,reviewed: Boolean) {
        val d=draft.value?:return
        viewModelScope.launch {try {
            val result=withContext(Dispatchers.IO) {repo.saveDraft(d,topic,front,back,explanation,reviewed)}
            draft.value=null;message.value=if(result.flashcards==1)"Flashcard salvo. Conteúdo revisado: $reviewed." else "Esse flashcard já existe; nenhum progresso foi alterado."
        } catch(e: Exception) {if(e is CancellationException)throw e;message.value="Não foi possível salvar: ${e.message}"}}
    }
    fun update(transform: (Settings)->Settings) {viewModelScope.launch {try {app.preferences.modify(transform)}
        catch(e: Exception) {if(e is CancellationException)throw e;message.value="Não foi possível salvar as configurações."}}}
    fun import(uri: Uri) {viewModelScope.launch {try {
        initialized.await()
        val result=withContext(Dispatchers.IO) {
            val bytes=app.contentResolver.openInputStream(uri)?.use {input ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
                while(true) {val n=input.read(buffer);if(n<0)break;total+=n;require(total<=PackCodec.MAX_BYTES) {"Pacote excede 5 MB"};out.write(buffer,0,n)}
                out.toByteArray()
            }?:error("Arquivo indisponível")
            repo.importStudyPack(bytes.toString(Charsets.UTF_8))
        }
        message.value="${result.questions} questões e ${result.flashcards} flashcards adicionados; ${result.linked} assuntos classificados; ${result.duplicates} duplicados preservados."
    } catch(e: Exception) {if(e is CancellationException)throw e;message.value="Importação recusada: ${e.message}"}}}
    fun clearQuestion() {ui.value=QuestionUi();persist()}
}
