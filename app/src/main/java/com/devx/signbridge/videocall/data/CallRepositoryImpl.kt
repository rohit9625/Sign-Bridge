package com.devx.signbridge.videocall.data

import android.util.Log
import com.devx.signbridge.videocall.domain.CallId
import com.devx.signbridge.videocall.domain.CallRepository
import com.devx.signbridge.videocall.domain.models.Call
import com.devx.signbridge.videocall.domain.models.CallState
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CallRepositoryImpl: CallRepository {
    private val db = Firebase.firestore

    override suspend fun createCall(call: Call): CallId {
        val docRef = db.collection("calls").document()
        val callWithId = call.copy(id = docRef.id)

        docRef.set(callWithId).await()
        return docRef.id
    }

    override fun listenForIncomingCalls(scope: CoroutineScope, currentUserId: String) = callbackFlow<CallState> {
        Log.d(TAG, "Listening for incoming calls: UserID: $currentUserId")
        val listener = Firebase.firestore.collection("calls")
            .whereEqualTo("calleeId", currentUserId)
            .limit(1) // Should look for one call at a time
            .addSnapshotListener { snapshots, error ->
                if(error != null && snapshots == null) {
                    Log.e(TAG, "Error listening for incoming calls", error)
                    return@addSnapshotListener
                }

                for (doc in snapshots?.documents ?: emptyList()) {
                    if (doc != null && !doc.exists()) {
                        Log.w(TAG, "Document Doesn't Exists :(")
                        trySend(CallState.CallEnded)
                    }
                    Log.d(TAG, "New Doc Change: ${doc.id}")
                    doc.toObject<Call>()?.let { call ->
                        trySend(CallState.IncomingCall(call))
                    }
                }
            }
        awaitClose {
            Log.w(TAG, "Removing Incoming Call Listener")
            listener.remove()
        }
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

    override fun deleteCall(callId: String) {
        try {
            db.collection("calls")
                .document(callId)
                .delete()
                .addOnCompleteListener {
                    if(it.isSuccessful) {
                        Log.d(TAG, "Call deleted successfully")
                    } else {
                        Log.e(TAG, "Call deletion failed", it.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting call: $e")
        }
    }

    companion object {
        private const val TAG = "CallRepositoryImpl"
    }
}