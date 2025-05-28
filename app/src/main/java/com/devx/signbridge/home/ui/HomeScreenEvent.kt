package com.devx.signbridge.home.ui

sealed interface HomeScreenEvent {
    data object SignOut: HomeScreenEvent
}