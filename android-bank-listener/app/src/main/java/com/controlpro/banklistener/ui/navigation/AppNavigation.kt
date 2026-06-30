package com.controlpro.banklistener.ui.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.controlpro.banklistener.data.model.DefaultBankApps
import com.controlpro.banklistener.ui.screens.*
import com.controlpro.banklistener.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Permission : Screen("permission")
    object Home : Screen("home")
    object Banks : Screen("banks")
    object Pending : Screen("pending")
    object History : Screen("history")
    object TestParser : Screen("test_parser")
    object Settings : Screen("settings")
    object Privacy : Screen("privacy")
}

@Composable
fun AppNavigation(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val autoMode by vm.autoMode.collectAsStateWithLifecycle()
    val enabledPackages by vm.enabledPackages.collectAsStateWithLifecycle()
    val minConfidence by vm.minConfidence.collectAsStateWithLifecycle()

    // Determinar tela inicial
    val startDestination = when {
        !uiState.isLoggedIn && !uiState.isLoading -> Screen.Welcome.route
        uiState.isLoggedIn -> Screen.Home.route
        else -> Screen.Welcome.route
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            vm.checkNotificationPermission()
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(onGetStarted = { navController.navigate(Screen.Login.route) })
        }

        composable(Screen.Login.route) {
            LoginScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                successMessage = uiState.successMessage,
                onLogin = vm::login,
                onSignUp = vm::signUp,
                onClearMessages = vm::clearMessages
            )
        }

        composable(Screen.Permission.route) {
            val permGranted = vm.checkNotificationPermission()
            PermissionScreen(
                isPermissionGranted = permGranted,
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                },
                onCheckPermission = {
                    vm.checkNotificationPermission()
                    if (uiState.notificationPermission) navController.navigateUp()
                }
            )
        }

        composable(Screen.Home.route) {
            LaunchedEffect(Unit) { vm.checkNotificationPermission() }
            HomeScreen(
                uiState = uiState,
                onRefresh = vm::loadImports,
                onCheckPermission = { vm.checkNotificationPermission() },
                onOpenPermissionSettings = {
                    navController.navigate(Screen.Permission.route)
                },
                onNavigateToPending = { navController.navigate(Screen.Pending.route) },
                onNavigateToBanks = { navController.navigate(Screen.Banks.route) },
                onNavigateToTest = { navController.navigate(Screen.TestParser.route) }
            )
        }

        composable(Screen.Banks.route) {
            BanksScreen(
                allowedApps = DefaultBankApps.list,
                enabledPackages = enabledPackages,
                onToggle = vm::togglePackage,
                onBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Pending.route) {
            PendingScreen(
                pendingImports = uiState.pendingImports,
                onConfirm = vm::confirmImport,
                onIgnore = vm::ignoreImport,
                onRefresh = vm::loadImports,
                onBack = { navController.navigateUp() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                recentImports = uiState.recentImports,
                onBack = { navController.navigateUp() }
            )
        }

        composable(Screen.TestParser.route) {
            TestParserScreen(
                onTest = vm::testParser,
                onBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                userEmail = uiState.userEmail,
                autoMode = autoMode,
                minConfidence = minConfidence,
                onAutoModeToggle = vm::toggleAutoMode,
                onLogout = vm::logout,
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(onBack = { navController.navigateUp() })
        }
    }
}
