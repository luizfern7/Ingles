package br.com.desbloqueia

import android.app.Application
import androidx.room.Room
import br.com.desbloqueia.data.*
import br.com.desbloqueia.unlock.UnlockMonitor
import kotlinx.coroutines.*

class StudyApplication: Application() {
    val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    val database by lazy { Room.databaseBuilder(this,StudyDatabase::class.java,"study.db").addMigrations(MIGRATION_1_2).build() }
    val repository by lazy { Repository(database) }
    val preferences by lazy { Preferences(this) }
    var activityVisible=false
    val overlayStatus=kotlinx.coroutines.flow.MutableStateFlow("Desativado")
    lateinit var monitor: UnlockMonitor
    override fun onCreate() {
        super.onCreate()
        monitor=UnlockMonitor(this)
        monitor.register()
    }
}
