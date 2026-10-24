
package com.tangyuanlongniang.pet

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val overlayRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(32, 32, 32, 32)
        root.setBackgroundColor(Color.rgb(255, 245, 235))

        val title = TextView(this)
        title.text = "🐉 汤圆龙娘桌宠"
        title.textSize = 27f
        title.setTextColor(Color.rgb(100, 55, 35))
        title.gravity = Gravity.CENTER
        root.addView(title)

        val desc = TextView(this)
        desc.text = "让龙娘陪你一起玩手机"
        desc.textSize = 16f
        desc.gravity = Gravity.CENTER
        desc.setPadding(0, 24, 0, 24)
        root.addView(desc)

        val startButton = Button(this)
        startButton.text = "开启桌宠"
        root.addView(startButton)

        val stopButton = Button(this)
        stopButton.text = "关闭桌宠"
        root.addView(stopButton)

        startButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, PetService::class.java))
                Toast.makeText(this, "龙娘来啦！", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, overlayRequest)
            }
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, PetService::class.java))
            Toast.makeText(this, "龙娘休息啦", Toast.LENGTH_SHORT).show()
        }

        setContentView(root)
    }

    @Deprecated("Legacy permission callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == overlayRequest &&
            Settings.canDrawOverlays(this)
        ) {
            startService(Intent(this, PetService::class.java))
            Toast.makeText(this, "龙娘来啦！", Toast.LENGTH_SHORT).show()
        }
    }
}
