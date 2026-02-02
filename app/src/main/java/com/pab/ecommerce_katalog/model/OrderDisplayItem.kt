package com.pab.ecommerce_katalog.model

sealed class OrderDisplayItem

data class UserHeaderItem(
    val userId: String,
    val userName: String,
    val orderCount: Int,
    var isExpanded: Boolean = false
) : OrderDisplayItem()

data class OrderContentItem(val order: Order) : OrderDisplayItem()
