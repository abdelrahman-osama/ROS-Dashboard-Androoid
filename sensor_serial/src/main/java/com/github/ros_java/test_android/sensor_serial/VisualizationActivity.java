package com.github.ros_java.test_android.sensor_serial;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import com.google.common.base.Preconditions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ros.android.AppCompatRosActivity;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.view.RosImageView;
import org.ros.android.view.visualization.layer.Layer;
import org.ros.android.view.visualization.VisualizationView;
import org.ros.android.view.visualization.layer.CameraControlLayer;
import org.ros.android.view.visualization.layer.CameraControlListener;
import org.ros.android.view.visualization.layer.LaserScanLayer;
import org.ros.android.view.visualization.layer.OccupancyGridLayer;
import org.ros.android.view.visualization.layer.PointCloud2DLayer;
import org.ros.android.view.visualization.layer.RobotLayer;
import org.ros.internal.message.RawMessage;
import org.ros.message.MessageListener;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.ArrayList;
import java.util.List;

import in.unicodelabs.kdgaugeview.KdGaugeView;
import sensor_msgs.CompressedImage;
import std_msgs.Int32;
import std_msgs.String;

import static com.github.ros_java.test_android.sensor_serial.Listener.errorSub;
import static com.github.ros_java.test_android.sensor_serial.Listener.subscriber;

public class VisualizationActivity extends AppCompatRosActivity implements OnMapReadyCallback ,DirectionCallback {
    private static final LatLngBounds GUC_BOUNDS = new LatLngBounds(new LatLng(29.9842014, 31.4387794),
            new LatLng(29.9899635, 31.4445531));
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap mMap;
    private LocationCallback mLocationCallback;
    boolean mRequestingLocationUpdates;
    LocationRequest mLocationRequest;
    LatLng mLastLocation;
    private ArrayList<Polyline> polylines;
    private LatLng loc;
    KdGaugeView speedoMeterView;
    private LocalBroadcastManager broadcaster;
    Runnable updater;
    static RosImageView<CompressedImage> videoStreamView;
    static RosImageView<CompressedImage> videoStreamView2;
    private Gson gson;
    boolean drawRoute = false;
    private VisualizationView visualizationView;
    LaserScanLayer x;
    private java.lang.String speedValue ="7";
    boolean viewMode;
    int errorCode;
//    Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);
        getSupportActionBar().hide();
        broadcaster = LocalBroadcastManager.getInstance(this);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();
//        visualizationView = (VisualizationView) findViewById(R.id.lidarView);
         //x = new LaserScanLayer("scan");

//        visualizationView.onCreate(Lists.<Layer>newArrayList(new PointCloud2DLayer("velodyne_points")));

        drawRoute = false;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.v("osama", "I reached callback");
                for (Location location : locationResult.getLocations()) {
                    mLastLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(mLastLocation));
                    if(!drawRoute) {
                       // Log.d("CurrentDestinationLocCB", String.valueOf(MapsFragment.tripToBe.getDestinations().size()));
                        //TODO #3
                        if(MapsFragment.tripToBe.getDestinations()!=null) {
                            GetRoutToMarker(MapsFragment.tripToBe.getDestinations().get(currentDestination()).getLocation());
                            drawRoute = true;
                        }
                    }
                    Log.v("osama", mLastLocation.toString());
                }
            }

        };
        MapFragment mGoogleMap = ((MapFragment) this.getFragmentManager().findFragmentById(R.id.map1));

        mGoogleMap.getMapAsync(this);

        ImageButton endButton = findViewById(R.id.cancelButton);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(VisualizationActivity.this);
                builder1.setMessage("Are you sure you want to cancel your trip?");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                onEndButton();
                            }
                        });

                builder1.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();

            }
        });

        ImageButton editButton = findViewById(R.id.modifyButton);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onModify();
            }
        });

        ImageButton arriveDestinationButton = findViewById(R.id.arriveDestination);
        arriveDestinationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapsFragment.tripToBe.getDestinations().get(currentDestination()).setArrived(true);
                onArrive();
            }
        });

        ImageButton swView = findViewById(R.id.switchView);
        swView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView();
            }
        });

        ImageButton emergencyButton = findViewById(R.id.emergencyButton);
        emergencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emergencyButton();
                AlertDialog.Builder builder1 = new AlertDialog.Builder(VisualizationActivity.this);
                builder1.setMessage("Emergency Button Pressed!");
                builder1.setCancelable(true);

                builder1.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });


                AlertDialog alert11 = builder1.create();
                alert11.show();


            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("FcmData")
        );
        videoStreamView =findViewById(R.id.visualizationImage);
        videoStreamView.setTopicName("axis_camera/image_raw/compressed");
        videoStreamView.setMessageType(CompressedImage._TYPE);
        videoStreamView.setMessageToBitmapCallable(new BitmapFromCompressedImage());


//        videoStreamView2 =findViewById(R.id.visualizationImage2);
//        videoStreamView2.setTopicName("axis_camera/image_raw/compressed");
//        videoStreamView2.setMessageType(CompressedImage._TYPE);
//        videoStreamView2.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        errorSub.addMessageListener(new MessageListener<Int32>() {
            @Override
            public void onNewMessage(Int32 int32) {
                errorCode = int32.getData();
                switch(errorCode){
                    case 501 :
                        Log.d("errorCode", java.lang.String.valueOf(501));
                        VisualizationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(VisualizationActivity.this);
                                builder1.setMessage("System Error 501, please check the steering system!");
                                builder1.setCancelable(true);

                                builder1.setPositiveButton(
                                        "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert11 = builder1.create();
                                alert11.show();
                            }
                        });
                        errorCode = 0;
                        break;
                    case 502 :

                        VisualizationActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder2 = new AlertDialog.Builder(VisualizationActivity.this);
                                builder2.setMessage("System Error 502, please check the throttle system!");
                                builder2.setCancelable(true);
                                builder2.setPositiveButton(
                                        "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert12 = builder2.create();
                                alert12.show();
                            }
                        });
                        errorCode = 0;
                        break;
                }
            }
        });

        subscriber.addMessageListener(new MessageListener<std_msgs.Float32>() {
            @Override
            public void onNewMessage(std_msgs.Float32 s) {
                speedValue = java.lang.String.valueOf((int)s.getData());
                // Log.d("listener-log","I heard: \"" + s.getData() + "\"");
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap = googleMap;
        mMap.setMinZoomPreference(16);
        mMap.setLatLngBoundsForCameraTarget(GUC_BOUNDS);
        LatLng latLng = new LatLng(29.9867788, 31.441697);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        createLocationRequest();
        startLocationUpdates();
        final TextView speedMeter= findViewById(R.id.speed);
        final Handler timerHandler = new Handler();
        updater = new Runnable() {
            @Override
            public void run() {
                speedMeter.setText(speedValue);
                timerHandler.postDelayed(updater,1000);
            }
        };
        timerHandler.post(updater);
    }
    @Override
    public void onResume() {
        super.onResume();

//        final Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if(mLastLocation!=null && MapsFragment.tripToBe.getDestinations().get(0).getLocation()!=null ) {
//                    GetRoutToMarker(MapsFragment.tripToBe.getDestinations().get(currentDestination()).getLocation());
//                }
//            }
//        }, 2000);
    }

    private void GetRoutToMarker(LatLng clickedMarker) {
        GoogleDirection.withServerKey("AIzaSyCZ5TH2mfl26LqDq6kkVbo85gLPZ9fmaik")
                .from(mLastLocation)
                .to(clickedMarker)
                .transportMode(TransportMode.DRIVING)
                .execute(this);

    }

    @Override
    public void onDirectionSuccess(Direction direction, java.lang.String rawBody) {
        if (direction.isOK()) {
            Route route = direction.getRouteList().get(0);
            ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
            polylines = new ArrayList<>();
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(Color.BLACK);
            polyOptions.width(6);
            polyOptions.addAll(directionPositionList);
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {


    }


    private void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //checkLocationPermission();
            } else {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    @SuppressLint("RestrictedApi")
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            java.lang.String status = bundle.getString("EVENT");
            java.lang.String carID = bundle.getString("CAR_ID");
            java.lang.String tripID=bundle.getString("TRIP_ID");
            switch (status) {

                case "START":
                   // getTripFromServer();
                    break;
                case "CANCEL":
                    cancelFromServer();
                    break;
                case "END":
                    cancelFromServer();
                    break;
                case "CHANGE_DESTINATION":
                    modifyFromServer();
                    break;

            }
        }

    };


    public  void removePolylines() {
        if(polylines!=null) {
            if (polylines.size() > 0) {
                for (Polyline poly : polylines) {
                    poly.remove();
                }
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mLastLocation));
        }
    }

    public void modifyFromServer(){
        java.lang.String tripUrl = "https://sdc-api-gateway.herokuapp.com/car/find/hadwa";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, tripUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONObject obj = response;
                Car car = gson.fromJson(obj.toString(), Car.class);
                if(car.getCurrentTrip()!=null){
                    MapsFragment.tripToBe = car.getCurrentTrip();
                    removePolylines();
                    //TODO #1
                    GetRoutToMarker(MapsFragment.tripToBe.getDestinations().get(currentDestination()).getLocation());

                    Toast.makeText(getApplicationContext(), "Trip Updated", Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error

            }
        });
        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);


    }

    public void cancelFromServer(){
        MapsFragment.confirmRide.setClickable(true);
        Intent intent = new Intent("onEnd");
        intent.putExtra("EVENT", "ResetAll");
        broadcaster.sendBroadcast(intent);
        this.finish();
    }




    public void onEndButton(){
        MapsFragment.confirmRide.setClickable(true);
        Intent intent = new Intent("onEnd");
        intent.putExtra("EVENT", "ResetAll");
        broadcaster.sendBroadcast(intent);

        this.finish();
        removePolylines();
        java.lang.String cancelUrl = "https://sdc-api-gateway.herokuapp.com/car/trip/end/tablet/hadwa";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, cancelUrl, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {

                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject responseObject = response.getJSONObject(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

         //Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsonArrayRequest);

    }


    public ArrayList<LatLng> tripToLatlng(Trip trip){
        List<TripDestination> tripDestination =trip.getDestinations();
        ArrayList<LatLng> latLng = new ArrayList<>();
        for(int i=0 ; i<tripDestination.size();i++) {
        latLng.add(tripDestination.get(i).getLocation());
        }

        return latLng;
    }


    public static int currentDestination(){
//        Log.d("CurrentDestination", String.valueOf(MapsFragment.tripToBe.getDestinations().size()));
        for (int i=0 ; i<MapsFragment.tripToBe.getDestinations().size();i++){
            if(!MapsFragment.tripToBe.getDestinations().get(i).isArrived())
                return i;
        }

        return MapsFragment.tripToBe.getDestinations().size();
    }


    public void onArrive(){
        java.lang.String tripUrl;

        if(currentDestination()<MapsFragment.tripToBe.getDestinations().size()){
            Log.d("testing","destination arrive");
            Log.d("testing", currentDestination() + "t" + MapsFragment.tripToBe.getDestinations().size());
            tripUrl = "https://sdc-api-gateway.herokuapp.com/car/trip/arrive/destination/hadwa";
            MapsFragment.confirmRide.setClickable(true);
            Intent intent = new Intent("onEnd");
            intent.putExtra("EVENT", "DestinationArrived");
            broadcaster.sendBroadcast(intent);
            this.finish();

        }
        else {
            Log.d("testing","final destination arrive");

            tripUrl = "https://sdc-api-gateway.herokuapp.com/car/trip/arrive/final/hadwa";
            MapsFragment.confirmRide.setClickable(true);
            Intent intent = new Intent("onEnd");
            intent.putExtra("EVENT", "FinalDestinationArrived");
            broadcaster.sendBroadcast(intent);
            this.finish();


        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, tripUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {


            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle error

            }
        });
        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }


    public void onModify(){
        if(MapsFragment.tripToBe.getDestinations().size()-1 == currentDestination()){
            AlertDialog.Builder builder1 = new AlertDialog.Builder(VisualizationActivity.this);
            builder1.setMessage("You can't modify your last destination, you can only cancel.");
            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            // onEndButton();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
        }else{
            MapsFragment.confirmRide.setClickable(true);
            Intent intent = new Intent("onEnd");
            intent.putExtra("EVENT", "Modify");
            broadcaster.sendBroadcast(intent);
            this.finish();
        }


    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

           }

     public void switchView(){
//         videoStreamView =findViewById(R.id.visualizationImage);
//         videoStreamView.setTopicName("camleft/raw/compressed");
//         videoStreamView.setMessageType(CompressedImage._TYPE);
//         videoStreamView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

//        videoStreamView2 =findViewById(R.id.visualizationImage2);
//        videoStreamView2.setTopicName("axis_camera/image_raw/compressed");
//        videoStreamView2.setMessageType(CompressedImage._TYPE);
//        videoStreamView2.setMessageToBitmapCallable(new BitmapFromCompressedImage());


         if(viewMode){
            viewMode = false;
            videoStreamView.setTopicName("axis_camera/image_raw/compressed");
            init(nodeMainExecutorService);
        }else{
            viewMode = true;
             videoStreamView.setTopicName("pg_15405694/image_raw/processed");
            init(nodeMainExecutorService);
        }
     }

     public void emergencyButton(){
        std_msgs.String x = Talker.publisher.newMessage();
        x.setData("emergency");
        Talker.publisher.publish(x);
     }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeConfiguration.setNodeName("node1");
        nodeMainExecutor.shutdownNodeMain(videoStreamView);
        nodeMainExecutor.execute(videoStreamView, nodeConfiguration);




//        nodeConfiguration.setNodeName("node2");
//        nodeMainExecutor.execute(videoStreamView, nodeConfiguration);
//




    }

}
