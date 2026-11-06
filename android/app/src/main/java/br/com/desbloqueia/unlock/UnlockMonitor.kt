package br.com.desbloqueia.unlock

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.desbloqueia.MainActivity
import br.com.desbloqueia.R

/** Permissões e notificações. O receptor só vive no serviço iniciado pelo usuário. */
class UnlockMonitor(private val context: Context) {
    companion object {
        const val SESSION="session"
        const val CHANNEL="study"
        const val PROMPT=200
        const val FOREGROUND=201
    }
    private val notifications=context.getSystemService(NotificationManager::class.java)
    fun register() {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL,"Estudo ao desbloquear",NotificationManager.IMPORTANCE_LOW).apply {
            description="Controle do modo automático e questões";setSound(null,null);enableVibration(false)
        })
    }
    fun canOverlay()=Settings.canDrawOverlays(context)
    fun canNotify()=notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE &&
        (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
    fun foreground(): Notification {
        val stop=PendingIntent.getService(context,3,Intent(context,UnlockStudyService::class.java).setAction(UnlockStudyService.STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return base("Estudo automático ativo","Questões e flashcards após desbloquear. Toque em Parar para desativar.",null)
            .setOngoing(true).addAction(0,"Parar",stop).setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()
    }
    fun fallback(session: String) {
        if(canNotify()) notifications.notify(PROMPT,base("Toque para estudar","A sobreposição não pôde ser exibida neste aparelho.",session).setAutoCancel(true).build())
    }
    fun clearPrompt() { notifications.cancel(PROMPT) }
    private fun base(title: String,text: String,session: String?): NotificationCompat.Builder {
        val intent=Intent(context,MainActivity::class.java).apply { flags=Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP;if(session!=null) putExtra(SESSION,session) }
        val pending=PendingIntent.getActivity(context,if(session==null) FOREGROUND else PROMPT,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_study).setContentTitle(title).setContentText(text)
            .setContentIntent(pending).setSilent(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true)
    }
}
