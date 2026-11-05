package br.com.desbloqueia

import android.content.Intent
import android.os.Bundle
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
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);consume(intent) }
    private fun consume(intent: Intent) { intent.getStringExtra(UnlockMonitor.SESSION)?.let { model.notification(it) };intent.removeExtra(UnlockMonitor.SESSION) }
}
