package com.pab.ecommerce_katalog.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

// NOTE: This class has been updated to be more robust for Firestore serialization.
// Properties that are only for local use (`id` and `isSelected`) are now excluded.
@IgnoreExtraProperties
data class CartItem(
    @get:Exclude // Exclude local DB id from being saved to Firestore
    var id: Int = 0,

    var productVariantId: Int = 0,
    var quantity: Int = 0,
    var productName: String = "",
    var productPrice: Double = 0.0,
    var productImagePath: String = "",
    var size: String = "",
    var color: String = "",

    @get:Exclude // Exclude UI state from being saved to Firestore
    var isSelected: Boolean = true
)
