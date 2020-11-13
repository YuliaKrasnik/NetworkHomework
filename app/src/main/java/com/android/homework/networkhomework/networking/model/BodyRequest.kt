package com.android.homework.networkhomework.networking.model

data class BodyRequest(
        val image: String,
        val name: String,
        val title: String? = null,
        val description: String? = null
)