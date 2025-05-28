package com.devx.signbridge.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.core.domain.model.DatabaseError
import com.devx.signbridge.core.domain.model.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompleteProfileViewModel(
    private val googleAuthClient: GoogleAuthClient,
    private val userRepository: UserRepository
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
                _uiState.update { it.copy(isLoading = true) }
                viewModelScope.launch {
                    when (val result = userRepository.storeUserDetails(user = uiState.value.user!!)) {
                        is Result.Error -> {
                            val error = when (result.error) {
                                DatabaseError.PERMISSION_DENIED -> "You don't have permission to perform this action"
                                DatabaseError.ALREADY_EXISTS -> "User already exists"
                                DatabaseError.UNAUTHENTICATED -> "You must be authenticated to perform this action"
                                DatabaseError.REQUEST_TIMEOUT -> "Please try after some time"
                                else -> "Something went wrong!"
                            }
                            _uiState.update { it.copy(error = error, isLoading = false) }
                        }
                        is Result.Success -> _uiState.update {
                            it.copy(canContinue = true, isLoading = false)
                        }
                    }
                }
            }
        }
    }
}