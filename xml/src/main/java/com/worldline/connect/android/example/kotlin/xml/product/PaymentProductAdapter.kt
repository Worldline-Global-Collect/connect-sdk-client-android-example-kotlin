/*
 * Copyright (c) 2022. Worldline Global Collect B.V
 */

package com.worldline.connect.android.example.kotlin.xml.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.worldline.connect.android.example.kotlin.xml.databinding.ListitemPaymentProductBinding
import com.worldline.connect.sdk.client.android.formatter.StringFormatter
import com.worldline.connect.sdk.client.android.model.accountonfile.AccountOnFile
import com.worldline.connect.sdk.client.android.model.paymentitem.BasicPaymentItem
import com.squareup.picasso.Picasso

class PaymentProductAdapter(
    var baseAssetsUrl: String
) : RecyclerView.Adapter<PaymentProductAdapter.PaymentProductViewHolder>() {

    var paymentProducts: List<Any> = emptyList()
        set(value) {
            field = value
            notifyItemRangeChanged(0, paymentProducts.size)
        }

    var onBasicPaymentItemClicked: ((BasicPaymentItem) -> Unit)? = null
    var onAccountOnFileClicked: ((AccountOnFile) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentProductViewHolder {
        val itemBinding = ListitemPaymentProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentProductViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: PaymentProductViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        when {
            paymentProducts[position] is BasicPaymentItem -> {
                holder.bindBasicPaymentItem(paymentProducts[position] as BasicPaymentItem)
            }
            paymentProducts[position] is AccountOnFile -> {
                holder.bindAccountOnFile(paymentProducts[position] as AccountOnFile)
            }
            paymentProducts[position] is String -> {
                holder.bindHeader(paymentProducts[position] as String)
            }
        }
    }

    override fun getItemCount(): Int {
        return paymentProducts.size
    }

    inner class PaymentProductViewHolder(
        private val binding: ListitemPaymentProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindBasicPaymentItem(basicPaymentItem: BasicPaymentItem) {
            binding.root.run {
                Picasso.get()
                    .load(baseAssetsUrl.plus(basicPaymentItem.displayHints.logoUrl))
                    .into(binding.ivPaymentProductLogo)
                binding.tvPaymentProductLabel.text = basicPaymentItem.displayHints.label
                binding.paymentProductItem.setOnClickListener { onBasicPaymentItemClicked?.invoke(basicPaymentItem) }
            }
        }

        fun bindAccountOnFile(accountOnFile: AccountOnFile) {
            itemView.run {
                Picasso.get()
                    .load(baseAssetsUrl.plus(accountOnFile.displayHints.logo))
                    .into(binding.ivPaymentProductLogo)

                binding.tvPaymentProductLabel.text = accountOnFile.displayHints.labelTemplate[0].mask?.let { mask ->
                    val formattedValue = StringFormatter().applyMask(mask.replace("9", "*"), accountOnFile.label)
                    formattedValue
                } ?: run {
                    accountOnFile.label
                }
                binding.paymentProductItem.setOnClickListener { onAccountOnFileClicked?.invoke(accountOnFile) }
            }
        }

        fun bindHeader(headerLabel: String) {
            itemView.run {
                binding.apply {
                    paymentProductItem.visibility = View.GONE
                    tvPaymentProductHeader.visibility = View.VISIBLE
                    tvPaymentProductHeader.text = headerLabel
                }
            }
        }
    }
}
