package com.devx.signbridge.home.ui

import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.videocall.domain.models.Call

data class HomeUiState(
    val isUserSignedOut: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val isOnCallScreen: Boolean = false,
    val incomingCall: Call? = null
)
