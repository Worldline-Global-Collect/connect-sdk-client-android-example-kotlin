/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.compose

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.worldline.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.worldline.connect.android.example.kotlin.common.googlepay.PaymentGooglePayUtil
import com.worldline.connect.android.example.kotlin.common.googlepay.PaymentGooglePayViewModel
import com.worldline.connect.android.example.kotlin.common.utils.Status
import com.worldline.connect.sdk.client.android.ConnectSDK
import com.worldline.connect.sdk.client.android.model.paymentrequest.EncryptedPaymentRequest
import com.worldline.connect.sdk.client.android.model.paymentproduct.BasicPaymentProduct
import org.json.JSONException
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val paymentGooglePayViewModel: PaymentGooglePayViewModel by viewModels()
    private val paymentSharedViewModel: PaymentSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observePaymentProductStatus()
        observeEncryptedPaymentRequestStatus()
        setContent {
            Box(Modifier.safeDrawingPadding()) {
                ComposeApp(paymentSharedViewModel, launchGooglePay = {
                    launchGooglePay()
                })
            }
        }
    }

    /**
     * Listener for when Google Pay sheet is finished
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            GOOGLE_PAY_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        data?.let { intent ->
                            PaymentData.getFromIntent(intent)?.let(::handleGooglePaySuccess)
                        }
                    Activity.RESULT_CANCELED -> {
                        // No need to do anything
                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let { status ->
                            paymentSharedViewModel.globalErrorMessage.value =
                                "Google pay loadPaymentData failed with error code: ${status.statusCode}"
                        }
                    }
                }
            }
        }
    }

    private fun observePaymentProductStatus() {
        paymentGooglePayViewModel.paymentProductStatus.observe(this) { paymentProductStatus ->
            when (paymentProductStatus) {
                is Status.ApiError -> {
                    paymentSharedViewModel.globalErrorMessage.value =
                        paymentProductStatus.apiError.errors.first().message
                }
                is Status.Success -> {
                    requestGooglePayPayment(paymentProductStatus.data as BasicPaymentProduct)
                }
                is Status.Failed -> {
                    paymentSharedViewModel.globalErrorMessage.value = paymentProductStatus.throwable.message
                }
                is Status.Loading -> {
                    // No loadingState needed for this fragment
                }
                is Status.None -> {
                    // Init status; nothing to do here
                }
            }
        }
    }

    private fun observeEncryptedPaymentRequestStatus() {
        paymentGooglePayViewModel.encryptedPaymentRequestStatus.observe(this) { encryptedPaymentRequestStatus ->
            when (encryptedPaymentRequestStatus) {
                is Status.ApiError -> {
                    paymentSharedViewModel.globalErrorMessage.value =
                        encryptedPaymentRequestStatus.apiError.errors.first().message
                }
                is Status.Success -> {
                    val encryptedFieldsData =
                        (encryptedPaymentRequestStatus.data as EncryptedPaymentRequest).encryptedFields
                    paymentSharedViewModel.googlePayData.value = encryptedFieldsData
                }
                is  Status.Failed -> {
                    paymentSharedViewModel.globalErrorMessage.value = encryptedPaymentRequestStatus.throwable.message
                }
                is Status.Loading -> {
                    // No loadingState needed for this fragment; Google pay has its own loading indicator
                }
                is Status.None -> {
                    // Init status; nothing to do here
                }
            }
        }
    }

    /**
     * Configure and show Google Pay sheet.
     */
    private fun requestGooglePayPayment(basicPaymentProduct: BasicPaymentProduct) {
        if (paymentSharedViewModel.googlePayConfiguration.isValid()) {
            // We are sure that merchantId and merchantName are not null here,
            // since they are checked in the isValid function
            val googlePayUtil = PaymentGooglePayUtil(
                this,
                paymentSharedViewModel.googlePayConfiguration.merchantId!!,
                paymentSharedViewModel.googlePayConfiguration.merchantName!!,
                basicPaymentProduct.paymentProduct320SpecificData
            )

            val paymentDataRequestJson = googlePayUtil.getPaymentDataRequest(
                ConnectSDK.getPaymentConfiguration().paymentContext.amountOfMoney.amount,
                basicPaymentProduct.acquirerCountry,
                ConnectSDK.getPaymentConfiguration().paymentContext.amountOfMoney.currencyCode
            )
            if (paymentDataRequestJson == null) {
                paymentSharedViewModel.globalErrorMessage.value =
                    "Google Pay Can't fetch payment data request"
                return
            }
            val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

            // Since loadPaymentData may show the UI asking the user to select a payment method, we use
            // AutoResolveHelper to wait for the user interacting with it. Once completed,
            // onActivityResult will be called with the result.
            AutoResolveHelper.resolveTask(
                googlePayUtil.paymentsClient.loadPaymentData(request),
                this,
                GOOGLE_PAY_REQUEST_CODE
            )
        } else {
            paymentSharedViewModel.globalErrorMessage.value =
                "Merchant ID and merchant name cannot be empty when using Google Pay"
        }
    }

    /**
     * After the user has successfully completed the Google Pay steps fetch token data and prepare a payment
     */
    private fun handleGooglePaySuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson()

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val googlePayToken = JSONObject(paymentInformation)
                .getJSONObject("paymentMethodData")
                .getJSONObject("tokenizationData").getString("token")

            paymentGooglePayViewModel.paymentRequest.setValue(
                GOOGLE_PAY_TOKEN_FIELD_ID,
                googlePayToken
            )
            paymentGooglePayViewModel.encryptGooglePayPayment()
        } catch (exception: JSONException) {
            paymentSharedViewModel.globalErrorMessage.value = "Google pay token error ${exception.message}"
        }
    }

    private fun launchGooglePay() {
        if (paymentSharedViewModel.googlePayConfiguration.isValid()) {
            paymentGooglePayViewModel.getPaymentProductDetails(
                (paymentSharedViewModel.selectedPaymentProduct as BasicPaymentProduct).id
            )
        } else {
            paymentSharedViewModel.globalErrorMessage.value =
                "Merchant ID and merchant name cannot be empty when using Google Pay"
        }
    }

    companion object {

        private const val GOOGLE_PAY_TOKEN_FIELD_ID = "encryptedPaymentData"
        private const val GOOGLE_PAY_REQUEST_CODE = 991
    }
}
