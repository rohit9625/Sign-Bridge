package com.devx.signbridge.webrtc.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
typealias SDPDescription = String

class SignalingClient {
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = Firebase.firestore

    private val _sessionStateFlow = MutableStateFlow(WebRTCSessionState.Offline)
    val sessionStateFlow: StateFlow<WebRTCSessionState> = _sessionStateFlow

    private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, SDPDescription>>()
    val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, SDPDescription>> = _signalingCommandFlow

    private var listenerRegistration: ListenerRegistration? = null
    private var iceCandidatesRef: CollectionReference? = null

    fun startIceCandidateListener(callId: String) {
        Log.i(TAG, "Started listening for ice candidates")
        listenerRegistration?.remove()
        iceCandidatesRef = db.collection("calls").document(callId).collection("ice_candidates")

        iceCandidatesRef!!.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val type = data["type"] as? String ?: continue
                val sdpDescription = data["sdpDescription"] as? String ?: continue

                val command = SignalingCommand.valueOf(type)
                Log.d(TAG, "Fetched [${command.name}] from existing documents")

                coroutineScope.launch {
                    _signalingCommandFlow.emit(command to sdpDescription)
                }
            }
        }

        listenerRegistration = iceCandidatesRef!!.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            for (docChange in snapshot.documentChanges) {
                // Skip if ICE Candidate, Offer, or Answer document is modified or removed
                /** Note: We only care about new documents added */
                if (docChange.type != DocumentChange.Type.ADDED) continue

                val data = docChange.document.data
                val type = data["type"] as? String ?: continue
                val sdpDescription = data["sdpDescription"] as? String ?: continue

                val command = SignalingCommand.valueOf(type)
                Log.d(TAG, "Received new [${command.name}] via listener")

                coroutineScope.launch {
                    _signalingCommandFlow.emit(command to sdpDescription)
                }

                // Optional: Clean up message after receiving
                // docChange.document.reference.delete()
            }
        }
    }

    fun sendCommand(
        callId: String,
        signalingCommand: SignalingCommand,
        sdpDescription: String
    ) {
        val message = hashMapOf(
            "type" to signalingCommand.name,
            "sdpDescription" to sdpDescription
        )
        iceCandidatesRef = db.collection("calls").document(callId).collection("ice_candidates")

        iceCandidatesRef!!.add(message).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "[${signalingCommand.name}] Sent Successfully")
            } else {
                Log.e(TAG, "Error sending [${signalingCommand.name}]", it.exception)
            }
        }
    }

    fun dispose() {
        _sessionStateFlow.value = WebRTCSessionState.Offline
        listenerRegistration?.remove()
        iceCandidatesRef?.document()?.delete()
        coroutineScope.cancel()
    }

    enum class WebRTCSessionState {
        Active, // Offer and Answer messages has been sent
        Creating, // Creating session, offer has been sent
        Ready, // Both clients available and ready to initiate session
        Impossible, // We have less than two clients connected to the server
        Offline // unable to connect signaling server
    }

    enum class SignalingCommand {
        STATE, // Command for WebRTCSessionState
        OFFER, // to send or receive offer
        ANSWER, // to send or receive answer
        ICE // to send and receive ice candidates
    }

    companion object {
        private const val TAG = "SignalingClient"
    }
}