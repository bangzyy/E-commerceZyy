package com.pab.ecommerce_katalog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductVariant(
    val id: Int,
    val productId: Int,
    val size: String,
    val color: String,
    val stock: Int
) : Parcelable
