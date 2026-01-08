package com.portwind.gametrans.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class ScreenCaptureManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ScreenCaptureManager"
        const val SCREEN_CAPTURE_REQUEST_CODE = 1000
        
        @Volatile
        private var INSTANCE: ScreenCaptureManager? = null
        
        fun getInstance(context: Context): ScreenCaptureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScreenCaptureManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.w(TAG, "MediaProjection session stopped.")
            // MediaProjection can be stopped by system at any time. Clear state so we can re-request permission.
            try {
                mediaProjection?.unregisterCallback(this)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister MediaProjection callback", e)
            }
            cleanup()
            mediaProjection = null
        }
    }
    
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    
    init {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        initScreenMetrics()
    }
    
    private fun initScreenMetrics() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 使用新的方式获取屏幕尺寸
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            
            val displayMetrics = context.resources.displayMetrics
            screenDensity = displayMetrics.densityDpi
        } else {
            // API 30以下使用传统方式
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }
        
        Log.d(TAG, "Screen metrics: ${screenWidth}x${screenHeight}, density: $screenDensity")
    }
    
    /**
     * 处理权限请求结果并初始化MediaProjection
     */
    fun handlePermissionResult(resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return false
        }
        
        // 如果已经有有效的MediaProjection，直接返回成功
        if (mediaProjection != null) {
            Log.d(TAG, "MediaProjection already exists, reusing it")
            return true
        }
        
        try {
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(projectionCallback, handler)
            Log.d(TAG, "MediaProjection created successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaProjection", e)
            return false
        }
    }
    
    /**
     * 异步截取屏幕并返回Bitmap
     */
    suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        val projection = mediaProjection
        if (projection == null) {
            Log.e(TAG, "MediaProjection is null, cannot capture screen")
            return@withContext null
        }
        
        Log.d(TAG, "Starting screen capture...")
        
        try {
            // 清理之前的资源
            cleanup()
            
            // 创建ImageReader
            imageReader = ImageReader.newInstance(
                screenWidth, 
                screenHeight, 
                PixelFormat.RGBA_8888, 
                2  // 增加buffer数量以提高稳定性
            )
            
            Log.d(TAG, "Creating VirtualDisplay: ${screenWidth}x${screenHeight}, density: $screenDensity")
            
            // 创建VirtualDisplay
            virtualDisplay = projection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                imageReader?.surface,
                null,
                null
            )
            
            if (virtualDisplay == null) {
                Log.e(TAG, "Failed to create VirtualDisplay")
                return@withContext null
            }
            
            // 等待一帧图像
            val image = waitForImage()
            val bitmap = image?.let { convertImageToBitmap(it) }
            
            if (bitmap != null) {
                Log.d(TAG, "Screen capture successful: ${bitmap.width}x${bitmap.height}")
            } else {
                Log.e(TAG, "Failed to convert image to bitmap")
            }
            
            return@withContext bitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screen", e)
            return@withContext null
        } finally {
            cleanup()
        }
    }
    
    /**
     * 等待第一帧图像（事件驱动，帧就绪时系统回调，更高效）
     */
    private suspend fun waitForImage(): Image? {
        val reader = imageReader ?: return null
        val timeoutMs = 1500L

        return suspendCancellableCoroutine { cont ->
            // 用于防止重复 resume
            var resumed = false

            val listener = ImageReader.OnImageAvailableListener { ir ->
                if (resumed) return@OnImageAvailableListener
                val image = try {
                    ir.acquireLatestImage()
                } catch (e: Exception) {
                    Log.w(TAG, "acquireLatestImage failed", e)
                    null
                }
                if (image != null && cont.isActive) {
                    resumed = true
                    // 移除 listener 避免后续多次回调
                    reader.setOnImageAvailableListener(null, null)
                    Log.d(TAG, "Image acquired successfully (event-driven)")
                    cont.resume(image)
                }
            }

            // 注册监听器（在主线程回调）
            reader.setOnImageAvailableListener(listener, handler)

            // 超时保护
            val timeoutRunnable = Runnable {
                if (!resumed && cont.isActive) {
                    resumed = true
                    reader.setOnImageAvailableListener(null, null)
                    Log.e(TAG, "Failed to acquire image (timeout ${timeoutMs}ms)")
                    cont.resume(null)
                }
            }
            handler.postDelayed(timeoutRunnable, timeoutMs)

            // 协程取消时清理
            cont.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                reader.setOnImageAvailableListener(null, null)
            }
        }
    }
    
    private fun convertImageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()
            
            // 如果有padding，需要裁剪到正确的尺寸
            if (rowPadding != 0) {
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                bitmap.recycle()
                croppedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert image to bitmap", e)
            image.close()
            null
        }
    }
    
    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null
    }
    
    /**
     * 释放资源
     */
    fun release() {
        cleanup()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        Log.d(TAG, "ScreenCaptureManager released.")
    }
    
    /**
     * 释放单例实例（用于应用完全退出时）
     */
    fun releaseInstance() {
        release()
        INSTANCE = null
        Log.d(TAG, "ScreenCaptureManager instance released.")
    }
    
    /**
     * 检查是否已有权限
     */
    fun hasPermission(): Boolean {
        return mediaProjection != null
    }
} 