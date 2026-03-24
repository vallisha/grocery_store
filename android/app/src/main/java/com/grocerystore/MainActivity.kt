package com.grocerystore

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.grocerystore.ui.navigation.NavGraph
import com.grocerystore.ui.theme.GroceryStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e("GroceryStore", "CRASH: ${e.message}", e)
            runOnUiThread {
                Toast.makeText(this, "Crash: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        setContent {
            GroceryStoreTheme {
                NavGraph()
            }
        }
    }
}
