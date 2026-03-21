package com.grocerystore.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.grocerystore.ui.screens.*
import com.grocerystore.ui.viewmodels.GroceryViewModel

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : NavItem("home", "Home", Icons.Default.Home)
    object Items : NavItem("items", "Items", Icons.Default.List)
    object Add : NavItem("add", "Add", Icons.Default.Add)
    object Manage : NavItem("manage", "Manage", Icons.Default.Settings)
}

val navItems = listOf(NavItem.Home, NavItem.Items, NavItem.Add, NavItem.Manage)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val viewModel: GroceryViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hideBottomBar = currentRoute?.startsWith("edit/") == true || currentRoute == "scan"

    Scaffold(
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = { navController.navigate(item.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(viewModel, onTotalClick = { navController.navigate("items") })
            }
            composable("items") {
                ItemsScreen(viewModel, onItemClick = { navController.navigate("edit/$it") })
            }
            composable("add") {
                AddEditItemScreen(viewModel, onBack = { navController.navigate("items") { popUpTo("items") { inclusive = true }; launchSingleTop = true } })
            }
            composable("edit/{itemId}", arguments = listOf(navArgument("itemId") { type = NavType.IntType })) {
                AddEditItemScreen(viewModel, itemId = it.arguments?.getInt("itemId"), onBack = { navController.popBackStack() })
            }
            composable("manage") {
                ManageScreen(viewModel)
            }
            composable("scan") {
                BarcodeScannerScreen(viewModel,
                    onResult = { name, category, unit, barcode ->
                        // Navigate to add screen with scanned data via savedStateHandle
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("scanned_name", name); set("scanned_category", category)
                            set("scanned_unit", unit); set("scanned_barcode", barcode)
                        }
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
