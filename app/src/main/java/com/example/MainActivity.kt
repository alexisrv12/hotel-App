package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.HotelViewModel
import com.example.ui.Screen
import com.example.ui.screens.CreateUserScreen
import com.example.ui.screens.DeviceDashboardScreen
import com.example.ui.screens.FirstStartSetupWizardScreen
import com.example.ui.screens.GerenteBackupScreen
import com.example.ui.screens.GerenteDashboardScreen
import com.example.ui.screens.GerenteHistoryScreen
import com.example.ui.screens.GerenteInvoicesScreen
import com.example.ui.screens.GerenteAuditScreen
import com.example.ui.screens.GerentePinDialog
import com.example.ui.screens.GerenteRatesScreen
import com.example.ui.screens.GerenteReportsScreen
import com.example.ui.screens.GerenteRoomsScreen
import com.example.ui.screens.HotelRoomGridScreen
import com.example.ui.screens.GerenteSalesScreen
import com.example.ui.screens.GerenteSettingsScreen
import com.example.ui.screens.GerenteSuppliesScreen
import com.example.ui.screens.GerenteUsersScreen
import com.example.ui.screens.GuestCheckInScreen
import com.example.ui.screens.LinkDeviceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.RecepcionScreen
import com.example.ui.theme.HotelRiveraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HotelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            HotelRiveraTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HotelRiveraApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HotelRiveraApp(viewModel: HotelViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val timeRates by viewModel.timeRates.collectAsStateWithLifecycle()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.LOGIN -> {
                LoginScreen(
                    hotelViewModel = viewModel,
                    onNavigateToLinkDevice = {
                        // Open Link Device screen (authorization only, no manager access)
                        viewModel.navigateTo(Screen.LINK_DEVICE)
                    },
                    onNavigateToCreateUser = {
                        viewModel.navigateTo(Screen.CREATE_USER)
                    }
                )
            }

            Screen.LINK_DEVICE -> {
                LinkDeviceScreen(
                    onBackToLogin = { viewModel.navigateTo(Screen.LOGIN) }
                )
            }

            Screen.CREATE_USER -> {
                CreateUserScreen(
                    hotelViewModel = viewModel,
                    onBackToLogin = { viewModel.navigateTo(Screen.LOGIN) }
                )
            }

            Screen.SETUP_WIZARD -> {
                FirstStartSetupWizardScreen(
                    onSetupComplete = { viewModel.navigateTo(Screen.LOGIN) }
                )
            }

            Screen.MAIN -> {
                MainScreen(
                    onNavigateToRecepcion = { viewModel.navigateTo(Screen.RECEPCION) },
                    onNavigateToGerentePin = { viewModel.navigateTo(Screen.GERENTE_PIN) },
                    onLogout = { viewModel.logout() }
                )
            }

            Screen.RECEPCION -> {
                RecepcionScreen(
                    viewModel = viewModel,
                    rooms = rooms,
                    timeRates = timeRates,
                    currentTimeMillis = currentTimeMillis,
                    activeUser = activeUser,
                    onNavigateToCheckInForm = { viewModel.navigateTo(Screen.CHECKIN_FORM) },
                    onBack = { viewModel.navigateTo(Screen.MAIN) }
                )
            }

            Screen.CHECKIN_FORM -> {
                GuestCheckInScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.RECEPCION) }
                )
            }

            Screen.GERENTE_PIN -> {
                // Show MainScreen in background while PIN dialog is open
                MainScreen(
                    onNavigateToRecepcion = { viewModel.navigateTo(Screen.RECEPCION) },
                    onNavigateToGerentePin = {},
                    onLogout = { viewModel.logout() }
                )
                GerentePinDialog(
                    pinError = pinError,
                    onDismiss = { viewModel.navigateTo(Screen.MAIN) },
                    onConfirmPin = { pin -> viewModel.validateManagerPin(pin) }
                )
            }

            Screen.GERENTE_DASHBOARD -> {
                GerenteDashboardScreen(
                    hotelViewModel = viewModel,
                    onNavigateToSection = { section -> viewModel.navigateTo(section) },
                    onBackToMain = { viewModel.navigateTo(Screen.MAIN) }
                )
            }

            Screen.GERENTE_ROOMS -> {
                HotelRoomGridScreen(
                    onNavigateBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_TIMES -> {
                GerenteRoomsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_RATES -> {
                GerenteRatesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_HISTORY -> {
                GerenteHistoryScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_SUPPLIES -> {
                GerenteSuppliesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_SALES -> {
                GerenteSalesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_REPORTS -> {
                GerenteReportsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_INVOICES -> {
                GerenteInvoicesScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_HOUSEKEEPING -> {
                com.example.ui.screens.GerenteHousekeepingScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_AUDIT -> {
                GerenteAuditScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_SETTINGS -> {
                GerenteSettingsScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_USERS -> {
                GerenteUsersScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_BACKUP -> {
                GerenteBackupScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }

            Screen.GERENTE_DEVICE_LINKING -> {
                DeviceDashboardScreen(
                    onBackToManagerMenu = { viewModel.navigateTo(Screen.GERENTE_DASHBOARD) }
                )
            }
        }
    }
}
