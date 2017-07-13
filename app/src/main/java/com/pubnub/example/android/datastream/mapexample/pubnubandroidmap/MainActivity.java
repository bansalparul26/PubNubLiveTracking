package com.pubnub.example.android.datastream.mapexample.pubnubandroidmap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.base.Throwables;
import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements  OnMapReadyCallback ,  LocationUpdateService.OnLocationReceived  {
    public static final String TAG = MainActivity.class.getName();

    public static final String DATASTREAM_PREFS = "com.pubnub.example.android.datastream.mapexample.DATASTREAM_PREFS";
    public static final String DATASTREAM_UUID = "com.pubnub.example.android.datastream.mapexample.DATASTREAM_UUID";

    public static final String PUBLISH_KEY = "pub-c-67e3bd92-eaa1-4bb6-b6d3-1fa736aa2389";
    public static final String SUBSCRIBE_KEY = "sub-c-a666d314-65ee-11e7-bfac-0619f8945a4f";
    public static final String CHANNEL_NAME = "maps-channel";
    private Timer timer;
    private TimerTask timerTask;
    private android.os.Handler handler = new android.os.Handler();
    public static final long TIMER_INTERVAL = 60000;
    LocationUpdateService locationUpdateService;
    private GoogleMap mMap;
    private PubNub mPubNub;
    private SharedPreferences mSharedPrefs;
    private Marker mMarker;
    private Polyline mPolyline;
    private List<LatLng> mPoints = new ArrayList<>();
    private double lat,longs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPrefs = getSharedPreferences(DATASTREAM_PREFS, MODE_PRIVATE);
        if (!mSharedPrefs.contains(DATASTREAM_UUID)) {
            Intent toLogin = new Intent(this, LoginActivity.class);
            startActivity(toLogin);
            return;
        }

        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        initPubNub();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocationUpdateService.startService(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        locationUpdateService = new LocationUpdateService(this);
        locationUpdateService.setLocationReceivedListener(this);
    }


    private final void initPubNub() {
        PNConfiguration config = new PNConfiguration();
        config.setPublishKey(PUBLISH_KEY);
        config.setSubscribeKey(SUBSCRIBE_KEY);
        config.setSecure(true);

        this.mPubNub = new PubNub(config);
        this.mPubNub.subscribe().channels(Arrays.asList(CHANNEL_NAME)).execute();
        SubscribeCallback subscribeCallback = new SubscribeCallback() {
            @Override
            public void status(PubNub pubnub, PNStatus status) {
                if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory) {
                    // internet got lost, do some magic and call reconnect when ready
                    pubnub.reconnect();
                } else if (status.getCategory() == PNStatusCategory.PNTimeoutCategory) {
                    // do some magic and call reconnect when ready
                    pubnub.reconnect();
                }
                       }

            @Override
            public void message(PubNub pubnub, PNMessageResult message) {
                try {
                    Log.v(TAG, JsonUtil.asJson(message));
                    Map<String, String> map = JsonUtil.convert(message.getMessage(), LinkedHashMap.class);
                    String lat = map.get("lat");
                    String lng = map.get("lng");
                    System.out.println("publish worked! timetoken: " + lat + lng);
                    updateLocation(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }

            @Override
            public void presence(PubNub pubnub, PNPresenceEventResult presence) {

            }
        };
        this.mPubNub.addListener(subscribeCallback);
       // startTimer();
    }


    private void updateLocation(final LatLng location) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPoints.add(location);
                if (MainActivity.this.mMarker != null) {
                    MainActivity.this.mMarker.setPosition(location);
                } else {
                    MainActivity.this.mMarker = mMap.addMarker(new MarkerOptions().position(location));
                }

                if (MainActivity.this.mPolyline != null) {
                    MainActivity.this.mPolyline.setPoints(mPoints);
                } else {
                    MainActivity.this.mPolyline = mMap.addPolyline(new PolylineOptions().color(Color.BLUE).addAll(mPoints));
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
               // startTimer();
            }
        });
    }

    @Override
    public void onLocationReceived(Location location) {
        Log.e("Updated", "Updatesffhjjjjhjhghg");
        lat = location.getLatitude();
        longs = location.getLongitude();
        Toast.makeText(this,"Latitude >>>> "+lat+" , "+"Longitude>>>> "+longs,Toast.LENGTH_SHORT).show();
        Map<String, String> map = new HashMap<String, String>();
        map.put("lat", lat + "");
        map.put("lng", longs + "");
        MainActivity.this.mPubNub.publish().message(map).channel(CHANNEL_NAME).shouldStore(true)
                .usePOST(true)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (status.isError()) {
                            // something bad happened.
                            System.out.println("error happened while publishing: " + status.toString());
                        } else {
                            System.out.println("publish worked! timetoken: " + result.getTimetoken());

                        }
                    }
                });
    }

    @Override
    public void onConntected(Bundle bundle) {

    }

 /*   public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(task);
            }
        };
        timer.schedule(timerTask, 1000, TIMER_INTERVAL);
    }

    Runnable task = new Runnable() {
        @Override
        public void run() {
            Log.e("Updated", "Updatesffhjjjjhjhghg");
            lat = lat + 1;
            longs = longs + 2;
            Log.e("Updated", "Lat "+lat+" LOng "+longs);
            Map<String, String> map = new HashMap<String, String>();
            map.put("lat", lat + "");
            map.put("lng", longs + "");
            MainActivity.this.mPubNub.publish().message(map).channel(CHANNEL_NAME).shouldStore(true)
                    .usePOST(true)
                    .async(new PNCallback<PNPublishResult>() {
                        @Override
                        public void onResponse(PNPublishResult result, PNStatus status) {
                            if (status.isError()) {
                                // something bad happened.
                                System.out.println("error happened while publishing: " + status.toString());
                            } else {
                                System.out.println("publish worked! timetoken: " + result.getTimetoken());

                            }
                        }
                    });
        }
    };*/

}
