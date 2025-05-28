package com.devx.signbridge.auth.domain

import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.core.domain.model.DatabaseError
import com.devx.signbridge.core.domain.model.Result

interface UserRepository {
    suspend fun storeUserDetails(user: User): Result<Unit, DatabaseError>

    suspend fun getUserDetails(userId: String): Result<User, DatabaseError>

    suspend fun getUserByEmail(email: String): Result<User, DatabaseError>

    suspend fun getUsersByUsername(username: String): Result<List<User>, DatabaseError>

    suspend fun isNewUser(userId: String): Boolean
}