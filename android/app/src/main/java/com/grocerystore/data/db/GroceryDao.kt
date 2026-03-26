package com.grocerystore.data.db

import androidx.room.*

@Dao
interface GroceryDao {
    @Query("SELECT * FROM items ORDER BY id DESC")
    suspend fun getAllItems(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE (:search IS NULL OR name LIKE '%' || :search || '%') AND (:category IS NULL OR category = :category) AND (:location IS NULL OR location = :location) ORDER BY id DESC")
    suspend fun getItems(search: String?, category: String?, location: String?): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Int): ItemEntity?

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): ItemEntity?

    @Insert
    suspend fun insertItem(item: ItemEntity)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: Int)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM items WHERE expiry_date IS NOT NULL AND expiry_date < :today")
    suspend fun expiredCount(today: String): Int

    @Query("SELECT COUNT(*) FROM items WHERE expiry_date IS NOT NULL AND expiry_date >= :today AND expiry_date <= :soon")
    suspend fun expiringSoonCount(today: String, soon: String): Int

    @Query("SELECT category, COUNT(*) as cnt FROM items GROUP BY category")
    suspend fun countByCategory(): List<GroupCount>

    @Query("SELECT location, COUNT(*) as cnt FROM items GROUP BY location")
    suspend fun countByLocation(): List<GroupCount>

    // Categories
    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getCategories(): List<CategoryEntity>

    @Insert
    suspend fun insertCategory(cat: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    // Locations
    @Query("SELECT * FROM locations ORDER BY name")
    suspend fun getLocations(): List<LocationEntity>

    @Insert
    suspend fun insertLocation(loc: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteLocation(id: Int)
}

data class GroupCount(
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "location") val location: String? = null,
    val cnt: Int,
)
