package com.portwind.gametrans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RegionSelectActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RegionSelectActivity"
    }

    private lateinit var geminiApiManager: GeminiApiManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d(TAG, "Screen capture permission granted for region selection.")
            lifecycleScope.launch {
                val screenCaptureManager = ScreenCaptureManager.getInstance(this@RegionSelectActivity)
                val success = screenCaptureManager.handlePermissionResult(result.resultCode, result.data)
                if (success) {
                    // 权限获取成功，开始区域选择
                    startRegionSelection()
                } else {
                    Toast.makeText(this@RegionSelectActivity, "初始化截图服务失败", Toast.LENGTH_SHORT).show()
                    finishAndRelease()
                }
            }
        } else {
            Log.w(TAG, "Screen capture permission denied for region selection.")
            Toast.makeText(this, "截图权限被拒绝", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "RegionSelectActivity onCreate called")
        
        // 设置全屏和透明
        setupFullScreenTransparent()
        
        try {
            geminiApiManager = GeminiApiManager(this)
            Log.d(TAG, "GeminiApiManager initialized")

            val screenCaptureManager = ScreenCaptureManager.getInstance(this)
            val hasPermission = screenCaptureManager.hasPermission()
            
            if (hasPermission) {
                Log.d(TAG, "MediaProjection permission already granted, starting region selection")
                startRegionSelection()
            } else {
                Log.d(TAG, "MediaProjection permission not available, requesting permission")
                try {
                    val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                    screenCaptureLauncher.launch(captureIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create or launch MediaProjection intent", e)
                    finishAndRelease()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            finishAndRelease()
        }
    }
    
    private fun setupFullScreenTransparent() {
        try {
            // 获取WakeLock防止被杀死
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GameTrans:RegionSelect"
            )
            wakeLock?.acquire(30000) // 30秒
            
            // 设置窗口标志
            window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                // 设置系统UI可见性，隐藏状态栏和导航栏
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup full screen transparent", e)
        }
    }

    private fun startRegionSelection() {
        Log.d(TAG, "Starting region selection UI")
        
        setContent {
            RegionSelectionScreen(
                onRegionSelected = { startOffset, endOffset ->
                    Log.d(TAG, "Region selected: start=$startOffset, end=$endOffset")
                    handleRegionSelected(startOffset, endOffset)
                },
                onCancel = {
                    Log.d(TAG, "Region selection cancelled")
                    finishAndRelease()
                }
            )
        }
    }

    private fun handleRegionSelected(startOffset: Offset, endOffset: Offset) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Processing selected region...")
                
                // 计算选择的矩形区域
                val left = min(startOffset.x, endOffset.x).toInt()
                val top = min(startOffset.y, endOffset.y).toInt()
                val right = max(startOffset.x, endOffset.x).toInt()
                val bottom = max(startOffset.y, endOffset.y).toInt()
                
                val selectedRect = Rect(left, top, right, bottom)
                Log.d(TAG, "Selected region rect: $selectedRect")
                
                // 验证区域有效性
                if (selectedRect.width() < 50 || selectedRect.height() < 50) {
                    Toast.makeText(this@RegionSelectActivity, "选择区域太小，请重新选择", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // 先隐藏翻译面板
                hideTranslationPanelBeforeCapture()
                delay(500) // 等待面板隐藏
                
                // 执行区域截图
                performRegionScreenshot(selectedRect)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing region selection", e)
                Toast.makeText(this@RegionSelectActivity, "区域处理失败：${e.message}", Toast.LENGTH_SHORT).show()
                finishAndRelease()
            }
        }
    }

    private suspend fun performRegionScreenshot(region: Rect) {
        try {
            Log.d(TAG, "Performing region screenshot for rect: $region")
            
            val screenCaptureManager = ScreenCaptureManager.getInstance(this@RegionSelectActivity)
            
            // 先获取全屏截图
            val fullScreenBitmap = screenCaptureManager.captureScreen()
            
            if (fullScreenBitmap != null) {
                Log.d(TAG, "Full screen captured: ${fullScreenBitmap.width}x${fullScreenBitmap.height}")
                
                // 裁剪出选定区域
                val regionBitmap = cropBitmapToRegion(fullScreenBitmap, region)
                fullScreenBitmap.recycle()
                
                if (regionBitmap != null) {
                    Log.d(TAG, "Region cropped successfully: ${regionBitmap.width}x${regionBitmap.height}")
                    
                    // 翻译区域截图
                    translateScreenshot(regionBitmap)
                    regionBitmap.recycle()
                } else {
                    Log.e(TAG, "Failed to crop region from screenshot")
                    Toast.makeText(this@RegionSelectActivity, "区域裁剪失败", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Failed to capture full screen for region selection")
                Toast.makeText(this@RegionSelectActivity, "截图失败，请重试", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception in performRegionScreenshot", e)
            Toast.makeText(this@RegionSelectActivity, "区域截图出错：${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            finishAndRelease()
        }
    }

    private fun cropBitmapToRegion(originalBitmap: Bitmap, region: Rect): Bitmap? {
        return try {
            // 确保裁剪区域在bitmap范围内
            val safeLeft = max(0, region.left)
            val safeTop = max(0, region.top)
            val safeRight = min(originalBitmap.width, region.right)
            val safeBottom = min(originalBitmap.height, region.bottom)
            
            val safeWidth = safeRight - safeLeft
            val safeHeight = safeBottom - safeTop
            
            if (safeWidth <= 0 || safeHeight <= 0) {
                Log.e(TAG, "Invalid crop region: width=$safeWidth, height=$safeHeight")
                return null
            }
            
            Log.d(TAG, "Cropping bitmap from ($safeLeft,$safeTop) with size ${safeWidth}x${safeHeight}")
            Bitmap.createBitmap(originalBitmap, safeLeft, safeTop, safeWidth, safeHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop bitmap", e)
            null
        }
    }

    private suspend fun translateScreenshot(bitmap: Bitmap) {
        try {
            Log.d(TAG, "开始调用Gemini API翻译区域截图...")
            
            if (!geminiApiManager.isApiAvailable()) {
                Log.w(TAG, "Gemini API密钥未配置，跳过翻译")
                Toast.makeText(this, "请先配置Gemini API密钥", Toast.LENGTH_SHORT).show()
                return
            }
            
            val result = geminiApiManager.translateImage(bitmap)
            
            if (result != null) {
                Log.d(TAG, "=== Gemini区域翻译结果 ===")
                Log.d(TAG, "格式化结果: ${result.translatedText}")
                Log.d(TAG, "=== 区域翻译结果结束 ===")
                
                FloatingWindowService.showTranslationResult(this, result.translatedText)
                Toast.makeText(this, "区域翻译完成", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "区域翻译失败：API返回空结果")
                Toast.makeText(this, "区域翻译失败，请重试", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "区域翻译过程中发生错误", e)
            Toast.makeText(this, "区域翻译失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideTranslationPanelBeforeCapture() {
        try {
            Log.d(TAG, "Requesting FloatingWindowService to hide translation panel")
            val intent = Intent().apply {
                action = "com.portwind.gametrans.HIDE_TRANSLATION_PANEL"
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send hide panel broadcast", e)
        }
    }

    private fun finishAndRelease() {
        Log.d(TAG, "Finishing region select activity and releasing resources.")
        
        // 通知服务恢复悬浮窗显示
        val restoreIntent = Intent("com.portwind.gametrans.RESTORE_FLOATING_WINDOW")
        sendBroadcast(restoreIntent)
        
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RegionSelectActivity onDestroy called")
        
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
}

@Composable
fun RegionSelectionScreen(
    onRegionSelected: (Offset, Offset) -> Unit,
    onCancel: () -> Unit
) {
    var startPoint by remember { mutableStateOf<Offset?>(null) }
    var endPoint by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startPoint = offset
                        endPoint = offset
                        isDragging = true
                    },
                    onDrag = { _, _ ->
                        // 在onDrag中更新endPoint
                    },
                    onDragEnd = {
                        isDragging = false
                        startPoint?.let { start ->
                            endPoint?.let { end ->
                                // 检查是否是有效的选择区域
                                val width = abs(end.x - start.x)
                                val height = abs(end.y - start.y)
                                if (width > 50 && height > 50) {
                                    onRegionSelected(start, end)
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // 添加一个简单的点击监听来更新endPoint
                androidx.compose.foundation.gestures.detectTapGestures { offset ->
                    if (isDragging) {
                        endPoint = offset
                    }
                }
            }
    ) {
        // 绘制选择区域
        if (startPoint != null && endPoint != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val start = startPoint!!
                val end = endPoint!!
                
                val left = minOf(start.x, end.x)
                val top = minOf(start.y, end.y)
                val right = maxOf(start.x, end.x)
                val bottom = maxOf(start.y, end.y)
                
                // 绘制选择框
                drawRect(
                    color = androidx.compose.ui.graphics.Color.Red,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(10f, 10f)
                        )
                    )
                )
                
                // 绘制半透明覆盖
                drawRect(
                    color = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.2f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top)
                )
            }
        }

        // 提示信息
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "区域翻译",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "拖拽选择要翻译的区域",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("取消", fontSize = 14.sp)
                }
            }
        }
    }
}