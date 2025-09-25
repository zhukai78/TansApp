package com.portwind.gametrans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portwind.gametrans.ui.theme.GameTransTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    var currentSettings by remember { mutableStateOf(settingsManager.getSettings()) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        // 设置内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 翻译设置
            SettingsSection(
                title = stringResource(R.string.translation_settings)
            ) {
                // 模型提供商切换
                Text(
                    text = stringResource(R.string.ai_provider),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSettings.modelProvider == ModelProvider.GEMINI,
                        onClick = {
                            currentSettings = currentSettings.copy(modelProvider = ModelProvider.GEMINI)
                            settingsManager.saveSettings(currentSettings)
                            onShowMessage("Gemini")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.provider_gemini))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSettings.modelProvider == ModelProvider.QWEN,
                        onClick = {
                            currentSettings = currentSettings.copy(modelProvider = ModelProvider.QWEN)
                            settingsManager.saveSettings(currentSettings)
                            onShowMessage("Qwen3-Omni")
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.provider_qwen))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 提示词模式选择
                PromptModeSelector(
                    selectedMode = currentSettings.promptMode,
                    onModeChanged = { mode ->
                        currentSettings = currentSettings.copy(promptMode = mode)
                        // 立即保存设置，确保模式切换生效
                        settingsManager.saveSettings(currentSettings.copy(promptMode = mode))
                    }
                )

                // 新增：自定义提示词输入框（仅在选择自定义模式时显示）
                if (currentSettings.promptMode == PromptMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var customPromptText by remember { mutableStateOf(currentSettings.customPrompt) }
                    
                    Column {
                        Text(
                            text = stringResource(R.string.custom_prompt_label),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = customPromptText,
                            onValueChange = { 
                                customPromptText = it
                                currentSettings = currentSettings.copy(customPrompt = it)
                                // 立即保存自定义提示词，确保更改生效
                                settingsManager.saveSettings(currentSettings.copy(customPrompt = it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            placeholder = {
                                Text(text = stringResource(R.string.custom_prompt_hint))
                            },
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "提示：自定义提示词应包含明确的翻译指令和输出格式要求",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // 图像优化设置
            SettingsSection(
                title = stringResource(R.string.image_optimization)
            ) {
                // 最大图像尺寸
                SliderSetting(
                    label = stringResource(R.string.max_image_size),
                    value = currentSettings.maxImageSize.toFloat(),
                    valueRange = 512f..2048f,
                    steps = 3,
                    valueFormatter = { "${it.toInt()}px" },
                    onValueChange = { size ->
                        currentSettings = currentSettings.copy(maxImageSize = size.toInt())
                        // 立即保存图像尺寸设置
                        settingsManager.saveSettings(currentSettings.copy(maxImageSize = size.toInt()))
                    }
                )
                
                // 压缩质量
                SliderSetting(
                    label = stringResource(R.string.compression_quality),
                    value = currentSettings.compressionQuality.toFloat(),
                    valueRange = 60f..100f,
                    steps = 8,
                    valueFormatter = { "${it.toInt()}%" },
                    onValueChange = { quality ->
                        currentSettings = currentSettings.copy(compressionQuality = quality.toInt())
                        // 立即保存压缩质量设置
                        settingsManager.saveSettings(currentSettings.copy(compressionQuality = quality.toInt()))
                    }
                )
            }
            

            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 恢复默认按钮
                OutlinedButton(
                    onClick = {
                        settingsManager.resetToDefaults()
                        currentSettings = settingsManager.getSettings()
                        onShowMessage(context.getString(R.string.settings_reset))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.reset_defaults))
                }
                
                // 保存设置按钮
                Button(
                    onClick = {
                        settingsManager.saveSettings(currentSettings)
                        onShowMessage(context.getString(R.string.settings_saved))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.save_settings))
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            content()
        }
    }
}

@Composable
fun PromptModeSelector(
    selectedMode: PromptMode,
    onModeChanged: (PromptMode) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.prompt_template),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        PromptMode.entries.forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = { onModeChanged(mode) }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    GameTransTheme {
        SettingsScreen()
    }
} 