package br.com.desbloqueia

import android.Manifest
import android.app.KeyguardManager
import android.os.Looper
import android.os.PowerManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import br.com.desbloqueia.data.Settings
import br.com.desbloqueia.data.UnlockState
import br.com.desbloqueia.domain.Selection
import br.com.desbloqueia.unlock.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28,35],application=StudyApplication::class)
class OverlayTest {
    @Test fun overlayRequiresOptInPermissionUnlockedQuietDeviceAndNoDuplicate() {
        fun allowed(enabled: Boolean=true,permission: Boolean=true,locked: Boolean=false,mode: Int=AudioManager.MODE_NORMAL,showing: Boolean=false,visible: Boolean=false)=OverlayPolicy.mayShow(enabled,permission,locked,mode,showing,visible)
        assertTrue(allowed())
        assertFalse(allowed(enabled=false));assertFalse(allowed(permission=false));assertFalse(allowed(locked=true))
        assertFalse(allowed(mode=AudioManager.MODE_IN_CALL));assertFalse(allowed(mode=AudioManager.MODE_IN_COMMUNICATION))
        assertFalse(allowed(showing=true));assertFalse(allowed(visible=true))
    }
    @Test fun overlayUsesOfficialWindowAndDoesNotTakeKeyboardFocus() {
        val p=OverlayPolicy.layout(1200)
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,p.type)
        assertEquals(1200,p.height)
        assertTrue(p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE!=0)
        assertTrue(p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL!=0)
    }
    @Test fun everyUnlockStillDeduplicatesCycle() {
        val s=Settings(unlock=true,frequency=0)
        assertTrue(Selection.due(UnlockState(cycle=2,handledCycle=1,lastPrompt=1000),s,1000))
        assertFalse(Selection.due(UnlockState(cycle=2,handledCycle=2,lastPrompt=1000),s,1000))
        assertFalse(Selection.due(UnlockState(cycle=2,handledCycle=1),s.copy(pausedUntil=2000),1000))
    }
    @Test fun windowCanBeShownClosedAndIsRemovedOnScreenOff(): Unit = runBlocking {
        val app=ApplicationProvider.getApplicationContext<StudyApplication>()
        ShadowSettings.setCanDrawOverlays(true)
        Shadows.shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        app.repository.initialize()
        val controller=Robolectric.buildService(UnlockStudyService::class.java).create()
        val service=controller.get()
        val session=app.repository.next(Settings())!!
        assertTrue(service.showOverlay(session,Settings(unlock=true)))
        assertTrue(service.isOverlayShowing)
        assertFalse(service.showOverlay(session,Settings(unlock=true)))
        service.dismissOverlay();assertFalse(service.isOverlayShowing)
        assertTrue(service.showOverlay(session,Settings(unlock=true)))
        service.handleEvent(Intent.ACTION_SCREEN_OFF)
        assertFalse(service.isOverlayShowing)
        controller.destroy()
    }
    @Test fun unlockEventShowsQuestionWithoutNotificationTapAndDeduplicates(): Unit = runBlocking {
        val app=ApplicationProvider.getApplicationContext<StudyApplication>()
        ShadowSettings.setCanDrawOverlays(true)
        Shadows.shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        Shadows.shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        app.activityVisible=false
        app.preferences.update(Settings(unlock=true,frequency=0))
        app.repository.initialize()
        val controller=Robolectric.buildService(UnlockStudyService::class.java).create()
        val service=controller.get()
        service.handleEvent(Intent.ACTION_SCREEN_OFF)
        service.handleEvent(Intent.ACTION_USER_PRESENT)
        assertTrue(service.isOverlayShowing)
        val first=service.questionUi.session!!.id
        service.handleEvent(Intent.ACTION_USER_PRESENT)
        assertEquals(first,service.questionUi.session!!.id)
        service.handleEvent(Intent.ACTION_SCREEN_OFF)
        assertFalse(service.isOverlayShowing)
        service.handleEvent(Intent.ACTION_USER_PRESENT)
        assertTrue(service.isOverlayShowing)
        assertNotEquals(first,service.questionUi.session!!.id)
        controller.destroy()
    }
    @Test fun runningServiceRegistersUnlockReceiverAndStopDisablesPreference(): Unit = runBlocking {
        val app=ApplicationProvider.getApplicationContext<StudyApplication>()
        ShadowSettings.setCanDrawOverlays(true)
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Shadows.shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
        Shadows.shadowOf(app.getSystemService(KeyguardManager::class.java)).setKeyguardLocked(false)
        app.preferences.update(Settings(unlock=true,frequency=0))
        app.repository.initialize()
        val controller=Robolectric.buildService(UnlockStudyService::class.java).create()
        val service=controller.get()
        service.onStartCommand(Intent(app,UnlockStudyService::class.java),0,1)
        assertNotNull(Shadows.shadowOf(service).lastForegroundNotification)
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        app.sendBroadcast(Intent(Intent.ACTION_USER_PRESENT))
        withTimeout(10_000) {
            while(!service.isOverlayShowing) {Shadows.shadowOf(Looper.getMainLooper()).idle();delay(10)}
        }
        assertNotNull(service.questionUi.session)
        service.onStartCommand(Intent(app,UnlockStudyService::class.java).setAction(UnlockStudyService.STOP),0,2)
        withTimeout(10_000) {
            while(app.preferences.settings.first().unlock) {Shadows.shadowOf(Looper.getMainLooper()).idle();delay(10)}
        }
        assertFalse(service.isOverlayShowing)
        controller.destroy()
    }
    @Test fun noOverlayWithoutSpecialPermission(): Unit = runBlocking {
        val app=ApplicationProvider.getApplicationContext<StudyApplication>()
        ShadowSettings.setCanDrawOverlays(false)
        app.repository.initialize()
        val controller=Robolectric.buildService(UnlockStudyService::class.java).create()
        val service=controller.get()
        assertFalse(service.showOverlay(app.repository.next(Settings())!!,Settings(unlock=true)))
        assertFalse(service.isOverlayShowing)
        controller.destroy()
    }
}
