package com.devx.signbridge.home.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SearchUserViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(SearchUserUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(e: SearchUserEvent) {
        when (e) {
            is SearchUserEvent.OnQueryChange -> {
                _uiState.update { it.copy(searchQuery = e.query) }
            }
            is SearchUserEvent.OnAddFriend -> {
                /*TODO("Query firestore")*/
            }
            SearchUserEvent.OnSearchAction -> {
                /*TODO("Trigger search")*/
            }
        }
    }
}