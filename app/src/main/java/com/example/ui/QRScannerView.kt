package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.QRScannerView

@Composable
fun QRScannerView(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    onCloseScanner: (() -> Unit)? = null
) {
    com.example.ui.components.QRScannerView(
        modifier = modifier,
        onQrCodeScanned = onQrCodeScanned,
        onCloseScanner = onCloseScanner
    )
}
