package br.com.desbloqueia.unlock

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.*
import android.view.WindowManager
import android.view.Display
import android.hardware.display.DisplayManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import br.com.desbloqueia.StudyApplication
import br.com.desbloqueia.data.*
import br.com.desbloqueia.ui.Feedback
import br.com.desbloqueia.ui.QuestionUi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first

/** FGS specialUse: somente mediante opt-in e iniciado a partir da Activity visível.
 * Não inicia Activities do background, não toca na tela de bloqueio e não usa acessibilidade. */
class UnlockStudyService: Service(),LifecycleOwner,SavedStateRegistryOwner {
    companion object { const val STOP="br.com.desbloqueia.STOP";const val PREVIEW="br.com.desbloqueia.PREVIEW" }
    private val registry=LifecycleRegistry(this)
    private val saved=SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get()=registry
    override val savedStateRegistry: SavedStateRegistry get()=saved.savedStateRegistry
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val events=Channel<String>(Channel.UNLIMITED)
    private val app get()=application as StudyApplication
    private val windowContext by lazy {
        if(Build.VERSION.SDK_INT>=30) {
            val display=getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null)
        } else this
    }
    private val windows get()=windowContext.getSystemService(WindowManager::class.java)
    private var overlay: ComposeView?=null
    private var watcher: Job?=null
    private var registered=false
    private lateinit var feedback: Feedback
    internal var questionUi by mutableStateOf(QuestionUi());private set
    private var overlaySettings by mutableStateOf(Settings())
    internal val isOverlayShowing get()=overlay!=null
    private val receiver=object: BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) {
            if(intent.action==Intent.ACTION_SCREEN_OFF) dismissOverlay()
            if(intent.action in listOf(Intent.ACTION_SCREEN_OFF,Intent.ACTION_USER_PRESENT)) events.trySend(intent.action!!)
        }
    }
    override fun onCreate() {
        super.onCreate()
        saved.performAttach();saved.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        feedback=Feedback(this)
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if(intent?.action==STOP) {
            dismissOverlay()
            scope.launch {
                try {app.preferences.modify {it.copy(unlock=false)}}
                catch(e: CancellationException) {throw e}
                catch(e: Exception) {android.util.Log.w("UnlockStudyService","Não foi possível salvar a desativação",e)}
                finally {finishMonitoring()}
            }
            return START_NOT_STICKY
        }
        if(!app.monitor.canOverlay() || !app.monitor.canNotify()) { app.overlayStatus.value="Faltam permissões para o modo automático.";stopSelf();return START_NOT_STICKY }
        try {
            ServiceCompat.startForeground(this,UnlockMonitor.FOREGROUND,app.monitor.foreground(),if(Build.VERSION.SDK_INT>=34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        } catch(e: RuntimeException) { app.overlayStatus.value="O Android impediu o serviço. Reabra o app para ativar.";stopSelf();return START_NOT_STICKY }
        if(!registered) {
            registered=true
            ContextCompat.registerReceiver(this,receiver,IntentFilter().apply {addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_USER_PRESENT)},ContextCompat.RECEIVER_EXPORTED)
            scope.launch {
                for(action in events) try { handleEvent(action) }
                catch(e: CancellationException) {throw e}
                catch(e: Exception) {dismissOverlay();app.overlayStatus.value="Falha ao preparar questão. Abra o app para tentar novamente."}
            }
            scope.launch {
                app.preferences.settings.collect { settings ->
                    if(!settings.unlock) finishMonitoring()
                    else {
                        overlaySettings=settings
                        if(settings.pausedUntil>System.currentTimeMillis()) dismissOverlay()
                        app.overlayStatus.value=if(settings.pausedUntil>System.currentTimeMillis()) "Pausado temporariamente" else "Ativo · janela após desbloquear"
                    }
                }
            }
        }
        if(intent?.action==PREVIEW) scope.launch {
            try {
            val settings=app.preferences.settings.first()
            if(settings.unlock && !isOverlayShowing && !getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                val session=withContext(Dispatchers.IO) {app.repository.initialize();app.repository.nextStudy(settings)}
                if(session!=null && !showStudyOverlay(session,settings)) app.overlayStatus.value="O Android recusou a janela. Confira a permissão."
            }
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {app.overlayStatus.value="Não foi possível testar a janela. Confira permissões e disciplinas."}
        }
        return START_NOT_STICKY
    }
    internal suspend fun handleEvent(action: String) {
        if(action==Intent.ACTION_SCREEN_OFF) {dismissOverlay();withContext(Dispatchers.IO) {app.repository.screenOff()};return}
        if(action!=Intent.ACTION_USER_PRESENT) return
        val settings=app.preferences.settings.first()
        if(!OverlayPolicy.mayShow(settings.unlock,app.monitor.canOverlay(),getSystemService(KeyguardManager::class.java).isKeyguardLocked,getSystemService(AudioManager::class.java).mode,isOverlayShowing,app.activityVisible)) return
        val session=withContext(Dispatchers.IO) {app.repository.initialize();app.repository.unlockStudy(settings)}?:return
        if(showStudyOverlay(session,settings)) withContext(Dispatchers.IO) {app.repository.pendingStudy(session.id)}
        else app.monitor.fallback(session.id)
    }
    internal fun showOverlay(session: Session,settings: Settings)=showStudyOverlay(StudySession(question=session),settings)
    internal fun showStudyOverlay(session: StudySession,settings: Settings): Boolean {
        if(!settings.unlock || overlay!=null || !app.monitor.canOverlay() ||
            !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
            getSystemService(AudioManager::class.java).mode!=AudioManager.MODE_NORMAL) return false
        val view=ComposeView(windowContext)
        view.setViewTreeLifecycleOwner(this);view.setViewTreeSavedStateRegistryOwner(this)
        questionUi=QuestionUi.from(session);overlaySettings=settings
        view.setContent { OverlayContent(questionUi,overlaySettings,::answer,::next,::dismissOverlay,::flip,::rate) }
        return try {
            // Ocupa 85% da altura; não toma foco do sistema, status bar, teclado ou navegação.
            val screenHeight=if(Build.VERSION.SDK_INT>=30) windows.currentWindowMetrics.bounds.height() else resources.displayMetrics.heightPixels
            val height=(screenHeight*0.85f).toInt()
            windows.addView(view,OverlayPolicy.layout(height))
            overlay=view
            watcher=scope.launch {
                while(isActive) {
                    delay(500)
                    if(!app.monitor.canOverlay() || !getSystemService(PowerManager::class.java).isInteractive || getSystemService(KeyguardManager::class.java).isKeyguardLocked || getSystemService(AudioManager::class.java).mode!=AudioManager.MODE_NORMAL) {dismissOverlay();break}
                }
            }
            true
        } catch(e: RuntimeException) {view.disposeComposition();false}
    }
    private fun answer(letter: String) {
        val state=questionUi;val session=state.session?:return
        if(state.busy || state.selected!=null) return
        questionUi=state.copy(busy=true)
        scope.launch {
            try {
                val result=withContext(Dispatchers.IO) {
                    val inserted=app.repository.answer(session,letter)
                    app.repository.dao.attempt(session.id)!! to inserted
                }
                if(questionUi.session?.id==session.id && overlay!=null) {
                    questionUi=state.copy(selected=result.first.selected)
                    if(result.second) feedback.play(result.first.correct,overlaySettings,overlay!!)
                }
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {if(questionUi.session?.id==session.id) questionUi=state.copy(error="Não foi possível salvar. Tente novamente.")}
        }
    }
    private fun flip() {
        val state=questionUi;val session=state.flash?:return
        if(state.busy)return
        if(state.flipped) {questionUi=state.copy(flipped=false);return}
        questionUi=state.copy(busy=true)
        scope.launch {try {
            val revealed=withContext(Dispatchers.IO) {app.repository.reveal(session)}
            if(questionUi.itemId==session.id && overlay!=null) questionUi=state.copy(flipped=revealed)
        } catch(e: CancellationException) {throw e}
        catch(e: Exception) {if(questionUi.itemId==session.id) questionUi=state.copy(error="Não foi possível virar.")}}
    }
    private fun rate(known: Boolean) {
        val state=questionUi;val session=state.flash?:return
        if(state.busy || !state.flipped || state.flashRating!=null)return
        questionUi=state.copy(busy=true)
        scope.launch {try {
            val result=withContext(Dispatchers.IO) {app.repository.rate(session,known)}
            val review=app.repository.dao.flashReview(session.id)
            if(questionUi.itemId==session.id && overlay!=null) {
                questionUi=state.copy(flashRating=review?.known,nextDue=review?.dueAt?:result.first.nextReviewAt)
                if(result.second) feedback.play(known,overlaySettings,overlay!!)
            }
        } catch(e: CancellationException) {throw e}
        catch(e: Exception) {if(questionUi.itemId==session.id) questionUi=state.copy(error="Não foi possível salvar a revisão.")}}
    }
    private fun next() {
        if(questionUi.busy) return
        questionUi=questionUi.copy(busy=true)
        val current=overlay
        scope.launch {
            try {
                val session=withContext(Dispatchers.IO) {app.repository.nextStudy(app.preferences.settings.first())}
                if(overlay===current && current!=null) questionUi=QuestionUi.from(session).copy(error=if(session==null) "Nenhum item elegível agora; confira as datas de revisão." else null)
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {if(overlay===current) questionUi=questionUi.copy(busy=false,error="Não foi possível carregar.")}
        }
    }
    internal fun dismissOverlay() {
        watcher?.cancel();watcher=null
        overlay?.let { view ->
            view.disposeComposition()
            try {windows.removeViewImmediate(view)} catch(_: IllegalArgumentException) {}
        }
        overlay=null;questionUi=QuestionUi()
    }
    private fun finishMonitoring() {dismissOverlay();app.monitor.clearPrompt();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onDestroy() {
        dismissOverlay()
        if(registered) {unregisterReceiver(receiver);registered=false}
        feedback.release();scope.cancel();events.close()
        app.overlayStatus.value="Desativado · abra o app para retomar"
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?)=null
}
