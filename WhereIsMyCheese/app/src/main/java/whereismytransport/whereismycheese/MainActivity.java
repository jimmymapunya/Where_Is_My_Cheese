package whereismytransport.whereismycheese;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.location.LocationEngineProvider;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static whereismytransport.whereismycheese.Constants.PERMISSIONS.SERVICE;
import static whereismytransport.whereismycheese.Constants.PERMISSIONS.SHARED_PREF_NAME;

public class MainActivity extends AppCompatActivity implements LocationEngineListener, OnMapReadyCallback, PermissionsListener {

    private List<Marker> markers = new ArrayList<>();

    private MapView mapView;
    private MapboxMap map;

    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private Location originLocation;
    SharedPreferences sharedPreferences;
    List<LatLng> arrPackageData;
    List<CheesyTreasure> cheesyTreasures;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Mapbox Access token
        Mapbox.getInstance(this, getString(R.string.access_token));

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // One does not simply just cheez, you require some permissions.
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            initializeMap();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.PERMISSIONS.ACCESS_FINE_LOCATION);
        }

        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME,MODE_APPEND);

     }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.PERMISSIONS.ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission granted
                    initializeMap();
                } else {
                    // permission denied, boo! Fine, no cheese for you!
                    // No need to do anything here, for this exercise we only care about people who like cheese and have location setting on.
                }
                return;
            }
        }
    }

    private void initializeMap() {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                MainActivity.this.map = mapboxMap;
                setupLongPressListener();

                clickMarker();
            }
        });
    }

    private void setupLongPressListener() {
        map.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng point) {
                createCheesyNote(point);
            }
        });
    }

    //Click cheese and see the information, pick up/remove cheese
    private void clickMarker(){
        map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {

                ShowCheesyDialog note = new ShowCheesyDialog(MainActivity.this, marker,marker.getTitle());
                note.show();
                return true;
            }
        });
    }

    //Click Save Button and save Note
    private void createCheesyNote(final LatLng point) {
        CheesyDialog note = new CheesyDialog(this, new CheesyDialog.INoteDialogListener() {
            @Override
            public void onNoteAdded(String note) {
                addCheeseToMap(point, note);

                saveToLocalService(point, note);
            }
        });
        note.show();
    }

    private void addCheeseToMap(LatLng point, String content) {
        IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
        Icon icon = iconFactory.fromBitmap(getBitmapFromDrawableId(R.drawable.cheese64));
        MarkerOptions marker = new MarkerOptions();
        marker.setIcon(icon);
        marker.setPosition(point);
        marker.setTitle(content);
        markers.add(map.addMarker(marker));
    }

    private Bitmap getBitmapFromDrawableId(int drawableId) {
        Drawable vectorDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vectorDrawable = getResources().getDrawable(drawableId, null);
        } else {
            vectorDrawable = getResources().getDrawable(drawableId);
        }

        Drawable wrapDrawable = DrawableCompat.wrap(vectorDrawable);

        int h = vectorDrawable.getIntrinsicHeight();
        int w = vectorDrawable.getIntrinsicWidth();

        h = h > 0 ? h : 96;
        w = w > 0 ? w : 96;

        wrapDrawable.setBounds(0, 0, w, h);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        wrapDrawable.draw(canvas);
        return bm;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationEngine != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onStart() {
        super.onStart();
        if (locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        mapView.onStop();
        stopService();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    //
    @Override
    public void onLocationChanged(Location location) {

        if(location != null){
            originLocation = location;
            setCameraPosition(location);
        }

        double cheeseLat, cheeseLon, distance;
        String title;

        for(int x =0;x<readFromLocalService().size(); x++)
        {
            cheeseLat = readFromLocalService().get(x).getLocation().getLatitude();
            cheeseLon = readFromLocalService().get(x).getLocation().getLongitude();
            title = readFromLocalService().get(x).getNote();

            distance = calculateDistance(cheeseLat, cheeseLon, location.getLatitude(), location.getLongitude());

            if(distance<=50){
                startService();
                addCheeseToMap(new LatLng(cheeseLat,cheeseLon),title);
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //Why they need access

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        enableLocation();

    }
    private void enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();

        }else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){

        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null){
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        }else {
            locationEngine.addLocationEngineListener(this);
        }

    }
    private void initializeLocationLayer(){

//        locationLayerPlugin = new LocationlayerPlugin(mapView, map, locationEngine);
//        locationLayerPlugin.setLocationLayerEnabled();
//        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
//        locationLayerPlugin.setRenderMode(RenderMode.normal);
    }

    //Start the service
    public void startService() {
        Intent serviceIntent = new Intent(this, CheesyService.class);
        serviceIntent.putExtra(SERVICE, getString(R.string.notification));
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    //Stop the service
    public void stopService() {
        Intent serviceIntent = new Intent(this, CheesyService.class);
        stopService(serviceIntent);
    }

    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15.0));

        //Show Current location
        addCheeseToMap(new LatLng(location.getLatitude(), location.getLongitude()), getString(R.string.current_location));
    }

    //Save data to SharedPreference
    public void saveToLocalService(LatLng point, String note) {

        List<CheesyTreasure> storeDataModels = readFromLocalService();
        CheesyTreasure tempDataStore = new CheesyTreasure(point,note);

        if(tempDataStore.equals(storeDataModels)){
            //Same Location, same note
        }
        storeDataModels.add(tempDataStore);

        Gson gson = new Gson();
        String json = gson.toJson(storeDataModels);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Set",json );
        editor.apply();

    }

    //Read data from sharedPreference
    public List<CheesyTreasure> readFromLocalService() {

        Gson gson = new Gson();
        String json = sharedPreferences.getString("Set", "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        } else {
            Type type = new TypeToken<List<CheesyTreasure>>() {
            }.getType();
            cheesyTreasures = gson.fromJson(json, type);
        }
        return cheesyTreasures;
    }

    //Calculate the distance between cheese note and current location
    private double  calculateDistance(double latCheese, double lonCheese, double latCurrentLocation, double lonCurrentLocation ){

        Location startPoint=new Location("CheeseLocation");
        startPoint.setLatitude(latCheese);
        startPoint.setLongitude(lonCheese);

        Location endPoint=new Location("CurrentLocation");
        endPoint.setLatitude(latCurrentLocation);
        endPoint.setLongitude(lonCurrentLocation);

        double distance=startPoint.distanceTo(endPoint);

        return distance;
    }

}
