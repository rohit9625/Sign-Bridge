package com.devx.signbridge.videocall.domain

import com.devx.signbridge.videocall.domain.models.Call
import com.devx.signbridge.videocall.domain.models.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

typealias CallId = String
interface CallRepository {
    suspend fun createCall(call: Call): CallId

    suspend fun updateCall(callId: String, updates: Map<String, Any>)

    fun listenForIncomingCalls(scope: CoroutineScope, currentUserId: String): Flow<CallState>
}