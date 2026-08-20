package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SharedAppState
import com.example.data.model.SyncHealthDataPoint
import com.example.data.model.SyncHealthReport
import com.example.data.model.SyncState
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.viewmodel.SharedSyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncHealthScreen(
    viewModel: SharedSyncViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.sharedState.collectAsState()
    val healthReport = state.healthReport

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("sync_health_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Salud de Sincronización",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Monitoreo en Tiempo Real & Cuellos de Botella",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_sync_health_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.forceSyncQueuedData() },
                        modifier = Modifier.testTag("btn_sync_health_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar métricas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Banner de Diagnóstico Global
            item {
                HealthDiagnosticBanner(
                    report = healthReport,
                    syncState = state.syncStatus,
                    queueSize = state.offlineQueueSize
                )
            }

            // 2. Tarjetas KPI de Calidad de Conexión
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        title = "Latencia RTT",
                        value = "${healthReport.currentLatencyMs} ms",
                        statusColor = if (healthReport.currentLatencyMs < 100) Color(0xFF2E7D32) else Color(0xFFE65100),
                        subtitle = if (healthReport.currentLatencyMs < 100) "Excelente" else "Elevada"
                    )
                    KpiMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CloudQueue,
                        title = "Cola Offline",
                        value = "${state.offlineQueueSize}",
                        statusColor = if (state.offlineQueueSize == 0) Color(0xFF1565C0) else Color(0xFFC62828),
                        subtitle = if (state.offlineQueueSize == 0) "Vaciada" else "Encolada"
                    )
                    KpiMetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Wifi,
                        title = "Señal Wi-Fi",
                        value = "${healthReport.signalStrengthPercent}%",
                        statusColor = Color(0xFF00796B),
                        subtitle = healthReport.connectionType.split(" ").firstOrNull() ?: "5G"
                    )
                }
            }

            // 3. Gráfico de Barras Estilo Recharts (Latencia & Cola de Sincronización)
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recharts_sync_health_chart"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rendimiento del Canal de Sincronización",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Latencia RTT (ms) vs. Tamaño de Cola por Intervalo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Componente de Gráfico Recharts-Style
                        RechartsSyncBarChart(
                            dataPoints = healthReport.hourlyDataPoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Leyenda de Colores
                        ChartLegend()
                    }
                }
            }

            // 4. Diagnóstico de Base de Datos Local Room & Respaldo Offline
            item {
                RoomDatabaseHealthCard(state = state)
            }

            // 5. Herramientas y Acciones de Diagnóstico para el Gerente
            item {
                DiagnosticActionsCard(
                    viewModel = viewModel,
                    queueSize = state.offlineQueueSize
                )
            }
        }
    }
}

/**
 * Banner de Diagnóstico de Salud
 */
@Composable
private fun HealthDiagnosticBanner(
    report: SyncHealthReport,
    syncState: SyncState,
    queueSize: Int
) {
    val isHealthy = syncState == SyncState.SYNCED && queueSize == 0
    val containerColor = if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val contentColor = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFE65100)
    val icon = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, contentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(contentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHealthy) "Canal de Sincronización Saludable" else "Detección de Cuellos de Botella",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = report.bottleneckDiagnosis,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Tarjeta de Métrica KPI
 */
@Composable
private fun KpiMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    statusColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = statusColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Componente de Gráfico de Barras Estilo Recharts
 * Dibuja barras duales (Latencia RTT y Tamaño de Cola) con eje Y, líneas de cuadrícula y etiquetas.
 */
@Composable
fun RechartsSyncBarChart(
    dataPoints: List<SyncHealthDataPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val normalBarColor = Color(0xFF0288D1)
    val bottleneckBarColor = Color(0xFFE53935)
    val queueBarColor = Color(0xFFFFA000)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val count = dataPoints.size
                        if (count > 0) {
                            val slotWidth = size.width / count
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, count - 1)
                            selectedIndex = if (selectedIndex == index) -1 else index
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height - 30f // Espacio para etiquetas del eje X
            val maxLatency = 350f // Escala máxima en ms

            // 1. Líneas de Cuadrícula Horizontales (0ms, 100ms, 200ms, 300ms)
            val gridLevels = listOf(100f, 200f, 300f)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            gridLevels.forEach { level ->
                val y = height - (level / maxLatency) * height
                drawLine(
                    color = gridColor,
                    start = Offset(40f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.5f,
                    pathEffect = pathEffect
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${level.toInt()}ms",
                    10f,
                    y + 10f,
                    android.graphics.Paint().apply {
                        color = textColor
                        textSize = 24f
                        isAntiAlias = true
                    }
                )
            }

            if (dataPoints.isEmpty()) return@Canvas

            // 2. Dibujar Barras por cada Punto de Datos
            val count = dataPoints.size
            val slotWidth = (width - 50f) / count
            val barWidth = slotWidth * 0.35f

            dataPoints.forEachIndexed { i, dp ->
                val xCenter = 50f + i * slotWidth + slotWidth / 2f

                // Barra de Latencia
                val latencyHeight = (dp.latencyMs / maxLatency * height).coerceIn(4f, height)
                val latencyTop = height - latencyHeight
                val barColor = if (dp.isBottleneck) bottleneckBarColor else normalBarColor

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(xCenter - barWidth - 2f, latencyTop),
                    size = Size(barWidth, latencyHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Barra de Cola de Sincronización
                val queueNormalized = (dp.queueSize / 4f * height * 0.7f).coerceIn(if (dp.queueSize > 0) 8f else 0f, height)
                if (dp.queueSize > 0) {
                    val queueTop = height - queueNormalized
                    drawRoundRect(
                        color = queueBarColor,
                        topLeft = Offset(xCenter + 2f, queueTop),
                        size = Size(barWidth, queueNormalized),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                // Etiqueta Eje X (Hora / Intervalo)
                drawContext.canvas.nativeCanvas.drawText(
                    dp.label,
                    xCenter - 20f,
                    height + 25f,
                    android.graphics.Paint().apply {
                        color = textColor
                        textSize = 26f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                )
            }
        }

        // Tooltip Emergente si se selecciona una barra
        if (selectedIndex in dataPoints.indices) {
            val point = dataPoints[selectedIndex]
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = HotelNavy,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${point.label}: ${point.latencyMs.toInt()} ms",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (point.queueSize > 0) {
                        Text(
                            text = "Cola: ${point.queueSize.toInt()} ops",
                            color = Color(0xFFFFA000),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Leyenda de Colores para el Gráfico
 */
@Composable
private fun ChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = Color(0xFF0288D1), label = "Latencia Normal (<150ms)")
        LegendItem(color = Color(0xFFE53935), label = "Cuello de Botella (>250ms)")
        LegendItem(color = Color(0xFFFFA000), label = "Cola Offline")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tarjeta de Diagnóstico de Room Database Local
 */
@Composable
private fun RoomDatabaseHealthCard(state: SharedAppState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Capa Local Room Database (Offline First)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Copia de seguridad local persistida en 'shared_app_state_backup'. Permite inicio instantáneo sin conectividad a internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Habitaciones en Caché Local: ${state.activeRooms.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Estado: Activo & Sincronizado",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Acciones de Diagnóstico y Pruebas
 */
@Composable
private fun DiagnosticActionsCard(
    viewModel: SharedSyncViewModel,
    queueSize: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Acciones de Control & Prueba",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.forceSyncQueuedData() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_force_sync_queue")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vacíar Cola (${queueSize})", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.simulateSessionExpiration() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_test_expire_session")
                ) {
                    Text("Expirar Sesión (1 Mes)", fontSize = 12.sp)
                }
            }
        }
    }
}
