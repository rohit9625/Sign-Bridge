package com.devx.signbridge.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devx.signbridge.auth.domain.GoogleAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class CompleteProfileViewModel(
    private val googleAuthClient: GoogleAuthClient
): ViewModel() {
    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        googleAuthClient.getSignedInUser()?.let { user ->
            _uiState.update { it.copy(user = user) }
        }
    }

    fun onEvent(e: CompleteProfileScreenEvent) {
        when (e) {
            is CompleteProfileScreenEvent.OnFullNameChange -> {
                _uiState.update { it.copy(user = it.user?.copy(username = e.fullName)) }
            }
            is CompleteProfileScreenEvent.OnEmailChange -> {
                _uiState.update { it.copy(user = it.user?.copy(email = e.email)) }
            }
            CompleteProfileScreenEvent.OnContinueClick -> {

            }
        }
    }
}