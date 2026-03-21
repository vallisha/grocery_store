package com.grocerystore.data.api

import com.grocerystore.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface GroceryApi {
    @POST("items/")
    suspend fun createItem(@Body item: CreateItemRequest): GroceryItem

    @GET("items/")
    suspend fun getItems(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("location") location: String? = null,
    ): List<GroceryItem>

    @GET("items/{id}")
    suspend fun getItem(@Path("id") id: Int): GroceryItem

    @PUT("items/{id}")
    suspend fun updateItem(@Path("id") id: Int, @Body item: UpdateItemRequest): GroceryItem

    @DELETE("items/{id}")
    suspend fun deleteItem(@Path("id") id: Int): Response<Unit>

    @GET("stats/")
    suspend fun getStats(): Stats

    @GET("categories/")
    suspend fun getCategories(): List<Category>

    @POST("categories/")
    suspend fun createCategory(@Body cat: CreateNameRequest): Category

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<Unit>

    @GET("locations/")
    suspend fun getLocations(): List<Location>

    @POST("locations/")
    suspend fun createLocation(@Body loc: CreateNameRequest): Location

    @DELETE("locations/{id}")
    suspend fun deleteLocation(@Path("id") id: Int): Response<Unit>

    @GET("barcode/{barcode}")
    suspend fun lookupBarcode(@Path("barcode") barcode: String): BarcodeResult
}
