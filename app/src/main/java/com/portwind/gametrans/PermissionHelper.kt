package com.portwind.gametrans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class PermissionHelper(private val activity: ComponentActivity) {
    
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    
    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> = 
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val hasPermission = hasOverlayPermission(activity)
            onPermissionResult?.invoke(hasPermission)
        }

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(onResult: (Boolean) -> Unit) {
        if (hasOverlayPermission(activity)) {
            onResult(true)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onPermissionResult = onResult
            
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            
            try {
                overlayPermissionLauncher.launch(intent)
            } catch (e: Exception) {
                // 如果无法打开设置页面，尝试打开通用应用设置页面
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                try {
                    overlayPermissionLauncher.launch(fallbackIntent)
                } catch (ex: Exception) {
                    onResult(false)
                }
            }
        } else {
            onResult(true)
        }
    }

    /**
     * 显示权限说明对话框
     */
    fun showPermissionRationale(context: Context): String {
        return context.getString(R.string.permission_message)
    }

    companion object {
        /**
         * 静态方法检查悬浮窗权限
         */
        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }
} 