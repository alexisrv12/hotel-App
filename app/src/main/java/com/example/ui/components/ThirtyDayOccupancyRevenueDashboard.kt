package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.InvoiceEntity
import com.example.data.entities.SaleRecordEntity
import com.example.data.entities.StayHistoryEntity
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Data model for daily occupancy and revenue metrics over the timeline.
 */
data class DayMetric(
    val dayNumber: Int,          // e.g. 1..30
    val dateString: String,      // e.g. "12/08"
    val fullDateString: String,  // e.g. "12 Ago 2026"
    val dayOfWeek: String,       // e.g. "Lun", "Mar"
    val occupancyRate: Float,    // 0..100 %
    val occupiedRooms: Int,
    val totalRooms: Int = 20,
    val checkInCount: Int,
    val staysRevenue: Double,
    val extraSalesRevenue: Double,
    val totalRevenue: Double
)

/**
 * Interactive Dashboard Component visualizing occupancy and revenue trends.
 * Supports Minimizing/Collapsing, Resetting metrics to zero, touch scrubbing, and area gradients.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThirtyDayOccupancyRevenueDashboard(
    modifier: Modifier = Modifier,
    stayHistory: List<StayHistoryEntity> = emptyList(),
    invoices: List<InvoiceEntity> = emptyList(),
    saleRecords: List<SaleRecordEntity> = emptyList(),
    totalRoomsCount: Int = 20,
    isInitiallyMinimized: Boolean = false,
    onResetMetrics: (() -> Unit)? = null
) {
    // Collapsed / Minimized state
    var isMinimized by remember { mutableStateOf(isInitiallyMinimized) }
    // Confirmation dialog state for zero reset
    var showResetDialog by remember { mutableStateOf(false) }

    // Time Range selector: 30 days (default), 14 days, 7 days
    var selectedRangeDays by remember { mutableIntStateOf(30) }
    // Metric mode: "BOTH" (Occupancy + Revenue), "OCCUPANCY" (%), "REVENUE" (Q)
    var selectedMetricMode by remember { mutableStateOf("BOTH") }

    // Generate days of data strictly based on real records (starting from zero if empty/reset)
    val all30DaysData = remember(stayHistory, invoices, saleRecords, totalRoomsCount) {
        generate30DayMetrics(stayHistory, invoices, saleRecords, totalRoomsCount)
    }

    val displayData = remember(all30DaysData, selectedRangeDays) {
        all30DaysData.takeLast(selectedRangeDays)
    }

    // Touch scrubbing state for interactive hover tooltip
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    val activeHoverIndex = hoveredIndex?.coerceIn(0, displayData.size - 1) ?: (displayData.size - 1)
    val activeMetric = displayData.getOrNull(activeHoverIndex) ?: displayData.last()

    // Aggregates for the selected period
    val avgOccupancy = if (displayData.isNotEmpty()) displayData.map { it.occupancyRate }.average().toFloat() else 0f
    val totalPeriodRevenue = displayData.sumOf { it.totalRevenue }
    val avgDailyRevenue = if (displayData.isNotEmpty()) totalPeriodRevenue / displayData.size else 0.0
    val peakOccupancyDay = displayData.maxByOrNull { it.occupancyRate }
    val isCleanZeroState = displayData.all { it.totalRevenue == 0.0 && it.occupancyRate == 0f }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("thirty_day_recharts_dashboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Title, Controls (Minimize / Expand & Reset to Zero)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isMinimized = !isMinimized }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = HotelNavy.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = HotelNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tendencia de Ocupación e Ingresos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCleanZeroState) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0D9488).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "En Cero",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D9488),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isMinimized) {
                                "Ocupación: ${String.format(Locale.US, "%.1f", avgOccupancy)}% • Total: Q${String.format(Locale.US, "%,.2f", totalPeriodRevenue)}"
                            } else {
                                "Histórico interactivo • Últimos $selectedRangeDays días"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action buttons: Reset to Zero & Minimize/Expand toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Reset to Zero Button
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_reset_trends_zero")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reestablecer a cero",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Minimize / Expand Toggle Button
                    IconButton(
                        onClick = { isMinimized = !isMinimized },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_toggle_minimize_trends")
                    ) {
                        Icon(
                            imageVector = if (isMinimized) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isMinimized) "Expandir vista" else "Minimizar vista",
                            tint = HotelNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Minimized Quick Summary Badges (Visible only when minimized)
            if (isMinimized) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0D9488).copy(alpha = 0.08f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isMinimized = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Hotel,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ocupación", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", avgOccupancy)}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D9488)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HotelGold.copy(alpha = 0.12f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isMinimized = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = HotelGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ingresos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "Q${String.format(Locale.US, "%,.2f", totalPeriodRevenue)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelGold
                            )
                        }
                    }
                }
            }

            // Expanded Full Dashboard Content
            AnimatedVisibility(
                visible = !isMinimized,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Time Range & Metric Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Range pills (30d, 14d, 7d)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(30 to "30 Días", 14 to "14 Días", 7 to "7 Días").forEach { (days, label) ->
                                FilterChip(
                                    selected = selectedRangeDays == days,
                                    onClick = {
                                        selectedRangeDays = days
                                        hoveredIndex = null
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = HotelNavy,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }

                        // Metric Toggle: BOTH, OCCUPANCY, REVENUE
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMetricMode == "BOTH") HotelNavy.copy(alpha = 0.12f) else Color.Transparent,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "Dual",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMetricMode == "BOTH") HotelNavy else Color.Gray,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .clickable { selectedMetricMode = "BOTH" }
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMetricMode == "OCCUPANCY") Color(0xFF0D9488).copy(alpha = 0.12f) else Color.Transparent,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "Ocupación",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMetricMode == "OCCUPANCY") Color(0xFF0D9488) else Color.Gray,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .clickable { selectedMetricMode = "OCCUPANCY" }
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedMetricMode == "REVENUE") HotelGold.copy(alpha = 0.18f) else Color.Transparent,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "Ingresos",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMetricMode == "REVENUE") HotelGold else Color.Gray,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .clickable { selectedMetricMode = "REVENUE" }
                                )
                            }
                        }
                    }

                    // Summary KPI Cards Grid
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
                    ) {
                        // KPI 1: Average Occupancy
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0D9488).copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Hotel,
                                        contentDescription = null,
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ocupación Media", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", avgOccupancy)}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D9488)
                                )
                                Text(
                                    text = if (peakOccupancyDay != null && peakOccupancyDay.occupancyRate > 0f) {
                                        "Pico: ${peakOccupancyDay.occupancyRate.toInt()}% (${peakOccupancyDay.dayOfWeek} ${peakOccupancyDay.dateString})"
                                    } else {
                                        "Base: 0% (Inicio limpio)"
                                    },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // KPI 2: Total Period Revenue
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HotelGold.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = HotelGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ingresos Totales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Q${String.format(Locale.US, "%,.2f", totalPeriodRevenue)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelGold
                                )
                                Text(
                                    text = "Prom. Diario: Q${String.format(Locale.US, "%,.0f", avgDailyRevenue)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Interactive Tooltip Card (scannable details for the hovered day)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HotelNavy.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HotelNavy.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = HotelNavy,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${activeMetric.dayOfWeek} ${activeMetric.fullDateString}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = HotelNavy
                                    )
                                }
                                Text(
                                    text = "Check-ins: ${activeMetric.checkInCount} • Habitaciones: ${activeMetric.occupiedRooms}/${activeMetric.totalRooms}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${activeMetric.occupancyRate.toInt()}% Ocupación",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0D9488)
                                )
                                Text(
                                    text = "Q${String.format(Locale.US, "%.2f", activeMetric.totalRevenue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = HotelGold
                                )
                            }
                        }
                    }

                    // Chart Legends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedMetricMode == "BOTH" || selectedMetricMode == "OCCUPANCY") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF0D9488), shape = CircleShape)
                                )
                                Text("Tasa de Ocupación (%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (selectedMetricMode == "BOTH") {
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        if (selectedMetricMode == "BOTH" || selectedMetricMode == "REVENUE") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(HotelGold, shape = CircleShape)
                                )
                                Text("Ingresos Diarios (Q)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // MAIN CANVAS VISUALIZATION (Area + Smooth Line Chart)
                    val occupancyColor = Color(0xFF0D9488)
                    val revenueColor = HotelGold
                    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .pointerInput(displayData) {
                                    detectTapGestures { offset ->
                                        val count = displayData.size.coerceAtLeast(1)
                                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                                        val idx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                                        hoveredIndex = idx
                                    }
                                }
                                .pointerInput(displayData) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val count = displayData.size.coerceAtLeast(1)
                                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                                        val idx = (change.position.x / stepX).toInt().coerceIn(0, count - 1)
                                        hoveredIndex = idx
                                    }
                                }
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val bottomPadding = 24f
                            val topPadding = 16f
                            val chartHeight = canvasHeight - bottomPadding - topPadding

                            val maxRevenue = displayData.maxOfOrNull { it.totalRevenue }?.coerceAtLeast(100.0) ?: 100.0
                            val count = displayData.size
                            val stepX = if (count > 1) canvasWidth / (count - 1) else canvasWidth

                            // 4 Horizontal Grid lines
                            for (i in 0..4) {
                                val y = topPadding + (chartHeight / 4) * i
                                drawLine(
                                    color = gridLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(canvasWidth, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                )
                            }

                            // 1. DRAW OCCUPANCY AREA & LINE
                            if (selectedMetricMode == "BOTH" || selectedMetricMode == "OCCUPANCY") {
                                val areaPath = Path()
                                val linePath = Path()

                                displayData.forEachIndexed { index, item ->
                                    val x = index * stepX
                                    val normalizedOcc = (item.occupancyRate / 100f).coerceIn(0f, 1f)
                                    val y = topPadding + chartHeight * (1f - normalizedOcc)

                                    if (index == 0) {
                                        areaPath.moveTo(x, canvasHeight - bottomPadding)
                                        areaPath.lineTo(x, y)
                                        linePath.moveTo(x, y)
                                    } else {
                                        val prevX = (index - 1) * stepX
                                        val prevNormalized = (displayData[index - 1].occupancyRate / 100f).coerceIn(0f, 1f)
                                        val prevY = topPadding + chartHeight * (1f - prevNormalized)

                                        val controlX1 = prevX + (x - prevX) / 2
                                        val controlY1 = prevY
                                        val controlX2 = prevX + (x - prevX) / 2
                                        val controlY2 = y

                                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                        areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                    }

                                    if (index == count - 1) {
                                        areaPath.lineTo(x, canvasHeight - bottomPadding)
                                        areaPath.close()
                                    }
                                }

                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            occupancyColor.copy(alpha = 0.30f),
                                            occupancyColor.copy(alpha = 0.05f),
                                            Color.Transparent
                                        ),
                                        startY = topPadding,
                                        endY = canvasHeight - bottomPadding
                                    )
                                )

                                drawPath(
                                    path = linePath,
                                    color = occupancyColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // 2. DRAW REVENUE TREND LINE
                            if (selectedMetricMode == "BOTH" || selectedMetricMode == "REVENUE") {
                                val revLinePath = Path()

                                displayData.forEachIndexed { index, item ->
                                    val x = index * stepX
                                    val normalizedRev = (item.totalRevenue / maxRevenue).toFloat().coerceIn(0f, 1f)
                                    val y = topPadding + chartHeight * (1f - normalizedRev)

                                    if (index == 0) {
                                        revLinePath.moveTo(x, y)
                                    } else {
                                        val prevX = (index - 1) * stepX
                                        val prevNorm = (displayData[index - 1].totalRevenue / maxRevenue).toFloat().coerceIn(0f, 1f)
                                        val prevY = topPadding + chartHeight * (1f - prevNorm)

                                        val controlX1 = prevX + (x - prevX) / 2
                                        val controlY1 = prevY
                                        val controlX2 = prevX + (x - prevX) / 2
                                        val controlY2 = y

                                        revLinePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                    }
                                }

                                drawPath(
                                    path = revLinePath,
                                    color = revenueColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // 3. CURSOR & ACTIVE POINT MARKER
                            val activeX = activeHoverIndex * stepX
                            drawLine(
                                color = HotelNavy.copy(alpha = 0.5f),
                                start = Offset(activeX, topPadding),
                                end = Offset(activeX, canvasHeight - bottomPadding),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            )

                            if (selectedMetricMode == "BOTH" || selectedMetricMode == "OCCUPANCY") {
                                val occY = topPadding + chartHeight * (1f - (activeMetric.occupancyRate / 100f).coerceIn(0f, 1f))
                                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(activeX, occY))
                                drawCircle(color = occupancyColor, radius = 4.dp.toPx(), center = Offset(activeX, occY))
                            }

                            if (selectedMetricMode == "BOTH" || selectedMetricMode == "REVENUE") {
                                val revY = topPadding + chartHeight * (1f - (activeMetric.totalRevenue / maxRevenue).toFloat().coerceIn(0f, 1f))
                                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(activeX, revY))
                                drawCircle(color = revenueColor, radius = 4.dp.toPx(), center = Offset(activeX, revY))
                            }
                        }
                    }

                    // X-Axis Date Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labelStep = when (selectedRangeDays) {
                            30 -> 6
                            14 -> 3
                            else -> 1
                        }
                        displayData.forEachIndexed { index, item ->
                            if (index % labelStep == 0 || index == displayData.size - 1) {
                                Text(
                                    text = item.dateString,
                                    fontSize = 10.sp,
                                    color = if (index == activeHoverIndex) HotelNavy else Color.Gray,
                                    fontWeight = if (index == activeHoverIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog to Reset to Zero
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reestablecer Tendencias a Cero",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = HotelNavy
                )
            },
            text = {
                Text(
                    text = "¿Deseas reiniciar el historial de ocupación e ingresos para que las gráficas y estadísticas empiecen desde cero (0% de ocupación y Q0.00)?\n\nLos nuevos registros se acumularán en tiempo real a partir de este momento.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetMetrics?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    modifier = Modifier.testTag("btn_confirm_reset_zero")
                ) {
                    Text("Sí, Reestablecer a Cero", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Generates days of metrics directly from real stay history, invoices, and sales.
 * Starts strictly from 0 (0% occupancy, Q0.00 revenue) if there are no registered records.
 */
private fun generate30DayMetrics(
    stayHistory: List<StayHistoryEntity>,
    invoices: List<InvoiceEntity>,
    saleRecords: List<SaleRecordEntity>,
    totalRooms: Int
): List<DayMetric> {
    val list = mutableListOf<DayMetric>()
    val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())

    for (dayOffset in 29 downTo 0) {
        val dayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dayOffset)
        }
        val dateString = dayFormat.format(dayCal.time)
        val fullDateString = fullDateFormat.format(dayCal.time)
        val dayOfWeek = dayNameFormat.format(dayCal.time).replaceFirstChar { it.uppercase() }

        // Filter real records matching this day
        val dayStays = stayHistory.filter { it.dateString.contains(dateString) }
        val dayInvoices = invoices.filter { !it.isVoided && it.dateString.contains(dateString) }
        val daySales = saleRecords.filter { dayFormat.format(Date(it.timestampMillis)).contains(dateString) }

        val realStaysRevenue = dayStays.sumOf { it.priceCharged } + dayInvoices.sumOf { it.totalAmount }
        val realSalesRevenue = daySales.sumOf { it.totalPrice }
        val realCheckIns = dayStays.size
        val totalRevenue = realStaysRevenue + realSalesRevenue

        // Calculate occupancy rate strictly from actual registered stays
        val occupancyRate = if (totalRooms > 0 && realCheckIns > 0) {
            ((realCheckIns.toFloat() / totalRooms.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        val occupiedRooms = if (totalRooms > 0) realCheckIns.coerceAtMost(totalRooms) else 0

        list.add(
            DayMetric(
                dayNumber = 30 - dayOffset,
                dateString = dateString,
                fullDateString = fullDateString,
                dayOfWeek = dayOfWeek,
                occupancyRate = occupancyRate,
                occupiedRooms = occupiedRooms,
                totalRooms = totalRooms,
                checkInCount = realCheckIns,
                staysRevenue = realStaysRevenue,
                extraSalesRevenue = realSalesRevenue,
                totalRevenue = totalRevenue
            )
        )
    }

    return list
}
