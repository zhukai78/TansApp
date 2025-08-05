package com.portwind.gametrans

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portwind.gametrans.ui.theme.GameTransTheme

@Composable
fun FloatingWindowCompose(
    onTranslateClick: () -> Unit = {},
    onRegionTranslateClick: () -> Unit = {},
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
                modifier = Modifier.padding(16.dp),
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
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row {
                        // 折叠按钮
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "折叠",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        // 关闭按钮
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 翻译按钮行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 全屏翻译按钮
                    FloatingTranslateButton(
                        onClick = onTranslateClick,
                        isTranslating = isTranslating,
                        buttonText = "译",
                        modifier = Modifier.size(42.dp)
                    )
                    
                    // 区域翻译按钮
                    FloatingTranslateButton(
                        onClick = onRegionTranslateClick,
                        isTranslating = isTranslating,
                        buttonText = "区",
                        modifier = Modifier.size(42.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isTranslating) translationProgress else "全屏/区域翻译",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTranslating) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp
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
                .width(20.dp)
                .height(80.dp)
                .shadow(6.dp, RoundedCornerShape(6.dp)),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
    IconButton(
        onClick = onClick,
        enabled = !isTranslating,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isTranslating) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else 
                    MaterialTheme.colorScheme.primary,
                CircleShape
            )
    ) {
        if (isTranslating) {
            Text(
                text = "⏳",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            Text(
                text = buttonText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (buttonText.length > 1) 12.sp else 16.sp
                )
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