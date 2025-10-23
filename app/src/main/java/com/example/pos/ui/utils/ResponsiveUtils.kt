package com.example.pos.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val minDimension = minOf(screenWidth, screenHeight)
    return minDimension >= 600
}

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

@Composable
fun getResponsivePadding(): Dp {
    return if (isTablet()) 24.dp else 16.dp
}

@Composable
fun getResponsiveSpacing(): Dp {
    return if (isTablet()) 16.dp else 12.dp
}

@Composable
fun getResponsiveCardElevation(): Dp {
    return if (isTablet()) 8.dp else 4.dp
}

@Composable
fun getResponsiveIconSize(): Dp {
    return if (isTablet()) 48.dp else 32.dp
}

@Composable
fun getResponsiveButtonHeight(): Dp {
    return if (isTablet()) 64.dp else 48.dp
}

@Composable
fun getResponsiveGridColumns(): Int {
    return if (isTablet()) {
        if (isLandscape()) 4 else 3
    } else {
        if (isLandscape()) 3 else 2
    }
}
