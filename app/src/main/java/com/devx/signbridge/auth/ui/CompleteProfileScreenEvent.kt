package com.devx.signbridge.auth.ui

sealed interface CompleteProfileScreenEvent {
    data class OnFullNameChange(val fullName: String) : CompleteProfileScreenEvent
    data class OnEmailChange(val email: String) : CompleteProfileScreenEvent
    data object OnContinueClick : CompleteProfileScreenEvent
}