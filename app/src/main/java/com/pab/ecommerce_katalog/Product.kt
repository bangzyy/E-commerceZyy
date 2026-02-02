package com.pab.ecommerce_katalog

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String,
    val category: String,
    val imagePath: String,
    val rating: Float,
    val reviews: Int,
    var quantity: Int = 0
) : Parcelable
