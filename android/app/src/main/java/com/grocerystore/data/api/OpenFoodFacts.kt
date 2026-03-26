package com.grocerystore.data.api

import com.grocerystore.data.models.BarcodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object OpenFoodFacts {
    suspend fun lookup(barcode: String): BarcodeResult? = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json").readText()
            val obj = JSONObject(json)
            if (obj.optInt("status") != 1) return@withContext null
            val product = obj.getJSONObject("product")
            BarcodeResult(
                source = "openfoodfacts",
                name = product.optString("product_name").ifBlank { null },
                category = product.optString("categories_tags").split(",").firstOrNull()?.removePrefix("en:")?.ifBlank { null },
                unit = product.optString("quantity").ifBlank { null },
            )
        } catch (_: Exception) { null }
    }
}
