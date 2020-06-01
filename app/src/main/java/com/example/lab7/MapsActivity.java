package com.example.lab7;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
GoogleMap.OnMapLoadedCallback,
GoogleMap.OnMarkerClickListener,
GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    List<Marker> markerList;
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;

    private SensorManager sensorManager;
    private Sensor sensor;
    Animation animShow, animHide;

    private final String MARKER_FILE = "marker.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerList= new ArrayList<>();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        assert sensorManager != null;
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        else {
            Toast.makeText(this, "Your device has no accelerometer", Toast.LENGTH_SHORT).show();
        }

        final FloatingActionButton fab_hide_x = findViewById(R.id.fab_hide_x);
        final FloatingActionButton fab_start_o = findViewById(R.id.fab_start_o);
        fab_hide_x.setAlpha(0.7f);
        fab_start_o.setAlpha(0.7f);

        fab_hide_x.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animHide = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide);
                fab_hide_x.startAnimation(animHide);
                fab_hide_x.setVisibility(View.GONE);
                fab_start_o.startAnimation(animHide);
                fab_start_o.setVisibility(View.GONE);

            }
        });

        final TextView accel_text = findViewById(R.id.accel_text);
        final boolean[] ifclick = {true};

        fab_start_o.setOnClickListener(new View.OnClickListener() { /*pokaz/ukryj gorny pasek*/
            @Override
            public void onClick(View view) {
                animShow = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show);
                animHide = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide);
                        if(ifclick[0]){
                            accel_text.startAnimation(animShow);
                            accel_text.setVisibility(View.VISIBLE);
                            ifclick[0] = false;
                        }
                        else if (!ifclick[0])
                        {
                            accel_text.startAnimation(animHide);
                            accel_text.setVisibility(View.GONE);
                            ifclick[0] = true;

                        }

            }
        });
     }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();

    }

    public void zoomInClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onMapLoaded() {
    Log.i(MapsActivity.class.getSimpleName(),"MapLoaded");
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=
            PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
    }
        Task<Location> lastLocation=fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location!=null&& mMap!=null){
                    mMap.addMarker(new MarkerOptions().position((new LatLng(location.getLatitude(), location.getLongitude())))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });
        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        mMap.getUiSettings().setMapToolbarEnabled(false);

        @SuppressLint("DefaultLocale") Marker marker = mMap.addMarker(new MarkerOptions()
        .position(new LatLng(latLng.latitude,latLng.longitude))
        .alpha(0.8f)
        .title(String.format("Position: (%.2f, %.2f)", latLng.latitude,latLng.longitude)));

        markerList.add(marker);

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        animShow = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show);

        FloatingActionButton fab_hide_x = findViewById(R.id.fab_hide_x);
        fab_hide_x.startAnimation(animShow);
        fab_hide_x.setVisibility(View.VISIBLE);
        FloatingActionButton fab_start_o = findViewById(R.id.fab_start_o);
        fab_start_o.startAnimation(animShow);
        fab_start_o.setVisibility(View.VISIBLE);

        return false;
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest,locationCallback,null);
    }

    private void createLocationCallback(){
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if (locationResult!=null){
                    if (gpsMarker!=null)
                        gpsMarker.remove();
                    Location location = locationResult.getLastLocation();
                    gpsMarker= mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(),location.getLongitude()))
                            .alpha(0.8f)
                            .title("Current Localisation"));
                }
            }
        };
    }

    @Override
    protected  void onPause(){
        super.onPause();
        stopLocationUpdates();

        if(sensor!=null)
            sensorManager.unregisterListener(this, sensor);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(sensor!=null)
            sensorManager.registerListener(this, sensor, 100000);
    }
    private void stopLocationUpdates(){
        if (locationCallback!=null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public void clearAll(View v) {/*czyszcenie znacznikow*/
        mMap.clear();
        markerList.clear();

        animHide = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide);

        FloatingActionButton fab_hide_x = findViewById(R.id.fab_hide_x);
        fab_hide_x.startAnimation(animHide);
        fab_hide_x.setVisibility(View.GONE);
        FloatingActionButton fab_start_o = findViewById(R.id.fab_start_o);
        fab_start_o.startAnimation(animHide);
        fab_start_o.setVisibility(View.GONE);
        TextView accel_text = findViewById(R.id.accel_text);
        accel_text.startAnimation(animHide);
        accel_text.setVisibility(View.GONE);

        deleteFile();
    }

    public void deleteFile(){
        File dir = getFilesDir();
        File file = new File(dir, MARKER_FILE);
        boolean clear = file.delete();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        TextView accel_text = findViewById(R.id.accel_text);

        double x = event.values[0];
        double y = event.values[1];

        String sx = String.format(Locale.ENGLISH, "x=%.4f ", x);
        String sy = String.format(Locale.ENGLISH, "y=%.4f ", y);

        String text = "Acceleration: " + "\n" + sx + sy;
        accel_text.setText(text);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void saveTasksToJson(){
        List<Pair<Double, Double>> list = new ArrayList<>();

        for (int i=0; i<markerList.size(); i++) {
            list.add(new Pair<>(markerList.get(i).getPosition().latitude, markerList.get(i).getPosition().longitude));
        }

            Gson gson = new Gson();
            String listJson = gson.toJson(list);
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(MARKER_FILE, MODE_APPEND);
                FileWriter writer = new FileWriter(outputStream.getFD());
                writer.write(listJson);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    protected void onDestroy(){
        super.onDestroy();
        deleteFile();
        saveTasksToJson();
    }

    public void restoreFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(MARKER_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Pair<Double, Double>>>() {
            }.getType();
            List<Pair<Double, Double>> o = gson.fromJson(readJson, collectionType);

            double[] x = new double[o.size()];
            double[] y = new double[o.size()];

            for(int i=0;i<o.size();i++){
                x[i] = o.get(i).first;
                y[i] = o.get(i).second;
            }

            for(int i=0;i<o.size();i++) {
                @SuppressLint("DefaultLocale") Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(x[i], y[i]))
                        .alpha(0.8f)
                        .title(String.format("Position: (%.2f, %.2f)", x[i], y[i])));

                markerList.add(marker);
            }
        }catch(FileNotFoundException e){
        e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

}
