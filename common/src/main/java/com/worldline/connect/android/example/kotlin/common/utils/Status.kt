/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.common.utils

import com.worldline.connect.sdk.client.android.network.ApiErrorResponse

/**
 * This class is  an example how to store the result of async call
 */
sealed class Status {
    data class Success(val data: Any?) : Status()
    data class Failed(val throwable: Throwable) : Status()
    data class ApiError(val apiError: ApiErrorResponse) : Status()
    data object Loading : Status()
    data object None: Status()
}
