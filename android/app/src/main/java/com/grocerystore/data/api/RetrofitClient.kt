package com.grocerystore.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 10.0.2.2 maps to host machine's localhost from Android emulator
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val api: GroceryApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroceryApi::class.java)
    }
}
