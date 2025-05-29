package com.devx.signbridge.home.ui

sealed interface HomeScreenEvent {
    data object SignOut: HomeScreenEvent
    data class OnQueryChange(val query: String): HomeScreenEvent
    data object OnSearchAction: HomeScreenEvent
}