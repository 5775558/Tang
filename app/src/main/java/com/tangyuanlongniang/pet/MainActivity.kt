
package com.tangyuanlongniang.pet

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.text.InputType

class MainActivity : Activity() {

    private val overlayRequest = 1001
    private lateinit var apiKeyInput: EditText

    private val prefs by lazy {
        getSharedPreferences("pet", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(255, 245, 235))
        }

        val title = TextView(this).apply {
            text = "🐉 汤圆龙娘桌宠"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(100, 55, 35))
        }
        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val description = TextView(this).apply {
            text = "让龙娘陪你聊天、互动和玩耍"
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        root.addView(description)

        apiKeyInput = EditText(this).apply {
            hint = "粘贴 DeepSeek API Key"
            inputType =
                InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
            setText(prefs.getString("api_key", "").orEmpty())
        }
        root.addView(apiKeyInput)

        addButton(root, "保存 API Key") {
            val key = apiKeyInput.text.toString().trim()
            if (key.isBlank()) {
                showToast("请先填写 API Key")
            } else {
                prefs.edit().putString("api_key", key).apply()
                showToast("API Key 已保存在本机")
            }
        }

        addButton(root, "和龙娘聊天") {
            val key = apiKeyInput.text.toString().trim()
            if (key.isBlank()) {
                showToast("请先填写并保存 API Key")
            } else {
                prefs.edit().putString("api_key", key).apply()
                startActivity(Intent(this, ChatActivity::class.java))
            }
        }

        addButton(root, "开启桌宠") {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, PetService::class.java))
                showToast("龙娘来啦！")
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, overlayRequest)
            }
        }

        addButton(root, "关闭桌宠") {
            stopService(Intent(this, PetService::class.java))
            showToast("龙娘休息啦")
        }

        val scroll = ScrollView(this).apply {
            addView(root)
        }
        setContentView(scroll)
    }

    private fun addButton(
        parent: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Legacy permission callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == overlayRequest &&
            Settings.canDrawOverlays(this)
        ) {
            startService(Intent(this, PetService::class.java))
            showToast("龙娘来啦！")
        }
    }
}
