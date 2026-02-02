package com.pab.ecommerce_katalog.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class OrderProduct(
    var productId: Int = -1,
    var productName: String = "",
    var quantity: Int = 0,
    var productPrice: Double = 0.0,
    var size: String = "",
    var color: String = "",
    var imagePath: String = ""
)
