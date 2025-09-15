package com.example.pizzamaniaapp;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int GPS_ENABLE_REQUEST = 1002;
    private static final String TAG = "MapActivity";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private double customerLat, customerLng;
    private boolean gpsPromptShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started ✅");
        setContentView(R.layout.activity_map);

        customerLat = getIntent().getDoubleExtra("lat", 0);
        customerLng = getIntent().getDoubleExtra("lng", 0);
        Log.d(TAG, "Received customer coordinates: lat=" + customerLat + ", lng=" + customerLng);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, requesting map async");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment NOT found!");
        }

        MaterialButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked, finishing activity");
            finish();
        });

        MaterialButton goToMapButton = findViewById(R.id.goToMapButton);
        goToMapButton.setOnClickListener(v -> {
            // Launch Google Maps navigation to customer
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + customerLat + "," + customerLng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady called");
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, requesting...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        checkGPSAndShowMarkers();
    }

    private void checkGPSAndShowMarkers() {
        if (gpsPromptShown) {
            Log.d(TAG, "GPS prompt already shown, skipping");
            return;
        }

        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(request);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(response -> {
            Log.d(TAG, "GPS is ON, showing markers");
            showMarkers();
        });

        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    Log.w(TAG, "GPS is OFF, prompting user to enable it");
                    gpsPromptShown = true;
                    ((ResolvableApiException) e).startResolutionForResult(MapActivity.this, GPS_ENABLE_REQUEST);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, "Failed to prompt GPS: " + sendEx.getMessage());
                }
            } else {
                Log.e(TAG, "Failed to check GPS: " + e.getMessage());
            }
        });
    }

    private void showMarkers() {
        LatLng customer = new LatLng(customerLat, customerLng);

        // Customer marker (RED)
        mMap.addMarker(new MarkerOptions()
                        .position(customer)
                        .title("Customer")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                .showInfoWindow(); // show label

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);

        fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    mMap.clear(); // clear previous markers

                    // Customer marker
                    mMap.addMarker(new MarkerOptions()
                                    .position(customer)
                                    .title("Customer")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                            .showInfoWindow();

                    // Deliveryman marker (BLUE)
                    LatLng deliveryman = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                                    .position(deliveryman)
                                    .title("You")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                            .showInfoWindow();

                    // Zoom to include both
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(customer);
                    builder.include(deliveryman);
                    LatLngBounds bounds = builder.build();
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                }
            }
        }, getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult called, requestCode=" + requestCode);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                checkGPSAndShowMarkers();
            } else {
                Log.w(TAG, "Location permission denied");
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
                if (mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(customerLat, customerLng), 15f));
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called, requestCode=" + requestCode);
        if (requestCode == GPS_ENABLE_REQUEST) {
            gpsPromptShown = false; // reset flag
            Log.d(TAG, "Returned from GPS enable prompt, checking GPS again");
            checkGPSAndShowMarkers();
        }
    }
}