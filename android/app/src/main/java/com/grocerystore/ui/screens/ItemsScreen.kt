package com.grocerystore.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocerystore.data.models.GroceryItem
import com.grocerystore.ui.viewmodels.GroceryViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(viewModel: GroceryViewModel, onItemClick: (Int) -> Unit) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedLocation by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) { viewModel.loadCategories(); viewModel.loadLocations() }
    LaunchedEffect(searchQuery, selectedCategory, selectedLocation) {
        viewModel.loadItems(
            search = searchQuery.ifBlank { null },
            category = if (selectedCategory == "All") null else selectedCategory,
            location = if (selectedLocation == "All") null else selectedLocation,
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("📦 All Items", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("🔍 Search items...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

        Text("Category", Modifier.padding(top = 8.dp), fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = selectedCategory == "All", onClick = { selectedCategory = "All" }, label = { Text("All") }) }
            items(categories) { c -> FilterChip(selected = selectedCategory == c.name, onClick = { selectedCategory = c.name }, label = { Text(c.name) }) }
        }

        Text("Location", Modifier.padding(top = 4.dp), fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = selectedLocation == "All", onClick = { selectedLocation = "All" }, label = { Text("All") }) }
            items(locations) { l -> FilterChip(selected = selectedLocation == l.name, onClick = { selectedLocation = l.name }, label = { Text(l.name) }) }
        }

        Spacer(Modifier.height(8.dp))
        if (isLoading) { CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(16.dp)) }

        LazyColumn {
            items(items, key = { it.id }) { item ->
                SwipeToDismissItem(item, onDelete = { viewModel.deleteItem(item.id) }, onClick = { onItemClick(item.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissItem(item: GroceryItem, onDelete: () -> Unit, onClick: () -> Unit) {
    val dismissState = rememberDismissState(confirmValueChange = { if (it == DismissValue.DismissedToStart) { onDelete(); true } else false })
    SwipeToDismiss(
        state = dismissState,
        background = {
            val color by animateColorAsState(if (dismissState.targetValue == DismissValue.DismissedToStart) Color(0xFFE53935) else Color.Transparent)
            Box(Modifier.fillMaxSize().background(color).padding(end = 20.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
        },
        dismissContent = { GroceryItemCard(item, onClick) },
        directions = setOf(DismissDirection.EndToStart),
    )
}

@Composable
fun GroceryItemCard(item: GroceryItem, onClick: () -> Unit) {
    val today = LocalDate.now()
    val borderColor: Color
    val expText: String?
    val expColor: Color

    if (item.expiry_date != null) {
        val exp = LocalDate.parse(item.expiry_date)
        when {
            exp.isBefore(today) -> { borderColor = Color(0xFFE53935); expText = "⚠️ Expired: ${item.expiry_date}"; expColor = Color(0xFFE53935) }
            exp.isBefore(today.plusDays(3)) -> { borderColor = Color(0xFFFB8C00); expText = "⏰ Expiring: ${item.expiry_date}"; expColor = Color(0xFFFB8C00) }
            else -> { borderColor = Color(0xFF1B8A3A); expText = "📅 Expires: ${item.expiry_date}"; expColor = Color.Unspecified }
        }
    } else { borderColor = Color(0xFF1B8A3A); expText = null; expColor = Color.Unspecified }

    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Row(Modifier.drawWithContent { drawContent(); drawRect(borderColor, size = Size(4.dp.toPx(), size.height)) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${item.quantity} ${item.unit}", fontSize = 12.sp, color = Color.Gray)
                expText?.let { Text(it, fontSize = 11.sp, color = expColor, fontWeight = FontWeight.Medium) }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE3F2FD)) { Text(item.location, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0)) }
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFD4EDDA)) { Text(item.category, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF14692C)) }
            }
        }
    }
}
