package com.marlow.global

sealed class RegistrationResult {
    data class Success(val message: String) : RegistrationResult()
    data class ValidationError(val message: String) : RegistrationResult()
    data class Conflict(val message: String) : RegistrationResult()
    data class Failure(val message: String) : RegistrationResult()
}