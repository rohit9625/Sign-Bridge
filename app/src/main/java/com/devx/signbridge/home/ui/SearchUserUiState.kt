package com.devx.signbridge.home.ui

import com.devx.signbridge.auth.domain.model.User

data class SearchUserUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
)
