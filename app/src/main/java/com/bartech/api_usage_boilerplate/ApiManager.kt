package com.bartech.api_usage_boilerplate

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiManager(private val context: Context, private val prefs: Prefs) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(context, prefs))
        // Add explicit timeouts to prevent indefinite waiting
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // The base URL below is a placeholder only. Retrofit requires *some* valid
    // baseUrl at build time, but every real request supplies a full absolute
    // URL via @Url (built from Prefs), which overrides this completely.
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(okHttpClient)
        .build()

    private val apiService: ApiService = retrofit.create(ApiService::class.java)

    private fun getBaseUrl(): String {
        val ipAddress = prefs.getIpAddress() ?: ""
        val port = prefs.getPortNumber() ?: ""
        val protocol = prefs.getProtocol() ?: ""
        return "$protocol://$ipAddress:$port"
    }

    @Throws(IOException::class, IllegalStateException::class, IllegalArgumentException::class)
    suspend fun callApi(
        endpoint: String,
        method: String,
        body: String? = null,
        token: String? = null
    ): Response<ResponseBody> = withContext(Dispatchers.IO) {
        val fullUrl = "${getBaseUrl()}/$endpoint"

        val headers = mutableMapOf<String, String>()
        token?.let { headers["Authorization"] = "Bearer $it" }

        try {
            when (method.uppercase()) {
                "GET" -> apiService.get(fullUrl, headers)
                "POST" -> {
                    val requestBody: RequestBody = (body ?: "")
                        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    apiService.post(fullUrl, headers, requestBody)
                }
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }
        } catch (e: IOException) {
            // Check for the unique error message from the interceptor
            if (e.message?.startsWith("AuthException:") == true) {
                // The interceptor handled the auth error and redirected.
                // Build a synthetic Retrofit error response with the correct
                // status code and message, same as the plain-OkHttp version did.
                val originalErrorCode = e.message!!.substringAfter("Code:").trim().toInt()
                val originalErrorMessage = when (originalErrorCode) {
                    440 -> "Session Timed Out. Please Log In again."
                    401 -> "Unauthorized Access Found."
                    else -> "Authentication Error"
                }

                val rawResponse = OkHttpResponse.Builder()
                    .request(Request.Builder().url(fullUrl).build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(originalErrorCode)
                    .message(originalErrorMessage)
                    .build()

                Response.error(
                    "".toResponseBody("application/json; charset=utf-8".toMediaTypeOrNull()),
                    rawResponse
                )
            } else {
                throw e
            }
        }
    }
}