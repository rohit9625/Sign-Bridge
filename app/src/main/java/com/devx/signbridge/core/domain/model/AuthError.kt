package com.devx.signbridge.core.domain.model

enum class AuthError: Error {
    SERVER_ERROR,
    UNKNOWN_ERROR,
    INVALID_TOKEN
}