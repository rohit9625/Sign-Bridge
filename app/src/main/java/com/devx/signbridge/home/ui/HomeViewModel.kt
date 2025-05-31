package com.devx.signbridge.home.ui

import android.nfc.Tag
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devx.signbridge.auth.domain.GoogleAuthClient
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.core.domain.model.Result
import com.devx.signbridge.videocall.domain.CallRepository
import com.devx.signbridge.videocall.domain.models.Call
import com.devx.signbridge.videocall.domain.models.CallState
import com.devx.signbridge.videocall.domain.models.CallStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val googleAuthClient: GoogleAuthClient,
    private val userRepository: UserRepository,
    private val callRepository: CallRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentUserId: String? = null

    init {
        currentUserId = googleAuthClient.getSignedInUser()?.userId
        userRepository.changeOnlineStatus(currentUserId!!, isOnline = true)
        viewModelScope.launch {
            callRepository.listenForIncomingCalls(this, currentUserId!!).collect { state ->
                when (state) {
                    is CallState.IncomingCall -> {
                        _uiState.update { it.copy(incomingCall = state.call)}
                        Log.d(TAG, "Incoming Call: ${state.call}")
                    }
                    else -> {
                        Log.d(TAG, "Other Call State: $state")
                    }
                }
            }
        }
    }

    fun onEvent(e: HomeScreenEvent) {
        when (e) {
            is HomeScreenEvent.OnQueryChange -> {
                _uiState.update { it.copy(searchQuery = e.query) }
            }
            HomeScreenEvent.OnSearchAction -> {
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
            HomeScreenEvent.SignOut -> signOutUser()
            is HomeScreenEvent.OnCallAction -> {
                _uiState.update { it.copy(isOnCallScreen = true) }
                viewModelScope.launch {
                    initiateCall(calleeId = e.callee.userId, calleeName = e.callee.username) {
                        e.onSuccess(it)
                    }
                }
                Log.d("HomeScreenEvent", "[OnCallAction] : To ${e.callee.username}")
            }
        }
    }

    suspend fun initiateCall(
        calleeId: String,
        calleeName: String,
        onSuccess: (callId: String) -> Unit
    ) {
        googleAuthClient.getSignedInUser()?.let { user ->
            val call = Call(
                callerId = user.userId,
                callerName = user.username,
                calleeId = calleeId,
                calleeName = calleeName,
                status = CallStatus.CALLING
            )
            val callId = callRepository.createCall(call)
            onSuccess(callId)
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

    fun signOutUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUserSignedOut = googleAuthClient.signOut()) }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}