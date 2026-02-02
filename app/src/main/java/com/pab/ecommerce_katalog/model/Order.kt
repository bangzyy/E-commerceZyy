package com.pab.ecommerce_katalog.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Order(
    @DocumentId
    var id: String? = null,
    var userId: String? = null,
    var userName: String? = null,
    var products: List<OrderItem> = emptyList(),
    var totalHarga: Long = 0,
    var alamat: String? = null,
    var phone: String? = null,
    var status: String = "Pending",
    var rated: Boolean = false, // Menandai jika pesanan sudah diberi rating

    @ServerTimestamp
    var timestamp: Date? = null
)
