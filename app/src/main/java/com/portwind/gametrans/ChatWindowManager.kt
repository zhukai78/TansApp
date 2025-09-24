package com.portwind.gametrans

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import android.os.Handler
import android.os.Looper

class ChatWindowManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val coroutineScope: CoroutineScope,
    private val onSendMessage: suspend (String, List<ChatMessage>) -> String?
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var chatView: ComposeView? = null
    private var layoutParams = WindowManager.LayoutParams()

    companion object {
        private const val TAG = "ChatWindowManager"
    }

    fun show() {
        if (chatView != null) return
        try {
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // 关键修复：移除所有可能阻止键盘弹出的标志
                // 不设置 FLAG_NOT_FOCUSABLE，允许窗口获得焦点
                // 不设置 FLAG_ALT_FOCUSABLE_IM，使用默认输入法行为
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                // 关键修复：设置正确的软键盘模式
                // SOFT_INPUT_ADJUST_RESIZE: 调整窗口大小以适应键盘
                // SOFT_INPUT_STATE_ALWAYS_VISIBLE: 强制显示键盘
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or 
                               WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            }

            chatView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setContent {
                    ChatDialog(
                        isVisible = true,
                        onDismissRequest = ::hide,
                        onSendMessage = this@ChatWindowManager.onSendMessage,
                        coroutineScope = coroutineScope
                    )
                }
            }
            windowManager.addView(chatView, layoutParams)
            
            // 强制显示软键盘
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
                    Log.d(TAG, "Soft keyboard forced to show")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to show soft keyboard", e)
                }
            }, 200)
            
            Log.d(TAG, "Chat window shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show chat window", e)
        }
    }

    fun hide() {
        chatView?.let { view ->
            try {
                // 隐藏软键盘
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                
                windowManager.removeView(view)
                Log.d(TAG, "Chat window hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide chat window", e)
            }
            chatView = null
        }
    }

    fun isShowing(): Boolean = chatView != null
}
