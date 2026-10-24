package com.tangyuanlongniang.pet

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

class PetService : Service() {

    companion object {
        const val ACTION_SHOW = "com.tangyuanlongniang.pet.SHOW"
        const val ACTION_HIDE = "com.tangyuanlongniang.pet.HIDE"
        const val ACTION_UPDATE_STYLE =
            "com.tangyuanlongniang.pet.UPDATE_STYLE"
    }

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var petView: View? = null
    private var collapsed = true
    private var isDragging = false

    private var avatarSize = 64
    private var petAlpha = 100

    private var screenWidth = 1080
    private var screenHeight = 1920

    private var windowX = 30
    private var windowY = 180

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downX = 0f
    private var downY = 0f

    private var idleEnabled = true
    private var lastInteractionTime = System.currentTimeMillis()
    private var idleStage = 0

    private var statusText = "汤圆，我在这里陪你"

    private val idleRunnable = object : Runnable {
        override fun run() {
            if (idleEnabled) {
                val idleSeconds =
                    (System.currentTimeMillis() - lastInteractionTime) / 1000

                when {
                    idleSeconds >= 120 && idleStage < 3 -> {
                        idleStage = 3
                        statusText = "转个圈，活动一下！"
                        showToast("龙娘开始跳舞啦")
                        refreshIfExpanded()
                    }

                    idleSeconds >= 60 && idleStage < 2 -> {
                        idleStage = 2
                        statusText = "休息一下，扇扇风"
                        refreshIfExpanded()
                    }

                    idleSeconds >= 30 && idleStage < 1 -> {
                        idleStage = 1
                        statusText = "让我看看书，思考一下"
                        refreshIfExpanded()
                    }
                }
            }

            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        // 默认头像尺寸随屏幕调整，但不超过屏幕宽度的 12%
        val maxDefaultSize = max(40, (screenWidth * 0.12f).toInt())
        avatarSize = min(avatarSize, maxDefaultSize)

        showCollapsed()
        handler.postDelayed(idleRunnable, 5000)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_HIDE -> removePet()
            ACTION_SHOW -> {
                if (petView == null) {
                    showCollapsed()
                }
            }
            ACTION_UPDATE_STYLE -> {
                if (petView == null) {
                    showCollapsed()
                } else {
                    refreshPet()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun makeWindowParams(width: Int, height: Int): WindowManager.LayoutParams {
        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        return WindowManager.LayoutParams(
            width,
            height,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowX
            y = windowY
        }
    }

    private fun install(view: View, width: Int, height: Int) {
        removePet()

        // 关键：添加窗口之前，就先设置明确的宽高
        val params = makeWindowParams(width, height)
        petView = view

        try {
            wm.addView(view, params)
        } catch (e: Exception) {
            petView = null
            showToast("悬浮窗添加失败，请检查悬浮窗权限")
        }
    }

    private fun removePet() {
        val old = petView
        if (old != null) {
            try {
                wm.removeView(old)
            } catch (_: Exception) {
            }
        }
        petView = null
    }

    private fun makeAvatar(): ImageView {
        return ImageView(this).apply {
            // 使用你上传的龙娘图片
            setImageResource(
                R.drawable.file_00000000954481f5a909b4bf0ebbc1c7
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = petAlpha / 100f
            setOnTouchListener { _, event ->
                handleAvatarTouch(event)
            }
        }
    }

    private fun showCollapsed() {
        collapsed = true

        val sizePx = dp(avatarSize)
        val avatar = makeAvatar()

        // 窗口尺寸与头像尺寸一致，不使用 WRAP_CONTENT
        install(avatar, sizePx, sizePx)
    }

    private fun showExpanded() {
        collapsed = false

        val card = buildCard()

        // 展开窗口最大不超过屏幕宽、高的 30%
        val maxWidth = max(1, (screenWidth * 0.30f).toInt())
        val maxHeight = max(1, (screenHeight * 0.30f).toInt())

        // 内容在固定尺寸窗口中排列，超出部分由滚动区域处理
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        install(card, maxWidth, maxHeight)
    }

    private fun buildCard(): View {
        val maxWidth = max(1, (screenWidth * 0.30f).toInt())

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(
                Color.argb(238, 255, 245, 250),
                dp(16).toFloat()
            )
            alpha = petAlpha / 100f
        }

        val title = TextView(this).apply {
            text = "汤圆龙娘"
            textSize = 16f
            setTextColor(Color.rgb(160, 70, 120))
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(6))
        }
        outer.addView(title)

        val avatar = makeAvatar().apply {
            setOnTouchListener { _, event ->
                handleCardTouch(event)
            }
        }

        val avatarRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
        }

        avatarRow.addView(
            avatar,
            LinearLayout.LayoutParams(
                min(dp(avatarSize), maxWidth - dp(20)),
                min(dp(avatarSize), maxWidth - dp(20))
            )
        )

        outer.addView(avatarRow)

        val status = TextView(this).apply {
            text = statusText
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(4))
        }
        outer.addView(status)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        addButton(controls, "喝水", "喝水时间到啦")
        addButton(controls, "吃饭", "汤圆，记得按时吃饭")
        addButton(controls, "跳舞", "龙娘跳一段给你看")
        addButton(controls, "睡觉", "晚安，汤圆")

        addButton(controls, "互动", "今天也来陪龙娘聊天吧")

        val sizeLabel = TextView(this).apply {
            text = "桌宠大小：${avatarSize}"
            textSize = 11f
            setTextColor(Color.DKGRAY)
        }
        controls.addView(sizeLabel)

        val sizeSeek = SeekBar(this).apply {
            max = 160
            progress = (avatarSize - 40).coerceIn(0, 160)
        }
        controls.addView(sizeSeek)

        sizeSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        avatarSize = 40 + progress
                        sizeLabel.text = "桌宠大小：$avatarSize"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    touchInteraction()
                    refreshPet()
                }
            }
        )

        val alphaLabel = TextView(this).apply {
            text = "透明度：${petAlpha}%"
            textSize = 11f
            setTextColor(Color.DKGRAY)
        }
        controls.addView(alphaLabel)

        val alphaSeek = SeekBar(this).apply {
            max = 80
            progress = (petAlpha - 20).coerceIn(0, 80)
        }
        controls.addView(alphaSeek)

        alphaSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        petAlpha = 20 + progress
                        alphaLabel.text = "透明度：${petAlpha}%"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    touchInteraction()
                    refreshPet()
                }
            }
        )

        addButton(controls, "收起桌宠", "收起来啦") {
            showCollapsed()
        }

        scroll.addView(controls)

        outer.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        return outer
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        message: String,
        action: (() -> Unit)? = null
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            setPadding(dp(2), 0, dp(2), 0)
            minHeight = dp(32)
            setOnClickListener {
                touchInteraction()
                statusText = message
                showToast(message)

                if (action != null) {
                    action()
                } else {
                    refreshIfExpanded()
                }
            }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
            )
        )
    }

    private fun handleAvatarTouch(event: MotionEvent): Boolean {
        return handleTouchCommon(event, true)
    }

    private fun handleCardTouch(event: MotionEvent): Boolean {
        return handleTouchCommon(event, false)
    }

    private fun handleTouchCommon(
        event: MotionEvent,
        toggleOnTap: Boolean
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                downX = event.rawX
                downY = event.rawY
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastTouchX
                val dy = event.rawY - lastTouchY

                if (
                    kotlin.math.abs(event.rawX - downX) > dp(5) ||
                    kotlin.math.abs(event.rawY - downY) > dp(5)
                ) {
                    isDragging = true
                }

                if (isDragging) {
                    windowX += dx.toInt()
                    windowY += dy.toInt()

                    petView?.let { view ->
                        try {
                            val params =
                                view.layoutParams as WindowManager.LayoutParams
                            params.x = windowX.coerceIn(
                                0,
                                max(0, screenWidth - view.width)
                            )
                            params.y = windowY.coerceIn(
                                0,
                                max(0, screenHeight - view.height)
                            )
                            wm.updateViewLayout(view, params)
                        } catch (_: Exception) {
                        }
                    }
                }

                lastTouchX = event.rawX
                lastTouchY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP -> {
                val wasDragging = isDragging
                isDragging = false

                if (!wasDragging && toggleOnTap) {
                    touchInteraction()
                    if (collapsed) {
                        showExpanded()
                    } else {
                        showCollapsed()
                    }
                }
                return true
            }
        }

        return true
    }

    private fun refreshPet() {
        val wasCollapsed = collapsed
        if (wasCollapsed) {
            showCollapsed()
        } else {
            showExpanded()
        }
    }

    private fun refreshIfExpanded() {
        if (!collapsed) {
            showExpanded()
        }
    }

    private fun touchInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        idleStage = 0
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleRunnable)
        removePet()
        super.onDestroy()
    }
}
