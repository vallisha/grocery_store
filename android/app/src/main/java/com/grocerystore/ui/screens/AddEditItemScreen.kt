package com.grocerystore.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grocerystore.data.models.CreateItemRequest
import com.grocerystore.data.models.UpdateItemRequest
import com.grocerystore.ui.viewmodels.GroceryViewModel
import java.time.LocalDate
import java.util.Calendar

val UNITS = listOf("pcs", "kg", "g", "liters", "ml", "packs", "bottles", "cans")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(viewModel: GroceryViewModel, itemId: Int? = null, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(UNITS[0]) }
    var category by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var expiryDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(itemId == null) }
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCategories(); viewModel.loadLocations() }
    LaunchedEffect(categories) { if (category.isEmpty() && categories.isNotEmpty()) category = categories.first().name }
    LaunchedEffect(locations) { if (location.isEmpty() && locations.isNotEmpty()) location = locations.first().name }

    LaunchedEffect(itemId) {
        if (itemId != null) {
            viewModel.getItem(itemId)?.let {
                name = it.name; quantity = it.quantity.toString(); unit = it.unit
                category = it.category; location = it.location
                purchaseDate = it.purchase_date; expiryDate = it.expiry_date ?: ""
                notes = it.notes ?: ""; barcode = it.barcode
            }
            loaded = true
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (itemId == null) "➕ Add Item" else "✏️ Edit Item") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
        )
    }) { padding ->
        if (!loaded) { CircularProgressIndicator(Modifier.padding(padding).padding(16.dp)); return@Scaffold }

        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Barcode scan result
            barcode?.let {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Text("📷 Barcode: $it", Modifier.padding(12.dp), color = Color(0xFF1B8A3A))
                }
            }

            OutlinedTextField(name, { name = it }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), placeholder = { Text("e.g. Aashirvaad Atta") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity *") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(12.dp))
                DropdownField("Unit", UNITS, unit, Modifier.weight(1f)) { unit = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField("Category", categories.map { it.name }, category, Modifier.weight(1f)) { category = it }
                DropdownField("Location", locations.map { it.name }, location, Modifier.weight(1f)) { location = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Purchase Date *", purchaseDate, Modifier.weight(1f)) { purchaseDate = it }
                DateField("Expiry Date", expiryDate, Modifier.weight(1f)) { expiryDate = it }
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp), placeholder = { Text("Optional notes...") })

            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: return@Button
                    if (name.isBlank()) return@Button
                    val exp = expiryDate.ifBlank { null }; val n = notes.ifBlank { null }
                    if (itemId == null) {
                        viewModel.addItem(CreateItemRequest(name, qty, unit, category, location, purchaseDate, exp, n, barcode)) { onBack() }
                    } else {
                        viewModel.updateItem(itemId, UpdateItemRequest(name, qty, unit, category, location, purchaseDate, exp, n)) { onBack() }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8A3A)),
            ) { Text("💾 Save Item") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, options: List<String>, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(selected, {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false }) }
        }
    }
}

@Composable
fun DateField(label: String, value: String, modifier: Modifier = Modifier, onPick: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedTextField(value, {}, readOnly = true, label = { Text(label) }, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.interactions.collect {
                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d -> onPick("$y-${(m+1).toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}") }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }
                }
            }
        })
}
