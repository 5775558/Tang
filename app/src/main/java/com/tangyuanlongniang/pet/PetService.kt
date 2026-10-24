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
import kotlin.math.abs
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
    private lateinit var prefs: android.content.SharedPreferences

    private val handler = Handler(Looper.getMainLooper())

    private var petView: View? = null
    private var collapsed = true
    private var isDragging = false

    private var avatarSize = 64
    private var petAlpha = 100

    // 面板宽高分别占屏幕的百分比
    private var panelWidthPercent = 45
    private var panelHeightPercent = 45

    private var screenWidth = 1080
    private var screenHeight = 1920

    private var windowX = 30
    private var windowY = 180

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downX = 0f
    private var downY = 0f

    private var affection = 0
    private var giftCount = 0
    private var winCount = 0
    private var interactionCount = 0

    private var idleEnabled = true
    private var sleeping = false
    private var lastInteractionTime = System.currentTimeMillis()
    private var idleStage = 0

    private var emotion = "开心"
    private var statusText = "汤圆，我在这里陪你"

    private val random = java.util.Random()

    private val chatLines = listOf(
        "汤圆，你来啦",
        "嘿嘿，终于有人陪我啦",
        "你猜猜我现在是什么心情",
        "今天准备带我去哪里玩呀",
        "我刚刚想到一个有趣的问题",
        "汤圆，休息一下嘛",
        "我在这里陪着你",
        "今天也要开开心心的",
        "让我想想接下来做什么",
        "偷偷问你一个小问题",
        "要不要陪我玩一会儿",
        "我是不是今天也很可爱",
        "有时候发发呆也不错",
        "汤圆，记得照顾好自己",
        "今天也来互动一下吧"
    )

    private val idleRunnable = object : Runnable {
        override fun run() {
            if (idleEnabled && !sleeping) {
                val seconds =
                    (System.currentTimeMillis() - lastInteractionTime) / 1000

                when {
                    seconds >= 120 && idleStage < 3 -> {
                        idleStage = 3
                        emotion = "兴奋"
                        statusText = "想给汤圆跳一支舞"
                        refreshIfExpanded()
                    }

                    seconds >= 60 && idleStage < 2 -> {
                        idleStage = 2
                        emotion = "放松"
                        statusText = "休息一下，放松一下"
                        refreshIfExpanded()
                    }

                    seconds >= 30 && idleStage < 1 -> {
                        idleStage = 1
                        emotion = "认真"
                        statusText = "让我读读书，思考一下"
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

        prefs = getSharedPreferences("tangyuan_pet_settings", MODE_PRIVATE)

        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        avatarSize = prefs.getInt("avatarSize", 64).coerceIn(40, 200)
        petAlpha = prefs.getInt("petAlpha", 100).coerceIn(20, 100)

        panelWidthPercent =
            prefs.getInt("panelWidth", 45).coerceIn(20, 60)

        panelHeightPercent =
            prefs.getInt("panelHeight", 45).coerceIn(20, 60)

        windowX = prefs.getInt("windowX", 30)
        windowY = prefs.getInt("windowY", 180)

        affection = prefs.getInt("affection", 0).coerceAtLeast(0)
        giftCount = prefs.getInt("giftCount", 0).coerceAtLeast(0)
        winCount = prefs.getInt("winCount", 0).coerceAtLeast(0)
        interactionCount =
            prefs.getInt("interactionCount", 0).coerceAtLeast(0)

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
                if (petView == null) showCollapsed()
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

    private fun saveSettings() {
        prefs.edit()
            .putInt("avatarSize", avatarSize)
            .putInt("petAlpha", petAlpha)
            .putInt("panelWidth", panelWidthPercent)
            .putInt("panelHeight", panelHeightPercent)
            .putInt("windowX", windowX)
            .putInt("windowY", windowY)
            .putInt("affection", affection)
            .putInt("giftCount", giftCount)
            .putInt("winCount", winCount)
            .putInt("interactionCount", interactionCount)
            .apply()
    }

    private fun makeWindowParams(
        width: Int,
        height: Int
    ): WindowManager.LayoutParams {
        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        return WindowManager.LayoutParams(
            width,
            height,
            type,
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

        // 窗口宽高在添加之前明确设置，防止意外撑满屏幕
        val safeWidth = width.coerceIn(1, screenWidth)
        val safeHeight = height.coerceIn(1, screenHeight)

        petView = view

        try {
            wm.addView(
                view,
                makeWindowParams(safeWidth, safeHeight)
            )
        } catch (_: Exception) {
            petView = null
            showToast("悬浮窗添加失败，请检查悬浮窗权限")
        }
    }

    private fun removePet() {
        petView?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }

        petView = null
    }

    private fun makeAvatar(): ImageView {
        return ImageView(this).apply {
            setImageResource(
                R.drawable.file_00000000954481f5a909b4bf0ebbc1c7
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = petAlpha / 100f

            setOnTouchListener { _, event ->
                handleTouchCommon(event, true)
            }
        }
    }

    private fun showCollapsed() {
        collapsed = true

        val size = dp(avatarSize)
        val avatar = makeAvatar()

        install(avatar, size, size)
        saveSettings()
    }

    private fun showExpanded() {
        collapsed = false

        // 面板宽高均可调整，但最大各占屏幕的 60%
        val maxWidth = max(1, (screenWidth * 0.60f).toInt())
        val maxHeight = max(1, (screenHeight * 0.60f).toInt())

        val width = min(
            maxWidth,
            max(dp(220), (screenWidth * panelWidthPercent / 100f).toInt())
        )

        val height = min(
            maxHeight,
            max(dp(240), (screenHeight * panelHeightPercent / 100f).toInt())
        )

        val card = buildCard()

        install(card, width, height)
        saveSettings()
    }

    private fun buildCard(): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = roundedBackground(
                Color.argb(242, 255, 245, 250),
                dp(16).toFloat()
            )
            alpha = petAlpha / 100f
        }

        val title = TextView(this).apply {
            text = "汤圆龙娘 🐉"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(160, 70, 120))
            setPadding(0, dp(4), 0, dp(4))
        }
        outer.addView(title)

        val avatar = makeAvatar()
        avatar.setOnTouchListener { _, event ->
            handleTouchCommon(event, false)
        }

        outer.addView(
            avatar,
            LinearLayout.LayoutParams(
                min(dp(avatarSize), dp(150)),
                min(dp(avatarSize), dp(150))
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        val info = TextView(this).apply {
            text = "$emotion · 好感度 $affection · ${affectionLevel()}"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(130, 65, 100))
        }
        outer.addView(info)

        val status = TextView(this).apply {
            text = statusText
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(6))
        }
        outer.addView(status)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        addButton(controls, "💬 随机聊天") {
            chat()
        }

        addButton(controls, "🥤 喝水") {
            interact("喝水", "汤圆，记得喝点水呀")
        }

        addButton(controls, "🍰 吃点心") {
            interact("开心", "一起吃点小点心吧")
        }

        addButton(controls, "💃 跳舞") {
            interact("兴奋", "龙娘开始表演啦")
            animatePet()
        }

        addButton(controls, "😴 睡觉 / 叫醒") {
            sleeping = !sleeping

            if (sleeping) {
                emotion = "困倦"
                statusText = "晚安，汤圆，我先睡一会儿"
                showToast(statusText)
            } else {
                emotion = "开心"
                statusText = "汤圆把我叫醒啦"
                touchInteraction()
                showToast(statusText)
            }

            refreshPet()
        }

        addButton(controls, "❤️ 摸摸头") {
            affection += 2
            interactionCount++
            emotion = "害羞"
            statusText = listOf(
                "嘿嘿，不要一直摸啦",
                "再摸一下也不是不行",
                "汤圆对我真好",
                "我现在有点开心"
            ).random()

            touchInteraction()
            saveSettings()
            showToast(statusText)
            refreshPet()
        }

        addButton(controls, "🎁 送礼物") {
            giftCount++
            affection += 5
            emotion = "开心"
            statusText = listOf(
                "谢谢汤圆送我的礼物",
                "我会好好收藏起来",
                "收到礼物真的很开心",
                "下次还可以送我吗"
            ).random()

            touchInteraction()
            saveSettings()
            showToast("$statusText（礼物数：$giftCount）")
            refreshPet()
        }

        addButton(controls, "✊ 猜拳小游戏") {
            playRockPaperScissors()
        }

        addButton(controls, "🎲 幸运抽签") {
            val rewards = listOf(
                "获得 1 点好感度",
                "今天运气不错",
                "龙娘送你一个拥抱",
                "获得 3 点好感度",
                "今天适合休息一下"
            )

            val result = rewards.random()

            if (result.contains("3 点")) affection += 3
            else if (result.contains("1 点")) affection++

            statusText = result
            emotion = "兴奋"
            touchInteraction()
            saveSettings()
            showToast(result)
            refreshPet()
        }

        addButton(controls, "📖 阅读 / 思考") {
            emotion = "认真"
            statusText = "正在认真思考中……"
            touchInteraction()
            showToast(statusText)
            refreshPet()
        }

        addButton(controls, "📊 查看我的龙娘") {
            statusText =
                "等级：${affectionLevel()} · 好感度：$affection · 互动：$interactionCount · 胜场：$winCount"
            showToast(statusText)
            refreshPet()
        }

        addButton(controls, "🔄 重置闲置计时") {
            touchInteraction()
            emotion = "开心"
            statusText = "重新开始陪伴啦"
            showToast(statusText)
            refreshPet()
        }

        val avatarLabel = TextView(this).apply {
            text = "头像大小：${avatarSize}dp"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }
        controls.addView(avatarLabel)

        val avatarSeek = SeekBar(this).apply {
            max = 160
            progress = (avatarSize - 40).coerceIn(0, 160)
        }
        controls.addView(avatarSeek)

        avatarSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        avatarSize = 40 + progress
                        avatarLabel.text = "头像大小：${avatarSize}dp"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    touchInteraction()
                    saveSettings()
                    refreshPet()
                }
            }
        )

        val widthLabel = TextView(this).apply {
            text = "面板宽度：${panelWidthPercent}%"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }
        controls.addView(widthLabel)

        val widthSeek = SeekBar(this).apply {
            max = 40
            progress = panelWidthPercent - 20
        }
        controls.addView(widthSeek)

        widthSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        panelWidthPercent = 20 + progress
                        widthLabel.text = "面板宽度：${panelWidthPercent}%"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    touchInteraction()
                    saveSettings()
                    refreshPet()
                }
            }
        )

        val heightLabel = TextView(this).apply {
            text = "面板高度：${panelHeightPercent}%"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }
        controls.addView(heightLabel)

        val heightSeek = SeekBar(this).apply {
            max = 40
            progress = panelHeightPercent - 20
        }
        controls.addView(heightSeek)

        heightSeek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        panelHeightPercent = 20 + progress
                        heightLabel.text = "面板高度：${panelHeightPercent}%"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    touchInteraction()
                    saveSettings()
                    refreshPet()
                }
            }
        )

        val alphaLabel = TextView(this).apply {
            text = "透明度：${petAlpha}%"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }
        controls.addView(alphaLabel)

        val alphaSeek = SeekBar(this).apply {
            max = 80
            progress = petAlpha - 20
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
                    saveSettings()
                    refreshPet()
                }
            }
        )

        addButton(controls, "收起桌宠") {
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
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            minHeight = dp(34)

            setOnClickListener {
                action()
            }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )
    }

    private fun interact(newEmotion: String, message: String) {
        emotion = newEmotion
        statusText = message
        affection++
        interactionCount++

        touchInteraction()
        saveSettings()
        showToast(message)
        refreshIfExpanded()
    }

    private fun chat() {
        statusText = chatLines.random()
        emotion = listOf("开心", "好奇", "害羞", "傲娇").random()

        affection++
        interactionCount++

        touchInteraction()
        saveSettings()
        showToast(statusText)
        refreshIfExpanded()
    }

    private fun playRockPaperScissors() {
        val choices = listOf("石头", "剪刀", "布")
        val petChoice = choices.random()

        // 随机模拟用户选择，后续可以改成真正的选择界面
        val userChoice = choices.random()

        val result = when {
            petChoice == userChoice -> "平局"
            (userChoice == "石头" && petChoice == "剪刀") ||
                (userChoice == "剪刀" && petChoice == "布") ||
                (userChoice == "布" && petChoice == "石头") -> "你赢啦"
            else -> "龙娘赢啦"
        }

        if (result == "你赢啦") {
            affection += 3
            winCount++
        } else if (result == "平局") {
            affection++
        }

        emotion = if (result == "你赢啦") "兴奋" else "傲娇"
        statusText = "你出$userChoice，龙娘出$petChoice，$result"

        interactionCount++
        touchInteraction()
        saveSettings()
        showToast(statusText)
        refreshIfExpanded()
    }

    private fun affectionLevel(): String {
        return when {
            affection >= 100 -> "灵魂搭档"
            affection >= 60 -> "亲密伙伴"
            affection >= 30 -> "默契搭档"
            affection >= 10 -> "熟悉伙伴"
            else -> "初次相遇"
        }
    }

    private fun animatePet() {
        val view = petView ?: return

        view.animate()
            .rotationBy(360f)
            .setDuration(700)
            .withEndAction {
                view.rotation = 0f
            }
            .start()
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
                    abs(event.rawX - downX) > dp(5) ||
                    abs(event.rawY - downY) > dp(5)
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
                            saveSettings()
                        } catch (_: Exception) {
                        }
                    }
                }

                lastTouchX = event.rawX
                lastTouchY = event.rawY
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dragged = isDragging
                isDragging = false

                if (!dragged && toggleOnTap) {
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
        if (collapsed) {
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
        sleeping = false
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
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(idleRunnable)
        saveSettings()
        removePet()
        super.onDestroy()
    }
}
