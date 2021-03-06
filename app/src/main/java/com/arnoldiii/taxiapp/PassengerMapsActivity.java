package com.arnoldiii.taxiapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.arnoldiii.taxiapp.databinding.ActivityDriverMapsBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PassengerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityDriverMapsBinding binding;

    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean isLocationUpdatesActive;

    private Button settingsButton, singOutButton, bookTaxiButton;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private DatabaseReference driversGeoFire;
    private DatabaseReference nearestDriverLocation;
    private int searchRadius = 3;
    private boolean isDriverFound = false;
    private String nearestDriverID;
    private Marker driverMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_maps);

        settingsButton = (Button) findViewById(R.id.settingsButton);
        singOutButton = (Button) findViewById(R.id.signOutButton);
        bookTaxiButton = (Button) findViewById(R.id.bookTaxiButton);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        driversGeoFire = FirebaseDatabase.getInstance().getReference().child("driversGeoFire");

        //button for sign out
        singOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                signOutPassenger();
            }
        });
        //button for booking taxi
        bookTaxiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bookTaxiButton.setText("Getting a taxi...");

                gettingNearestTaxi();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        buildLocationRequest();
        buildLocationCallBack();
        buildLocationSettingsRequest();

        startLocationUpdates();


    }

    /*this method searches for the nearest drivers within a 3 km radius using driver geolocation
    if the driver is not found, the search radius will increase
    */
    private void gettingNearestTaxi() {
        GeoFire geoFire = new GeoFire(driversGeoFire);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude()),
                searchRadius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            //when driver found,then
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!isDriverFound) {
                    isDriverFound = true;
                    nearestDriverID = key;

                    getNearestDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }


            //unless drive isn`t found,radius will increased
            @Override
            public void onGeoQueryReady() {
                if (!isDriverFound) {
                    searchRadius++;
                    gettingNearestTaxi();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    //getting info about nearest driver location
    private void getNearestDriverLocation() {

        bookTaxiButton.setText("Getting your driver location");

        nearestDriverLocation = FirebaseDatabase
                .getInstance("https://taxi-app-71e05-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference().child("driversGeoFire").child(nearestDriverID).child("l");

        nearestDriverLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    List<Object> driverLocationParameters = (List<Object>) snapshot.getValue();

                    double latitude = 0;
                    double longitude = 0;

                    if (driverLocationParameters.get(0) != null) {
                        latitude = Double.parseDouble(driverLocationParameters.get(0).toString());
                    }
                    if (driverLocationParameters.get(1) != null) {
                        longitude = Double.parseDouble(driverLocationParameters.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(latitude, longitude);

                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location driverLocation = new Location("");
                    driverLocation.setLatitude(latitude);
                    driverLocation.setLongitude(longitude);
                    float distanceToDriver = driverLocation.distanceTo(currentLocation);

                    bookTaxiButton.setText("Distance to driver: " + distanceToDriver);

                    driverMarker = mMap.addMarker(
                            new MarkerOptions().position(driverLatLng).title("Your driver is here")
                    );
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

    }
    //method for sign out passenger and redirecting user to driver or client activity
    private void signOutPassenger() {
        String passengerUserID = currentUser.getUid();
        DatabaseReference passengers = FirebaseDatabase.
                getInstance("https://taxi-app-71e05-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference().child("passengers");

        GeoFire geoFire = new GeoFire(passengers);
        geoFire.removeLocation(passengerUserID);

        Intent intent = new Intent(PassengerMapsActivity.this, DriverOrClientActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (currentLocation != null) {
            // Add a marker of passenger and move the camera to passenger location
            LatLng passengerLocation = new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("Passenger location marker"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
        }
    }

    //checking if isLocationUpdatesActive = false,then return or location updating stopped
    private void stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return;
        }
        fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        isLocationUpdatesActive = false;
                    }
                });
    }

    //asks the user for permission to use location
    private void startLocationUpdates() {

        isLocationUpdatesActive = true;


        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(
                                PassengerMapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                                (PassengerMapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        //if success
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                                Looper.myLooper());
                        updateLocationUi();

                    }
                })
                //if dismiss
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvableApiException =
                                            (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(
                                            PassengerMapsActivity.this, CHECK_SETTINGS_CODE
                                    );
                                } catch (IntentSender.SendIntentException sendIntentException) {
                                    sendIntentException.printStackTrace();
                                }

                                break;
                            //if the application is unable to access the user's location by itself
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String message = "Adjust location settings on your device";
                                Toast.makeText(PassengerMapsActivity.this,
                                        message, Toast.LENGTH_LONG).show();

                                isLocationUpdatesActive = false;
                        }
                        updateLocationUi();
                    }
                });

    }

    //getting the result, whether the user has given permission to use location
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.d("MainActivity", "User has agreed to change location settings");
                        startLocationUpdates();
                        break;

                    case Activity.RESULT_CANCELED:
                        Log.d("MainActivity", "User has not agreed to change location settings");
                        isLocationUpdatesActive = false;
                        updateLocationUi();
                        break;
                }
                break;
        }
    }

    //build locationSettingRequest
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    //initialization of locationRequest,setting options:interval,priority
    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //initialization callBack,for getting current location and updating interface
    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();

                updateLocationUi();

            }
        };
    }

    /*updating interface with current location of user with the help of
    getting Latitude and Longitide and setting a marker of current user location
    */
    private void updateLocationUi() {
        if (currentLocation != null) {

            LatLng passengerLocation = new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
            mMap.addMarker(new MarkerOptions().position(passengerLocation).title("Passenger location marker"));

            //in this code, a database is created and in which the location of drivers is  passes

            String passengerUserID = currentUser.getUid();
            DatabaseReference passengersGeoFire = FirebaseDatabase.
                    getInstance("https://taxi-app-71e05-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference().child("passengersGeoFire");

            DatabaseReference passengers = FirebaseDatabase.
                    getInstance("https://taxi-app-71e05-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference().child("passengers");
            passengers.setValue(true);


            GeoFire geoFire = new GeoFire(passengersGeoFire);
            geoFire.setLocation(passengerUserID, new GeoLocation(currentLocation.getLatitude(),
                    currentLocation.getLongitude()));
        }
    }

    //when application is being on pause,location updates stopped
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    //checking if there is permission after pausing the application
    @Override
    protected void onResume() {
        super.onResume();

        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }
    //explains to the user why the app needs access to the location using the SnackBar
    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            showSnackBar("Location permission is needed for app functionality", "Ok",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(PassengerMapsActivity.this, new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION
                            );
                        }
                    });
            //ask user again about giving permission without explanation
        } else {
            ActivityCompat.requestPermissions(PassengerMapsActivity.this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }
    //initialization of SnackBar
    private void showSnackBar(final String mainText, final String action,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
                .setAction(action, listener).show();

    }

    //processes the received response to the request for permission to use the location
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            //request was cancelled
            if (grantResults.length <= 0) {
                Log.d("onRequestPermissionsRes", "Request was canceled");
                //request is accepted and the location is started
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates();
                }
                /*if user deny request again,then new snackBar appears with new parameters
                and action to open device settings
                 */
            } else {
                showSnackBar("Turn on location settings", "Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            }
        }
    }
    //checking if permission for location is granted
    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission
                .ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

}