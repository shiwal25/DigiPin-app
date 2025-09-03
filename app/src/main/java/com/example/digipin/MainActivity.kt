package com.example.digipin

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.OnMapReadyCallback
import android.Manifest
import android.util.Log
import android.annotation.SuppressLint
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import kotlin.collections.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var map: GoogleMap? = null
    private lateinit var mapView: MapView
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted = false
    private val defaultLocation = LatLng(20.5937, 78.9629)
    lateinit var currentLatLng: LatLng
     lateinit var idtext:TextView

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    companion object{
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private val TAG = MainActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getLocationPermission()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapViewBundle = savedInstanceState?.getBundle(MAPVIEW_BUNDLE_KEY)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(mapViewBundle)

        mapView.getMapAsync(this)
        idtext = findViewById(R.id.idBtn)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("Ktor", "onSaveInstanceState: ")
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }

        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("Ktor", "onMapReady: ")
        this.map = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        updateLocationUI()
        getDeviceLocation()

    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = true

            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            currentLatLng = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                            map?.addMarker(
                                MarkerOptions()
                                    .position(currentLatLng)
                                    .title(currentLatLng.toString())
                                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                                    ))
                            )
                            idtext.setOnClickListener {
                                lifecycleScope.launch {
                                    try {
                                        calling()
                                    } catch (e: Exception) {
                                        Log.e("Ktor", "API Call Failed: ${e.message}")
                                    }
                                }
                            }

                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
                updateLocationUI()
                getDeviceLocation()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    suspend fun calling(){
        Log.d("Ktor", "calling: ")
        if (lastKnownLocation == null) {
            Log.e("Ktor", "lastKnownLocation is null")
            return
        }
        Log.d("Ktor", "calling: lastKnownLocation!!.latitude"+lastKnownLocation!!.latitude)
        Log.d("Ktor", "calling: lastKnownLocation!!.longitude"+lastKnownLocation!!.longitude)
        val client = HttpClient(CIO)
        val response: HttpResponse = client.get {
            url("https://digipin.onrender.com/api/digipin/encode")
            parameter("latitude",lastKnownLocation!!.latitude)
            parameter("longitude",lastKnownLocation!!.longitude)
        }
        Log.d("Ktor", "calling: "+response.status)
        val body = response.bodyAsText()
        Log.d("Ktor", "calling: "+body)
        client.close()

        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.decodeFromString<DigiPinResponse>(body)
        val dialog = PinDisplaySheet.newInstance(parsed.digipin)
        dialog.show(supportFragmentManager, "DigiPinBottomSheet")
    }

    @Serializable
    data class DigiPinResponse(val digipin: String)

}