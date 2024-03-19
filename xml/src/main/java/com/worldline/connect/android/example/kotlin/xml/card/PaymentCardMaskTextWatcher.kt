/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.xml.card

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.worldline.connect.sdk.client.android.model.paymentproduct.field.PaymentProductField

/**
 * TextWatcher that applies the masking to a cardField when necessary.
 *  example masking are:
 *  cardNumber: 1234 1234 1234 1234 {{9999 9999 9999 9999}}
 *  expiryDate: 11/11 {{99/99}}
 */
class PaymentCardMaskTextWatcher(
    private val editText: EditText,
    private val paymentProductField: PaymentProductField
) : TextWatcher {

    private var oldValue: String? = null
    private var start = 0
    private var count = 0
    private var after = 0
    private var isRunning = false

    override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
        oldValue = charSequence.toString()
        this.start = start
        this.count = count
        this.after = after
    }

    override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable?) {
        if (isRunning) {
            return
        }
        isRunning = true
        val formatResult = paymentProductField.applyMask(editable.toString(), oldValue, start, count, after)
        editText.setText(formatResult.formattedResult)
        editText.setSelection(formatResult.cursorIndex)
        isRunning = false
    }
}
