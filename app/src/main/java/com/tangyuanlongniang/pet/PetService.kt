
package com.tangyuanlongniang.pet

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

class PetService : Service() {

    private lateinit var windowManager: WindowManager
    private var petView: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (petView != null) return START_NOT_STICKY

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val pet = TextView(this)
        pet.text = "🐉\n汤圆的龙娘"
        pet.textSize = 20f
        pet.setTextColor(Color.rgb(100, 55, 35))
        pet.setBackgroundColor(Color.rgb(255, 235, 220))
        pet.gravity = Gravity.CENTER
        pet.setPadding(16, 16, 16, 16)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 80
        params.y = 250

        pet.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.tag = floatArrayOf(
                        event.rawX - params.x,
                        event.rawY - params.y
                    )
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val pos = view.tag as FloatArray
                    params.x = (event.rawX - pos[0]).toInt()
                    params.y = (event.rawY - pos[1]).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> true
            }
        }

        petView = pet
        windowManager.addView(pet, params)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        petView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        petView = null
        super.onDestroy()
    }
}
