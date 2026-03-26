package com.grocerystore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ItemEntity::class, CategoryEntity::class, LocationEntity::class], version = 1)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun dao(): GroceryDao

    companion object {
        @Volatile private var INSTANCE: GroceryDatabase? = null

        fun get(context: Context): GroceryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, GroceryDatabase::class.java, "grocery.db")
                    .build().also { INSTANCE = it }
            }
    }
}
