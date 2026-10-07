/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.xml

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.worldline.connect.android.example.kotlin.common.googlepay.PaymentGooglePayUtil
import com.worldline.connect.android.example.kotlin.common.googlepay.PaymentGooglePayViewModel
import com.worldline.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.worldline.connect.android.example.kotlin.common.utils.Status
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.worldline.connect.sdk.client.android.ConnectSDK
import com.worldline.connect.sdk.client.android.model.paymentrequest.EncryptedPaymentRequest
import com.worldline.connect.sdk.client.android.model.paymentproduct.BasicPaymentProduct
import org.json.JSONException
import org.json.JSONObject

/**
 * Fragment without a view in which the Google Pay functions are handled.
 * @see (https://developers.google.com/pay/api/android/guides/tutorial)
 */
class PaymentGooglePayFragment : BottomSheetDialogFragment() {

    private val paymentGooglePayViewModel: PaymentGooglePayViewModel by viewModels()
    private val paymentSharedViewModel: PaymentSharedViewModel by activityViewModels()

    /**
     * Listener for when Google Pay sheet is finished
     */
    private val paymentDataLauncher = registerForActivityResult(GetPaymentDataResult()) { taskResult ->
        when (taskResult.status.statusCode) {
            CommonStatusCodes.SUCCESS ->
                taskResult.result?.let(::handleGooglePaySuccess)
            CommonStatusCodes.CANCELED -> dismiss()
            else -> {
                paymentSharedViewModel.globalErrorMessage.value =
                    "Google pay loadPaymentData failed with error code: ${taskResult.status.statusCode}"
                dismiss()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observePaymentProductStatus()
        observeEncryptedPaymentRequestStatus()
        paymentGooglePayViewModel.getPaymentProductDetails(
            (paymentSharedViewModel.selectedPaymentProduct as BasicPaymentProduct).id
        )
    }

    private fun observePaymentProductStatus() {
        paymentGooglePayViewModel.paymentProductStatus.observe(this) { paymentProductStatus ->
            when (paymentProductStatus) {
                is Status.ApiError -> {
                    paymentSharedViewModel.globalErrorMessage.value =
                        paymentProductStatus.apiError.errors.first().message
                }
                is Status.Loading -> {
                    // No loadingState needed for this fragment; Google pay has its own loading indicator
                }
                is Status.Success -> {
                    requestGooglePayPayment(paymentProductStatus.data as BasicPaymentProduct)
                }
                is Status.Failed -> {
                    paymentSharedViewModel.globalErrorMessage.value = paymentProductStatus.throwable.message
                }
                Status.None -> {
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
                is Status.Loading -> {
                    // No loadingState needed for this fragment; Google pay has its own loading indicator
                }
                is Status.Success -> {
                    val encryptedFieldsData =
                        (encryptedPaymentRequestStatus.data as EncryptedPaymentRequest).encryptedFields
                    findNavController().navigate(
                        PaymentGooglePayFragmentDirections.navigateToPaymentResultFragment(encryptedFieldsData)
                    )
                }
                is Status.Failed -> {
                    paymentSharedViewModel.globalErrorMessage.value = encryptedPaymentRequestStatus.throwable.message
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
                requireActivity(),
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
                paymentSharedViewModel.globalErrorMessage.value = "Google Pay Can't fetch payment data request"
                return
            }
            val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

            // Since loadPaymentData may show the UI asking the user to select a payment method, we use
            // GetPaymentDataResult to wait for the user interacting with it. Once completed,
            // paymentDataLauncher will be called with the result.
            googlePayUtil.paymentsClient
                .loadPaymentData(request)
                .addOnCompleteListener(requireActivity(), paymentDataLauncher::launch)
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

            paymentGooglePayViewModel.paymentRequest.setValue(GOOGLE_PAY_TOKEN_FIELD_ID, googlePayToken)
            paymentGooglePayViewModel.encryptGooglePayPayment()
        } catch (exception: JSONException) {
            paymentSharedViewModel.globalErrorMessage.value = "Google pay token error ${exception.message}"
        }
    }

    companion object {

        private const val GOOGLE_PAY_TOKEN_FIELD_ID = "encryptedPaymentData"
    }
}
