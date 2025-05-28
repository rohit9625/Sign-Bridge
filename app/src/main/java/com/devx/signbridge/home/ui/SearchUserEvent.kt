package com.devx.signbridge.home.ui

sealed interface SearchUserEvent {
    data class OnQueryChange(val query: String): SearchUserEvent
    data object OnSearchAction: SearchUserEvent
    data class OnAddFriend(val userId: String): SearchUserEvent
}