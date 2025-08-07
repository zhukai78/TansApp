package com.portwind.gametrans

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.view.View
import android.os.Handler
import android.content.BroadcastReceiver
import android.content.IntentFilter

class FloatingWindowService : Service(), SavedStateRegistryOwner, ViewModelStoreOwner {
    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null
    private var collapsedView: ComposeView? = null // 新增：折叠状态的小长条视图
    private var layoutParams = WindowManager.LayoutParams() // 正常悬浮窗的布局参数
    private var collapsedLayoutParams = WindowManager.LayoutParams() // 折叠悬浮窗的布局参数
    private var isCollapsed = false // 新增：折叠状态标志
    private var isTranslating = false // 新增：翻译状态标志
    private var translationProgress = "" // 新增：翻译进度文本
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val _viewModelStore = ViewModelStore()
    
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private lateinit var translationPanelManager: TranslationPanelManager
    private val handler = Handler()
    
    private val restoreReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.portwind.gametrans.RESTORE_FLOATING_WINDOW" -> {
                    restoreFloatingWindow()
                }
                "com.portwind.gametrans.HIDE_TRANSLATION_PANEL" -> {
                    Log.d(TAG, "Received request to hide translation panel")
                    if (translationPanelManager.isShowing()) {
                        translationPanelManager.hideTranslationResult()
                        Log.d(TAG, "Translation panel hidden on request")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "floating_window_channel"
        
        const val ACTION_START_PROJECTION = "com.portwind.gametrans.START_PROJECTION"
        const val ACTION_STOP = "com.portwind.gametrans.STOP"
        const val ACTION_SHOW_TRANSLATION = "com.portwind.gametrans.SHOW_TRANSLATION"
        const val EXTRA_RESULT_CODE = "com.portwind.gametrans.RESULT_CODE"
        const val EXTRA_RESULT_DATA = "com.portwind.gametrans.RESULT_DATA"
        const val EXTRA_TRANSLATION_RESULT = "com.portwind.gametrans.TRANSLATION_RESULT"

        
        fun stop(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
        
        fun showTranslationResult(context: Context, translationResult: String) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_SHOW_TRANSLATION
                putExtra(EXTRA_TRANSLATION_RESULT, translationResult)
            }
            context.startService(intent)
        }
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = _viewModelStore

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        screenCaptureManager = ScreenCaptureManager.getInstance(this)
        translationPanelManager = TranslationPanelManager(this, this, this, this)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("服务运行中"))
        
        // 注册广播接收器用于恢复悬浮窗和隐藏翻译面板
        val filter = IntentFilter().apply {
            addAction("com.portwind.gametrans.RESTORE_FLOATING_WINDOW")
            addAction("com.portwind.gametrans.HIDE_TRANSLATION_PANEL")
        }
        registerReceiver(restoreReceiver, filter)
        
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START_PROJECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

                if (resultCode == Activity.RESULT_OK && data != null) {
                    val success = screenCaptureManager.handlePermissionResult(resultCode, data)
                    if (success) {
                        createFloatingWindow()
                        updateNotification("服务运行中，点击'译'可截图")
                    } else {
                        Toast.makeText(this, "无法初始化截图服务", Toast.LENGTH_SHORT).show()
                        stopSelf()
                    }
                } else {
                    Toast.makeText(this, "截图权限未授予", Toast.LENGTH_SHORT).show()
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_SHOW_TRANSLATION -> {
                val translationResult = intent.getStringExtra(EXTRA_TRANSLATION_RESULT)
                if (!translationResult.isNullOrBlank()) {
                    // 显示翻译完成状态
                    updateTranslationState(true, "翻译完成")
                    
                    // 显示翻译结果面板
                    translationPanelManager.showTranslationResult(translationResult)
                    
                    // 延迟重置状态，让用户看到"翻译完成"
                    handler.postDelayed({
                        updateTranslationState(false)
                    }, 1000)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeFloatingWindow()
        removeCollapsedWindow()
        // 不要在服务销毁时释放单例的ScreenCaptureManager
        // screenCaptureManager.release() 
        translationPanelManager.release()
        
        // 注销广播接收器
        try {
            unregisterReceiver(restoreReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister receiver", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.floating_window_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.floating_window_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(getString(R.string.floating_window_title))
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createFloatingWindow() {
        if (floatingView != null) {
            return
        }

        try {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 100
            }

            floatingView = ComposeView(this).apply {
                // 设置生命周期和ViewModel相关的组件
                setViewTreeLifecycleOwner(this@FloatingWindowService)
                setViewTreeViewModelStoreOwner(this@FloatingWindowService)
                setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
                
                // 设置Compose内容
                setContent {
                    FloatingWindowCompose(
                        onTranslateClick = ::handleTranslateClick,
                        onClose = { stopSelf() },
                        onCollapse = ::handleCollapseClick, // 新增：折叠回调
                        onDrag = { dragAmount ->
                            updateWindowPosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                        },
                        isTranslating = isTranslating,
                        translationProgress = translationProgress
                    )
                }
            }

            windowManager.addView(floatingView, layoutParams)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating window", e)
            Toast.makeText(this, "创建悬浮窗失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 新增：创建折叠状态的小长条悬浮窗
    private fun createCollapsedWindow() {
        if (collapsedView != null) {
            return
        }

        try {
            val displayMetrics = resources.displayMetrics
            collapsedLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 0 // 贴右边
                y = displayMetrics.heightPixels / 3 // 屏幕中间偏上位置
            }

            collapsedView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@FloatingWindowService)
                setViewTreeViewModelStoreOwner(this@FloatingWindowService)
                setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
                
                setContent {
                    CollapsedFloatingWindow(
                        onExpand = ::handleExpandClick,
                        onDrag = { dragAmount ->
                            updateCollapsedWindowPosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    )
                }
            }

            windowManager.addView(collapsedView, collapsedLayoutParams)
            Log.d(TAG, "Collapsed floating window created")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create collapsed window", e)
        }
    }

    // 新增：处理折叠按钮点击
    private fun handleCollapseClick() {
        Log.d(TAG, "Collapse button clicked")
        removeFloatingWindow()
        createCollapsedWindow()
        isCollapsed = true
        updateNotification("悬浮窗已折叠到右侧")
    }

    // 新增：处理展开按钮点击
    private fun handleExpandClick() {
        Log.d(TAG, "Expand button clicked")
        removeCollapsedWindow()
        createFloatingWindow()
        isCollapsed = false
        updateNotification("服务运行中，点击'译'可截图")
    }
    
    // 新增：更新翻译状态
    private fun updateTranslationState(translating: Boolean, progress: String = "") {
        isTranslating = translating
        translationProgress = progress
        
        // 重新创建悬浮窗以更新状态显示
        if (!isCollapsed) {
            removeFloatingWindow()
            createFloatingWindow()
        }
        
        // 更新通知
        if (translating) {
            updateNotification("翻译中: $progress")
        } else {
            updateNotification("服务运行中，点击'译'可截图")
        }
    }
    
    /**
     * 更新悬浮窗位置
     */
    private fun updateWindowPosition(deltaX: Int, deltaY: Int) {
        floatingView?.let { view ->
            try {
                layoutParams.x += deltaX
                layoutParams.y += deltaY
                
                // 添加边界检查，防止悬浮窗完全拖出屏幕
                val displayMetrics = resources.displayMetrics
                val maxX = displayMetrics.widthPixels - view.width
                val maxY = displayMetrics.heightPixels - view.height
                
                layoutParams.x = layoutParams.x.coerceIn(0, maxX)
                layoutParams.y = layoutParams.y.coerceIn(0, maxY)
                
                windowManager.updateViewLayout(view, layoutParams)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update window position", e)
            }
        }
    }

    // 新增：更新折叠窗口位置
    private fun updateCollapsedWindowPosition(deltaX: Int, deltaY: Int) {
        collapsedView?.let { view ->
            try {
                collapsedLayoutParams.x += deltaX
                collapsedLayoutParams.y += deltaY
                
                // 边界检查，确保小长条不会拖出屏幕
                val displayMetrics = resources.displayMetrics
                val maxY = displayMetrics.heightPixels - view.height
                
                // 限制X轴只能在屏幕右侧移动
                collapsedLayoutParams.x = collapsedLayoutParams.x.coerceIn(-100, 100)
                collapsedLayoutParams.y = collapsedLayoutParams.y.coerceIn(0, maxY)
                
                windowManager.updateViewLayout(view, collapsedLayoutParams)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update collapsed window position", e)
            }
        }
    }
    
    /**
     * 检测是否在模拟器中运行
     */
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT
    }
    


    /**
     * 处理翻译按钮点击事件，启动截图Activity
     */
    private fun handleTranslateClick() {
        if (isTranslating) {
            Log.d(TAG, "Translation already in progress, ignoring click")
            return
        }
        
        Log.d(TAG, "Translate button clicked, preparing for screenshot.")
        
        try {
            // 截图前先折叠悬浮窗到右边，避免遮挡屏幕内容
            Log.d(TAG, "Collapsing floating window before screenshot")
            handleCollapseClick()
            
            // 如果翻译面板显示中，也要隐藏
            if (translationPanelManager.isShowing()) {
                Log.d(TAG, "Translation panel is visible, hiding it before screenshot")
                translationPanelManager.hideTranslationResult()
            }
            
            // 等待较短时间确保窗口折叠和面板隐藏完成
            handler.postDelayed({
                proceedWithScreenshot()
            }, 800)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleTranslateClick", e)
            updateTranslationState(false)
        }
    }
    
    /**
     * 执行截图流程
     */
    private fun proceedWithScreenshot() {
        try {
            // 更新翻译状态
            updateTranslationState(true, "准备截图...")
            
            // 启动兜底恢复机制
            scheduleFloatingWindowRestore()
            
            // 延迟一下让UI更新，然后启动截图
            handler.postDelayed({
                try {
                    updateTranslationState(true, "正在截图...")
                    
                    // 启动透明的Activity来请求截图权限
                    val intent = Intent(this, ScreenCaptureActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        // 传递标志，告诉Activity需要恢复悬浮窗
                        putExtra("RESTORE_FLOATING_WINDOW", true)
                    }
                    
                    Log.d(TAG, "Starting ScreenCaptureActivity with intent: $intent")
                    startActivity(intent)
                    Log.d(TAG, "ScreenCaptureActivity started successfully")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start ScreenCaptureActivity", e)
                    updateTranslationState(false)
                    updateNotification("启动截图失败，请重试")
                }
            }, 100) // 短暂延迟让UI更新
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in proceedWithScreenshot", e)
            updateTranslationState(false)
        }
    }
    
    /**
     * 恢复悬浮窗显示 - 翻译完成后展开折叠的悬浮窗
     */
    fun restoreFloatingWindow() {
        // 重置翻译状态
        updateTranslationState(false)
        
        // 如果当前是折叠状态，展开悬浮窗
        if (isCollapsed) {
            Log.d(TAG, "Expanding collapsed window after translation")
            handleExpandClick()
        }
        
        Log.d(TAG, "Translation state reset.")
    }
    
    /**
     * 延迟恢复悬浮窗（兜底方案）
     */
    private fun scheduleFloatingWindowRestore() {
        // 15秒后自动重置翻译状态，防止状态卡住
        handler.postDelayed({
            if (isTranslating) {
                Log.w(TAG, "Translation timeout, resetting state")
                updateTranslationState(false)
                updateNotification("翻译超时，请检查网络连接")
            }
        }, 15000) // 15秒超时，给API调用更多时间
    }

    private fun removeFloatingWindow() {
        floatingView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Floating window removed")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove floating window", e)
            }
            floatingView = null
        }
    }

    // 新增：移除折叠窗口
    private fun removeCollapsedWindow() {
        collapsedView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "Collapsed window removed")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove collapsed window", e)
            }
            collapsedView = null
        }
    }
} 