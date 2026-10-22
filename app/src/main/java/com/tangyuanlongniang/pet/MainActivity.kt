
package com.tangyuanlongniang.pet

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("pet_data", MODE_PRIVATE)
    }

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) =
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(Color.rgb(255, 245, 235))
        }

        fun text(s: String, size: Float = 16f): TextView {
            return TextView(this).apply {
                text = s
                textSize = size
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(100, 55, 35))
                setPadding(0, dp(8), 0, dp(8))
            }
        }

        fun button(s: String, action: () -> Unit) {
            val b = Button(this)
            b.text = s
            b.setOnClickListener { action() }
            root.addView(b, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
            ))
        }

        root.addView(text("🐉 汤圆龙娘桌宠", 26f))
        root.addView(text("陪你看书、休息和专注"))

        val stats = text("")
        root.addView(stats)

        fun refreshStats() {
            val affection = prefs.getInt("affection", 0)
            val done = prefs.getBoolean("daily_done", false)
            val focusEnd = prefs.getLong("focus_end", 0L)
            val remaining = ((focusEnd - System.currentTimeMillis())
                .coerceAtLeast(0L)) / 1000

            stats.text = "好感度：$affection\n每日互动：${if (done) "已完成" else "待完成"}\n" +
                "专注剩余：${remaining / 60}分${remaining % 60}秒"
        }

        button("开启 / 显示桌宠") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else {
                val intent = Intent(this, PetService::class.java)
                intent.action = "SHOW"
                intent.putExtra("size", prefs.getInt("size", 100))
                intent.putExtra("opacity", prefs.getInt("opacity", 100))
                try {
                    startService(intent)
                    toast("已发送桌宠启动请求")
                } catch (e: Exception) {
                    toast("启动失败，请检查服务代码")
                }
            }
        }

        button("随机互动 + 好感度") {
            val lines = listOf(
                "龙娘：今天也要加油哦",
                "龙娘：再看我就要害羞啦",
                "女仆：主人今天辛苦了",
                "龙娘：陪我看一会儿书吧",
                "女仆：要不要休息一下呀"
            )
            val affection = prefs.getInt("affection", 0) + 1
            prefs.edit().putInt("affection", affection).apply()
            AlertDialog.Builder(this)
                .setTitle("今日互动")
                .setMessage(lines.random() + "\n好感度 +1")
                .setPositiveButton("好呀") { _, _ -> refreshStats() }
                .show()
        }

        button("领取每日互动任务") {
            val today = java.text.SimpleDateFormat(
                "yyyyMMdd", java.util.Locale.US
            ).format(java.util.Date())

            if (prefs.getString("task_date", "") != today) {
                prefs.edit()
                    .putString("task_date", today)
                    .putBoolean("daily_done", false)
                    .apply()
            }

            if (prefs.getBoolean("daily_done", false)) {
                toast("今天已经领取过奖励啦")
            } else {
                val affection = prefs.getInt("affection", 0) + 5
                prefs.edit()
                    .putInt("affection", affection)
                    .putBoolean("daily_done", true)
                    .apply()
                toast("任务完成，好感度 +5")
            }
            refreshStats()
        }

        button("开始 25 分钟专注") {
            val end = System.currentTimeMillis() + 25 * 60 * 1000L
            prefs.edit().putLong("focus_end", end).apply()
            toast("专注开始，放下手机认真做事吧")
            refreshStats()
        }

        button("结束专注") {
            prefs.edit().putLong("focus_end", 0L).apply()
            toast("已结束专注")
            refreshStats()
        }

        root.addView(text("桌宠大小"))
        val sizeBar = SeekBar(this).apply {
            max = 150
            progress = (prefs.getInt("size", 100) - 50).coerceIn(0, 150)
        }
        root.addView(sizeBar)

        root.addView(text("桌宠透明度"))
        val opacityBar = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("opacity", 100).coerceIn(20, 100)
        }
        root.addView(opacityBar)

        button("保存大小和透明度") {
            val size = 50 + sizeBar.progress
            val opacity = opacityBar.progress.coerceAtLeast(20)
            prefs.edit()
                .putInt("size", size)
                .putInt("opacity", opacity)
                .apply()

            val intent = Intent(this, PetService::class.java).apply {
                action = "UPDATE_STYLE"
                putExtra("size", size)
                putExtra("opacity", opacity)
            }
            try {
                if (Settings.canDrawOverlays(this)) startService(intent)
                toast("设置已保存")
            } catch (_: Exception) {
                toast("已保存设置，下次开启时生效")
            }
        }

        button("切换角色台词") {
            val lines = listOf(
                "🐉 龙娘：今天由我陪你",
                "🩵 女仆：我也想和你一起玩",
                "🐉 龙娘：不许只看女仆哦",
                "🩵 女仆：要不要一起跳舞？"
            )
            AlertDialog.Builder(this)
                .setTitle("双角色互动")
                .setMessage(lines.random())
                .setPositiveButton("继续") { _, _ -> }
                .show()
        }

        button("隐藏 / 退出桌宠") {
            AlertDialog.Builder(this)
                .setTitle("桌宠控制")
                .setItems(arrayOf("隐藏桌宠", "退出桌宠服务")) { _, which ->
                    if (which == 0) {
                        val i = Intent(this, PetService::class.java)
                        i.action = "HIDE"
                        try {
                            startService(i)
                        } catch (_: Exception) {
                            toast("操作失败")
                        }
                    } else {
                        stopService(Intent(this, PetService::class.java))
                        toast("桌宠已退出")
                    }
                }
                .show()
        }

        refreshStats()
        setContentView(root)
    }
}
