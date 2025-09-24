package com.portwind.gametrans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ScreenCaptureActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ScreenCaptureActivity"
    }

    private lateinit var geminiApiManager: GeminiApiManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d(TAG, "Screen capture permission granted.")
            // 在协程中执行截图
            lifecycleScope.launch {
                // 使用全局的ScreenCaptureManager实例
                val screenCaptureManager = ScreenCaptureManager.getInstance(this@ScreenCaptureActivity)
                val success = screenCaptureManager.handlePermissionResult(result.resultCode, result.data)
                if (success) {
                    performScreenshot()
                } else {
                    Toast.makeText(this@ScreenCaptureActivity, "初始化截图服务失败", Toast.LENGTH_SHORT).show()
                    finishAndRelease()
                }
            }
        } else {
            Log.w(TAG, "Screen capture permission denied.")
            Toast.makeText(this, "截图权限被拒绝", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ScreenCaptureActivity onCreate called")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Log.d(TAG, "Android version: ${android.os.Build.VERSION.SDK_INT}")
        
        // 针对小米手机的特殊处理
        setupForXiaomiDevice()
        
        try {
            geminiApiManager = GeminiApiManager(this)
            Log.d(TAG, "GeminiApiManager initialized")

            // 检查是否已有权限，如果有就直接截图，否则请求权限
            val screenCaptureManager = ScreenCaptureManager.getInstance(this)
            Log.d(TAG, "ScreenCaptureManager instance obtained")
            
            val hasPermission = screenCaptureManager.hasPermission()
            Log.d(TAG, "Current permission status: $hasPermission")
            
            if (hasPermission) {
                Log.d(TAG, "MediaProjection permission already granted, proceeding with screenshot")
                // 直接执行截图
                lifecycleScope.launch {
                    Log.d(TAG, "Starting screenshot in coroutine")
                    performScreenshot()
                }
            } else {
                Log.d(TAG, "MediaProjection permission not available, requesting permission")
                // 启动权限请求
                try {
                    val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
                    Log.d(TAG, "MediaProjection intent created, launching permission request")
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
    
    /**
     * 针对小米设备的特殊设置
     */
    private fun setupForXiaomiDevice() {
        try {
            // 获取WakeLock防止被杀死
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GameTrans:ScreenCapture"
            )
            wakeLock?.acquire(10000) // 10秒
            Log.d(TAG, "WakeLock acquired for Xiaomi device")
            
            // 设置窗口标志，防止被系统优化
            window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                Log.d(TAG, "Window flags set for Xiaomi device")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup for Xiaomi device", e)
        }
    }

    /**
     * 执行屏幕截图和翻译
     */
    private suspend fun performScreenshot() {
        Log.d(TAG, "performScreenshot started")
        
        try {
            // 先隐藏翻译面板，避免截取到之前的翻译内容
            hideTranslationPanelBeforeCapture()
            
            // 等待较短时间确保面板完全消失（优化性能）
            kotlinx.coroutines.delay(800)
            
            val screenCaptureManager = ScreenCaptureManager.getInstance(this@ScreenCaptureActivity)
            Log.d(TAG, "ScreenCaptureManager obtained for screenshot")
            
            val bitmap = screenCaptureManager.captureScreen()
            Log.d(TAG, "Screenshot capture completed, bitmap: $bitmap")
            
            if (bitmap != null) {
                Log.d(TAG, "Screenshot taken successfully: ${bitmap.width}x${bitmap.height}")
                
                // 任务3&4：发送bitmap到Gemini API进行翻译并显示结果
                translateScreenshot(bitmap)
                
                bitmap.recycle()
                Log.d(TAG, "Bitmap recycled")
            } else {
                Log.e(TAG, "Failed to capture screen - bitmap is null")
                Toast.makeText(this@ScreenCaptureActivity, "截图失败，请重试", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception in performScreenshot", e)
            Toast.makeText(this@ScreenCaptureActivity, "截图过程出错：${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            // 截图完成后，无论成功与否都关闭Activity
            Log.d(TAG, "performScreenshot finished, calling finishAndRelease")
            finishAndRelease()
        }
    }

    /**
     * 使用Gemini API翻译截图内容
     */
    private suspend fun translateScreenshot(bitmap: android.graphics.Bitmap) {
        try {
            Log.d(TAG, "开始调用Gemini API翻译截图...")
            
            // 检查API是否可用
            if (!geminiApiManager.isApiAvailable()) {
                Log.w(TAG, "Gemini API密钥未配置，跳过翻译")
                Toast.makeText(this, "请先配置Gemini API密钥", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 读取来自服务的临时提示词（如果有）
            val promptOverride = intent.getStringExtra("PROMPT_OVERRIDE")
            if (!promptOverride.isNullOrBlank()) {
                Log.d(TAG, "检测到临时提示词长度: ${promptOverride.length}")
            }

            // 调用Gemini API进行翻译（支持临时提示词覆盖）
            val result = geminiApiManager.translateImage(bitmap, promptOverride)
            
            if (result != null) {
                // 任务3要求：在日志中打印翻译结果
                Log.d(TAG, "=== Gemini翻译结果 ===")
                Log.d(TAG, "格式化结果: ${result.translatedText}")
                Log.d(TAG, "=== 翻译结果结束 ===")
                
                // 任务4：将翻译结果发送给悬浮窗服务显示
                FloatingWindowService.showTranslationResult(this, result.translatedText)
                
                // 显示简短的成功提示
                Toast.makeText(this, "翻译完成，查看结果面板", Toast.LENGTH_SHORT).show()
            } else {
                Log.e(TAG, "翻译失败：API返回空结果")
                Toast.makeText(this, "翻译失败，请重试", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "翻译过程中发生错误", e)
            Toast.makeText(this, "翻译失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finishAndRelease() {
        Log.d(TAG, "Finishing activity and releasing resources.")
        
        // 通知服务恢复悬浮窗显示
        if (intent.getBooleanExtra("RESTORE_FLOATING_WINDOW", false)) {
            // 通过广播通知服务恢复悬浮窗
            val restoreIntent = Intent("com.portwind.gametrans.RESTORE_FLOATING_WINDOW")
            sendBroadcast(restoreIntent)
        }
        
        finish()
    }
    
    /**
     * 通知FloatingWindowService隐藏翻译面板
     */
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

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ScreenCaptureActivity onResume called")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "ScreenCaptureActivity onPause called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScreenCaptureActivity onDestroy called")
        
        // 释放WakeLock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }
} 