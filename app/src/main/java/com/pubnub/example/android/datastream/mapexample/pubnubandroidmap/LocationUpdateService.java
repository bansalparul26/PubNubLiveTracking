package com.pubnub.example.android.datastream.mapexample.pubnubandroidmap;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class LocationUpdateService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "LocationUpdateService";
    private static final long INTERVAL = 5000;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1233;
    private static LocationUpdateService mInstance;
    private static OnLocationReceived mLocationReceived;
    private static Context activityContext;
    private LatLng latLong;
    private static Context mContext;
    public long TIMER_INTERVAL = 5000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private boolean boolOut = false;
    private Timer timer;
    private TimerTask timerTask;
    private Handler handler = new Handler();
    private MainActivity baseActivity;

    public LocationUpdateService(Context context) {
        log("In Constructor");
        mContext = context;
        //  syncManager.setBaseUrl(Const.SERVER_REMOTE_URL, getString(R.string.app_name));
        init();
    }


    public LocationUpdateService() {

    }

    public static LocationUpdateService getInstance() {
        if (mInstance == null) {
            mInstance = new LocationUpdateService();
        }
        return mInstance;
    }

    public static void startService(Context activity) {
        Intent callIntent = new Intent(activity, LocationUpdateService.class);
        activity.startService(callIntent);
        activityContext = activity;

    }

    public static void stopService(Context context) {
        Intent myService = new Intent(context, LocationUpdateService.class);
        context.stopService(myService);
        log("  stopService");
    }

    protected static void log(String string) {
        Log.e("LocationUpdateService", string);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void setLocationReceivedListener(OnLocationReceived mLocationReceived) {
        LocationUpdateService.mLocationReceived = mLocationReceived;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setSmallestDisplacement(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log(">>>>>>>>>>>>>>> Service Start");
        if (mContext == null)
            mContext = this;
        log("Context" + mContext);
        baseActivity = (MainActivity) activityContext;
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    public void init() {
        createLocationRequest();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        getLastLocation();
        startTimer();
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
        int resultCode = gApi.isGooglePlayServicesAvailable(mContext);
        return resultCode == ConnectionResult.SUCCESS;
    }


    @Override
    public void onDestroy() {
        log(">>>>>>>>> Service Destroy");
        stopTimer();
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        log("onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());
        if (mLocationReceived != null)
            mLocationReceived.onConntected(bundle);
        if (mGoogleApiClient.isConnected())
            startLocationUpdates();
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission((Activity) mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        log("Location update started ..............: ");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        log("Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        log("Firing onLocationChanged................    latitude=  " + location.getLatitude() + "    longitude=   " + location.getLongitude() + "  " + mLastUpdateTime);
        if (mLocationReceived != null)
            mLocationReceived.onLocationReceived(location);
        /*RequestParams params = new RequestParams();
        params.put("User[lat]", "" + location.getLatitude());
        params.put("User[longt]", "" + location.getLongitude());
        syncManager.sendToServer("api/user/set-location", params, this);*/
    }

    public LatLng getLatLng(Location currentLocation) {
        if (currentLocation != null) {
            LatLng latLong = new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude());
            return latLong;
        } else {
            return null;
        }
    }

    public Location getLastLocation() {
        if (isGooglePlayServicesAvailable()) {
            Location location = null;
            if (mGoogleApiClient.isConnected()) {
               /* if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkLocationPermission();
                }*/
                if (ContextCompat.checkSelfPermission((Activity)mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return null;
                }
                location = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
                latLong = getLatLng(location);
            }
        }
        Location location = new Location("");
        if (latLong != null) {
            location.setLatitude(latLong.latitude);
            location.setLongitude(latLong.longitude);

            return location;
        }
        return null;
    }

    protected void stopLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log("Location update stopped .......................");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public interface OnLocationReceived {
        void onLocationReceived(Location location);
        void onConntected(Bundle bundle);
    }

    Runnable task = new Runnable() {
        @Override
        public void run() {
            Location location = getLastLocation();
            if (mGoogleApiClient.isConnected() && location != null) {
                onLocationChanged(location);
            }
        }
    };

    public void startTimer() {
        stopTimer();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(task);
            }
        };
        timer.schedule(timerTask, 1000, TIMER_INTERVAL);
    }

    public void stopTimer() {
        if (timer != null && timerTask != null) {
            timerTask.cancel();
            timer.cancel();
            timer = null;
        }
    }
}