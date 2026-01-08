package com.portwind.gametrans.overlay

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portwind.gametrans.settings.AiTask
import com.portwind.gametrans.ui.theme.GameTransTheme
import com.portwind.gametrans.ui.theme.TextPrimary
import com.portwind.gametrans.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranslationResultPanel(
    translationResult: String?,
    isVisible: Boolean = true,
    onClose: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onPlayOriginal: (String) -> Unit = {},
    onPromptTaskSelected: (AiTask) -> Unit = {}
) {
    var showPanel by remember(isVisible) { mutableStateOf(isVisible) }
    val clipboardManager = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    AnimatedVisibility(
        visible = showPanel && !translationResult.isNullOrBlank(),
        enter = slideInVertically(
            animationSpec = tween(400, easing = androidx.compose.animation.core.EaseOutCubic)
        ) { it / 4 } + fadeIn(animationSpec = tween(400)),
        exit = slideOutVertically(
            animationSpec = tween(300, easing = androidx.compose.animation.core.EaseInCubic)
        ) { it / 4 } + fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (configuration.screenHeightDp * 0.9f).dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        onDrag(dragAmount)
                    }
                }
        ) {
            // Main Panel Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background // Paper white
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 24.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Translation",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MinimalIconButton(
                                onClick = {
                                    translationResult?.let { result ->
                                        clipboardManager.setText(AnnotatedString(result))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                icon = Icons.Default.ContentCopy,
                                contentDescription = "Copy"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            MinimalIconButton(
                                onClick = {
                                    showPanel = false
                                    onClose()
                                },
                                icon = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Content Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (configuration.screenHeightDp * 0.6f).dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            val paragraphs = resultTextToParagraphs(translationResult)

                            paragraphs.forEachIndexed { index, paragraph ->
                                val isOriginal = index % 2 == 0
                                
                                if (isOriginal) {
                                    // Original Text Section (日文原文)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = paragraph,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                lineHeight = 22.sp,
                                                letterSpacing = 0.2.sp,
                                                color = TextSecondary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .combinedClickable(
                                                    onClick = {},
                                                    onLongClick = {
                                                        clipboardManager.setText(AnnotatedString(paragraph))
                                                        Toast.makeText(context, "Original Copied", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        // Play Button
                                        IconButton(
                                            onClick = { onPlayOriginal(paragraph) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    // 换行：原文和翻译之间的间距
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    // Translated Text Section (中文翻译)
                                    Text(
                                        text = paragraph,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 26.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    clipboardManager.setText(AnnotatedString(paragraph))
                                                    Toast.makeText(context, "Translation Copied", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                    )

                                    // 段落之间的分隔
                                    if (index < paragraphs.size - 1) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            thickness = 1.dp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to parse result
fun resultTextToParagraphs(text: String?): List<String> {
    if (text == null) return emptyList()
    return text.split("\n\n").filter { it.trim().isNotEmpty() }.map { it.trim() }
}

// MinimalIconButton is defined in FloatingWindowCompose.kt

@Preview
@Composable
fun TranslationResultPanelPreview() {
    GameTransTheme {
        Box(modifier = Modifier.background(Color.Gray)) {
            TranslationResultPanel(
                translationResult = "Original Text Here\n\nTranslated Text Here is shown below.",
                isVisible = true
            )
        }
    }
} 