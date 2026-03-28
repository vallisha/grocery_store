package com.grocerystore.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.grocerystore.data.db.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSync(private val dao: GroceryDao) {
    private val db = FirebaseFirestore.getInstance()
    private val itemsCol = db.collection("items")
    private val categoriesCol = db.collection("categories")
    private val locationsCol = db.collection("locations")

    // Push local item to Firestore
    suspend fun pushItem(item: ItemEntity): String {
        val data = mapOf(
            "name" to item.name, "quantity" to item.quantity, "unit" to item.unit,
            "category" to item.category, "location" to item.location,
            "purchase_date" to item.purchase_date, "expiry_date" to item.expiry_date,
            "notes" to item.notes, "barcode" to item.barcode,
        )
        val doc = if (item.firestoreId != null) {
            itemsCol.document(item.firestoreId).set(data).await()
            item.firestoreId
        } else {
            val ref = itemsCol.add(data).await()
            ref.id
        }
        // Save firestoreId back to Room
        dao.updateFirestoreId(item.id, doc)
        return doc
    }

    suspend fun deleteItem(firestoreId: String) {
        itemsCol.document(firestoreId).delete().await()
    }

    suspend fun pushCategory(cat: CategoryEntity): String {
        val data = mapOf("name" to cat.name)
        val doc = if (cat.firestoreId != null) {
            categoriesCol.document(cat.firestoreId).set(data).await()
            cat.firestoreId
        } else {
            val ref = categoriesCol.add(data).await()
            ref.id
        }
        dao.updateCategoryFirestoreId(cat.id, doc)
        return doc
    }

    suspend fun deleteCategory(firestoreId: String) {
        categoriesCol.document(firestoreId).delete().await()
    }

    suspend fun pushLocation(loc: LocationEntity): String {
        val data = mapOf("name" to loc.name)
        val doc = if (loc.firestoreId != null) {
            locationsCol.document(loc.firestoreId).set(data).await()
            loc.firestoreId
        } else {
            val ref = locationsCol.add(data).await()
            ref.id
        }
        dao.updateLocationFirestoreId(loc.id, doc)
        return doc
    }

    suspend fun deleteLocation(firestoreId: String) {
        locationsCol.document(firestoreId).delete().await()
    }

    // Listen for remote changes and sync to Room
    fun listenItems(): Flow<Unit> = callbackFlow {
        val reg = itemsCol.addSnapshotListener { snapshot, _ ->
            snapshot ?: return@addSnapshotListener
            trySend(Unit)
        }
        awaitClose { reg.remove() }
    }

    suspend fun pullAll() {
        // Pull items
        val remoteItems = itemsCol.get().await()
        val remoteIds = mutableSetOf<String>()
        for (doc in remoteItems) {
            remoteIds.add(doc.id)
            val existing = dao.getItemByFirestoreId(doc.id)
            val entity = ItemEntity(
                id = existing?.id ?: 0,
                name = doc.getString("name") ?: "",
                quantity = doc.getDouble("quantity") ?: 0.0,
                unit = doc.getString("unit") ?: "pcs",
                category = doc.getString("category") ?: "",
                location = doc.getString("location") ?: "",
                purchase_date = doc.getString("purchase_date") ?: "",
                expiry_date = doc.getString("expiry_date"),
                notes = doc.getString("notes"),
                barcode = doc.getString("barcode"),
                firestoreId = doc.id,
            )
            if (existing != null) dao.updateItem(entity) else dao.insertItem(entity)
        }
        // Remove locally items deleted remotely
        dao.getAllItems().filter { it.firestoreId != null && it.firestoreId !in remoteIds }
            .forEach { dao.deleteItem(it.id) }

        // Pull categories
        val remoteCats = categoriesCol.get().await()
        val remoteCatIds = mutableSetOf<String>()
        for (doc in remoteCats) {
            remoteCatIds.add(doc.id)
            val existing = dao.getCategoryByFirestoreId(doc.id)
            val entity = CategoryEntity(id = existing?.id ?: 0, name = doc.getString("name") ?: "", firestoreId = doc.id)
            if (existing != null) dao.updateCategory(entity) else dao.insertCategory(entity)
        }
        dao.getCategories().filter { it.firestoreId != null && it.firestoreId !in remoteCatIds }
            .forEach { dao.deleteCategory(it.id) }

        // Pull locations
        val remoteLocs = locationsCol.get().await()
        val remoteLocIds = mutableSetOf<String>()
        for (doc in remoteLocs) {
            remoteLocIds.add(doc.id)
            val existing = dao.getLocationByFirestoreId(doc.id)
            val entity = LocationEntity(id = existing?.id ?: 0, name = doc.getString("name") ?: "", firestoreId = doc.id)
            if (existing != null) dao.updateLocation(entity) else dao.insertLocation(entity)
        }
        dao.getLocations().filter { it.firestoreId != null && it.firestoreId !in remoteLocIds }
            .forEach { dao.deleteLocation(it.id) }
    }
}
