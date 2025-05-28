package com.devx.signbridge.auth.domain

import com.devx.signbridge.auth.domain.model.User
import com.devx.signbridge.core.domain.model.DatabaseError
import com.devx.signbridge.core.domain.model.Result

interface UserRepository {
    suspend fun storeUserDetails(user: User): Result<Unit, DatabaseError>
}