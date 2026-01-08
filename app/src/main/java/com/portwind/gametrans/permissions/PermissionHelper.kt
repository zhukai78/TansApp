package com.portwind.gametrans.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.portwind.gametrans.R

class PermissionHelper(private val activity: ComponentActivity) {
    
    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null
    
    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> = 
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val hasPermission = hasOverlayPermission(activity)
            onPermissionResult?.invoke(hasPermission)
        }
    
    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            onLocationPermissionResult?.invoke(allGranted)
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
     * 检查是否有地理位置权限
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * 请求地理位置权限
     */
    fun requestLocationPermission(onResult: (Boolean) -> Unit) {
        if (hasLocationPermission(activity)) {
            onResult(true)
            return
        }

        onLocationPermissionResult = onResult
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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
        
        /**
         * 静态方法检查地理位置权限
         */
        fun hasLocationPermission(context: Context): Boolean {
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val coarseLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            return fineLocationGranted || coarseLocationGranted
        }
    }
} 