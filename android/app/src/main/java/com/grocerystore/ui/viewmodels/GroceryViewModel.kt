package com.grocerystore.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grocerystore.data.db.*
import com.grocerystore.data.models.*
import com.grocerystore.data.sync.FirestoreSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class GroceryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = GroceryDatabase.get(app).dao()
    private val sync = FirestoreSync(dao)

    private val _items = MutableStateFlow<List<GroceryItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _stats = MutableStateFlow<Stats?>(null)
    val stats = _stats.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        // Pull remote data on startup and listen for changes
        viewModelScope.launch {
            try { sync.pullAll() } catch (_: Exception) {}
            loadItems(); loadStats(); loadCategories(); loadLocations()
        }
        viewModelScope.launch {
            sync.listenItems().collect {
                try { sync.pullAll() } catch (_: Exception) {}
                loadItems(); loadStats(); loadCategories(); loadLocations()
            }
        }
    }

    fun loadItems(search: String? = null, category: String? = null, location: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _items.value = dao.getItems(search, category, location).map { it.toModel() }
            } catch (e: Exception) { _error.value = e.message }
            _isLoading.value = false
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()
                val soon = LocalDate.now().plusDays(3).toString()
                _stats.value = Stats(
                    total_items = dao.totalCount(),
                    expired_count = dao.expiredCount(today),
                    expiring_soon_count = dao.expiringSoonCount(today, soon),
                    by_category = dao.countByCategory().associate { (it.category ?: "") to it.cnt },
                    by_location = dao.countByLocation().associate { (it.location ?: "") to it.cnt },
                )
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun loadCategories() { viewModelScope.launch { try { _categories.value = dao.getCategories().map { Category(it.id, it.name) } } catch (_: Exception) {} } }
    fun loadLocations() { viewModelScope.launch { try { _locations.value = dao.getLocations().map { Location(it.id, it.name) } } catch (_: Exception) {} } }

    fun addItem(item: CreateItemRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val entity = ItemEntity(name = item.name, quantity = item.quantity, unit = item.unit, category = item.category, location = item.location, purchase_date = item.purchase_date, expiry_date = item.expiry_date, notes = item.notes, barcode = item.barcode)
                dao.insertItem(entity)
                // Get the inserted item (last inserted) and push to Firestore
                val inserted = dao.getItems(null, null, null).firstOrNull { it.name == item.name && it.firestoreId == null }
                if (inserted != null) try { sync.pushItem(inserted) } catch (_: Exception) {}
                onSuccess()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun updateItem(id: Int, item: UpdateItemRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                dao.getItem(id)?.let { existing ->
                    val updated = existing.copy(
                        name = item.name ?: existing.name,
                        quantity = item.quantity ?: existing.quantity,
                        unit = item.unit ?: existing.unit,
                        category = item.category ?: existing.category,
                        location = item.location ?: existing.location,
                        purchase_date = item.purchase_date ?: existing.purchase_date,
                        expiry_date = item.expiry_date ?: existing.expiry_date,
                        notes = item.notes ?: existing.notes,
                    )
                    dao.updateItem(updated)
                    try { sync.pushItem(updated) } catch (_: Exception) {}
                }
                onSuccess()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            try {
                val item = dao.getItem(id)
                dao.deleteItem(id)
                item?.firestoreId?.let { try { sync.deleteItem(it) } catch (_: Exception) {} }
                loadItems(); loadStats()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    suspend fun getItem(id: Int): GroceryItem? = try { dao.getItem(id)?.toModel() } catch (_: Exception) { null }

    fun addCategory(name: String) {
        viewModelScope.launch {
            try {
                val entity = CategoryEntity(name = name)
                dao.insertCategory(entity)
                val inserted = dao.getCategories().firstOrNull { it.name == name && it.firestoreId == null }
                if (inserted != null) try { sync.pushCategory(inserted) } catch (_: Exception) {}
                loadCategories()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            try {
                val cat = dao.getCategories().firstOrNull { it.id == id }
                dao.deleteCategory(id)
                cat?.firestoreId?.let { try { sync.deleteCategory(it) } catch (_: Exception) {} }
                loadCategories()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun addLocation(name: String) {
        viewModelScope.launch {
            try {
                val entity = LocationEntity(name = name)
                dao.insertLocation(entity)
                val inserted = dao.getLocations().firstOrNull { it.name == name && it.firestoreId == null }
                if (inserted != null) try { sync.pushLocation(inserted) } catch (_: Exception) {}
                loadLocations()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            try {
                val loc = dao.getLocations().firstOrNull { it.id == id }
                dao.deleteLocation(id)
                loc?.firestoreId?.let { try { sync.deleteLocation(it) } catch (_: Exception) {} }
                loadLocations()
            } catch (e: Exception) { _error.value = e.message }
        }
    }

    suspend fun lookupBarcode(barcode: String): BarcodeResult? {
        dao.getItemByBarcode(barcode)?.let {
            return BarcodeResult(source = "local", name = it.name, category = it.category, unit = it.unit)
        }
        return com.grocerystore.data.api.OpenFoodFacts.lookup(barcode)
    }
}

private fun ItemEntity.toModel() = GroceryItem(id, name, quantity, unit, category, location, purchase_date, expiry_date, notes, barcode)
