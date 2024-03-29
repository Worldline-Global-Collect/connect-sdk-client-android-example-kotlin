/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.compose.card.textfield

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.worldline.connect.android.example.kotlin.compose.card.CardFieldVisualTransformation
import com.worldline.connect.android.example.kotlin.compose.components.OutlinedTextFieldWithError
import com.worldline.connect.android.example.kotlin.compose.extensions.convertToString
import com.worldline.connect.sdk.client.android.model.paymentproduct.field.PaymentProductField

@Composable
fun CardNumberField(
    modifier: Modifier = Modifier,
    cardNumberTextFieldState: CardNumberTextFieldState,
    issuerIdentificationNumberChanged: (String) -> Unit,
    onValueChanged: (paymentProductField: PaymentProductField, value: String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextFieldWithError(
        value = cardNumberTextFieldState.text,
        onValueChange = {
            if (it.length <= cardNumberTextFieldState.maxSize) cardNumberTextFieldState.text = it

            cardNumberTextFieldState.paymentProductField?.let { paymentProductField ->
                onValueChanged(
                    paymentProductField,
                    it
                )
            }

            if (isChanged(
                    it,
                    cardNumberTextFieldState.lastCheckedCardValue
                )
            ) issuerIdentificationNumberChanged(it)
            cardNumberTextFieldState.lastCheckedCardValue = it

        },
        modifier = modifier
            .fillMaxWidth(),
        enabled = cardNumberTextFieldState.enabled,
        placeholder = {
            Text(text = cardNumberTextFieldState.label.convertToString())
        },
        leadingIcon = {
            Icon(imageVector = cardNumberTextFieldState.leadingIcon, null)
        },
        trailingIcon = {
            TrailingIcon(cardNumberTextFieldState)
        },
        error = {
            if (cardNumberTextFieldState.networkErrorMessage.isNotBlank()) {
                Text(
                    text = cardNumberTextFieldState.networkErrorMessage,
                    color = MaterialTheme.colors.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        },
        isError = cardNumberTextFieldState.networkErrorMessage.isNotBlank(),
        visualTransformation = CardFieldVisualTransformation(
            cardNumberTextFieldState.mask
        ),
        keyboardOptions = cardNumberTextFieldState.keyboardOptions,
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun TrailingIcon(cardNumberTextFieldState: CardNumberTextFieldState) {
    if (cardNumberTextFieldState.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(25.dp)
                .height(25.dp),
            strokeWidth = 2.dp
        )
    } else if (cardNumberTextFieldState.trailingImageUrl.isNotBlank()) {
        Image(
            painter = rememberAsyncImagePainter(cardNumberTextFieldState.trailingImageUrl),
            contentDescription = null,
            modifier = Modifier
                .width(50.dp)
                .height(25.dp)
        )
    }
}

private fun isChanged(currentCardNumber: String, lastCheckedCardValue: String): Boolean {
    val formattedCurrentCardNumber = (currentCardNumber + "xxxxxxxx").take(8)
    val formattedLastCheckedCardValue = (lastCheckedCardValue + "xxxxxxxx").take(8)
    return currentCardNumber.length >= 6 && formattedCurrentCardNumber != formattedLastCheckedCardValue
}
