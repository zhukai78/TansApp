package com.portwind.gametrans

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import android.view.inputmethod.InputMethodManager
import android.os.Handler
import android.os.Looper

class PromptWindowManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val coroutineScope: CoroutineScope,
    private val onConfirm: (String) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var promptView: ComposeView? = null
    private var layoutParams = WindowManager.LayoutParams()

    companion object {
        private const val TAG = "PromptWindowManager"
    }

    fun show() {
        if (promptView != null) return
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
                // 允许窗口获得焦点，避免阻止输入法；保留外部点击监听与全屏布局
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                // 调整大小并请求显示软键盘
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                               WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            }

            promptView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setContent {
                    PromptDialog(
                        isVisible = true,
                        onDismissRequest = ::hide,
                        onConfirm = { prompt ->
                            onConfirm(prompt)
                        }
                    )
                }
            }
            windowManager.addView(promptView, layoutParams)

            // 强制显示软键盘，确保弹出
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
                    Log.d(TAG, "Soft keyboard forced to show for prompt")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to show soft keyboard for prompt", e)
                }
            }, 200)

            Log.d(TAG, "Prompt window shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show prompt window", e)
        }
    }

    fun hide() {
        promptView?.let { view ->
            try {
                // 隐藏软键盘
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)

                windowManager.removeView(view)
                Log.d(TAG, "Prompt window hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide prompt window", e)
            }
            promptView = null
        }
    }

    fun isShowing(): Boolean = promptView != null
}
