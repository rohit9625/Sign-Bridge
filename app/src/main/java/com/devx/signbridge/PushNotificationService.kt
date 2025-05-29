package com.devx.signbridge

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val data = hashMapOf(
                "token" to token,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val db = Firebase.firestore
            db.collection("fcmTokens").document(user.uid)
                .set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Token Updated Successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error Updating Token", e)
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message) 
        /*TODO("Show Notification to User")*/
        Log.d(TAG, "New Message Received, Data : ${message.data} | Notification : ${message.notification} | From : ${message.from} | To : ${message.messageType}")
    }

    companion object {
        private const val TAG = "PushNotificationService"
    }
}