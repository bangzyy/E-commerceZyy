package com.pab.ecommerce_katalog.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val password: String,
    val address: String,
    val phone: String
) : Parcelable