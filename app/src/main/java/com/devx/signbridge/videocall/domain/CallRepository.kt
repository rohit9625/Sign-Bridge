package com.devx.signbridge.videocall.domain

import com.devx.signbridge.videocall.domain.models.Call

typealias CallId = String
interface CallRepository {
    suspend fun createCall(call: Call): CallId

    suspend fun updateCall(callId: String, updates: Map<String, Any>)
}