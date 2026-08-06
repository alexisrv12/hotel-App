package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen

data class DayOccupancy(
    val dayName: String,
    val dayShort: String,
    val occupancyPercent: Int, // 0 to 100
    val occupiedRooms: Int,
    val totalRooms: Int = 20,
    val checkIns: Int,
    val estimatedRevenue: Double
)

@Composable
fun OccupancyTrendCard(
    modifier: Modifier = Modifier,
    dayData: List<DayOccupancy> = default7DayData()
) {
    var selectedDayIndex by remember { mutableStateOf(5) } // Default: Sábado
    val selectedDay = dayData.getOrElse(selectedDayIndex) { dayData.last() }

    val avgOccupancy = dayData.map { it.occupancyPercent }.average().toInt()
    val peakDay = dayData.maxByOrNull { it.occupancyPercent }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Tendencia de Ocupación",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ocupación Semanal (7 días)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Promedio: $avgOccupancy% • Pico: ${peakDay?.dayName} (${peakDay?.occupancyPercent}%)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StatusGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+5.4%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Bars Layout
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = HotelGold

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dayData.forEachIndexed { index, data ->
                    val isSelected = index == selectedDayIndex
                    val animatedHeightRatio by animateFloatAsState(
                        targetValue = data.occupancyPercent / 100f,
                        animationSpec = tween(durationMillis = 600, delayMillis = index * 50),
                        label = "BarHeightAnimation"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable { selectedDayIndex = index }
                    ) {
                        // Percent text over bar
                        Text(
                            text = "${data.occupancyPercent}%",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Visual Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Background track bar
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )

                            // Filled bar with gradient
                            val gradientColors = if (isSelected) {
                                listOf(HotelGold, primaryColor)
                            } else {
                                listOf(primaryColor.copy(alpha = 0.6f), primaryColor.copy(alpha = 0.9f))
                            }

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(animatedHeightRatio)
                                    .background(
                                        brush = Brush.verticalGradient(gradientColors),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day Short Label
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) primaryColor else Color.Transparent,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = data.dayShort,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Day Breakdown Detail Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Día Seleccionado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedDay.dayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Habitaciones Ocupadas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${selectedDay.occupiedRooms} / ${selectedDay.totalRooms}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Check-ins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${selectedDay.checkIns}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ingreso Estimado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%.0f", selectedDay.estimatedRevenue)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HotelGold)
                    }
                }
            }
        }
    }
}

fun default7DayData(): List<DayOccupancy> {
    return listOf(
        DayOccupancy("Lunes", "L", 60, 12, 20, 4, 1440.0),
        DayOccupancy("Martes", "M", 65, 13, 20, 5, 1560.0),
        DayOccupancy("Miércoles", "M", 70, 14, 20, 6, 1680.0),
        DayOccupancy("Jueves", "J", 75, 15, 20, 7, 1800.0),
        DayOccupancy("Viernes", "V", 85, 17, 20, 9, 2160.0),
        DayOccupancy("Sábado", "S", 92, 18, 20, 11, 2380.0),
        DayOccupancy("Domingo", "D", 80, 16, 20, 6, 1920.0)
    )
}
