package com.devx.signbridge.auth.domain

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import android.os.Build
import android.util.Log
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.devx.signbridge.R
import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.core.domain.model.AuthError
import com.devx.signbridge.core.domain.model.Result
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthClient(
    private val context: Context,
    private val credentialManager: CredentialManager,
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun signInWithGoogle(): Result<User, AuthError> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setNonce(null) /*TODO("Create a nonce and pass it here")*/
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            return handleSignIn(result.credential)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Error while signing in with Google", e)
            Result.Error(AuthError.UNKNOWN_ERROR)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Received an invalid Google id token response", e)
            Result.Error(AuthError.INVALID_TOKEN)
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error while signing in with Google", e)
            Result.Error(AuthError.UNKNOWN_ERROR)

        }
    }

    private suspend fun handleSignIn(credential: Credential): Result<User, AuthError> {
        if(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)

            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            return Result.Success(
                User(
                    userId = user?.uid ?: "",
                    username = user?.displayName ?: "",
                    profilePictureUrl = user?.photoUrl?.toString() ?: "",
                    email = user?.email ?: ""
                )
            )
        }
        return Result.Error(AuthError.UNKNOWN_ERROR)
    }

    suspend fun signOut() {
        try {
            auth.signOut()
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val clearCredentialRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearCredentialRequest)
            }
        } catch (e: ClearCredentialException) {
            Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error while signing out user", e)
        }
    }

    fun getSignedInUser(): User? = auth.currentUser?.run {
        User(
            userId = uid,
            username = displayName ?: "",
            profilePictureUrl = photoUrl?.toString(),
            email = email ?: ""
        )
    }

    companion object {
        private const val TAG = "GoogleAuthClient"
    }
}
