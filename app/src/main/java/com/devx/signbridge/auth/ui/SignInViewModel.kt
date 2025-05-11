package com.devx.signbridge.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.core.domain.model.AuthError
import com.devx.signbridge.core.domain.model.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    private val googleAuthClient: GoogleAuthClient
): ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(e: SignInScreenEvent) {
        when (e) {
            SignInScreenEvent.SignInWithGoogle -> signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            when (val result = googleAuthClient.signInWithGoogle()) {
                is Result.Error -> when (result.error) {
                    AuthError.INVALID_TOKEN -> {
                        _uiState.update { it.copy(error = "Invalid Google ID token") }
                    }
                    AuthError.SERVER_ERROR -> {
                        _uiState.update { it.copy(error = "Internal server error") }
                    }
                    AuthError.UNKNOWN_ERROR -> {
                        _uiState.update { it.copy(error = "Unexpected error occurred") }
                    }
                }
                is Result.Success -> {
                    _uiState.update { it.copy(isSuccessful = true, isSigningIn = false) }
                }
            }
        }
    }
}