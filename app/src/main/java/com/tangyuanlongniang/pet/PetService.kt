
package com.tangyuanlongniang.pet

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class PetService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var petView: View? = null
    private var collapsed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (petView != null) return START_NOT_STICKY

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

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
        params.x = 40
        params.y = 250

        showExpanded()
        return START_NOT_STICKY
    }

    private fun background(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            setStroke(2, Color.rgb(220, 190, 175))
        }
    }

    private fun makeAvatar(): TextView {
        return TextView(this).apply {
            text = "🐉"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(100, 55, 35))
            background = background(Color.rgb(255, 240, 230), 1000f)
            elevation = 8f
        }
    }

    private fun install(view: View) {
        val old = petView
        if (old != null) {
            try {
                wm.removeView(old)
            } catch (_: Exception) {
            }
        }

        petView = view
        wm.addView(view, params)
        addDrag(view)
    }

    private fun addDrag(view: View) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY

                    if (abs(dx) > 8 || abs(dy) > 8) moved = true

                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()

                    try {
                        wm.updateViewLayout(view, params)
                    } catch (_: Exception) {
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved && collapsed) {
                        showExpanded()
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun showExpanded() {
        collapsed = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 8, 8)
            background = background(
                Color.rgb(255, 245, 235), 36f
            )
            elevation = 8f
        }

        val avatar = makeAvatar()
        root.addView(
            avatar,
            LinearLayout.LayoutParams(76, 76)
        )

        val words = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 0, 8, 0)
        }

        val title = TextView(this).apply {
            text = "汤圆的龙娘"
            textSize = 16f
            setTextColor(Color.rgb(100, 55, 35))
        }

        val hint = TextView(this).apply {
            text = "点击收起，拖动头像移动"
            textSize = 11f
            setTextColor(Color.DKGRAY)
        }

        words.addView(title)
        words.addView(hint)
        root.addView(words)

        val hideButton = Button(this).apply {
            text = "收起"
            textSize = 12f
            setOnClickListener {
                showCollapsed()
            }
        }

        root.addView(hideButton)
        install(root)

        avatar.setOnTouchListener { _, event ->
            handleAvatarDrag(avatar, event)
        }
    }

    private fun handleAvatarDrag(
        view: View,
        event: MotionEvent
    ): Boolean {
        var startX = 0
        var startY = 0
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = params.x
                startY = params.y
                view.tag = floatArrayOf(
                    event.rawX,
                    event.rawY,
                    startX.toFloat(),
                    startY.toFloat()
                )
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val data = view.tag as? FloatArray
                if (data != null) {
                    params.x = data[2].toInt() +
                        (event.rawX - data[0]).toInt()
                    params.y = data[3].toInt() +
                        (event.rawY - data[1]).toInt()
                    try {
                        wm.updateViewLayout(petView, params)
                    } catch (_: Exception) {
                    }
                }
                true
            }

            else -> true
        }
    }

    private fun showCollapsed() {
        collapsed = true

        val avatar = makeAvatar()
        avatar.layoutParams = LinearLayout.LayoutParams(64, 64)
        install(avatar)
    }

    override fun onDestroy() {
        petView?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        petView = null
        super.onDestroy()
    }
}
