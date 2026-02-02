package com.pab.ecommerce_katalog.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Review(
    var userId: String = "",
    var userName: String = "",
    var rating: Float = 0f,
    var comment: String = "",
    @ServerTimestamp
    var timestamp: Date? = null
)
