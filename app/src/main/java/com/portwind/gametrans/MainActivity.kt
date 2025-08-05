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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    private var showSettings by mutableStateOf(false)
    
    // 屏幕捕获权限结果处理
    private val screenCapturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Toast.makeText(this, "权限已授予，正在启动服务...", Toast.LENGTH_SHORT).show()
            
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
            Toast.makeText(this, "屏幕捕获权限被拒绝", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionHelper = PermissionHelper(this)
        
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
                            hasOverlayPermission = permissionHelper.hasOverlayPermission(this)
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
                    Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun stopFloatingWindow() {
        FloatingWindowService.stop(this)
        Toast.makeText(this, "悬浮窗已停止", Toast.LENGTH_SHORT).show()
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
    hasOverlayPermission: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 标题栏和设置按钮
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
            
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "智能屏幕翻译助手",
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
                    text = "主要功能",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "• 悬浮窗常驻，随时可用\n" +
                          "• 一键屏幕截图翻译\n" +
                          "• 支持区域选择翻译\n" +
                          "• 集成Gemini AI智能识别",
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
                    text = "启动服务需要悬浮窗权限和屏幕捕获权限。",
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
                text = "启动翻译服务",
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
            text = "任务1：悬浮窗创建、显示、拖动和权限申请 ✓\n" +
                   "任务2：集成MediaProjection API屏幕截图 ✓",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
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