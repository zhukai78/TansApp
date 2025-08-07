package com.portwind.gametrans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portwind.gametrans.ui.theme.GameTransTheme


@Composable
fun TranslationResultPanel(
    translationResult: String?,
    isVisible: Boolean = true,
    onClose: () -> Unit = {},
    onDrag: (Offset) -> Unit = {}
) {
    var showPanel by remember(isVisible) { mutableStateOf(isVisible) }
    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val scrollState = rememberScrollState()
    
    AnimatedVisibility(
        visible = showPanel && !translationResult.isNullOrBlank(),
        enter = slideInVertically(
            animationSpec = tween(400, easing = androidx.compose.animation.core.EaseOutCubic)
        ) { it / 2 } + fadeIn(animationSpec = tween(400)),
        exit = slideOutVertically(
            animationSpec = tween(300, easing = androidx.compose.animation.core.EaseInCubic)
        ) { it / 2 } + fadeOut(animationSpec = tween(300))
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
                    .widthIn(max = (configuration.screenWidthDp * 0.88).dp) // 减少宽度到88%
                    .heightIn(max = (configuration.screenHeightDp * 0.65).dp) // 稍微增加高度到65%
                    .shadow(
                        16.dp, 
                        RoundedCornerShape(24.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 翻译图标 - 使用渐变背景的翻译符号
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "译",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = "翻译结果",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row {
                            // 复制按钮
                            IconButton(
                                onClick = {
                                    translationResult?.let { result ->
                                        clipboardManager.setText(AnnotatedString(result))
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "复制",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            }
                            
                            // 关闭按钮
                            IconButton(
                                onClick = {
                                    showPanel = false
                                    onClose()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 分割线
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 翻译内容 - 新格式：原文和译文段落对应显示
                    translationResult?.let { result ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = (configuration.screenHeightDp * 0.42).dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                            ) {
                                // 解析并显示格式化的翻译结果
                                val paragraphs = result.split("\n\n").filter { it.trim().isNotEmpty() }
                                
                                paragraphs.forEachIndexed { index, paragraph ->
                                    val isOriginal = index % 2 == 0 // 偶数索引为原文，奇数索引为译文
                                    
                                    if (isOriginal) {
                                        // 原文样式（统一为大字体）
                                        Text(
                                            text = paragraph.trim(),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = 0.3.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                    } else {
                                        // 译文样式
                                        Text(
                                            text = paragraph.trim(),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.2.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        // 在译文后添加分隔空间（除了最后一段）
                                        if (index < paragraphs.size - 1) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            // 添加淡色分隔线
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                            )
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
             
                }
            }
        }
    }



} 