package com.grocerystore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.grocerystore.ui.navigation.NavGraph
import com.grocerystore.ui.theme.GroceryStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroceryStoreTheme {
                NavGraph()
            }
        }
    }
}
