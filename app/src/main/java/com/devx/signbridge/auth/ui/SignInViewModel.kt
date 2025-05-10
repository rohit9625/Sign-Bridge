package com.devx.signbridge.auth.ui

import androidx.lifecycle.ViewModel

class SignInViewModel: ViewModel() {

    fun onEvent(e: SignInScreenEvent) {
        when (e) {
            SignInScreenEvent.SignInWithGoogle -> {
                TODO("")
            }
        }
    }
}