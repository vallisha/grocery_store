package com.grocerystore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocerystore.ui.viewmodels.GroceryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: GroceryViewModel, onTotalClick: () -> Unit) {
    val stats by viewModel.stats.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadStats() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero header
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1B8A3A), Color(0xFF2ECC71))))
                .padding(28.dp, 32.dp, 28.dp, 28.dp)
        ) {
            Column {
                Text("🏪 Grocery Warehouse", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Your home inventory at a glance", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")), fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
            }
        }

        Column(Modifier.padding(16.dp)) {
            // Stats row
            stats?.let { s ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Total", s.total_items, Color(0xFF1B8A3A), Modifier.weight(1f).clickable { onTotalClick() })
                    StatCard("Expiring", s.expiring_soon_count, Color(0xFFFB8C00), Modifier.weight(1f))
                    StatCard("Expired", s.expired_count, Color(0xFFE53935), Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))

                if (s.by_category.isNotEmpty()) {
                    Text("📦 By Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    s.by_category.entries.sortedByDescending { it.value }.forEach { (k, v) ->
                        DashRow(k, v)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (s.by_location.isNotEmpty()) {
                    Text("📍 By Location", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    s.by_location.entries.sortedByDescending { it.value }.forEach { (k, v) ->
                        DashRow(k, v)
                    }
                }
            } ?: CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
        }
    }
}

@Composable
fun StatCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = color)
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DashRow(label: String, value: Int) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, Modifier.weight(1f), fontSize = 14.sp)
            Text("$value", fontWeight = FontWeight.Bold, color = Color(0xFF1B8A3A), fontSize = 16.sp)
        }
    }
}
