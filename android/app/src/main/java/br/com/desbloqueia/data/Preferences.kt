package br.com.desbloqueia.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("study_settings")
data class Settings(
    val sound: Boolean=false,val vibration: Boolean=true,val animations: Boolean=true,
    val unlock: Boolean=false,val frequency: Int=0,val pausedUntil: Long=0,
    val disciplines: Set<String> = emptySet(),val theme: String="auto",
    val studyMode: String="mixed",val flashPercent: Int=50
)
class Preferences(context: Context,private val store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> = context.settingsStore) {
    private val sound=booleanPreferencesKey("sound");private val vibration=booleanPreferencesKey("vibration")
    private val animations=booleanPreferencesKey("animations");private val unlock=booleanPreferencesKey("unlock")
    private val frequency=intPreferencesKey("frequency");private val pause=longPreferencesKey("pause")
    private val disciplines=stringSetPreferencesKey("disciplines");private val theme=stringPreferencesKey("theme")
    private val method=stringPreferencesKey("study_method");private val proportion=intPreferencesKey("flash_percent")
    private fun read(p: androidx.datastore.preferences.core.Preferences)=Settings(
        p[sound]?:false,p[vibration]?:true,p[animations]?:true,p[unlock]?:false,p[frequency]?:0,p[pause]?:0,p[disciplines]?:emptySet(),p[theme]?:"auto",
        p[method]?.takeIf {it in setOf("questions","flashcards","mixed")}?:"mixed",(p[proportion]?:50).coerceIn(0,100)/10*10
    )
    private fun write(p: MutablePreferences,s: Settings) {
        p[sound]=s.sound;p[vibration]=s.vibration;p[animations]=s.animations;p[unlock]=s.unlock
        p[frequency]=s.frequency;p[pause]=s.pausedUntil;p[disciplines]=s.disciplines;p[theme]=s.theme
        p[method]=s.studyMode;p[proportion]=s.flashPercent.coerceIn(0,100)/10*10
    }
    val settings=store.data.catch {if(it is IOException)emit(emptyPreferences()) else throw it}.map(::read)
    suspend fun update(s: Settings) {store.edit {write(it,s)}}
    suspend fun modify(transform: (Settings)->Settings) {store.edit {write(it,transform(read(it)))}}
}
