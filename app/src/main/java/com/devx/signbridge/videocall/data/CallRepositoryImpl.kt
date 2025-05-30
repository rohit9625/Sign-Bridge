package com.devx.signbridge.videocall.data

import com.devx.signbridge.videocall.domain.CallId
import com.devx.signbridge.videocall.domain.CallRepository
import com.devx.signbridge.videocall.domain.models.Call
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class CallRepositoryImpl: CallRepository {
    private val db = Firebase.firestore

    override suspend fun createCall(call: Call): CallId {
        val docRef = db.collection("calls").document()
        val callWithId = call.copy(id = docRef.id)

        docRef.set(callWithId).await()
        return docRef.id
    }

    override suspend fun updateCall(
        callId: String,
        updates: Map<String, Any>
    ) {
        db.collection("calls")
            .document(callId)
            .update(updates)
            .await()
    }
}