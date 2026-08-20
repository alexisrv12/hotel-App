package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entities.SupplyEntity
import com.example.ui.HotelViewModel
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusYellow

/**
 * Automated Banner and Interactive Management Sheet for room inventory items
 * that have fallen below their defined minimum threshold.
 */
@Composable
fun InventoryThresholdAlertBanner(
    lowStockSupplies: List<SupplyEntity>,
    viewModel: HotelViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSupplies: (() -> Unit)? = null
) {
    var showManageDialog by remember { mutableStateOf(false) }
    var dismissedForSession by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = lowStockSupplies.isNotEmpty() && !dismissedForSession,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = StatusRed.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("low_inventory_threshold_alert_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StatusRed,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Alerta de Inventario Mínimo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = StatusRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusRed
                            ) {
                                Text(
                                    text = "${lowStockSupplies.size} insumo(s) críticos",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = lowStockSupplies.take(3).joinToString(", ") { "${it.name} (${it.stockCurrent}/${it.stockMinimum} ${it.unit})" } +
                                    if (lowStockSupplies.size > 3) " y ${lowStockSupplies.size - 3} más..." else "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { showManageDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Reabastecer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { dismissedForSession = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Ocultar Alerta", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Modal Dialog to review and adjust threshold / restock supplies
    if (showManageDialog) {
        var editingThresholdSupply by remember { mutableStateOf<SupplyEntity?>(null) }
        var newThresholdText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showManageDialog = false
                editingThresholdSupply = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = StatusRed)
                    Text("Insumos Bajo Umbral Mínimo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Los siguientes artículos han caído por debajo del umbral de seguridad. Puede reabastecerlos o ajustar su umbral mínimo de notificación:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (editingThresholdSupply != null) {
                        // Quick Edit Threshold View
                        val item = editingThresholdSupply!!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Ajustar Umbral Mínimo para: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = newThresholdText,
                                    onValueChange = { newThresholdText = it },
                                    label = { Text("Nuevo Umbral Mínimo (${item.unit})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(onClick = { editingThresholdSupply = null }) {
                                        Text("Cancelar", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            val newMin = newThresholdText.toDoubleOrNull() ?: item.stockMinimum
                                            viewModel.saveSupply(item.copy(stockMinimum = newMin))
                                            editingThresholdSupply = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                                    ) {
                                        Text("Guardar Umbral", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lowStockSupplies, key = { it.id }) { supply ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = StatusRed.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(supply.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Stock: ${supply.stockCurrent} ${supply.unit} • Mínimo: ${supply.stockMinimum} ${supply.unit}",
                                            fontSize = 11.sp,
                                            color = StatusRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingThresholdSupply = supply
                                                newThresholdText = supply.stockMinimum.toString()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = "Ajustar Umbral", tint = HotelNavy, modifier = Modifier.size(16.dp))
                                        }

                                        Button(
                                            onClick = { viewModel.restockSupply(supply.id, 50.0) },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("+50", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManageDialog = false
                        editingThresholdSupply = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                if (onNavigateToSupplies != null) {
                    OutlinedButton(onClick = {
                        showManageDialog = false
                        onNavigateToSupplies()
                    }) {
                        Text("Ir a Inventario Completo")
                    }
                }
            }
        )
    }
}
