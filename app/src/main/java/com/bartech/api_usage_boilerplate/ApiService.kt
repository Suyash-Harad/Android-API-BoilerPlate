package com.bartech.api_usage_boilerplate

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * We intentionally use @Url instead of a fixed Retrofit baseUrl, because
 * the real base URL (protocol/ip/port) comes from Prefs and can change
 * at runtime. ApiManager builds the full URL and passes it in per call.
 */
interface ApiService {

    @GET
    suspend fun get(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<ResponseBody>

    @POST
    suspend fun post(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody
    ): Response<ResponseBody>
}