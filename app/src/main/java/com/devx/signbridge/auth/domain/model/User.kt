package com.devx.signbridge.auth.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null,
    val online: Boolean = false
)