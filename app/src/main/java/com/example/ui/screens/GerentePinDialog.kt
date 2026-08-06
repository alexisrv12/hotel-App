package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.StatusRed

@Composable
fun GerentePinDialog(
    pinError: String?,
    onDismiss: () -> Unit,
    onConfirmPin: (String) -> Unit
) {
    var pinValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = HotelGold,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Acceso Módulo Gerente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ingrese el PIN de Seguridad", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // PIN Dots Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pinValue.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) HotelNavy else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (isFilled) HotelNavy else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (pinError != null) {
                    Text(
                        text = pinError,
                        color = StatusRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Numeric Keypad Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLEAR", "0", "BACK")
                    )

                    keyRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    label = key,
                                    onClick = {
                                        when (key) {
                                            "CLEAR" -> pinValue = ""
                                            "BACK" -> if (pinValue.isNotEmpty()) pinValue = pinValue.dropLast(1)
                                            else -> if (pinValue.length < 4) pinValue += key
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPin(pinValue) },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                enabled = pinValue.length == 4,
                modifier = Modifier.testTag("btn_pin_confirm")
            ) {
                Text("Ingresar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (label == "CLEAR" || label == "BACK") MaterialTheme.colorScheme.surfaceVariant else HotelNavy.copy(alpha = 0.08f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                "BACK" -> Icon(Icons.Default.Backspace, contentDescription = "Borrar", modifier = Modifier.size(20.dp), tint = HotelNavy)
                "CLEAR" -> Text("C", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 16.sp)
                else -> Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
            }
        }
    }
}
