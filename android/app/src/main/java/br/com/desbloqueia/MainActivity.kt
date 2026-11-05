package br.com.desbloqueia

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import br.com.desbloqueia.unlock.UnlockStudyService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import br.com.desbloqueia.ui.*
import br.com.desbloqueia.unlock.UnlockMonitor

class MainActivity: ComponentActivity() {
    private val model: StudyViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState==null) consume(intent)
        setContent { StudyApp(model,onClose={ finish() }) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val app=application as StudyApplication
                combine(app.preferences.settings,app.repository.dao.observeQuestionCount()) { settings,count -> settings to count }.collect { (settings,count) ->
                    if(settings.unlock && count>0) {
                        if(app.monitor.canOverlay() && app.monitor.canNotify()) {
                            try { androidx.core.content.ContextCompat.startForegroundService(this@MainActivity,Intent(this@MainActivity,UnlockStudyService::class.java)) }
                            catch(e: RuntimeException) { app.overlayStatus.value="Não foi possível ativar. Reabra o app e confira as permissões." }
                        } else app.overlayStatus.value="Autorize sobreposição e notificações em Ajustes."
                    } else {
                        stopService(Intent(this@MainActivity,UnlockStudyService::class.java))
                        if(count==0) app.overlayStatus.value="Importe um JSON para começar."
                    }
                }
            }
        }
    }
    override fun onStart() { super.onStart();(application as StudyApplication).activityVisible=true }
    override fun onStop() { (application as StudyApplication).activityVisible=false;super.onStop() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);consume(intent) }
    private fun consume(intent: Intent) { intent.getStringExtra(UnlockMonitor.SESSION)?.let { model.notification(it) };intent.removeExtra(UnlockMonitor.SESSION) }
}
