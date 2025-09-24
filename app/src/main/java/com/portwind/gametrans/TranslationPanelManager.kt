package com.portwind.gametrans

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt

class TranslationPanelManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var translationPanel: ComposeView? = null
    private var panelLayoutParams = WindowManager.LayoutParams()

    
    companion object {
        private const val TAG = "TranslationPanelManager"
    }
    
    /**
     * 显示翻译结果面板
     */
    fun showTranslationResult(
        translationResult: String,
        onPlayOriginal: (String) -> Unit = {},
        onPromptTaskSelected: (AiTask) -> Unit = {}
    ) {
        // 如果面板已存在，先移除
        hideTranslationResult()
        
        try {
            panelLayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                x = 0
                y = 0
            }
            
            translationPanel = ComposeView(context).apply {
                // 设置生命周期相关组件
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                
                // 设置Compose内容
                setContent {
                    TranslationResultPanel(
                        translationResult = translationResult,
                        isVisible = true,
                        onClose = { hideTranslationResult() },
                        onDrag = { dragAmount ->
                            updatePanelPosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                        },
                        onPlayOriginal = onPlayOriginal,
                        onPromptTaskSelected = onPromptTaskSelected
                    )
                }
            }
            
            windowManager.addView(translationPanel, panelLayoutParams)
            Log.d(TAG, "Translation result panel shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show translation result panel", e)
        }
    }
    
    /**
     * 隐藏翻译结果面板
     */
    fun hideTranslationResult() {
        translationPanel?.let { panel ->
            try {
                windowManager.removeView(panel)
                Log.d(TAG, "Translation result panel hidden")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to hide translation result panel", e)
            }
            translationPanel = null
        }
    }
    
    /**
     * 更新面板位置
     */
    private fun updatePanelPosition(deltaX: Int, deltaY: Int) {
        translationPanel?.let { panel ->
            try {
                panelLayoutParams.x += deltaX
                panelLayoutParams.y += deltaY
                
                // 边界检查，防止面板被拖出屏幕
                val displayMetrics = context.resources.displayMetrics
                val maxX = displayMetrics.widthPixels / 2 - panel.width / 2
                val maxY = displayMetrics.heightPixels / 2 - panel.height / 2
                val minX = -displayMetrics.widthPixels / 2 + panel.width / 2
                val minY = -displayMetrics.heightPixels / 2 + panel.height / 2
                
                panelLayoutParams.x = panelLayoutParams.x.coerceIn(minX, maxX)
                panelLayoutParams.y = panelLayoutParams.y.coerceIn(minY, maxY)
                
                windowManager.updateViewLayout(panel, panelLayoutParams)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update panel position", e)
            }
        }
    }
    
    /**
     * 检查面板是否正在显示
     */
    fun isShowing(): Boolean {
        return translationPanel != null
    }
    
    /**
     * 清理资源
     */
    fun release() {
        hideTranslationResult()
    }
} 