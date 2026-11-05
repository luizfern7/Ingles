package br.com.desbloqueia.ui

import android.content.Context
import android.media.*
import android.view.HapticFeedbackConstants
import android.view.View
import br.com.desbloqueia.R
import br.com.desbloqueia.data.Settings

class Feedback(private val context: Context) {
    private val pool=SoundPool.Builder().setMaxStreams(1).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val loaded=mutableSetOf<Int>()
    private val success: Int
    private val error: Int
    init {
        pool.setOnLoadCompleteListener { _,id,status -> if(status==0) loaded.add(id) }
        success=pool.load(context,R.raw.success,1);error=pool.load(context,R.raw.error,1)
    }
    fun play(correct: Boolean,settings: Settings,view: View) {
        val audio=context.getSystemService(AudioManager::class.java)
        val id=if(correct) success else error
        if(settings.sound && audio.ringerMode==AudioManager.RINGER_MODE_NORMAL && audio.mode==AudioManager.MODE_NORMAL && id in loaded)
            pool.play(id,0.45f,0.45f,1,0,1f)
        if(settings.vibration) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    fun release() { pool.release() }
}
