package com.devx.signbridge.auth.ui

sealed interface SignInScreenEvent {
    data object SignInWithGoogle: SignInScreenEvent
}