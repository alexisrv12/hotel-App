package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HotelViewModel
import com.example.ui.Screen
import com.example.ui.components.OccupancyTrendCard
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy

data class ManagerMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val targetScreen: Screen,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteDashboardScreen(
    hotelViewModel: HotelViewModel? = null,
    onNavigateToSection: (Screen) -> Unit,
    onBackToMain: () -> Unit
) {
    val isDarkTheme = hotelViewModel?.isDarkTheme?.collectAsState()?.value ?: false

    val lowStockSupplies = hotelViewModel?.lowStockSupplies?.collectAsState()?.value ?: emptyList()

    val menuItems = listOf(
        ManagerMenuItem("Habitaciones", "Gestión de cuartos y estado", Icons.Default.MeetingRoom, Screen.GERENTE_ROOMS, HotelNavy),
        ManagerMenuItem("Limpieza", "Asignación y estado de mucamas", Icons.Default.CleaningServices, Screen.GERENTE_HOUSEKEEPING, HotelGold),
        ManagerMenuItem("Precios y Tarifas", "Edición de tarifas y promociones", Icons.Default.AttachMoney, Screen.GERENTE_RATES, HotelNavy),
        ManagerMenuItem("Tiempos", "Duración de horas, día y noche", Icons.Default.Timer, Screen.GERENTE_ROOMS, HotelGold),
        ManagerMenuItem("Historial", "Consulta general de hospedajes", Icons.Default.History, Screen.GERENTE_HISTORY, HotelNavy),
        ManagerMenuItem("Facturación", "Gestión, reimpresión y anulación", Icons.Default.Receipt, Screen.GERENTE_INVOICES, HotelGold),
        ManagerMenuItem("Auditoría", "Historial de acciones y movimientos", Icons.Default.Security, Screen.GERENTE_AUDIT, HotelNavy),
        ManagerMenuItem("Inventario Insumos", "Descuento automático de insumos", Icons.Default.Inventory2, Screen.GERENTE_SUPPLIES, HotelGold),
        ManagerMenuItem("Inventario Ventas", "Registro de consumos y ventas", Icons.Default.PointOfSale, Screen.GERENTE_SALES, HotelNavy),
        ManagerMenuItem("Reportes", "Ingresos, ocupación y métricas", Icons.Default.Assessment, Screen.GERENTE_REPORTS, HotelGold),
        ManagerMenuItem("Ajustes", "PIN, nombre hotel, moneda", Icons.Default.Settings, Screen.GERENTE_SETTINGS, HotelNavy),
        ManagerMenuItem("Usuarios", "Gestión de personal y accesos", Icons.Default.People, Screen.GERENTE_USERS, HotelGold),
        ManagerMenuItem("Respaldo", "Copia y restauración de datos", Icons.Default.Backup, Screen.GERENTE_BACKUP, HotelNavy),
        ManagerMenuItem("Dispositivos", "Vinculación, historial y alertas", Icons.Default.Devices, Screen.GERENTE_DEVICE_LINKING, HotelGold)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Módulo Gerente", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Hotel Rivera • Administración General", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMain) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar al Menú Principal")
                    }
                },
                actions = {
                    IconButton(onClick = { hotelViewModel?.toggleDarkTheme() }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar Modo Claro/Oscuro",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HotelNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 7-Day Occupancy Trend Card
            OccupancyTrendCard()

            Text(
                text = "Panel de Control Gerencial",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.height(650.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSection(item.targetScreen) }
                            .testTag("menu_${item.title.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Surface(
                                color = item.color.copy(alpha = 0.12f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.color,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

