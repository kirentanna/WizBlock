package com.wizblock.domain

sealed interface NormalizedDomainResult {
    data class Success(val domain: String) : NormalizedDomainResult
    data class Error(val reason: ErrorReason) : NormalizedDomainResult
}

enum class ErrorReason {
    Empty,
    InvalidHost
}
