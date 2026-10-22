package com.tangyuanlongniang.pet

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class PetService : Service() {

    private lateinit var windowManager: WindowManager
    private var petView: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var state = 0

    private val handler = Handler(Looper.getMainLooper())
    private var lastAction = System.currentTimeMillis()

    private val states = arrayOf(
        "📖 正在认真看书",
        "💭 正在思考",
        "⚡ 正在加速思考",
        "🧠 正在深度思考",
        "🥵 好热呀 正在扇风"
    )

    private val idleTask = object : Runnable {
        override fun run() {
            val idleSeconds =
                (System.currentTimeMillis() - lastAction) / 1000

            if (idleSeconds >= 120) {
                state = 4
            } else if (idleSeconds >= 60) {
                state = 4
            } else if (idleSeconds >= 30) {
                state = 1 + ((idleSeconds / 15) % 3).toInt()
            }

            updatePet()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(16, 12, 16, 12)
        root.setBackgroundColor(Color.rgb(255, 225, 205))

        val character = TextView(this)
        character.text = "👧🏻"
        character.textSize = 56f
        character.gravity = Gravity.CENTER

        val speech = TextView(this)
        speech.text = states[0]
        speech.textSize = 14f
        speech.setTextColor(Color.rgb(100, 55, 35))
        speech.gravity = Gravity.CENTER

        val hint = TextView(this)
        hint.text = "点我切换状态"
        hint.textSize = 10f
        hint.setTextColor(Color.DKGRAY)
        hint.gravity = Gravity.CENTER

        root.addView(character)
        root.addView(speech)
        root.addView(hint)

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 40
        layoutParams.y = 200

        petView = root
        params = layoutParams

        root.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = params ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()

                        if (kotlin.math.abs(dx) > 5 ||
                            kotlin.math.abs(dy) > 5) {
                            moved = true
                        }

                        p.x = startX + dx
                        p.y = startY + dy

                        try {
                            windowManager.updateViewLayout(v, p)
                        } catch (_: Exception) {
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            state = (state + 1) % states.size
                            lastAction = System.currentTimeMillis()
                            updatePet()
                        }
                        return true
                    }
                }

                return false
            }
        })

        windowManager.addView(root, layoutParams)
        handler.postDelayed(idleTask, 5000)
    }

    private fun updatePet() {
        val root = petView ?: return

        if (root.childCount >= 2) {
            val speech = root.getChildAt(1) as TextView
            speech.text = states[state]
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleTask)

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
