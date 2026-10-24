
package com.tangyuanlongniang.pet

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeepSeekClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun chat(
        key: String,
        history: List<Pair<String, String>>
    ): String {
        require(key.isNotBlank()) {
            "请先填写 DeepSeek API Key"
        }

        val messages = JSONArray()

        messages.put(
            JSONObject()
                .put("role", "system")
                .put(
                    "content",
                    "你是汤圆的龙娘，使用自然亲切的中文交流。" +
                        "称呼用户为汤圆，温柔、有趣、真诚地回答。"
                )
        )

        for ((role, content) in history) {
            if (role == "user" || role == "assistant") {
                messages.put(
                    JSONObject()
                        .put("role", role)
                        .put("content", content)
                )
            }
        }

        val requestJson = JSONObject()
            .put("model", "deepseek-flash")
            .put("messages", messages)
            .put("stream", false)
            .put(
                "thinking",
                JSONObject().put("type", "disabled")
            )

        val body = requestJson.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .header("Authorization", "Bearer ${key.trim()}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val detail = try {
                    JSONObject(responseText)
                        .optJSONObject("error")
                        ?.optString("message")
                        .orEmpty()
                } catch (_: Exception) {
                    ""
                }

                throw IllegalStateException(
                    "请求失败，HTTP ${response.code}" +
                        if (detail.isNotBlank()) "：$detail"
                        else "。请检查 API Key、账户额度和模型权限。"
                )
            }

            val json = JSONObject(responseText)
            val choices = json.optJSONArray("choices")
                ?: throw IllegalStateException("接口没有返回回答")

            if (choices.length() == 0) {
                throw IllegalStateException("接口返回了空回答")
            }

            return choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content")
                .takeIf { it.isNotBlank() }
                ?: "汤圆，我这次没有收到文字回答，我们再试一次吧。"
        }
    }
}
