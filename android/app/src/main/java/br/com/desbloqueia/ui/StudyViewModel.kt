package br.com.desbloqueia.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import br.com.desbloqueia.StudyApplication
import br.com.desbloqueia.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class QuestionUi(val session: Session?=null,val selected: String?=null,val busy: Boolean=false,val error: String?=null)
class StudyViewModel(application: Application,private val saved: SavedStateHandle): AndroidViewModel(application) {
    private val app=application as StudyApplication
    private val repo=app.repository
    val settings=app.preferences.settings.stateIn(viewModelScope,SharingStarted.Eagerly,Settings())
    val stats=repo.dao.stats().stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
    val reviews=repo.dao.reviewCount().stateIn(viewModelScope,SharingStarted.Eagerly,0)
    val disciplines=MutableStateFlow<List<String>>(emptyList())
    val ui=MutableStateFlow(QuestionUi())
    val message=MutableStateFlow<String?>(null)
    val feedback=MutableSharedFlow<Boolean>(extraBufferCapacity=1)
    val ready=MutableStateFlow(false)
    private val initialized=viewModelScope.async(Dispatchers.IO) {
        repo.initialize(); disciplines.value=repo.dao.questions().map { it.discipline }.distinct().sorted()
    }
    init { viewModelScope.launch {
        try {
            initialized.await()
            val json=saved.get<String>("question")
            if(json!=null) ui.value=QuestionUi(Session(saved["presentation"]?:UUID.randomUUID().toString(),PackCodec.question(json)),saved["selected"])
            ready.value=true
        } catch(e: Exception) { if(e is CancellationException) throw e;message.value="Falha ao carregar banco: ${e.message}" }
    } }
    private fun persist() {
        saved["question"]=ui.value.session?.question?.let { PackCodec.encode(it) }
        saved["presentation"]=ui.value.session?.id;saved["selected"]=ui.value.selected
    }
    fun next() {
        if(ui.value.busy) return
        ui.value=ui.value.copy(busy=true,error=null)
        viewModelScope.launch {
            try {
                initialized.await()
                val session=withContext(Dispatchers.IO) { repo.next(app.preferences.settings.first()) }
                ui.value=QuestionUi(session,error=if(session==null) "Nenhuma questão para as disciplinas selecionadas." else null);persist()
            } catch(e: Exception) { if(e is CancellationException) throw e; ui.value=ui.value.copy(busy=false,error="Não foi possível carregar a questão: ${e.message}") }
        }
    }
    fun notification(id: String) { viewModelScope.launch {
        try {
            initialized.await()
            val session=withContext(Dispatchers.IO) { repo.pending(id) }
            if(session!=null) { ui.value=QuestionUi(session);persist() }
            else if(ui.value.session==null) next()
        } catch(e: Exception) { if(e is CancellationException) throw e; message.value="Não foi possível abrir o lembrete." }
    } }
    fun answer(letter: String) {
        val state=ui.value; val session=state.session?:return
        if(state.busy || state.selected!=null) return
        ui.value=state.copy(busy=true)
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    val inserted=repo.answer(session,letter)
                    (repo.dao.attempt(session.id) ?: error("Resposta não encontrada")) to inserted
                }
                ui.value=state.copy(selected=result.first.selected,busy=false);persist()
                if(result.second) feedback.emit(result.first.correct)
            } catch(e: Exception) { if(e is CancellationException) throw e;ui.value=state.copy(error="Não foi possível salvar a resposta. Tente novamente.") }
        }
    }
    fun update(transform: (Settings)->Settings) { viewModelScope.launch {
        try { app.preferences.update(transform(app.preferences.settings.first())) }
        catch(e: Exception) { if(e is CancellationException) throw e;message.value="Não foi possível salvar as configurações." }
    } }
    fun import(uri: Uri) { viewModelScope.launch {
        try {
            initialized.await()
            val count=withContext(Dispatchers.IO) {
                val bytes=app.contentResolver.openInputStream(uri)?.use { input -> val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192); var total=0; while(true) { val n=input.read(buffer); if(n<0) break; total+=n; require(total<=PackCodec.MAX_BYTES) { "Pacote excede 5 MB" }; output.write(buffer,0,n) }; output.toByteArray() }?:error("Arquivo indisponível")
                require(bytes.size<=PackCodec.MAX_BYTES) { "Pacote excede 5 MB" }
                repo.importPack(bytes.toString(Charsets.UTF_8))
            }
            disciplines.value=withContext(Dispatchers.IO) { repo.dao.questions().map { it.discipline }.distinct().sorted() }
            message.value="$count questões novas importadas. IDs existentes e histórico preservados."
        } catch(e: Exception) { if(e is CancellationException) throw e;message.value="Importação recusada: ${e.message}" }
    } }
    fun clearQuestion() { ui.value=QuestionUi();persist() }
}
