package com.devx.signbridge.auth.ui

data class SignInUiState(
    val isSigningIn: Boolean = false,
    val isSuccessful: Boolean = false,
    val isSignUp: Boolean = false,
    val error: String? = null
)
