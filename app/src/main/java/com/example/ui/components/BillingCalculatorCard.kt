package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BillingCalculatorCard(
    modifier: Modifier = Modifier,
    initialRoomRate: Double = 120.0,
    initialNights: Int = 2,
    roomNumber: String = "104",
    guestName: String = "Huésped General"
) {
    val context = LocalContext.current

    var roomRateInput by remember { mutableStateOf(initialRoomRate.toString()) }
    var nightsCount by remember { mutableIntStateOf(initialNights) }
    var includeCleaningFee by remember { mutableStateOf(false) }
    var includeMinibarFee by remember { mutableStateOf(false) }
    var includeParkingFee by remember { mutableStateOf(false) }
    var applyIvaTax by remember { mutableStateOf(true) }

    var showPrintReceiptDialog by remember { mutableStateOf(false) }

    // Calculations
    val roomRate = roomRateInput.toDoubleOrNull() ?: 0.0
    val baseStaySubtotal = roomRate * nightsCount
    val cleaningFee = if (includeCleaningFee) 15.0 else 0.0
    val minibarFee = if (includeMinibarFee) 25.0 else 0.0
    val parkingFee = if (includeParkingFee) 10.0 * nightsCount else 0.0
    val extraServicesSubtotal = cleaningFee + minibarFee + parkingFee

    val subtotal = baseStaySubtotal + extraServicesSubtotal
    val taxAmount = if (applyIvaTax) subtotal * 0.16 else 0.0
    val totalAmount = subtotal + taxAmount

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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HotelNavy.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Calculadora de Cobro",
                        tint = HotelNavy,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column {
                    Text(
                        text = "Calculadora Automática de Facturación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cálculo de estadía, insumos e impresión de comprobante",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inputs Row: Rate & Nights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = roomRateInput,
                    onValueChange = { roomRateInput = it },
                    label = { Text("Tarifa Noche ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Nights Counter
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Noches / Horas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (nightsCount > 1) nightsCount-- },
                            enabled = nightsCount > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos")
                        }
                        Text(
                            text = "$nightsCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { nightsCount++ }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Extra Services Checkchips
            Text("Servicios Adicionales:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = includeCleaningFee,
                    onClick = { includeCleaningFee = !includeCleaningFee },
                    label = { Text("Limpieza ($15)") },
                    leadingIcon = { Icon(Icons.Default.LocalLaundryService, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = includeMinibarFee,
                    onClick = { includeMinibarFee = !includeMinibarFee },
                    label = { Text("Minibar ($25)") },
                    leadingIcon = { Icon(Icons.Default.WineBar, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = includeParkingFee,
                    onClick = { includeParkingFee = !includeParkingFee },
                    label = { Text("Park ($10/n)") },
                    leadingIcon = { Icon(Icons.Default.LocalParking, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tax Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aplicar IVA (16%)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = applyIvaTax,
                    onCheckedChange = { applyIvaTax = it }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider()
            Spacer(modifier = Modifier.height(14.dp))

            // Summary Breakdown Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = HotelNavy.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal Estadía ($nightsCount n. @ $$roomRate)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%.2f", baseStaySubtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (extraServicesSubtotal > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Servicios Adicionales", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%.2f", extraServicesSubtotal)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (applyIvaTax) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("IVA (16%)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%.2f", taxAmount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL A COBRAR", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = HotelNavy)
                        Text(
                            text = "$${String.format("%.2f", totalAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HotelGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Print Receipt Action Button
            Button(
                onClick = { showPrintReceiptDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Imprimir Comprobante de Pago", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // Print Receipt Dialog Mock
    if (showPrintReceiptDialog) {
        Dialog(onDismissRequest = { showPrintReceiptDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Thermal Ticket Preview Styling
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFFBEB), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFCD34D), shape = RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HOTEL RIVERA S.A.C.", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Text("RUC: 20123456789", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.DarkGray)
                        Text("Av. Principal 456, Rivera", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.DarkGray)
                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)

                        Text("COMPROBANTE DE PAGO POS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                        Text("Fecha: $currentDate", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("Habitación: $roomNumber • Huésped: $guestName", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$nightsCount n. Hab $roomNumber @ $$roomRate", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                Text("$${String.format("%.2f", baseStaySubtotal)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                            if (includeCleaningFee) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Servicio de Limpieza", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("$$cleaningFee", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                            if (includeMinibarFee) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Consumos Minibar", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("$$minibarFee", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                            if (includeParkingFee) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estacionamiento ($nightsCount n.)", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("$$parkingFee", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                            if (applyIvaTax) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("IGV / IVA (16%)", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    Text("$${String.format("%.2f", taxAmount)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                }
                            }
                        }

                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL PAGADO:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$${String.format("%.2f", totalAmount)}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("----------------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Text("¡Gracias por su preferencia!", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("[||| | ||||| ||| |||| |||||]", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.DarkGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { showPrintReceiptDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cerrar")
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Enviando ticket a impresora térmica POS...", Toast.LENGTH_LONG).show()
                                showPrintReceiptDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imprimir")
                        }
                    }
                }
            }
        }
    }
}
