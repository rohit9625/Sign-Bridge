package com.devx.signbridge.home.ui

import com.devx.signbridge.auth.domain.model.User

sealed interface HomeScreenEvent {
    data object SignOut: HomeScreenEvent
    data class OnQueryChange(val query: String): HomeScreenEvent
    data object OnSearchAction: HomeScreenEvent
    data class OnCallAction(val callee: User, val onSuccess: (callId: String) -> Unit): HomeScreenEvent
}