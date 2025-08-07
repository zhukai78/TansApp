package com.portwind.gametrans

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.portwind.gametrans.ui.theme.GameTransTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun FloatingWindowCompose(
    onTranslateClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
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
        Card(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GameTrans",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 折叠按钮
                        FloatingActionButton(
                            onClick = onCollapse,
                            icon = Icons.Default.KeyboardArrowRight,
                            contentDescription = "折叠",
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        
                        // 关闭按钮
                        FloatingActionButton(
                            onClick = onClose,
                            icon = Icons.Default.Close,
                            contentDescription = "关闭",
                            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // 翻译按钮
                FloatingTranslateButton(
                    onClick = onTranslateClick,
                    isTranslating = isTranslating,
                    buttonText = "译",
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (isTranslating) translationProgress else "全屏翻译",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTranslating) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 新增：折叠状态的小长条组件
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
        Card(
            onClick = onExpand,
            modifier = Modifier
                .width(36.dp)
                .height(42.dp)
                .shadow(
                    10.dp, 
                    RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 0.dp,
                        bottomStart = 24.dp, 
                        bottomEnd = 0.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 0.dp,
                bottomStart = 12.dp, 
                bottomEnd = 0.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "译",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
@Composable
fun FloatingTranslateButton(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isTranslating: Boolean = false,
    buttonText: String = "译"
) {
    // 动画值
    val buttonScale by animateFloatAsState(
        targetValue = if (isTranslating) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "buttonScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isTranslating)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 200),
        label = "buttonColor"
    )
    
    val shadowElevation by animateFloatAsState(
        targetValue = if (isTranslating) 3f else 10f,
        animationSpec = tween(durationMillis = 200),
        label = "shadowElevation"
    )

    IconButton(
        onClick = onClick,
        enabled = !isTranslating,
        modifier = modifier
            .size(40.dp)
            .scale(buttonScale)
            .shadow(
                elevation = shadowElevation.dp,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            .background(
                color = buttonColor,
                shape = CircleShape
            )
            .border(
                width = if (isTranslating) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = CircleShape
            )
    ) {
        if (isTranslating) {
            Text(
                text = "⏳",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        } else {
            Text(
                text = buttonText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}

// 美观的浮动操作按钮组件
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // 动画效果
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "buttonScale"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )

    Box(
        modifier = modifier
            .size(28.dp)
            .scale(scale)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = iconColor.copy(alpha = 0.2f),
                spotColor = iconColor.copy(alpha = 0.3f)
            )
            .background(
                color = animatedBackgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 0.5.dp,
                color = iconColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = iconColor
        )
    }
}

@Preview
@Composable
fun FloatingWindowComposePreview() {
    GameTransTheme {
        FloatingWindowCompose()
    }
}

@Preview
@Composable
fun CollapsedFloatingWindowPreview() {
    GameTransTheme {
        CollapsedFloatingWindow()
    }
}

@Preview
@Composable
fun FloatingTranslateButtonPreview() {
    GameTransTheme {
        FloatingTranslateButton()
    }
}

@Preview
@Composable
fun FloatingActionButtonPreview() {
    GameTransTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FloatingActionButton(
                onClick = {},
                icon = Icons.Default.KeyboardArrowRight,
                contentDescription = "折叠",
                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                iconColor = MaterialTheme.colorScheme.primary
            )
            FloatingActionButton(
                onClick = {},
                icon = Icons.Default.Close,
                contentDescription = "关闭",
                backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                iconColor = MaterialTheme.colorScheme.error
            )
        }
    }
} 