package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.LinkingHistoryScreen
import com.example.ui.viewmodel.DeviceLinkingViewModel

@Composable
fun LinkingHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceLinkingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    com.example.ui.screens.LinkingHistoryScreen(
        modifier = modifier,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}
