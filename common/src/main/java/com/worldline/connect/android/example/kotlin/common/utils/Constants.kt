/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.common.utils

object Constants {

    /** Application Identifier, used for identifying the application in network calls  */
    const val APPLICATION_IDENTIFIER = "Android Example Application Kotlin/v2.0.2"

    /** GooglePay identifier */
    const val GOOGLE_PAY_PRODUCT_ID = "320"

    /** Card payment method */
    const val PAYMENT_METHOD_CARD = "card"

    /** Cards product group */
    const val PAYMENT_PRODUCT_GROUP_CARDS = "cards"

    /** Payment product field ids */
    const val CARD_NUMBER = "cardNumber"
    const val EXPIRY_DATE = "expiryDate"
    const val SECURITY_NUMBER = "cvv"
    const val CARD_HOLDER = "cardholderName"
}
