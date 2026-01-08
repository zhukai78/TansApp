package com.portwind.gametrans.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimeCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 0.dp, // Flat by default for Zen style
    containerColor: Color = MaterialTheme.colorScheme.surface, // Paper style (opaque)
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            spotColor = Color.Black.copy(alpha = 0.05f) // Soft shadow
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = null, // No border for cleaner look
        content = content
    )
}
