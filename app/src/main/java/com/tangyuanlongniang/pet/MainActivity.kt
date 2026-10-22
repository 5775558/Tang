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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(dp(24), dp(24), dp(24), dp(24))
        layout.setBackgroundColor(Color.rgb(255, 245, 235))

        val title = TextView(this)
        title.text = "汤圆龙娘桌宠"
        title.textSize = 28f
        title.setTextColor(Color.rgb(220, 100, 60))
        title.gravity = Gravity.CENTER

        val description = TextView(this)
        description.text = "让橙发少女陪你看书、思考和休息"
        description.textSize = 16f
        description.gravity = Gravity.CENTER
        description.setTextColor(Color.DKGRAY)

        val startButton = Button(this)
        startButton.text = "开启桌宠"

        startButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, PetService::class.java))
                Toast.makeText(this, "正在开启桌宠", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        val tip = TextView(this)
        tip.text = "首次使用需要允许应用显示在其他应用上层"
        tip.textSize = 14f
        tip.gravity = Gravity.CENTER
        tip.setTextColor(Color.GRAY)

        layout.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        layout.addView(
            description,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        layout.addView(
            startButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60)
            )
        )

        layout.addView(
            tip,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(70)
            )
        )

        setContentView(layout)
    }
}
