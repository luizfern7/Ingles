package br.com.desbloqueia.unlock

import android.media.AudioManager
import android.view.WindowManager
import android.graphics.PixelFormat
import android.view.Gravity

object OverlayPolicy {
    fun mayShow(enabled: Boolean,permitted: Boolean,locked: Boolean,audioMode: Int,alreadyShowing: Boolean,appVisible: Boolean)=
        enabled && permitted && !locked && audioMode==AudioManager.MODE_NORMAL && !alreadyShowing && !appVisible
    fun layout(height: Int)=WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT
    ).apply { gravity=Gravity.BOTTOM }
}
