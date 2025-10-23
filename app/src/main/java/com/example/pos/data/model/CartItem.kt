package com.example.pos.data.model

import com.example.pos.data.entity.Item

data class CartItem(
    val item: Item,
    val quantity: Int,
    val lineTotal: Double = item.price * quantity
)