package com.devx.signbridge.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devx.signbridge.auth.domain.GoogleAuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val googleAuthClient: GoogleAuthClient
): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(e: HomeScreenEvent) {
        when (e) {
            HomeScreenEvent.SignOut -> signOutUser()
        }
    }

    fun signOutUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserSignedOut = googleAuthClient.signOut()) }
        }
    }
}