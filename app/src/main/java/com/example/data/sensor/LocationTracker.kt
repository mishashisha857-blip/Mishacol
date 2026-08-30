package com.example.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.data.model.LatLngPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class LocationTracker(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<LatLngPoint?>(
        // Default to scenic central coordinates for initial preview
        LatLngPoint(55.7558, 37.6173)
    )
    val currentLocation: StateFlow<LatLngPoint?> = _currentLocation.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLngPoint>>(
        generateInitialPreviewRoute(55.7558, 37.6173)
    )
    val routePoints: StateFlow<List<LatLngPoint>> = _routePoints.asStateFlow()

    private val _workoutDistanceMeters = MutableStateFlow(0f)
    val workoutDistanceMeters: StateFlow<Float> = _workoutDistanceMeters.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (locationManager == null) return
        _isTracking.value = true

        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3f,
                    this
                )
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    3f,
                    this
                )
            }

            // Get last known
            val lastGps = if (isGpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNet = if (isNetworkEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            val bestLast = lastGps ?: lastNet
            if (bestLast != null) {
                val point = LatLngPoint(bestLast.latitude, bestLast.longitude)
                _currentLocation.value = point
            }
        } catch (e: SecurityException) {
            // Permission not granted yet, fallback to internal route simulator
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun resetWorkout() {
        _workoutDistanceMeters.value = 0f
        val current = _currentLocation.value ?: LatLngPoint(55.7558, 37.6173)
        _routePoints.value = listOf(current)
    }

    /**
     * Simulates next GPS step for interactive testing in emulator
     */
    fun simulateNextStep() {
        val current = _currentLocation.value ?: LatLngPoint(55.7558, 37.6173)
        val deltaLat = (kotlin.random.Random.nextDouble(-0.0004, 0.0006))
        val deltaLng = (kotlin.random.Random.nextDouble(-0.0004, 0.0006))
        val newPoint = LatLngPoint(current.latitude + deltaLat, current.longitude + deltaLng)

        val dist = calculateDistanceBetween(current.latitude, current.longitude, newPoint.latitude, newPoint.longitude)
        _workoutDistanceMeters.value += dist
        _currentLocation.value = newPoint
        _routePoints.value = _routePoints.value + newPoint
    }

    override fun onLocationChanged(location: Location) {
        val newPoint = LatLngPoint(location.latitude, location.longitude)
        val prev = _currentLocation.value

        if (prev != null && _isTracking.value) {
            val dist = calculateDistanceBetween(prev.latitude, prev.longitude, newPoint.latitude, newPoint.longitude)
            if (dist in 1.0..100.0) { // filter GPS jumps
                _workoutDistanceMeters.value += dist.toFloat()
                _routePoints.value = _routePoints.value + newPoint
            }
        }

        _currentLocation.value = newPoint
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    companion object {
        fun calculateDistanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val earthRadius = 6371000.0 // meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return (earthRadius * c).toFloat()
        }

        private fun generateInitialPreviewRoute(centerLat: Double, centerLng: Double): List<LatLngPoint> {
            val list = mutableListOf<LatLngPoint>()
            var lat = centerLat - 0.003
            var lng = centerLng - 0.004
            for (i in 0..8) {
                lat += 0.0007 + (i % 2) * 0.0003
                lng += 0.0009 - (i % 3) * 0.0002
                list.add(LatLngPoint(lat, lng))
            }
            return list
        }
    }
}
