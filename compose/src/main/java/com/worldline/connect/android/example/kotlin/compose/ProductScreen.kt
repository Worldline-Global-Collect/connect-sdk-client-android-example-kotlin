/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.worldline.connect.android.example.kotlin.compose.R
import com.worldline.connect.android.example.kotlin.common.PaymentScreen
import com.worldline.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.worldline.connect.android.example.kotlin.common.utils.Constants
import com.worldline.connect.android.example.kotlin.common.utils.Status
import com.worldline.connect.android.example.kotlin.compose.components.BottomSheetContent
import com.worldline.connect.android.example.kotlin.compose.components.FailedText
import com.worldline.connect.android.example.kotlin.compose.components.ProgressIndicator
import com.worldline.connect.sdk.client.android.ConnectSDK
import com.worldline.connect.sdk.client.android.formatter.StringFormatter
import com.worldline.connect.sdk.client.android.model.accountonfile.AccountOnFile
import com.worldline.connect.sdk.client.android.model.paymentitem.BasicPaymentItems
import com.worldline.connect.sdk.client.android.model.paymentproduct.BasicPaymentProduct
import com.worldline.connect.sdk.client.android.model.paymentproductgroup.BasicPaymentProductGroup

@Composable
fun ProductScreen(
    navController: NavHostController,
    paymentSharedViewModel: PaymentSharedViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit,
    launchGooglePay: () -> Unit
) {

    val paymentProductStatus by paymentSharedViewModel.paymentProductsStatus.observeAsState(Status.None)

    ProductContent(
        paymentProductStatus = paymentProductStatus,
        assetsBaseUrl = ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl,
        onItemClicked = { selectedPaymentProduct ->
            paymentSharedViewModel.selectedPaymentProduct = selectedPaymentProduct
            navigateToScreen(navController, selectedPaymentProduct, showBottomSheet = {
                showBottomSheet(it)
            }, launchGooglePay = { launchGooglePay() })
        })
}

@Composable
fun ProductContent(
    paymentProductStatus: Status,
    assetsBaseUrl: String,
    onItemClicked: (Any) -> Unit
) {
    when (paymentProductStatus) {
        is Status.ApiError -> {
            FailedText()
        }
        is Status.Loading -> {
            ProgressIndicator()
        }
        is Status.Success -> {
            PaymentProductItems(
                basicPaymentItems = paymentProductStatus.data as BasicPaymentItems,
                assetsBaseUrl = assetsBaseUrl,
                onItemClicked = { onItemClicked(it) }
            )
        }
        is Status.None -> {
            // Init status; nothing to do here
        }
        is Status.Failed -> {
            FailedText()
        }
    }
}

@Composable
private fun PaymentProductItems(
    basicPaymentItems: BasicPaymentItems,
    assetsBaseUrl: String,
    onItemClicked: (Any) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (basicPaymentItems.accountsOnFile.isNotEmpty()) {
            item {
                SectionHeader(
                    text = stringResource(id = R.string.gc_app_paymentProductSelection_accountsOnFileTitle),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(basicPaymentItems.accountsOnFile) { accountOnFile ->
                PaymentProductItem(
                    imageUrl = assetsBaseUrl + accountOnFile.displayHints.logo,
                    label = accountOnFile.displayHints.labelTemplate[0].mask?.let { mask ->
                        StringFormatter().applyMask(
                            mask.replace(
                                "9",
                                "*"
                            ),
                            accountOnFile.label
                        )} ?: run {
                            accountOnFile.label
                        },
                    onItemClicked = {
                        onItemClicked(accountOnFile)
                    }
                )
            }

            item {
                SectionHeader(
                    text = stringResource(id = R.string.gc_app_paymentProductSelection_paymentProductsTitle),
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }
        }

        items(basicPaymentItems.basicPaymentItems) { basicPaymentItem ->
            PaymentProductItem(
                imageUrl = assetsBaseUrl + basicPaymentItem.displayHints.logoUrl,
                label = basicPaymentItem.displayHints.label,
                onItemClicked = {
                    onItemClicked(basicPaymentItem)
                })
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.h6,
        modifier = modifier
    )
}

@Composable
private fun PaymentProductItem(imageUrl: String, label: String, onItemClicked: () -> Unit) {
    Surface(
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clickable { onItemClicked() }) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .width(50.dp)
                    .height(25.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(alignment = CenterVertically)
            )
        }
    }
}

private fun navigateToScreen(
    navController: NavHostController,
    selectedPaymentProduct: Any,
    showBottomSheet: (BottomSheetContent) -> Unit,
    launchGooglePay: () -> Unit
) {
    when (selectedPaymentProduct) {

        is AccountOnFile -> {
            navController.navigate(PaymentScreen.CARD.route)
        }

        is BasicPaymentProduct -> {
            when {
                selectedPaymentProduct.paymentMethod.equals(Constants.PAYMENT_METHOD_CARD, ignoreCase = true) -> {
                    navController.navigate(PaymentScreen.CARD.route)
                }
                selectedPaymentProduct.id.equals(Constants.GOOGLE_PAY_PRODUCT_ID) -> {
                    launchGooglePay()
                }
                else -> {
                    showBottomSheet(BottomSheetContent(R.string.gc_general_errors_productUnavailable))
                }
            }
        }
        is BasicPaymentProductGroup -> {
            if (selectedPaymentProduct.id.equals(Constants.PAYMENT_PRODUCT_GROUP_CARDS, ignoreCase = true)) {
                navController.navigate(PaymentScreen.CARD.route)

            } else {
                showBottomSheet(BottomSheetContent(R.string.gc_general_errors_productUnavailable))
            }
        }
        else -> {
            showBottomSheet(BottomSheetContent(R.string.gc_general_errors_productUnavailable))
        }
    }
}
