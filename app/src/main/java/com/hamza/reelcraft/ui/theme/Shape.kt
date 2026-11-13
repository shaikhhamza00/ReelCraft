package com.hamza.reelcraft.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ReelShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp), // Cards
    medium = RoundedCornerShape(24.dp), // Buttons & Chips
    large = RoundedCornerShape(28.dp), // Dialogs / Bottom Sheets
    extraLarge = RoundedCornerShape(32.dp)
)
