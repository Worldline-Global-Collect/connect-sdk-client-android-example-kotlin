/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.worldline.connect.android.example.kotlin.common.googlepay.GooglePayConfiguration
import com.worldline.connect.android.example.kotlin.common.utils.Constants.APPLICATION_IDENTIFIER
import com.worldline.connect.android.example.kotlin.common.utils.Status
import com.worldline.connect.sdk.client.android.ConnectSDK.getClientApi
import com.worldline.connect.sdk.client.android.ConnectSDK.initialize
import com.worldline.connect.sdk.client.android.configuration.ConnectSDKConfiguration
import com.worldline.connect.sdk.client.android.configuration.PaymentConfiguration
import com.worldline.connect.sdk.client.android.configuration.SessionConfiguration
import com.worldline.connect.sdk.client.android.model.paymentcontext.PaymentContext
import com.worldline.connect.sdk.client.android.model.paymentitem.BasicPaymentItems
import com.worldline.connect.sdk.client.android.network.ApiErrorResponse

/**
 * Shared ViewModel for sharing objects with multiple fragments.
 * As a result, the session and paymentContext object is created only once and can be used by multiple fragments.
 */
class PaymentSharedViewModel(application: Application) : AndroidViewModel(application) {

    var googlePayConfiguration: GooglePayConfiguration = GooglePayConfiguration(false, null, null)

    val globalErrorMessage = MutableLiveData<String>()
    val paymentProductsStatus = MutableLiveData<Status>()
    var selectedPaymentProduct: Any? = null

    // Only used in XML example
    val activePaymentScreen = MutableLiveData(PaymentScreen.CONFIGURATION)

    // Only used in Compose example
    val googlePayData = MutableLiveData<String>()

    /**
     * After filling in and validating all configuration fields,
     * the Worldline Connect SDK can be initialized and the result is saved in the shared viewModel.
     */
    fun configureConnectSDK(
        sessionConfiguration: SessionConfiguration,
        paymentContext: PaymentContext,
        groupPaymentProducts: Boolean,
    ) {
        val connectSDKConfiguration = ConnectSDKConfiguration.Builder(
            sessionConfiguration,
            getApplication<Application>().applicationContext
        )
            .enableNetworkLogs(BuildConfig.LOGGING_ENABLED)
            .preLoadImages(true)
            .applicationId(APPLICATION_IDENTIFIER)
            .build()

        val paymentConfiguration = PaymentConfiguration.Builder(paymentContext)
            .groupPaymentProducts(groupPaymentProducts)
            .build()

        initialize(connectSDKConfiguration, paymentConfiguration)

        getPaymentProducts()
    }

    /**
     * Gets all Payment products for a provided payment context
     */
    private fun getPaymentProducts() {
        paymentProductsStatus.postValue(Status.Loading)
        getClientApi().getPaymentItems({ paymentItems: BasicPaymentItems ->
            paymentProductsStatus.postValue(Status.Success(paymentItems))
        },
            { apiError: ApiErrorResponse ->
                paymentProductsStatus.postValue(Status.ApiError(apiError))
            },
            { failure: Throwable ->
                paymentProductsStatus.postValue(Status.Failed(failure))
            }
        )
    }
}
