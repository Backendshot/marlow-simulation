package com.marlow.globals

sealed class VerificationResult {
    data class Success(val message: String, val userId: Int) : VerificationResult()
    data class Failure(val message: String) : VerificationResult()
    data class NotFound(val message: String) : VerificationResult()
    data class Error(val message: String): VerificationResult()
}