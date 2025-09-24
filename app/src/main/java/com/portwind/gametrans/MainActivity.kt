package com.portwind.gametrans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.portwind.gametrans.ui.theme.GameTransTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var settingsManager: SettingsManager
    private var showSettings by mutableStateOf(false)
    
    // 屏幕捕获权限结果处理
    private val screenCapturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Toast.makeText(this, getString(R.string.permission_granted_starting_service), Toast.LENGTH_SHORT).show()
            
            // 将权限授予结果通过Intent发送给Service
            val intent = Intent(this, FloatingWindowService::class.java).apply {
                action = FloatingWindowService.ACTION_START_PROJECTION
                putExtra(FloatingWindowService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(FloatingWindowService.EXTRA_RESULT_DATA, result.data)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            Toast.makeText(this, getString(R.string.screen_capture_permission_denied), Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionHelper = PermissionHelper(this)
        settingsManager = SettingsManager(this)
        
        // 应用保存的语言设置
        settingsManager.applyLanguage(settingsManager.getLanguage())
        
        setContent {
            GameTransTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (showSettings) {
                        SettingsScreen(
                            onNavigateBack = { showSettings = false },
                            onShowMessage = { message ->
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        MainScreen(
                            onStartFloatingWindow = ::startOrRequestPermissions,
                            onStopFloatingWindow = ::stopFloatingWindow,
                            onOpenSettings = { showSettings = true },
                            onLanguageChanged = { language ->
                                settingsManager.setLanguage(language)
                                Toast.makeText(this@MainActivity, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
                                recreate()
                            },
                            hasOverlayPermission = permissionHelper.hasOverlayPermission(this),
                            currentLanguage = settingsManager.getLanguage()
                        )
                    }
                }
            }
        }
    }
    
    private fun startOrRequestPermissions() {
        if (permissionHelper.hasOverlayPermission(this)) {
            // 如果已有悬浮窗权限，直接请求截图权限
            requestScreenCapturePermission()
        } else {
            // 否则，先请求悬浮窗权限
            permissionHelper.requestOverlayPermission { granted ->
                if (granted) {
                    // 成功后再请求截图权限
                    requestScreenCapturePermission()
                } else {
                    Toast.makeText(this, getString(R.string.overlay_permission_denied), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun stopFloatingWindow() {
        FloatingWindowService.stop(this)
        Toast.makeText(this, getString(R.string.floating_window_stopped), Toast.LENGTH_SHORT).show()
    }
    
    private fun requestScreenCapturePermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCapturePermissionLauncher.launch(captureIntent)
    }
}

@Composable
fun MainScreen(
    onStartFloatingWindow: () -> Unit = {},
    onStopFloatingWindow: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onLanguageChanged: (AppLanguage) -> Unit = {},
    hasOverlayPermission: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.CHINESE
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 标题栏和按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GameTrans",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Row {
                // 语言切换按钮
                LanguageSelector(
                    currentLanguage = currentLanguage,
                    onLanguageChanged = onLanguageChanged
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 设置按钮
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 功能介绍卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.main_features_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.main_features_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 权限状态提示
        if (!hasOverlayPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.permission_warning),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 操作按钮
        Button(
            onClick = onStartFloatingWindow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.start_translation_service),
                style = MaterialTheme.typography.labelLarge
            )
        }
            
        Spacer(modifier = Modifier.height(12.dp))
            
        OutlinedButton(
            onClick = onStopFloatingWindow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.stop_floating_window),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.task_status),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Filled.Language,
            contentDescription = stringResource(R.string.language_settings),
            tint = MaterialTheme.colorScheme.primary
        )
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        AppLanguage.values().forEach { language ->
            DropdownMenuItem(
                text = { Text(language.displayName) },
                onClick = {
                    onLanguageChanged(language)
                    expanded = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    GameTransTheme {
        MainScreen(hasOverlayPermission = false)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenWithPermissionPreview() {
    GameTransTheme {
        MainScreen(hasOverlayPermission = true)
    }
}