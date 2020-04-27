package com.example.food_app;

import androidx.fragment.app.FragmentActivity;

import android.app.DownloadManager;
import android.os.Bundle;
import android.view.textclassifier.ConversationActions;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.android.volley.Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.food_app.WebApi;
import com.example.food_app.Restaurant;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<Restaurant> nearbyRestaurants;
    private final int PERMISSION_REQUEST_CONSTANT = 1;
    private LocationRequest mLocReq = new LocationRequest();
    private Marker mCurrLocMarker;
    private FusedLocationProviderClient mFusedLocClient;

    private void locDetails(double latitude, double longitude) {
        String latString = String.valueOf(latitude);
        String longString = String.valueOf(longitude);
        WebApi.startRequest(this,
                WebApi.API_BASE + "/geocode" + "?lat=" + latString + "&lon=" + longString,
                Request.Method.GET, null, response -> {
            final JsonObject resp = response;
            final JsonArray nearbyRest = resp.getAsJsonArray("nearby_restaurants");
            for (int i = 0; i < nearbyRest.size(); i++) {
                final JsonObject currRestJSON = nearbyRest.get(i).getAsJsonObject().getAsJsonObject("restaurant");
                final JsonObject currRestLocJSON = currRestJSON.get("location").getAsJsonObject();
                final String currRestName = currRestJSON.get("name").getAsString();
                final String currRestURL = currRestJSON.get("url").getAsString();
                final String currRestCuis = currRestJSON.get("cuisines").getAsString();
                final int currRestPrice42 = currRestJSON.get("average_cost_for_two").getAsInt();
                final double currRestLat = Double.valueOf(currRestLocJSON.get("latitude").getAsString());
                final double currRestLong = Double.valueOf(currRestLocJSON.get("longitude").getAsString());
                Restaurant currRestaurant = new Restaurant(currRestName, currRestPrice42, currRestURL, currRestCuis, currRestLat, currRestLong);
                nearbyRestaurants.add(currRestaurant);
            }
            }, error -> {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Called on when the activity begins. Sets up layout with map area and heading
     * Instantiates FusedLocationProviderClient object which is used to get
     * user's location information. Requests the user for location permission if permission
     * has not already been granted.
     * @param savedInstanceState The state of the activity when it was last saved
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Titling the activity
        //getSupportActionBar().setTitle("Find a Restaurant!");

        mFusedLocClient = LocationServices.getFusedLocationProviderClient(this);

        // Get the SupportMapFragment and get notified when map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Request the permission of the user to access location before moving on
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CONSTANT);
        } else {
            System.out.println("We have permission!");
        }
    }
    public void setUpRestPoints() {
        for (int i = 0; i < nearbyRestaurants.size(); i++) {
            LatLng currRest = new LatLng(nearbyRestaurants.get(i).getLatitude(), nearbyRestaurants.get(i).getLongitude());
            mMap.addMarker(new MarkerOptions().position(currRest).title(nearbyRestaurants.get(i).getName()));
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mLocReq.setInterval(1000);
        mLocReq.setFastestInterval(1000);
        mLocReq.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mFusedLocClient.requestLocationUpdates(mLocReq, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);

    }
    int i = 0;
    LocationCallback mLocationCallback = new LocationCallback() {
        /**
         * Gets the user's most recent location, places a marker
         * at that location and centers the view on this location. a
         * @param locationResult Used to retrieve list of all location updates from mFusedLocClient I think?
         */
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current location");
                locDetails(latLng.latitude, latLng.longitude);
                setUpRestPoints();
                if (mCurrLocMarker == null) {
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 9));
                }
                mCurrLocMarker = mMap.addMarker(markerOptions);
            }
        }
    };
}
