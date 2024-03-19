/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.common.googlepay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.worldline.connect.android.example.kotlin.common.utils.Status
import com.worldline.connect.sdk.client.android.ConnectSDK
import com.worldline.connect.sdk.client.android.model.paymentrequest.EncryptedPaymentRequest
import com.worldline.connect.sdk.client.android.model.paymentrequest.PaymentRequest
import com.worldline.connect.sdk.client.android.model.paymentproduct.PaymentProduct
import com.worldline.connect.sdk.client.android.network.ApiErrorResponse

/**
 * ViewModel for retrieving Google Pay payment product and
 * prepare payment request
 */
class PaymentGooglePayViewModel(application: Application) : AndroidViewModel(application) {

    val paymentProductStatus = MutableLiveData<Status>()
    val encryptedPaymentRequestStatus = MutableLiveData<Status>()
    val paymentRequest = PaymentRequest()

    fun getPaymentProductDetails(paymentProductId: String) {
        ConnectSDK.getClientApi()
            .getPaymentProduct(paymentProductId, { paymentProduct: PaymentProduct ->
                paymentRequest.paymentProduct = paymentProduct
                paymentProductStatus.postValue(
                    Status.Success(paymentProduct)
                )
            },
                { apiError: ApiErrorResponse ->
                    paymentProductStatus.postValue(Status.ApiError(apiError))
                },
                { failure: Throwable ->
                    paymentProductStatus.postValue(Status.Failed(failure))
                }
            )
    }

    fun encryptGooglePayPayment() {
        ConnectSDK.encryptPaymentRequest(
            paymentRequest,
            { encryptedPaymentRequest: EncryptedPaymentRequest ->
                encryptedPaymentRequestStatus.postValue(Status.Success(encryptedPaymentRequest))
            },
            { failure: Throwable ->
                encryptedPaymentRequestStatus.postValue(Status.Failed(failure))
            }
        )
    }
}
