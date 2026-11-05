package br.com.desbloqueia

import android.app.Application
import androidx.room.Room
import br.com.desbloqueia.data.*
import br.com.desbloqueia.unlock.UnlockMonitor
import kotlinx.coroutines.*

class StudyApplication: Application() {
    val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    val database by lazy { Room.databaseBuilder(this,StudyDatabase::class.java,"study.db").build() }
    val repository by lazy { Repository(database,this) }
    val preferences by lazy { Preferences(this) }
    lateinit var monitor: UnlockMonitor
    override fun onCreate() {
        super.onCreate()
        monitor=UnlockMonitor(this,repository,preferences,scope)
        monitor.register()
    }
}
