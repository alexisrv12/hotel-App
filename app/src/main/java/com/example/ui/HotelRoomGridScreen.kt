package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HotelRoomGridScreen
import com.example.ui.viewmodel.GuestCheckInViewModel

@Composable
fun HotelRoomGridScreen(
    modifier: Modifier = Modifier,
    viewModel: GuestCheckInViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    com.example.ui.screens.HotelRoomGridScreen(
        modifier = modifier,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}
