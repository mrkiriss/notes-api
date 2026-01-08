package ru.miet.kvosk.notes.service

sealed class ServiceResult<out T> {
    data class Success<T>(val value: T) : ServiceResult<T>()

    data class Error(val error: ServiceError) : ServiceResult<Nothing>()
}

sealed class ServiceError {
    data class NotFound(val entity: String, val id: String) : ServiceError()

    data class Validation(val field: String, val message: String) : ServiceError()

    data class Conflict(val field: String, val message: String) : ServiceError()
}
