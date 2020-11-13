package com.android.homework.networkhomework.networking

import com.android.homework.networkhomework.Constants.CLIENT_ID
import com.android.homework.networkhomework.networking.model.BodyRequest
import com.android.homework.networkhomework.networking.model.ServerResponse
import retrofit2.Call
import retrofit2.http.*

interface IApiImgur {
    @POST("3/image")
    @Headers("Authorization: Client-ID $CLIENT_ID")
    fun uploadImage(
            @Body bodyRequest: BodyRequest
    ): Call<ServerResponse>
}