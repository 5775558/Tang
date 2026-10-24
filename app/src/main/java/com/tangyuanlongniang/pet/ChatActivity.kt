package com.tangyuanlongniang.pet

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import java.util.concurrent.Executors

class ChatActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<Pair<String, String>>()
    private lateinit var log: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        root.addView(TextView(this).apply {
            text = "汤圆和龙娘聊天"
            textSize = 24f
        })

        log = TextView(this).apply {
            text = "汤圆，我在这里。先在主界面保存 DeepSeek API Key，就可以聊天啦。"
            textSize = 16f
        }

        root.addView(
            ScrollView(this).apply { addView(log) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        input = EditText(this).apply {
            hint = "跟龙娘说点什么……"
        }
        root.addView(input)

        root.addView(Button(this).apply {
            text = "发送"
            setOnClickListener { sendMessage() }
        })

        setContentView(root)
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        input.setText("")
        log.append("\n\n汤圆：$message\n\n龙娘：正在思考……")

        val key = getSharedPreferences("pet", 0)
            .getString("api_key", "").orEmpty()

        if (key.isBlank()) {
            log.append("\n请先回主界面保存 DeepSeek API Key。")
            return
        }

        history.add("user" to message)

        executor.execute {
            try {
                val answer = DeepSeekClient.chat(key, history.toList())
                history.add("assistant" to answer)
                runOnUiThread {
                    log.append("\n$answer")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    log.append("\n请求失败：${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
