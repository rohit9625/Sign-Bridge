package com.devx.signbridge.auth.domain.model

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null
)