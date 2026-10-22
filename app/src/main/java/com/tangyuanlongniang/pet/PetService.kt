
package com.tangyuanlongniang.pet

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

class PetService : Service() {

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var petView: LinearLayout? = null
    private var character: TextView? = null
    private var speech: TextView? = null
    private var status: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private var petSize = 100
    private var petOpacity = 100
    private var maidMode = false
    private var tapCount = 0
    private var lastTapTime = 0L
    private var lastMoveTime = 0L
    private var mood = "开心"

    private val dragonLines = listOf(
        "今天也要加油哦",
        "陪我看一会儿书吧",
        "我刚才在偷偷发呆",
        "休息一下也没关系",
        "哼 你终于想起我啦",
        "再点我就要害羞了",
        "今天的你也很努力呢",
        "让我想想接下来做什么"
    )

    private val maidLines = listOf(
        "主人 要休息一下吗",
        "女仆今天也很精神",
        "请记得喝水哦",
        "要不要和龙娘聊聊天",
        "今天也要照顾好自己",
        "我来陪你一会儿吧"
    )

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            "HIDE" -> removePet()
            "SHOW" -> {
                readStyle(intent)
                showPet()
            }
            "UPDATE_STYLE" -> {
                readStyle(intent)
                applyStyle()
            }
            else -> showPet()
        }

        return START_NOT_STICKY
    }

    private fun readStyle(intent: Intent) {
        petSize = intent.getIntExtra("size", petSize)
            .coerceIn(50, 200)
        petOpacity = intent.getIntExtra("opacity", petOpacity)
            .coerceIn(20, 100)
    }

    private fun showPet() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this, "请先允许显示在其他应用上层",
                Toast.LENGTH_SHORT
            ).show()
            stopSelf()
            return
        }

        if (petView != null) {
            applyStyle()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(255, 238, 220))
        }

        character = TextView(this).apply {
            text = "🐉"
            textSize = 38f
            gravity = Gravity.CENTER
        }

        speech = TextView(this).apply {
            text = "龙娘来陪你啦"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(95, 50, 35))
            typeface = Typeface.DEFAULT_BOLD
        }

        status = TextView(this).apply {
            text = "心情：开心"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
        }

        val hint = TextView(this).apply {
            text = "轻点互动 · 长按拖动"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
        }

        root.addView(character)
        root.addView(speech)
        root.addView(status)
        root.addView(hint)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val p = WindowManager.LayoutParams(
            dp(190),
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(30)
            y = dp(180)
        }

        root.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = p.x
                        startY = p.y
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()

                        if (kotlin.math.abs(dx) > dp(5) ||
                            kotlin.math.abs(dy) > dp(5)) {
                            moved = true
                        }

                        if (moved) {
                            p.x = (startX + dx).coerceAtLeast(0)
                            p.y = (startY + dy).coerceAtLeast(0)
                            try {
                                wm.updateViewLayout(root, p)
                            } catch (_: Exception) {
                            }
                            lastMoveTime = System.currentTimeMillis()
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!moved) handleTap()
                        return true
                    }
                }
                return true
            }
        })

        try {
            wm.addView(root, p)
            petView = root
            params = p
            applyStyle()
            startIdleLoop()
        } catch (_: Exception) {
            petView = null
            params = null
            Toast.makeText(
                this, "桌宠显示失败，请检查悬浮窗权限",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
        }
    }

    private fun handleTap() {
        val now = System.currentTimeMillis()
        tapCount++

        if (now - lastTapTime < 600) {
            maidMode = !maidMode
            say(if (maidMode) {
                "蓝发女仆：轮到我陪你啦"
            } else {
                "龙娘：哼 还是我最懂你"
            })
            lastTapTime = 0L
            return
        }

        lastTapTime = now

        if (tapCount % 7 == 0) {
            say("隐藏彩蛋：今天也要开心呀")
            setMood("惊喜")
            return
        }

        if (Random.nextInt(100) < 25) {
            maidMode = !maidMode
        }

        val line = if (maidMode) maidLines.random()
            else dragonLines.random()

        say((if (maidMode) "🩵 女仆：" else "🐉 龙娘：") + line)

        val moods = listOf("开心", "害羞", "思考", "困倦")
        setMood(moods.random())
    }

    private fun say(line: String) {
        speech?.text = line
        character?.text = if (maidMode) "🩵" else "🐉"
    }

    private fun setMood(value: String) {
        mood = value
        status?.text = "心情：$mood"
    }

    private fun startIdleLoop() {
        handler.removeCallbacks(idleRunnable)
        handler.postDelayed(idleRunnable, 30_000L)
    }

    private val idleRunnable = object : Runnable {
        override fun run() {
            if (petView == null) return

            val now = System.currentTimeMillis()
            if (now - lastMoveTime >= 30_000L) {
                val action = Random.nextInt(4)
                when (action) {
                    0 -> {
                        say(if (maidMode) "女仆：我在整理今天的计划"
                            else "龙娘：让我思考一下人生")
                        setMood("思考")
                    }
                    1 -> {
                        say(if (maidMode) "女仆：休息一下吧"
                            else "龙娘：有点困啦 Zzz")
                        setMood("困倦")
                    }
                    2 -> {
                        maidMode = !maidMode
                        say(if (maidMode) "女仆：我来啦"
                            else "龙娘：我回来啦")
                        setMood("开心")
                    }
                    else -> {
                        say(if (maidMode) "女仆：记得喝水哦"
                            else "龙娘：别忘了活动一下")
                    }
                }
            }

            handler.postDelayed(this, 30_000L)
        }
    }

    private fun applyStyle() {
        val root = petView ?: return
        val p = params ?: return

        root.alpha = petOpacity / 100f

        val width = dp(190 * petSize / 100)
        p.width = width.coerceAtLeast(dp(90))
        try {
            wm.updateViewLayout(root, p)
        } catch (_: Exception) {
        }

        root.requestLayout()
    }

    private fun removePet() {
        handler.removeCallbacks(idleRunnable)
        val root = petView ?: return
        try {
            wm.removeView(root)
        } catch (_: Exception) {
        }
        petView = null
        params = null
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        removePet()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
