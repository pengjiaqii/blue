package com.example.demo.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;


public class LocationUtils {

    private static volatile LocationUtils instance;

    private double longitude;
    private double latitude;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    private Context mContext;

    @SuppressLint("MissingPermission")
    public final void getLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new NullPointerException("null cannot be cast to non-null type android.location.LocationManager");
        } else {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(1);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(1);
            String locationProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(locationProvider);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            Log.i("TcpService", "location.latitude: " + latitude);
            Log.i("TcpService", "location.longitude:  " + longitude);
            locationManager.requestLocationUpdates(locationProvider, 2000L, 8.0F, (LocationListener) (new LocationListener() {
                public void onStatusChanged( String provider, int status,  Bundle arg2) {
                }

                public void onProviderEnabled( String provider) {
                }

                public void onProviderDisabled( String provider) {
                }

                public void onLocationChanged( Location loc) {
                    Log.i("TcpService", "onLocationChanged");
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
            }));
        }
    }


    private LocationUtils(Context context) {
        mContext = context.getApplicationContext();

    }


    public static LocationUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (LocationUtils.class) {
                if (instance == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    instance = new LocationUtils(context);
                }
            }

        }
        return instance;
    }

}
