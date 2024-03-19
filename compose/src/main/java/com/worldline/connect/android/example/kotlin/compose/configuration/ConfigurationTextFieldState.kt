/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.compose.configuration

import androidx.compose.foundation.text.KeyboardOptions
import com.worldline.connect.android.example.kotlin.compose.R
import com.worldline.connect.android.example.kotlin.compose.components.BottomSheetContent
import com.worldline.connect.android.example.kotlin.compose.components.IsValid
import com.worldline.connect.android.example.kotlin.compose.components.TextFieldState

class ConfigurationTextFieldState(
    text: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: Any = "",
    enabled: Boolean = true,
    liveValidating: Boolean = false,
    val bottomSheetContent: BottomSheetContent = BottomSheetContent("")
) : TextFieldState(
    validator = ::isFieldValid,
    errorText = ::fieldValidationError,
    text = text,
    keyboardOptions = keyboardOptions,
    label = label,
    enabled = enabled,
    liveValidating = liveValidating
)

private fun fieldValidationError(errorText: Any): Any {
    return errorText
}

private fun isFieldValid(value: String): IsValid {
    return if (value.isNotBlank()) IsValid.Yes else IsValid.No(R.string.payment_configuration_field_not_valid_error)
}
