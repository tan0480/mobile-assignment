package com.example.gadgetmover.data

import com.example.gadgetmover.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin client for the Resend transactional email API, used to deliver OTP codes for
 * registration and password-reset. All calls run on [Dispatchers.IO] and never throw —
 * network or API failures are reported back as `false` so callers can fall back gracefully
 * (the OTP is still generated and checked locally even if the email never lands).
 */
object EmailService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendOtpEmail(to: String, otpCode: String, purposeLabel: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("from", BuildConfig.RESEND_FROM_ADDRESS)
                    put("to", JSONArray().put(to))
                    put("subject", "Your Gadget Mover verification code")
                    put("html", buildOtpHtml(otpCode, purposeLabel))
                }

                val request = Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer ${BuildConfig.RESEND_API_KEY}")
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response -> response.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }

    private fun buildOtpHtml(otpCode: String, purposeLabel: String): String = """
        <div style="font-family: -apple-system, Segoe UI, Roboto, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
          <h2 style="color: #1B4B91; margin-bottom: 4px;">Gadget Mover</h2>
          <p style="color: #1A1C1E; font-size: 15px;">Use the code below to $purposeLabel:</p>
          <div style="font-size: 32px; font-weight: 700; letter-spacing: 8px; background: #F7F8FA; padding: 16px 24px; border-radius: 12px; text-align: center; margin: 20px 0; color: #1A1C1E;">
            $otpCode
          </div>
          <p style="color: #62666D; font-size: 13px;">This code expires in <strong>5 minutes</strong>. If you didn't request this, you can safely ignore this email.</p>
        </div>
    """.trimIndent()
}
