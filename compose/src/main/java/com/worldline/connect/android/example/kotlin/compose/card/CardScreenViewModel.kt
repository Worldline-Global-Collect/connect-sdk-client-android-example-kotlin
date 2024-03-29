/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.compose.card

import android.app.Application
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.AndroidViewModel
import com.worldline.connect.android.example.kotlin.compose.R
import com.worldline.connect.android.example.kotlin.common.PaymentCardValidationErrorMessageMapper
import com.worldline.connect.android.example.kotlin.common.utils.Constants.CARD_HOLDER
import com.worldline.connect.android.example.kotlin.common.utils.Constants.CARD_NUMBER
import com.worldline.connect.android.example.kotlin.common.utils.Constants.EXPIRY_DATE
import com.worldline.connect.android.example.kotlin.common.utils.Constants.SECURITY_NUMBER
import com.worldline.connect.android.example.kotlin.compose.card.textfield.CardNumberTextFieldState
import com.worldline.connect.android.example.kotlin.compose.card.textfield.CardTextFieldState
import com.worldline.connect.android.example.kotlin.compose.components.CheckBoxField
import com.worldline.connect.android.example.kotlin.compose.components.TextFieldState
import com.worldline.connect.sdk.client.android.model.accountonfile.AccountOnFile
import com.worldline.connect.sdk.client.android.model.paymentproduct.field.PaymentProductField
import com.worldline.connect.sdk.client.android.model.validation.ValidationErrorMessage

class CardScreenViewModel(application: Application) : AndroidViewModel(application) {

    var cardFields by mutableStateOf(CardFields())
        private set

    private var isAccountOnFileDataLoaded = false

    /**
     * When the payment product fields properties change.
     * Update the card fields with the new properties values.
     */
    fun updateFields(
        paymentProductFields: List<PaymentProductField>,
        logoUrl: String,
        accountOnFile: AccountOnFile?
    ) {
        paymentProductFields.forEach { paymentProductField ->
            when (paymentProductField.id) {
                cardFields.cardNumberField.id -> {
                    updateCardNumberField(logoUrl, accountOnFile, paymentProductField)
                }

                cardFields.expiryDateField.id -> {
                    updateExpiryDateField(accountOnFile, paymentProductField)
                }

                cardFields.securityNumberField.id -> {
                   updateSecurityNumberField(accountOnFile, paymentProductField)
                }

                cardFields.cardHolderField.id -> {
                    updateCardHolderField(accountOnFile, paymentProductField)
                }
            }
        }

        if (accountOnFile != null && !isAccountOnFileDataLoaded) isAccountOnFileDataLoaded = true
    }

    private fun updateCardNumberField(
        logoUrl: String,
        accountOnFile: AccountOnFile?,
        paymentProductField: PaymentProductField
    ) {
        cardFields.cardNumberField.apply {
            paymentProductField.displayHints?.let { displayHints ->
                label = displayHints.placeholderLabel
                mask = displayHints.mask
            }
            maxSize = paymentProductField.dataRestrictions.validator.length.maxLength
            trailingImageUrl = logoUrl
            this.paymentProductField = paymentProductField
        }

        accountOnFile?.let {
            accountOnFileAttributes(
                it,
                paymentProductField.id,
                cardFields.cardNumberField
            )
        }
    }

    private fun updateExpiryDateField(accountOnFile: AccountOnFile?, paymentProductField: PaymentProductField) {
        cardFields.expiryDateField.apply {
            paymentProductField.displayHints?.let { displayHints ->
                label = displayHints.placeholderLabel
                mask = displayHints.mask
            }
            maxSize = paymentProductField.dataRestrictions.validator.length.maxLength
            this.paymentProductField = paymentProductField
        }

        if (accountOnFile != null && !isAccountOnFileDataLoaded) {
            accountOnFileAttributes(
                accountOnFile,
                paymentProductField.id,
                cardFields.expiryDateField
            )
        }
    }

    private fun updateSecurityNumberField(accountOnFile: AccountOnFile?, paymentProductField: PaymentProductField) {
        cardFields.securityNumberField.apply {
            paymentProductField.displayHints?.let { displayHints ->
                label = displayHints.placeholderLabel
                mask = displayHints.mask
            }
            maxSize = paymentProductField.dataRestrictions.validator.length.maxLength
            tooltipImageUrl = paymentProductField.displayHints?.tooltip?.imageURL
            tooltipText = paymentProductField.displayHints?.tooltip?.label
            this.paymentProductField = paymentProductField
        }

        if (accountOnFile != null && !isAccountOnFileDataLoaded) {
            accountOnFileAttributes(
                accountOnFile,
                paymentProductField.id,
                cardFields.securityNumberField
            )
        }
    }

    private fun updateCardHolderField(accountOnFile: AccountOnFile?, paymentProductField: PaymentProductField) {
        cardFields.cardHolderField.apply {
            paymentProductField.displayHints?.let { displayHints ->
                label = displayHints.placeholderLabel
            }
            this.paymentProductField = paymentProductField
        }
        if (accountOnFile != null && !isAccountOnFileDataLoaded) {
            accountOnFileAttributes(
                accountOnFile,
                paymentProductField.id,
                cardFields.cardHolderField
            )
        }
    }

    /**
     * Update field errors after modification in one of the fields.
     */
    fun setFieldErrors(fieldErrors: List<ValidationErrorMessage>) { cardFields.cardNumberField.networkErrorMessage = ""
        cardFields.expiryDateField.networkErrorMessage = ""
        cardFields.securityNumberField.networkErrorMessage = ""
        cardFields.cardHolderField.networkErrorMessage = ""
        fieldErrors.forEach { validationErrorMessage ->
            val errorMessage =
                PaymentCardValidationErrorMessageMapper.mapValidationErrorMessageToString(
                    getApplication<Application>().applicationContext,
                    validationErrorMessage
                )
            when (validationErrorMessage.paymentProductFieldId) {
                cardFields.cardNumberField.id -> {
                    cardFields.cardNumberField.networkErrorMessage = errorMessage
                }
                cardFields.expiryDateField.id -> {
                    cardFields.expiryDateField.networkErrorMessage = errorMessage
                }
                cardFields.securityNumberField.id -> {
                    cardFields.securityNumberField.networkErrorMessage = errorMessage
                }
                cardFields.cardHolderField.id -> {
                    cardFields.cardHolderField.networkErrorMessage = errorMessage
                }
            }
        }
    }

    /**
     * Disable/Enable all fields
     */
    fun cardFieldsEnabled(enabled: Boolean) {
        cardFields.apply {
            cardNumberField.enabled = enabled
            expiryDateField.enabled = enabled
            securityNumberField.enabled = enabled
            cardHolderField.enabled = enabled
        }
    }

    private fun accountOnFileAttributes(
        accountOnFile: AccountOnFile,
        paymentProductFieldId: String,
        textFieldState: TextFieldState
    ) {
        cardFields.rememberCardField.visible.value = false
        accountOnFile.attributes.firstOrNull { it.key == paymentProductFieldId }
            ?.let { attribute ->
                textFieldState.text = if (paymentProductFieldId == cardFields.cardNumberField.id) {
                    accountOnFile.displayHints.labelTemplate[0].mask?.let { mask ->
                        cardFields.cardNumberField.mask =
                            mask.replace(
                                "9",
                                "*"
                            )
                    }
                    accountOnFile.label
                } else {
                    attribute.value
                }

                // CARD_NUMBER field should always be disabled for AccountOnFile) {
                if (!attribute.isEditingAllowed || attribute.key == CARD_NUMBER) {
                    textFieldState.enabled = false
                }
            }
    }
}

data class CardFields(
    val cardNumberField: CardNumberTextFieldState = CardNumberTextFieldState(
        leadingIcon = Icons.Filled.CreditCard,
        id = CARD_NUMBER,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    ),
    val expiryDateField: CardTextFieldState = CardTextFieldState(
        leadingIcon = Icons.Filled.CalendarToday,
        id = EXPIRY_DATE,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    ),
    val securityNumberField: CardTextFieldState = CardTextFieldState(
        leadingIcon = Icons.Filled.Lock,
        trailingIcon = Icons.Outlined.Info,
        id = SECURITY_NUMBER,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    ),
    val cardHolderField: CardTextFieldState = CardTextFieldState(
        leadingIcon = Icons.Filled.Person,
        id = CARD_HOLDER
    ),
    val rememberCardField: CheckBoxField = CheckBoxField(R.string.gc_app_paymentProductDetails_rememberMe)
)

