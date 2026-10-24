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
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun chat(
        key: String,
        history: List<Pair<String, String>>,
        effort: String = "high"
    ): String {

        require(key.isNotBlank()) {
            "请先填写 DeepSeek API Key"
        }

        val validEffort = when (effort) {
            "none", "low", "high", "max" -> effort
            else -> "high"
        }

        val messages = JSONArray()

        messages.put(
            JSONObject()
                .put("role", "system")
                .put(
                    "content",
                    "你是汤圆的龙娘。请使用自然、亲切的中文交流，" +
                        "称呼用户为汤圆，回答真诚、温柔、有趣。"
                )
        )

        history.forEach { (role, content) ->
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

        if (validEffort != "none") {
            requestJson.put("reasoning_effort", validEffort)
        }

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
                    "请求失败 HTTP ${response.code}" +
                        if (detail.isNotBlank()) "：$detail"
                        else "，请检查 API Key、账户余额或请求参数"
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
                .ifBlank { "汤圆，我这次没有收到文字回答，我们再试一次吧。" }
        }
    }

    fun getBalance(key: String): String {
        require(key.isNotBlank()) {
            "请先填写 DeepSeek API Key"
        }

        val request = Request.Builder()
            .url("https://api.deepseek.com/user/balance")
            .header("Authorization", "Bearer ${key.trim()}")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "余额查询失败 HTTP ${response.code}，请检查 API Key"
                )
            }

            val json = JSONObject(responseText)
            val balances = json.optJSONArray("balance_infos")
                ?: throw IllegalStateException("没有读取到余额信息")

            if (balances.length() == 0) {
                return "账户暂未返回余额详情"
            }

            val result = StringBuilder()
            result.append(
                "API 调用额度是否充足："
            ).append(
                if (json.optBoolean("is_available")) "是" else "否"
            )

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
