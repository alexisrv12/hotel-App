package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Helper class for handling location services and proximity detection
 * for staff check-ins and hotel perimeter verification.
 */
object LocationProximityHelper {

    // Default Hotel Coordinates (Hotel Rivera, customizable)
    private const val HOTEL_LATITUDE = 19.432608
    private const val HOTEL_LONGITUDE = -99.133209
    private const val MAX_CHECKIN_RADIUS_METERS = 300.0 // 300 meters perimeter

    /**
     * Checks if location permissions (FINE or COARSE) are granted.
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
     * Calculates the distance in meters between a given location and the hotel premises.
     */
    fun calculateDistanceToHotel(lat: Double, lng: Double): Float {
        val hotelLoc = Location("hotel").apply {
            latitude = HOTEL_LATITUDE
            longitude = HOTEL_LONGITUDE
        }
        val currentLoc = Location("current").apply {
            latitude = lat
            longitude = lng
        }
        return currentLoc.distanceTo(hotelLoc)
    }

    /**
     * Checks if the given coordinates are within the authorized check-in perimeter.
     */
    fun isWithinHotelPerimeter(lat: Double, lng: Double): Boolean {
        val distance = calculateDistanceToHotel(lat, lng)
        return distance <= MAX_CHECKIN_RADIUS_METERS
    }

    /**
     * Retrieves the current device location asynchronously using FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onLocationReceived: (Location?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onLocationReceived(null)
            return
        }

        try {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).addOnSuccessListener { location: Location? ->
                onLocationReceived(location)
            }.addOnFailureListener {
                onLocationReceived(null)
            }
        } catch (e: SecurityException) {
            onLocationReceived(null)
        } catch (e: Exception) {
            onLocationReceived(null)
        }
    }
}
