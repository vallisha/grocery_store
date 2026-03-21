package com.grocerystore.data.models

data class GroceryItem(
    val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val location: String,
    val purchase_date: String,
    val expiry_date: String? = null,
    val notes: String? = null,
    val barcode: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

data class CreateItemRequest(
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val location: String,
    val purchase_date: String,
    val expiry_date: String? = null,
    val notes: String? = null,
    val barcode: String? = null,
)

data class UpdateItemRequest(
    val name: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val category: String? = null,
    val location: String? = null,
    val purchase_date: String? = null,
    val expiry_date: String? = null,
    val notes: String? = null,
)

data class Stats(
    val total_items: Int,
    val expired_count: Int,
    val expiring_soon_count: Int,
    val by_category: Map<String, Int>,
    val by_location: Map<String, Int>,
)

data class Category(val id: Int, val name: String)
data class Location(val id: Int, val name: String)
data class CreateNameRequest(val name: String)
data class BarcodeResult(
    val source: String,
    val name: String?,
    val category: String?,
    val unit: String?,
)
