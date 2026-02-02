package com.pab.ecommerce_katalog

import com.pab.ecommerce_katalog.model.OrderProduct

data class Order(
    var id: String = "",
    var products: List<OrderProduct> = emptyList(),
    var totalHarga: Double = 0.0,
    var status: String = "",
    var timestamp: com.google.firebase.Timestamp? = null,
    var userId: String = "",
    var alamat: String = "",
    var phone: String = "",
    var rated: Boolean = false // Tambahkan field ini
)
