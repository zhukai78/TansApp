package com.portwind.gametrans.context

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * ContextManager
 * Responsible for gathering real-world context (Time, Location) to inject into AI prompts.
 */
class ContextManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    companion object {
        private const val TAG = "ContextManager"
        private const val LOCATION_TIMEOUT_MS = 10000L // 10秒超时（增加时间）
    }

    /**
     * Get the current context string.
     * Includes Time and Location (if permission granted).
     */
    suspend fun getCurrentContext(): String {
        Log.d(TAG, "========== 获取上下文信息 ==========")
        val timeInfo = getTimeContext()
        Log.d(TAG, "时间: $timeInfo")
        
        val locationInfo = getLocationContext()
        Log.d(TAG, "位置: $locationInfo")
        Log.d(TAG, "===================================")
        
        return """
            Current Context:
            - Time: $timeInfo
            - Location: $locationInfo
        """.trimIndent()
    }

    private fun getTimeContext(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocationContext(): String {
        // 检查权限
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return "Unknown (Permission Denied)"
        }

        // 检查 Google Play Services 是否可用
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.w(TAG, "Google Play Services not available: $resultCode")
            // 回退到系统 LocationManager
            return getLocationFromSystemManager()
        }

        // 检查位置服务是否开启
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d(TAG, "Location providers - GPS: $isGpsEnabled, Network: $isNetworkEnabled")
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.w(TAG, "Both GPS and Network location are disabled")
            return "Unknown (Please enable location services)"
        }

        // 先尝试获取 lastLocation（快速）
        val lastLocation = try {
            getLastLocation()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get last location", e)
            null
        }

        if (lastLocation != null) {
            Log.d(TAG, "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
            return formatLocation(lastLocation)
        }

        // 如果 lastLocation 为空，主动请求当前位置（带超时）
        Log.d(TAG, "Last location is null, requesting current location...")
        val currentLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            requestCurrentLocation()
        }

        if (currentLocation != null) {
            Log.d(TAG, "Got current location: ${currentLocation.latitude}, ${currentLocation.longitude}")
            return formatLocation(currentLocation)
        }
        
        // 最后尝试：使用系统 LocationManager 作为回退
        Log.w(TAG, "FusedLocation timeout, trying system LocationManager...")
        return getLocationFromSystemManager()
    }
    
    @SuppressLint("MissingPermission")
    private fun getLocationFromSystemManager(): String {
        try {
            // 尝试从 GPS 或网络获取最后已知位置
            val gpsLocation = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
            
            val networkLocation = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
            
            // 选择更新时间更近的位置
            val location = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
            
            return if (location != null) {
                Log.d(TAG, "Got location from system LocationManager: ${location.latitude}, ${location.longitude}")
                formatLocation(location)
            } else {
                Log.w(TAG, "System LocationManager also returned null")
                "Unknown (Location unavailable - please try outdoors)"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get location from system LocationManager", e)
            return "Unknown (Location error)"
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                cont.resume(location)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last location", e)
                cont.resume(null)
            }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = result.lastLocation
                Log.d(TAG, "Location callback received: ${location?.latitude}, ${location?.longitude}")
                cont.resume(location)
            }
        }

        cont.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates", e)
            cont.resume(null)
        }
    }

    private fun formatLocation(location: Location): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用异步 API，但这里简化处理，直接返回坐标 + 尝试同步获取
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                formatAddress(addresses, location)
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                formatAddress(addresses, location)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoder failed", e)
            "Lat: ${String.format(Locale.US, "%.4f", location.latitude)}, Lon: ${String.format(Locale.US, "%.4f", location.longitude)}"
        }
    }

    private fun formatAddress(addresses: List<android.location.Address>?, location: Location): String {
        return if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Unknown City"
            val district = address.subLocality ?: ""
            val country = address.countryName ?: "Unknown Country"
            
            if (district.isNotEmpty()) {
                "$city $district, $country"
            } else {
                "$city, $country"
            }
        } else {
            "Lat: ${String.format(Locale.US, "%.4f", location.latitude)}, Lon: ${String.format(Locale.US, "%.4f", location.longitude)}"
        }
    }
}
