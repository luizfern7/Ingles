package br.com.desbloqueia.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("study_settings")
data class Settings(
    val sound: Boolean=false, val vibration: Boolean=true, val animations: Boolean=true,
    val unlock: Boolean=false, val frequency: Int=30, val pausedUntil: Long=0,
    val disciplines: Set<String> = emptySet(), val theme: String="auto"
)
class Preferences(context: Context) {
    private val store=context.settingsStore
    private val sound=booleanPreferencesKey("sound"); private val vibration=booleanPreferencesKey("vibration")
    private val animations=booleanPreferencesKey("animations"); private val unlock=booleanPreferencesKey("unlock")
    private val frequency=intPreferencesKey("frequency"); private val pause=longPreferencesKey("pause")
    private val disciplines=stringSetPreferencesKey("disciplines"); private val theme=stringPreferencesKey("theme")
    val settings=store.data.catch { if(it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        Settings(p[sound]?:false,p[vibration]?:true,p[animations]?:true,p[unlock]?:false,p[frequency]?:30,p[pause]?:0,p[disciplines]?:emptySet(),p[theme]?:"auto")
    }
    suspend fun update(s: Settings) { store.edit { p ->
        p[sound]=s.sound;p[vibration]=s.vibration;p[animations]=s.animations;p[unlock]=s.unlock
        p[frequency]=s.frequency;p[pause]=s.pausedUntil;p[disciplines]=s.disciplines;p[theme]=s.theme
    } }
}
