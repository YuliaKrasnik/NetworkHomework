package com.android.homework.networkhomework.networking.model

data class ServerResponse(
        val success: Boolean,
        val status: Int,
        val data: Data
)

data class Data(
        val id: String,
        val title: String,
        val description: String,
        val type: String,
        val link: String
)