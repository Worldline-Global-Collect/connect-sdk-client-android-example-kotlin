/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.common.utils

import com.worldline.connect.sdk.client.android.model.validation.ValidationErrorMessage

/**
 * This class is an example how to store the result of a form validation.
 */
sealed class FormValidationResult {
    data class Invalid(val exceptions: List<Exception>?) : FormValidationResult()
    data class InvalidWithValidationErrorMessage(val exceptions: List<ValidationErrorMessage>) : FormValidationResult()
    data object Valid : FormValidationResult()
    data object NotValidated: FormValidationResult()
}
