package com.grocerystore.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ItemEntity::class, CategoryEntity::class, LocationEntity::class], version = 2)
abstract class GroceryDatabase : RoomDatabase() {
    abstract fun dao(): GroceryDao

    companion object {
        @Volatile private var INSTANCE: GroceryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN firestoreId TEXT")
                db.execSQL("ALTER TABLE categories ADD COLUMN firestoreId TEXT")
                db.execSQL("ALTER TABLE locations ADD COLUMN firestoreId TEXT")
            }
        }

        fun get(context: Context): GroceryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, GroceryDatabase::class.java, "grocery.db")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
