package com.portwind.gametrans.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portwind.gametrans.ui.theme.GameTransTheme
import com.portwind.gametrans.ui.components.AnimeCard
import com.portwind.gametrans.ui.theme.AnimeTextPrimary
import com.portwind.gametrans.ui.theme.AnimePrimary
import com.portwind.gametrans.ui.theme.AnimeSecondary
import com.portwind.gametrans.settings.AiTask
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush

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
            AnimeCard(
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(24.dp)),
                elevation = 6.dp,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 左上角：切换下拉
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { expanded = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "切换任务",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                AiTask.values().forEach { task ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = task.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = task.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        },
                                        onClick = {
                                            expanded = false
                                            onPromptTaskSelected(task)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "GameTrans",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 折叠按钮
                        ModernIconButton(
                            onClick = onCollapse,
                            icon = Icons.Default.KeyboardArrowRight,
                            contentDescription = "折叠",
                            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        
                        // 关闭按钮
                        ModernIconButton(
                            onClick = onClose,
                            icon = Icons.Default.Close,
                            contentDescription = "关闭",
                            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                // 临时提示词：使用WindowManager管理的对话框

                // 聊天对话框状态
                var showChatDialog by remember { mutableStateOf(false) }

                // This is now just a trigger, the dialog itself is a separate window
                if (showChatDialog) {
                    onAskClicked() // Call the service to show the window
                    showChatDialog = false // Reset the trigger
                }

                // 主要操作按钮 - 横向排列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ask 按钮
                    ModernCircleTextButton(
                        onClick = { showChatDialog = true },
                        enabled = !isTranslating,
                        text = "💬",
                        size = 32.dp,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        textColor = MaterialTheme.colorScheme.primary,
                        contentDescription = "AI问答"
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    // 翻译按钮
                    FloatingTranslateButton(
                        onClick = onTranslateClick,
                        isTranslating = isTranslating,
                        buttonText = "译",
                        modifier = Modifier.size(48.dp) // 稍稍增大主按钮
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    // 临时提示词按钮
                    ModernCircleTextButton(
                        onClick = onPromptDialogClicked,
                        enabled = !isTranslating,
                        text = "✏️",
                        size = 32.dp,
                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        textColor = MaterialTheme.colorScheme.primary,
                        contentDescription = "编辑提示词"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isTranslating) translationProgress else "全屏翻译",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTranslating) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // 对话框现在由专门的WindowManager处理
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
        // Anime-style pill shape
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(56.dp)
                .shadow(
                    8.dp, 
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 0.dp,
                        bottomStart = 28.dp, 
                        bottomEnd = 0.dp
                    )
                )
                .background(
                    color = AnimePrimary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 0.dp,
                        bottomStart = 28.dp, 
                        bottomEnd = 0.dp
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.5f),
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 0.dp,
                        bottomStart = 28.dp, 
                        bottomEnd = 0.dp
                    )
                )
                .clickable(onClick = onExpand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "展开",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
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
            // 使用脉冲动画效果
            val alpha by animateFloatAsState(
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulse"
            )
            
            Text(
                text = "🌐",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = alpha)
            )
        } else {
            Text(
                text = "🌐",
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

// 现代化图标按钮组件
@Composable
fun ModernIconButton(
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
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

// 现代化圆形按钮组件
@Composable
fun ModernCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    iconColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // 动画效果
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "buttonScale"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )
    
    val animatedIconColor by animateColorAsState(
        targetValue = if (enabled) iconColor else iconColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (enabled) 4.dp else 2.dp,
                shape = CircleShape,
                ambientColor = iconColor.copy(alpha = 0.2f),
                spotColor = iconColor.copy(alpha = 0.3f)
            )
            .background(
                color = animatedBackgroundColor,
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = animatedIconColor.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedIconColor,
            modifier = Modifier.size((size.value * 0.5f).dp)
        )
    }
}

// 现代化圆形文本按钮组件
@Composable
fun ModernCircleTextButton(
    onClick: () -> Unit,
    text: String,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    textColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // 动画效果
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "buttonScale"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "backgroundColor"
    )
    
    val animatedTextColor by animateColorAsState(
        targetValue = if (enabled) textColor else textColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 200),
        label = "textColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (enabled) 4.dp else 2.dp,
                shape = CircleShape,
                ambientColor = textColor.copy(alpha = 0.2f),
                spotColor = textColor.copy(alpha = 0.3f)
            )
            .background(
                color = animatedBackgroundColor,
                shape = CircleShape
            )
            .border(
                width = 0.5.dp,
                color = animatedTextColor.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = animatedTextColor,
            fontSize = (size.value * 0.4f).sp,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
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
fun ModernButtonsPreview() {
    GameTransTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernIconButton(
                    onClick = {},
                    icon = Icons.Default.KeyboardArrowRight,
                    contentDescription = "折叠",
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    iconColor = MaterialTheme.colorScheme.primary
                )
                ModernIconButton(
                    onClick = {},
                    icon = Icons.Default.Close,
                    contentDescription = "关闭",
                    backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    iconColor = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernCircleTextButton(
                    onClick = {},
                    text = "💬",
                    contentDescription = "AI问答",
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    textColor = MaterialTheme.colorScheme.primary
                )
                ModernCircleTextButton(
                    onClick = {},
                    text = "✏️",
                    contentDescription = "编辑提示词",
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    textColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 