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
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun chat(
        key: String,
        history: List<Pair<String, String>>,
        effort: String,
        persona: String
    ): String {
        require(key.isNotBlank()) { "请先填写 API Key" }

        val validEffort = when (effort) {
            "none", "low", "high", "max" -> effort
            else -> "high"
        }

        val personality = when (persona) {
            "呆萌弱智" ->
                "你正在扮演一个故意呆萌、迷糊、可爱、有点傻乎乎的龙娘。" +
                "说话简单直白，偶尔犯迷糊，可以幽默搞笑，但不要故意给出危险或虚假的信息。"

            "活泼搞笑" ->
                "你是一个活泼、幽默、爱开玩笑的龙娘。回答自然，适当调侃，不要过度油腻。"

            "认真严谨" ->
                "你是一个认真严谨的龙娘。分析问题时注重逻辑、证据、步骤和不确定性。"

            else ->
                "你是汤圆的龙娘。使用自然、温柔、亲切的中文交流，称呼用户为汤圆。"
        }

        val messages = JSONArray()

        messages.put(
            JSONObject()
                .put("role", "system")
                .put(
                    "content",
                    "$personality\n" +
                    "请使用中文回答。不要声称自己是真人。" +
                    "如果问题复杂，认真分析后再给出清楚的结论。"
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
                JSONObject().put(
                    "type",
                    if (validEffort == "none") "disabled" else "enabled"
                )
            )
            .put("reasoning_effort", validEffort)

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
            val text = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val detail = try {
                    JSONObject(text)
                        .optJSONObject("error")
                        ?.optString("message")
                        .orEmpty()
                } catch (_: Exception) {
                    ""
                }

                throw IllegalStateException(
                    "聊天请求失败 HTTP ${response.code}" +
                    if (detail.isNotBlank()) "：$detail"
                    else "，请检查 API Key、模型权限和账户额度"
                )
            }

            val json = JSONObject(text)
            val choices = json.optJSONArray("choices")
                ?: throw IllegalStateException("接口没有返回回答")

            if (choices.length() == 0) {
                throw IllegalStateException("接口返回了空回答")
            }

            return choices.getJSONObject(0)
                .getJSONObject("message")
                .optString("content")
                .ifBlank { "汤圆，我这次没有收到回答，我们再试一次吧。" }
        }
    }

    fun getBalance(key: String): String {
        require(key.isNotBlank()) { "请先填写 API Key" }

        val request = Request.Builder()
            .url("https://api.deepseek.com/user/balance")
            .header("Authorization", "Bearer ${key.trim()}")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "余额查询失败 HTTP ${response.code}，请检查 API Key"
                )
            }

            val json = JSONObject(text)
            val balances = json.optJSONArray("balance_infos")
                ?: throw IllegalStateException("没有找到余额信息")

            val result = StringBuilder()
            result.append("账户额度是否充足：")
                .append(if (json.optBoolean("is_available")) "是" else "否")

            for (i in 0 until balances.length()) {
                val item = balances.getJSONObject(i)

                result.append("\n\n币种：")
                    .append(item.optString("currency"))
                result.append("\n总余额：")
                    .append(item.optString("total_balance"))
                result.append("\n赠送余额：")
                    .append(item.optString("granted_balance"))
                result.append("\n充值余额：")
                    .append(item.optString("topped_up_balance"))
            }

            return result.toString()
        }
    }
}
