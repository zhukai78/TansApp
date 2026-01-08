package com.portwind.gametrans.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portwind.gametrans.settings.AiTask
import com.portwind.gametrans.ui.theme.GameTransTheme
import com.portwind.gametrans.ui.theme.AnimePrimary
import com.portwind.gametrans.ui.theme.TextPrimary

@Composable
fun FloatingWindowCompose(
    onTranslateClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onPromptTaskSelected: (AiTask) -> Unit = {},
    onAskClicked: () -> Unit = {},
    onPromptDialogClicked: () -> Unit = {},
    isTranslating: Boolean = false,
    translationProgress: String = ""
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
    ) {
        // Main Card - Japanese Minimalist Style (Paper-like)
        Card(
            modifier = Modifier.shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background // PaperWhite
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat, using shadow modifier instead
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Task Selector (Minimalist)
                    TaskSelector(onPromptTaskSelected)

                    Spacer(modifier = Modifier.width(12.dp))

                    // Window Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MinimalIconButton(
                            onClick = onCollapse,
                            icon = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Collapse"
                        )
                        MinimalIconButton(
                            onClick = onClose,
                            icon = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chat Dialog Trigger Logic
                var showChatDialog by remember { mutableStateOf(false) }
                if (showChatDialog) {
                    onAskClicked()
                    showChatDialog = false
                }

                // Main Actions Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ask AI
                    MinimalCircleButton(
                        onClick = { showChatDialog = true },
                        enabled = !isTranslating,
                        text = "?",
                        contentDescription = "Ask AI"
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // Translate Button (Centerpiece)
                    ZenTranslateButton(
                        onClick = onTranslateClick,
                        isTranslating = isTranslating
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    // Edit Prompt
                    MinimalCircleButton(
                        onClick = onPromptDialogClicked,
                        enabled = !isTranslating,
                        icon = Icons.Outlined.Edit,
                        contentDescription = "Edit Prompt"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Text
                Text(
                    text = if (isTranslating) translationProgress else "GameTrans",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isTranslating)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TaskSelector(onPromptTaskSelected: (AiTask) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Task", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Task",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            AiTask.values().forEach { task ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = task.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        expanded = false
                        onPromptTaskSelected(task)
                    }
                )
            }
        }
    }
}

@Composable
fun ZenTranslateButton(
    onClick: () -> Unit,
    isTranslating: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isTranslating) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isTranslating) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
                      else MaterialTheme.colorScheme.primary,
        label = "color"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            .background(containerColor, CircleShape)
            .clickable(enabled = !isTranslating, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isTranslating) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = "Translate",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MinimalIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun MinimalCircleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String? = null,
    icon: ImageVector? = null,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(
                1.dp, 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), 
                CircleShape
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Light),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

// Collapsed State View
@Composable
fun CollapsedFloatingWindow(
    onExpand: () -> Unit = {},
    onDrag: (Offset) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDrag(dragAmount)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                )
                .clickable(onClick = onExpand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Expand",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview
@Composable
fun FloatingWindowComposePreview() {
    GameTransTheme {
        FloatingWindowCompose()
    }
} 