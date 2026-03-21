package com.grocerystore.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocerystore.data.api.RetrofitClient
import com.grocerystore.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroceryViewModel : ViewModel() {
    private val api = RetrofitClient.api

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

    fun loadItems(search: String? = null, category: String? = null, location: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try { _items.value = api.getItems(search, category, location) } catch (e: Exception) { _error.value = e.message }
            _isLoading.value = false
        }
    }

    fun loadStats() { viewModelScope.launch { try { _stats.value = api.getStats() } catch (_: Exception) {} } }
    fun loadCategories() { viewModelScope.launch { try { _categories.value = api.getCategories() } catch (_: Exception) {} } }
    fun loadLocations() { viewModelScope.launch { try { _locations.value = api.getLocations() } catch (_: Exception) {} } }

    fun addItem(item: CreateItemRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try { api.createItem(item); onSuccess() } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun updateItem(id: Int, item: UpdateItemRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try { api.updateItem(id, item); onSuccess() } catch (e: Exception) { _error.value = e.message }
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            try { api.deleteItem(id); loadItems(); loadStats() } catch (e: Exception) { _error.value = e.message }
        }
    }

    suspend fun getItem(id: Int): GroceryItem? = try { api.getItem(id) } catch (_: Exception) { null }

    fun addCategory(name: String) { viewModelScope.launch { try { api.createCategory(CreateNameRequest(name)); loadCategories() } catch (e: Exception) { _error.value = e.message } } }
    fun deleteCategory(id: Int) { viewModelScope.launch { try { api.deleteCategory(id); loadCategories() } catch (e: Exception) { _error.value = e.message } } }
    fun addLocation(name: String) { viewModelScope.launch { try { api.createLocation(CreateNameRequest(name)); loadLocations() } catch (e: Exception) { _error.value = e.message } } }
    fun deleteLocation(id: Int) { viewModelScope.launch { try { api.deleteLocation(id); loadLocations() } catch (e: Exception) { _error.value = e.message } } }

    suspend fun lookupBarcode(barcode: String): BarcodeResult? = try { api.lookupBarcode(barcode) } catch (_: Exception) { null }
}
