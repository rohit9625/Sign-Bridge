package com.devx.signbridge.home.ui

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.core.domain.model.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchUserViewModel(
    private val userRepository: UserRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(SearchUserUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onEvent(e: SearchUserEvent) {
        when (e) {
            is SearchUserEvent.OnQueryChange -> {
                _uiState.update { it.copy(searchQuery = e.query) }
            }
            is SearchUserEvent.OnAddFriend -> {
                /*TODO("Query firestore")*/
            }
            SearchUserEvent.OnSearchAction -> {
                _uiState.update { it.copy(isSearching = true) }
                val currentQuery = _uiState.value.searchQuery.trim()
                if (currentQuery.isBlank()) {
                    _uiState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearching = false,
                        )
                    }
                    return
                }

                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    performSearch(currentQuery)
                }
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update {
            it.copy(
                isSearching = true,
                searchResults = emptyList()
            )
        }

        var results: List<User> = emptyList()
        if (isEmail(query)) {
            // Search by email
            when (val result = userRepository.getUserByEmail(query)) {
                is Result.Error -> {

                }
                is Result.Success -> {
                    results = listOf(result.data)
                }
            }
        } else {
            when (val result = userRepository.getUsersByUsername(query)) {
                is Result.Error -> {

                }
                is Result.Success -> {
                    results = result.data
                }
            }
        }

        _uiState.update {
            it.copy(
                isSearching = false,
                searchResults = results
            )
        }
    }

    /**
     * A simple utility to check if the query string looks like an email.
     * For more robust validation, consider using Android's Patterns.EMAIL_ADDRESS.
     */
    private fun isEmail(query: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(query).matches()
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel() // Ensure coroutine is cancelled when ViewModel is cleared
    }
}