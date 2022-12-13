package com.example.demo.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

@SuppressLint("MissingPermission")
public class LocationUtils {

    public static final String TAG = "LocationUtils";

    private static volatile LocationUtils instance;

    private double longitude;
    private double latitude;

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    private Context mContext;

    public final void getLocation() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new NullPointerException("null cannot be cast to non-null type android.location.LocationManager");
        } else {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            String locationProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(locationProvider);
            //            Location location = getLastKnownLocation(locationManager);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            Log.i(TAG, "location.latitude: " + latitude);
            Log.i(TAG, "location.longitude:  " + longitude);
            locationManager.requestLocationUpdates(locationProvider, 10000, 1,
                    (LocationListener) (new LocationListener() {
                public void onStatusChanged(String provider, int status, Bundle arg2) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }

                public void onLocationChanged(Location loc) {
                    Log.i(TAG, "onLocationChanged");
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                    Log.d(TAG, "location.latitude: " + latitude);
                    Log.d(TAG, "location.longitude:  " + longitude);
                }
            }));
        }
    }

    private Location getLastKnownLocation(LocationManager locationManager) {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
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
