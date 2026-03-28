package com.grocerystore.data.db

import androidx.room.*

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val location: String,
    val purchase_date: String,
    val expiry_date: String? = null,
    val notes: String? = null,
    val barcode: String? = null,
    val firestoreId: String? = null,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val firestoreId: String? = null,
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val firestoreId: String? = null,
)
