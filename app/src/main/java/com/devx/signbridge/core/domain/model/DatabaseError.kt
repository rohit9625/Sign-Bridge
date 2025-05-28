package com.devx.signbridge.core.domain.model

enum class DatabaseError: Error {
    PERMISSION_DENIED,
    NOT_FOUND,
    ALREADY_EXISTS,
    UNAUTHENTICATED,
    INTERNAL_ERROR,
    UNKNOWN_ERROR,
    REQUEST_TIMEOUT,
    INVALID_REQUEST
}