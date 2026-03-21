package com.grocerystore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocerystore.ui.viewmodels.GroceryViewModel

@Composable
fun ManageScreen(viewModel: GroceryViewModel) {
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()
    var newCat by remember { mutableStateOf("") }
    var newLoc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadCategories(); viewModel.loadLocations() }

    LazyColumn(Modifier.padding(16.dp).fillMaxSize()) {
        // Categories section
        item {
            Text("📋 Manage Categories", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newCat, { newCat = it }, label = { Text("New category") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                FilledIconButton(onClick = { if (newCat.isNotBlank()) { viewModel.addCategory(newCat.trim()); newCat = "" } }, shape = RoundedCornerShape(12.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1B8A3A))) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(categories, key = { it.id }) { cat ->
            ManageRow(cat.name) { viewModel.deleteCategory(cat.id) }
        }

        // Locations section
        item {
            Spacer(Modifier.height(24.dp))
            Text("📍 Manage Locations", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B8A3A))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newLoc, { newLoc = it }, label = { Text("New location") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("e.g. shelf-4, kitchen rack") })
                FilledIconButton(onClick = { if (newLoc.isNotBlank()) { viewModel.addLocation(newLoc.trim()); newLoc = "" } }, shape = RoundedCornerShape(12.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1B8A3A))) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(locations, key = { it.id }) { loc ->
            ManageRow(loc.name) { viewModel.deleteLocation(loc.id) }
        }
    }
}

@Composable
fun ManageRow(name: String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFE53935)) }
        }
    }
}
