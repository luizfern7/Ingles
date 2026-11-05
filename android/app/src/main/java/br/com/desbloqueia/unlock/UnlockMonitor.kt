package br.com.desbloqueia.unlock

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.desbloqueia.MainActivity
import br.com.desbloqueia.R
import br.com.desbloqueia.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** Receiver em tempo de execução: funciona somente enquanto o processo está vivo.
 * Não abre Activities do background e não promete sobreviver a encerramento/reboot.
 * Notificação silenciosa é o caminho de entrada iniciado pelo usuário. */
class UnlockMonitor(private val context: Context, private val repo: Repository, private val preferences: Preferences, private val scope: CoroutineScope) {
    companion object { const val SESSION="session"; const val CHANNEL="study"; const val PROMPT=200; const val SHORTCUT=201 }
    private val notifications=context.getSystemService(NotificationManager::class.java)
    private val receiver=object: BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val pending=goAsync()
            scope.launch {
                try {
                    val settings=preferences.settings.first()
                    if(!settings.unlock) return@launch
                    if(intent.action==Intent.ACTION_SCREEN_OFF) repo.screenOff()
                    if(intent.action==Intent.ACTION_USER_PRESENT && canNotify()) {
                        val audio=context.getSystemService(AudioManager::class.java)
                        if(audio.mode!=AudioManager.MODE_NORMAL || context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) return@launch
                        repo.unlockPrompt(settings)?.let { session ->
                            notifications.notify(PROMPT,base("Uma questão para o seu próximo passo",session.question.disciplina,session.id).setAutoCancel(true).build())
                        }
                    }
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { android.util.Log.w("UnlockMonitor","Não foi possível preparar a notificação",e) }
                finally { pending.finish() }
            }
        }
    }
    fun register() {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL,"Lembretes de estudo",NotificationManager.IMPORTANCE_LOW).apply {
            description="Questões silenciosas, abertas somente ao tocar";setSound(null,null);enableVibration(false)
        })
        ContextCompat.registerReceiver(context,receiver,IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_USER_PRESENT) },ContextCompat.RECEIVER_EXPORTED)
        scope.launch { preferences.settings.collect { s ->
            if(s.unlock && canNotify()) notifications.notify(SHORTCUT,base("Estudo por desbloqueio ativado","Detecção enquanto o processo estiver ativo. Toque para estudar.",null).setOngoing(true).build())
            else { notifications.cancel(SHORTCUT);notifications.cancel(PROMPT) }
        } }
    }
    fun canNotify() = notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE && (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
    private fun base(title: String, text: String, session: String?): NotificationCompat.Builder {
        val intent=Intent(context,MainActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if(session!=null) putExtra(SESSION,session)
        }
        val pending=PendingIntent.getActivity(context,if(session==null) SHORTCUT else PROMPT,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_study).setContentTitle(title).setContentText(text)
            .setContentIntent(pending).setSilent(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true)
    }
}
