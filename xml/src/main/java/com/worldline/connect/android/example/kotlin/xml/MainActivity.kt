/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.xml

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.worldline.connect.android.example.kotlin.xml.databinding.ActivityMainBinding
import com.worldline.connect.android.example.kotlin.xml.utils.extentions.hideKeyboard
import com.worldline.connect.android.example.kotlin.common.PaymentScreen
import com.worldline.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.google.android.material.snackbar.Snackbar
import nl.isaac.android.StepIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val paymentSharedViewModel: PaymentSharedViewModel by viewModels()

    private val navHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.fcMainNavigationHostFragment) as NavHostFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeSessionStatus()
        observeActivePaymentScreen()

        binding.tbMain.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.tbMain.setNavigationOnClickListener {
            this.hideKeyboard(it)
            navHostFragment.findNavController().popBackStack()
        }

        setWindowInsets()

        initStepIndicator()
        setOnBackPressedListener();
    }

    private fun setWindowInsets() {
        binding.mainActivityContainer.apply {
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, wi ->
                val insets = wi.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
                v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    private fun setOnBackPressedListener() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = navHostFragment.findNavController().currentDestination?.id
                when {
                    currentFragment == R.id.payment_result_fragment -> {
                        navHostFragment.findNavController().navigate(
                            PaymentResultFragmentDirections.navigateToPaymentConfigurationFragment()
                        )
                    }
                    navHostFragment.childFragmentManager.backStackEntryCount != 0 -> {
                        navHostFragment.findNavController().popBackStack()
                    }
                }
            }
        })
    }

    /**
     * Listen for onActivityResult updates
     * and forward them to all fragments so that the result can be handled in the right place.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        navHostFragment.childFragmentManager.fragments.forEach { fragment ->
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun observeSessionStatus() {
        paymentSharedViewModel.globalErrorMessage.observe(this) { globalErrorMessage ->
            if(!globalErrorMessage.isNullOrBlank()) {
                Log.e(javaClass.name, "Global error message: $globalErrorMessage")
                Snackbar.make(
                    binding.mainActivityContainer,
                    globalErrorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeActivePaymentScreen() {
        paymentSharedViewModel.activePaymentScreen.observe(this) { paymentScreen ->
            when (paymentScreen) {
                PaymentScreen.CONFIGURATION -> binding.tbMain.navigationIcon = null
                PaymentScreen.PRODUCT -> binding.tbMain.setNavigationIcon(R.drawable.ic_arrow_back)
                PaymentScreen.CARD -> binding.tbMain.setNavigationIcon(R.drawable.ic_arrow_back)
                PaymentScreen.RESULT -> binding.tbMain.navigationIcon = null
                else -> {
                }
            }
            updateStepIndicatorActiveStepPosition(paymentScreen.ordinal)
        }
    }

    private fun updateStepIndicatorActiveStepPosition(position: Int) {
        binding.mainStepIndicator.activeStepPosition = position
    }

    private fun initStepIndicator() {
        binding.mainStepIndicator.stepType = StepIndicator.StepType.Mixed(
            listOf(
                Pair(StepIndicator.StepType.StepIndicatorTypeMixedOption.ICON,
                    R.drawable.ic_settings
                ),
                Pair(StepIndicator.StepType.StepIndicatorTypeMixedOption.TEXT, "1"),
                Pair(StepIndicator.StepType.StepIndicatorTypeMixedOption.TEXT, "2"),
                Pair(StepIndicator.StepType.StepIndicatorTypeMixedOption.TEXT, "3")
            )
        )
    }
}
