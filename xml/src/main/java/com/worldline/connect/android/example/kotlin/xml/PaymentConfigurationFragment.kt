/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.xml

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.worldline.connect.android.example.kotlin.xml.databinding.FragmentPaymentConfigurationBinding
import com.worldline.connect.android.example.kotlin.xml.utils.extentions.getDataFromClipboard
import com.worldline.connect.android.example.kotlin.xml.utils.extentions.hideKeyboard
import com.worldline.connect.android.example.kotlin.common.utils.Status
import com.worldline.connect.android.example.kotlin.common.googlepay.GooglePayConfiguration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.worldline.connect.android.example.kotlin.xml.R
import com.worldline.connect.sdk.client.android.model.paymentcontext.AmountOfMoney
import com.worldline.connect.sdk.client.android.model.paymentcontext.PaymentContext
import java.util.Currency
import java.util.Locale
import com.worldline.connect.android.example.kotlin.xml.utils.extentions.getCurrentLocale
import com.worldline.connect.android.example.kotlin.common.PaymentScreen
import com.worldline.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.worldline.connect.android.example.kotlin.xml.utils.extentions.deepForEach
import com.worldline.connect.sdk.client.android.ConnectSDK
import com.worldline.connect.sdk.client.android.ConnectSDKNotInitializedException
import com.worldline.connect.sdk.client.android.configuration.SessionConfiguration

class PaymentConfigurationFragment : Fragment() {

    private var _binding: FragmentPaymentConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var configurationInputFields: List<Pair<TextInputLayout, TextInputEditText>>

    private val paymentSharedViewModel: PaymentSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPaymentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetInputData()
        initLayout()
        prefillPaymentConfigurationFields()
        initInputFieldsDrawableEndClickListeners()
        observePaymentSharedViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        paymentSharedViewModel.activePaymentScreen.value = PaymentScreen.CONFIGURATION
    }

    private fun resetInputData() {
        paymentSharedViewModel.globalErrorMessage.value = null
        paymentSharedViewModel.paymentProductsStatus.value = null
    }

    private fun initLayout() {
         configurationInputFields = listOf(
                Pair(binding.tilPaymentConfigurationClientSessionId, binding.etPaymentConfigurationClientSessionId),
                Pair(binding.tilPaymentConfigurationCustomerId, binding.etPaymentConfigurationCustomerId),
                Pair(binding.tilPaymentConfigurationClientApiUrl, binding.etPaymentConfigurationClientApiUrl),
                Pair(binding.tilPaymentConfigurationAssetsUrl, binding.etPaymentConfigurationAssetsUrl),
                Pair(binding.tilPaymentConfigurationAmount, binding.etPaymentConfigurationAmount),
                Pair(binding.tilPaymentConfigurationCountryCode, binding.etPaymentConfigurationCountryCode),
                Pair(binding.tilPaymentConfigurationCurrencyCode, binding.etPaymentConfigurationCurrencyCode),
                Pair(binding.tilPaymentConfigurationMerchantId, binding.etPaymentConfigurationMerchantId),
                Pair(binding.tilPaymentConfigurationMerchantName, binding.etPaymentConfigurationMerchantName))

        binding.btnPaymentConfigurationProceedToCheckout.loadingButtonMaterialButton.setOnClickListener {
            view?.clearFocus()
            requireContext().hideKeyboard(it)
            validatePaymentConfiguration()
        }
        binding.btnPaymentConfigurationClientSessionJsonResponse.setOnClickListener {
            view?.clearFocus()
            requireContext().hideKeyboard(it)
            parseJsonDataFromClipboard()
        }

        // prefill country and currency
        val currentLocale = Locale.getDefault()
        binding.apply {
            etPaymentConfigurationCountryCode.setText(currentLocale.country)
            etPaymentConfigurationCurrencyCode.setText(Currency.getInstance(currentLocale)
                .toString())
            cbPaymentConfigurationGooglePay.setOnCheckedChangeListener { _, isChecked ->
                clPaymentConfigurationGooglePayInputContainer.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
            }
        }

        initFieldsRemoveErrorAfterTextChange()

    }

    private fun initInputFieldsDrawableEndClickListeners() {
        binding.apply {
            tilPaymentConfigurationClientSessionId.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_client_session_id_helper_text))
            }

            tilPaymentConfigurationCustomerId.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_customer_id_helper_text))
            }

            tilPaymentConfigurationClientApiUrl.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_clientApiUrl_helper_text))
            }

            tilPaymentConfigurationAssetsUrl.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_assetsUrl_helper_text))
            }

            tilPaymentConfigurationAmount.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_amount_helper_text))
            }

            tilPaymentConfigurationCountryCode.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_country_code_helper_text))
            }

            tilPaymentConfigurationCurrencyCode.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_currency_code_helper_text))
            }

            tilPaymentConfigurationMerchantId.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_merchant_name_helper_text))
            }

            tilPaymentConfigurationMerchantName.setEndIconOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_merchant_name_helper_text))
            }

            ivPaymentConfigurationRecurringPaymentHelperIcon.setOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_recurring_payment_helper_text))
            }

            ivPaymentConfigurationPaymentInInstallmentsIcon.setOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_payment_in_installments_helper_text))
            }

            ivPaymentConfigurationGroupPaymentProductsHelperIcon.setOnClickListener {
                showBottomSheetDialog(getString(R.string.payment_configuration_group_payment_products_helper_text))
            }
        }
    }

    private fun prefillPaymentConfigurationFields() {
        try {
                binding.apply {
                    etPaymentConfigurationClientSessionId.setText(
                        ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.clientSessionId
                    )
                    etPaymentConfigurationCustomerId.setText(
                        ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.customerId
                    )
                    etPaymentConfigurationClientApiUrl.setText(
                        ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.clientApiUrl
                    )
                    etPaymentConfigurationAssetsUrl.setText(
                        ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl
                    )
                    etPaymentConfigurationAmount.setText(
                        String.format(
                            ConnectSDK.getPaymentConfiguration().paymentContext.amountOfMoney.amount.toString()
                        )
                    )
                    etPaymentConfigurationCountryCode.setText(
                        ConnectSDK.getPaymentConfiguration().paymentContext.countryCode
                    )
                    etPaymentConfigurationCurrencyCode.setText(
                        ConnectSDK.getPaymentConfiguration().paymentContext.amountOfMoney.currencyCode
                    )
                    cbPaymentConfigurationGroupPaymentProducts.isChecked =
                        ConnectSDK.getPaymentConfiguration().groupPaymentProducts
                    cbPaymentConfigurationGooglePay.isChecked =
                        paymentSharedViewModel.googlePayConfiguration.configureGooglePay
                    etPaymentConfigurationMerchantId.setText(paymentSharedViewModel.googlePayConfiguration.merchantId)
                    etPaymentConfigurationMerchantName.setText(
                        paymentSharedViewModel.googlePayConfiguration.merchantName
                    )
                }
        } catch (exception: ConnectSDKNotInitializedException) {
            // SDK is not initialized, fields are not prefilled
            Log.e(javaClass.name, exception.toString())
        }
    }

    private fun validatePaymentConfiguration() {
        var isFormValid = true
        configurationInputFields.forEach {
            isFormValid = isInputFieldValid(it)
        }

        if (isFormValid){
            configureSDK()
        }
    }

    private fun isInputFieldValid(textInput: Pair<TextInputLayout, TextInputEditText>): Boolean {
        if (
            textInput.second.text?.toString()?.isBlank() == true &&
            textInput.first.id != binding.tilPaymentConfigurationMerchantId.id &&
            textInput.first.id != binding.tilPaymentConfigurationMerchantName.id
        ) {
            textInput.first.error = getString(R.string.payment_configuration_field_not_valid_error)
            return false
        }

        if (binding.cbPaymentConfigurationGooglePay.isChecked) {
            if (
                textInput.first.id == binding.tilPaymentConfigurationMerchantId.id ||
                textInput.first.id == binding.tilPaymentConfigurationMerchantName.id
            ) {
                if (textInput.second.text?.isBlank() == true) {
                    textInput.first.error = getString(R.string.payment_configuration_field_not_valid_error)
                    return false
                }
            }
        }

        return true
    }

    private fun configureSDK() {
        val amount = try {
            binding.etPaymentConfigurationAmount.text.toString().toLong()
        } catch (e: NumberFormatException) {
            0
        }

        if (binding.cbPaymentConfigurationGooglePay.isChecked) {
            paymentSharedViewModel.googlePayConfiguration =
                GooglePayConfiguration(
                    true,
                    binding.etPaymentConfigurationMerchantId.text.toString(),
                    binding.etPaymentConfigurationMerchantName.text.toString()
                )
        } else {
            paymentSharedViewModel.googlePayConfiguration =
                GooglePayConfiguration(
                    true,
                    null,
                    null
                )
        }

        paymentSharedViewModel.configureConnectSDK(SessionConfiguration(
            binding.etPaymentConfigurationClientSessionId.text.toString(),
            binding.etPaymentConfigurationCustomerId.text.toString(),
            binding.etPaymentConfigurationClientApiUrl.text.toString(),
            binding.etPaymentConfigurationAssetsUrl.text.toString()
        ),
            PaymentContext(
                AmountOfMoney(
                    amount, binding.etPaymentConfigurationCurrencyCode.text.toString()
                ),
                binding.etPaymentConfigurationCountryCode.text.toString(),
                binding.cbPaymentConfigurationRecurringPayment.isChecked,
                context?.getCurrentLocale(),
                binding.cbPaymentConfigurationPaymentInInstallments.isChecked,
            ),
            binding.cbPaymentConfigurationGroupPaymentProducts.isChecked
        )
    }

    private fun observePaymentSharedViewModel() {
        paymentSharedViewModel.paymentProductsStatus.observe(viewLifecycleOwner) { paymentProductStatus ->
            when (paymentProductStatus) {
                is Status.ApiError -> {
                    binding.clPaymentConfigurationInputForm.deepForEach { isEnabled = true }
                    binding.btnPaymentConfigurationProceedToCheckout.hideLoadingIndicator()
                    paymentSharedViewModel.globalErrorMessage.value =
                        paymentProductStatus.apiError.errors.first().message
                }
                is Status.Loading -> {
                    binding.clPaymentConfigurationInputForm.deepForEach { isEnabled = false }
                    binding.btnPaymentConfigurationProceedToCheckout.showLoadingIndicator()
                }
                is Status.Success -> {
                    findNavController().navigate(
                        PaymentConfigurationFragmentDirections.navigateToPaymentProductFragment()
                    )
                }
                is Status.Failed -> {
                    binding.clPaymentConfigurationInputForm.deepForEach { isEnabled = true }
                    binding.btnPaymentConfigurationProceedToCheckout.hideLoadingIndicator()
                    paymentSharedViewModel.globalErrorMessage.value =
                        paymentProductStatus.throwable.message
                }
                is Status.None, null -> {
                    // Init status; nothing to do here
                }
            }
        }
    }

    private fun parseJsonDataFromClipboard() {
        val jsonString = context?.getDataFromClipboard()
        try {
            Gson().fromJson(jsonString, SessionConfiguration::class.java).apply {
                binding.etPaymentConfigurationClientSessionId.setText(this.clientSessionId)
                binding.etPaymentConfigurationCustomerId.setText(this.customerId)
                binding.etPaymentConfigurationClientApiUrl.setText(this.clientApiUrl)
                binding.etPaymentConfigurationAssetsUrl.setText(this.assetUrl)
            }
        } catch (exception: JsonSyntaxException) {
            paymentSharedViewModel.globalErrorMessage.value = "Json data from clipboard can't be parsed."
            Log.e(javaClass.name, exception.toString())
        }
    }

    private fun initFieldsRemoveErrorAfterTextChange() {
        configurationInputFields.forEach { field ->
            field.second.doAfterTextChanged { editable ->
                if (editable?.isNotBlank() == true) {
                    field.first.isErrorEnabled = false
                    field.first.error = null
                    return@doAfterTextChanged
                }
            }
        }
    }

    private fun showBottomSheetDialog(bottomSheetText: String) {
        BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bottomsheet_information)
            findViewById<TextView>(R.id.tvInformationText)?.apply {
                text = bottomSheetText
            }
        }.show()
    }
}
