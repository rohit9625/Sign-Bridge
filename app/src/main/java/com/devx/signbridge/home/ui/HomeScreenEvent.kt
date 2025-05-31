package com.devx.signbridge.home.ui

import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.videocall.domain.models.Call

sealed interface HomeScreenEvent {
    data object SignOut: HomeScreenEvent
    data class OnQueryChange(val query: String): HomeScreenEvent
    data object OnSearchAction: HomeScreenEvent
    data class OnCallAction(val callee: User, val onCallCreated: (call: Call) -> Unit): HomeScreenEvent
}