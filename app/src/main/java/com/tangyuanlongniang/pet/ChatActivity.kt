package com.tangyuanlongniang.pet

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.concurrent.Executors

class ChatActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<Pair<String, String>>()

    private lateinit var log: TextView
    private lateinit var input: EditText
    private lateinit var personaSpinner: Spinner
    private lateinit var effortSpinner: Spinner
    private lateinit var sendButton: Button
    private lateinit var balanceButton: Button
    private lateinit var scroll: ScrollView

    private val prefs by lazy {
        getSharedPreferences("pet", MODE_PRIVATE)
    }

    private val personas = listOf(
        "普通龙娘",
        "呆萌弱智",
        "活泼搞笑",
        "认真严谨"
    )

    private val efforts = listOf(
        "关闭思考",
        "低度思考",
        "深度思考",
        "极限思考"
    )

    private val effortValues = listOf(
        "none",
        "low",
        "high",
        "max"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.rgb(255, 245, 250))
        }

        root.addView(TextView(this).apply {
            text = "🐉 汤圆和龙娘"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(160, 65, 115))
        })

        root.addView(TextView(this).apply {
            text = "角色风格"
            textSize = 14f
        })

        personaSpinner = Spinner(this)
        personaSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            personas
        )

        val savedPersona = prefs.getString("persona", "普通龙娘")
        personaSpinner.setSelection(
            personas.indexOf(savedPersona).coerceAtLeast(0)
        )
        root.addView(personaSpinner)

        root.addView(TextView(this).apply {
            text = "思考强度"
            textSize = 14f
        })

        effortSpinner = Spinner(this)
        effortSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            efforts
        )

        val savedEffort = prefs.getString("thinking_mode", "high")
        effortSpinner.setSelection(
            effortValues.indexOf(savedEffort).coerceAtLeast(0)
        )
        root.addView(effortSpinner)

        root.addView(Button(this).apply {
            text = "保存当前设置"
            isAllCaps = false

            setOnClickListener {
                saveSettings()
                appendText("\n设置已保存")
            }
        })

        log = TextView(this).apply {
            text = "汤圆，我在这里。你可以分别调节角色风格和思考强度。"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, 12, 0, 12)
        }

        scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(log)
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        input = EditText(this).apply {
            hint = "跟龙娘说点什么……"
            minLines = 1
            maxLines = 4
        }
        root.addView(input)

        sendButton = Button(this).apply {
            text = "发送消息"
            isAllCaps = false
            setOnClickListener { sendMessage() }
        }
        root.addView(sendButton)

        balanceButton = Button(this).apply {
            text = "查看 API 余额"
            isAllCaps = false
            setOnClickListener { queryBalance() }
        }
        root.addView(balanceButton)

        setContentView(root)
    }

    private fun currentKey(): String {
        return prefs.getString("api_key", "").orEmpty().trim()
    }

    private fun currentEffort(): String {
        return effortValues.getOrElse(
            effortSpinner.selectedItemPosition
        ) { "high" }
    }

    private fun currentPersona(): String {
        return personas.getOrElse(
            personaSpinner.selectedItemPosition
        ) { "普通龙娘" }
    }

    private fun saveSettings() {
        prefs.edit()
            .putString("thinking_mode", currentEffort())
            .putString("persona", currentPersona())
            .apply()
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return

        val key = currentKey()
        if (key.isBlank()) {
            appendText("\n请先回主界面保存 API Key。")
            return
        }

        saveSettings()

        val effort = currentEffort()
        val persona = currentPersona()

        history.add("user" to message)

        appendText(
            "\n\n汤圆：$message\n" +
                "龙娘（$persona / ${efforts[effortSpinner.selectedItemPosition]}）：思考中……"
        )

        input.setText("")
        sendButton.isEnabled = false
        balanceButton.isEnabled = false

        executor.execute {
            try {
                val answer = DeepSeekClient.chat(
                    key = key,
                    history = history.toList(),
                    effort = effort,
                    persona = persona
                )

                history.add("assistant" to answer)

                runOnUiThread {
                    appendText("\n$answer")
                    sendButton.isEnabled = true
                    balanceButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendText("\n请求失败：${e.message ?: "未知错误"}")
                    sendButton.isEnabled = true
                    balanceButton.isEnabled = true
                }
            }
        }
    }

    private fun queryBalance() {
        val key = currentKey()

        if (key.isBlank()) {
            appendText("\n请先回主界面保存 API Key。")
            return
        }

        balanceButton.isEnabled = false
        appendText("\n\n正在查询账户余额……")

        executor.execute {
            try {
                val result = DeepSeekClient.getBalance(key)

                runOnUiThread {
                    appendText("\n$result")
                    balanceButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendText("\n余额查询失败：${e.message ?: "未知错误"}")
                    balanceButton.isEnabled = true
                }
            }
        }
    }

    private fun appendText(text: String) {
        log.append(text)
        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
