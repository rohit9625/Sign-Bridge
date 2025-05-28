package com.devx.signbridge.auth.data

import android.util.Log
import com.devx.signbridge.auth.domain.UserRepository
import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.core.domain.model.DatabaseError
import com.devx.signbridge.core.domain.model.Result
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl: UserRepository {
    private val db = Firebase.firestore

    override suspend fun storeUserDetails(user: User): Result<Unit, DatabaseError> {
        return try {
            db.collection("users").document(user.userId).set(user).await()

            return Result.Success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Error while storing user details", e)
            val error = when (e.code) {
                FirebaseFirestoreException.Code.INVALID_ARGUMENT -> DatabaseError.INVALID_REQUEST
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> DatabaseError.REQUEST_TIMEOUT
                FirebaseFirestoreException.Code.NOT_FOUND -> DatabaseError.NOT_FOUND
                FirebaseFirestoreException.Code.ALREADY_EXISTS -> DatabaseError.ALREADY_EXISTS
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> DatabaseError.PERMISSION_DENIED
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> DatabaseError.UNAUTHENTICATED
                else -> DatabaseError.INTERNAL_ERROR
            }
            return Result.Error(error)
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error while storing user details", e)
            Result.Error(DatabaseError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getUserDetails(userId: String): Result<User, DatabaseError> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserByEmail(email: String): Result<User, DatabaseError> {
        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1) // Because email is unique
                .get()
                .await()
            val user = querySnapshot.documents.firstOrNull()?.toObject<User>()
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(DatabaseError.NOT_FOUND)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while searching user by email", e)
            Result.Error(DatabaseError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getUsersByUsername(username: String): Result<List<User>, DatabaseError> {
        return try {
            val querySnapshot = db.collection("users")
                .orderBy("name") // Necessary for range queries
                .startAt(username)
                .endAt(username + "\uf8ff")
                .limit(10) // Always limit your results
                .get()
                .await()
            val users = querySnapshot.toObjects(User::class.java).toList()
            Result.Success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error while searching users by username", e)
            Result.Error(DatabaseError.UNKNOWN_ERROR)
        }
    }

    override suspend fun isNewUser(userId: String): Boolean {
        return try {
            val document = db.collection("users").document(userId).get().await()
            document.toObject<User>() == null
        } catch (e: Exception) {
            Log.e(TAG, "Error while checking new user", e)
            false
        }
    }

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }
}