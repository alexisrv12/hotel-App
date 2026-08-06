package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DeviceDashboardScreen
import com.example.ui.viewmodel.DeviceLinkingViewModel

@Composable
fun DeviceDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceLinkingViewModel = viewModel(),
    onBackToManagerMenu: (() -> Unit)? = null
) {
    com.example.ui.screens.DeviceDashboardScreen(
        modifier = modifier,
        viewModel = viewModel,
        onBackToManagerMenu = onBackToManagerMenu
    )
}
