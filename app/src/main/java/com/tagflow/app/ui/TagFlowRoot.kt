package com.tagflow.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.tagflow.app.nfc.NfcStatus
import com.tagflow.app.ui.components.OpSheet
import com.tagflow.app.ui.screens.HistoryScreen
import com.tagflow.app.ui.screens.HomeScreen
import com.tagflow.app.ui.screens.LibraryScreen
import com.tagflow.app.ui.screens.SettingsScreen
import com.tagflow.app.ui.screens.TagDetailScreen
import com.tagflow.app.ui.screens.ToolsScreen
import com.tagflow.app.ui.screens.WriteScreen
import com.tagflow.app.ui.vm.HistoryViewModel
import com.tagflow.app.ui.vm.LibraryViewModel
import com.tagflow.app.ui.vm.NfcViewModel
import com.tagflow.app.ui.vm.SettingsViewModel
import com.tagflow.app.ui.vm.TagDetailViewModel
import com.tagflow.app.nfc.NfcOp

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab("home", "Scan", Icons.Rounded.Nfc),
    Tab("library", "Library", Icons.Rounded.Sell),
    Tab("write", "Write", Icons.Rounded.Edit),
    Tab("tools", "Tools", Icons.Rounded.Build)
)

@Composable
fun TagFlowRoot(nfcStatus: NfcStatus, onOpenNfcSettings: () -> Unit) {
    val nav = rememberNavController()
    val nfcVm: NfcViewModel = hiltViewModel()
    val op by nfcVm.opState.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route != null && Tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(inner),
            enterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)) },
            popExitTransition = { fadeOut(tween(160)) }
        ) {
            composable("home") {
                HomeScreen(
                    vm = nfcVm, nfcStatus = nfcStatus, onOpenNfcSettings = onOpenNfcSettings,
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenHistory = { nav.navigate("history") },
                    onOpenTag = { id -> nav.navigate("tag/$id") }
                )
            }
            composable("library") {
                val vm: LibraryViewModel = hiltViewModel()
                LibraryScreen(vm, onOpen = { id -> nav.navigate("tag/$id") }, onAdd = { nfcVm.startOp(NfcOp.AddTag) })
            }
            composable("write") { WriteScreen(nfcVm) }
            composable("tools") { ToolsScreen(nfcVm) }
            composable("settings") {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(vm, onBack = { nav.popBackStack() })
            }
            composable("history") {
                val vm: HistoryViewModel = hiltViewModel()
                HistoryScreen(vm, onBack = { nav.popBackStack() })
            }
            composable("tag/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val vm: TagDetailViewModel = hiltViewModel()
                TagDetailScreen(vm, nfcVm, onBack = { nav.popBackStack() })
            }
        }
    }

    OpSheet(state = op, onCancel = nfcVm::cancelOp, onClose = nfcVm::clearOp)
}
