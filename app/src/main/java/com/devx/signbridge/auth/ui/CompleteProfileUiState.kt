package com.devx.signbridge.auth.ui

import com.devx.signbridge.auth.domain.model.User

data class CompleteProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val canContinue: Boolean = false,
    val error: String? = null
)