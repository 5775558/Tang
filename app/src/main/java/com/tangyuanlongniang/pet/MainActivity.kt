
package com.tangyuanlongniang.pet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(24), dp(24), dp(24))
                setBackgroundColor(Color.rgb(255, 245, 235))
            }

            val title = TextView(this).apply {
                text = "汤圆龙娘桌宠"
                textSize = 28f
                setTextColor(Color.rgb(220, 100, 60))
                gravity = Gravity.CENTER
            }

            val description = TextView(this).apply {
                text = "让橙发少女陪你看书、思考和休息"
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
            }

            val startButton = Button(this).apply {
                text = "开启桌宠"
                setOnClickListener {
                    try {
                        if (Settings.canDrawOverlays(this@MainActivity)) {
                            startService(
                                Intent(this@MainActivity, PetService::class.java)
                            )
                            Toast.makeText(
                                this@MainActivity,
                                "已发送启动请求",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "启动失败：" + (e.message ?: "请检查权限和服务代码"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            val tip = TextView(this).apply {
                text = "首次使用需要允许应用显示在其他应用上层"
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
            }

            layout.addView(title, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)
            ))
            layout.addView(description, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60)
            ))
            layout.addView(startButton, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60)
            ))
            layout.addView(tip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)
            ))

            setContentView(layout)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "主界面加载失败：" + (e.message ?: "未知错误"),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}
