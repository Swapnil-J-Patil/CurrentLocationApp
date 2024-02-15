package com.swapnil.yourloaction

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.currentCameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import com.swapnil.yourloaction.ui.theme.YourLoactionTheme

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequired: Boolean = false
    private val permissions=arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    override fun onResume() {
        super.onResume()
        if(locationRequired)
        {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,100
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapsInitializer.initialize(this,MapsInitializer.Renderer.LATEST){

        }
        fusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        setContent {

            var currentLocation by remember{ mutableStateOf(LatLng(0.toDouble(),0.toDouble())) }
            val cameraPosition= rememberCameraPositionState{
                position= CameraPosition.fromLatLngZoom(
                    currentLocation,20f
                )
            }
            val origin = LatLng(18.891915, 72.928267)//You can add your area location it's for camera position

            val cameraPositionStateNew = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(origin, 18f)
            }

            var cameraPositionState by remember {
                mutableStateOf(cameraPosition)
            }
            locationCallback=object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult){
                    super.onLocationResult(p0)
                    for (location in p0.locations)
                    {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        cameraPositionState= CameraPositionState(
                            position= CameraPosition.fromLatLngZoom(
                                currentLocation,20f
                            )
                        )
                    }
                }
            }
            YourLoactionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationScreen(this@MainActivity,currentLocation,cameraPositionStateNew)
                }
            }
        }
    }

    @Composable
    private fun LocationScreen(context: Context, currentLocation: LatLng,cameraPositionState: CameraPositionState) {
        val launchMultiplePermissions= rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions() )
        {
                permissionMaps->
            val areGranted=permissionMaps.values.reduce{acc,next->acc && next}
            if(areGranted)
            {
                locationRequired=true
                startLocationUpdates()
                Toast.makeText(context,"Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(context,"Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

        Box(modifier=Modifier.fillMaxSize()) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(
                        position = currentLocation,
                    ),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    title = "current location",
                    snippet = "You are here!!!"
                )

            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Your location is ${currentLocation.latitude} and ${currentLocation.longitude}")
                Button(onClick = {

                    if (permissions.all {
                            ContextCompat.checkSelfPermission(
                                context,
                                it
                            ) == PackageManager.PERMISSION_GRANTED
                        }) {
                        //get location
                        startLocationUpdates()
                    } else {
                        launchMultiplePermissions.launch(permissions)
                    }
                }) {
                    Text(text = "Get location")
                }
            }
        }
    }
}

