package br.com.desbloqueia

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.*
import br.com.desbloqueia.data.Preferences
import br.com.desbloqueia.data.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=android.app.Application::class)
class StudyPreferencesTest {
    @Test fun methodsAndPercentagesPersistToDiskAndAtomicUpdatesKeepOtherFields(): Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val source=File(context.filesDir,"datastore/isolated-study-settings.preferences_pb")
        val writingScope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val dataStore=PreferenceDataStoreFactory.create(scope=writingScope,produceFile={source})
        val preferences=Preferences(context,dataStore)
        preferences.update(Settings())
        assertEquals("mixed",preferences.settings.first().studyMode);assertEquals(50,preferences.settings.first().flashPercent)
        coroutineScope {
            launch {preferences.modify {it.copy(theme="dark")}}
            launch {preferences.modify {it.copy(studyMode="flashcards",flashPercent=70)}}
        }
        val fresh=Preferences(context,dataStore).settings.first()
        assertEquals("dark",fresh.theme);assertEquals("flashcards",fresh.studyMode);assertEquals(70,fresh.flashPercent)
        assertTrue(source.exists() && source.length()>0)
        val snapshot=File(context.cacheDir,"settings-snapshot.preferences_pb");source.copyTo(snapshot,overwrite=true)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try {
            val recreated=PreferenceDataStoreFactory.create(scope=scope,produceFile={snapshot})
            val data=recreated.data.first()
            assertEquals("flashcards",data[stringPreferencesKey("study_method")])
            assertEquals(70,data[intPreferencesKey("flash_percent")])
            assertEquals("dark",data[stringPreferencesKey("theme")])
        } finally {scope.cancel();writingScope.cancel()}
    }
}
