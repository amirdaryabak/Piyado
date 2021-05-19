package com.amirdaryabak.runningapp.utils

data class ResourceTest<out T>(val status: Status, val data: T?, val message: String?, val errorCode: Int = 0) {
    companion object {
        fun <T> success(data: T?): ResourceTest<T> {
            return ResourceTest(Status.SUCCESS, data, null)
        }

        fun <T> error(msg: String?, data: T?, errorCode: Int = 0): ResourceTest<T> {
            return ResourceTest(Status.ERROR, data, msg, errorCode)
        }

        fun <T> loading(data: T?): ResourceTest<T> {
            return ResourceTest(Status.LOADING, data, null)
        }
    }
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}