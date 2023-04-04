package com.example.demo.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

@SuppressLint("MissingPermission")
public class LocationUtils {

    public static final String TAG = "LocationUtils";

    private static volatile LocationUtils instance;
    private final LocationManager locationManager;

    private double longitude;
    private double latitude;
    private float speed;

    private int gpsCount = 0;
    private int direction = 0;

    private SensorManager mSensorManager;
    private Sensor aSensor;
    private Sensor mSensor;

    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];

    public double getLongitude() {
        BigDecimal longitude100 = new BigDecimal(longitude).movePointRight(2);
        return longitude100.doubleValue();
    }

    public double getLatitude() {
        //跟后台定义的规则，小数点右移两位再传过去，不能直接乘以100会丢失精度
        BigDecimal latitude100 = new BigDecimal(latitude).movePointRight(2);
        return latitude100.doubleValue();
    }

    public float getSpeed() {
        return speed;
    }

    public int getDirection() {
        Log.i(TAG, "方位角--direction->" + direction);
        return direction;
    }

    private Context mContext;

    public void getLocation() {
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
            Log.i(TAG, "location--->" + location);
            //            Location location = getLastKnownLocation(locationManager);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                speed = location.getSpeed();
            }

            Log.i(TAG, "location.latitude: " + latitude);
            Log.i(TAG, "location.longitude:  " + longitude);
            Log.i(TAG, "location.speed:  " + speed);
            //经纬度监听变化
            locationManager.requestLocationUpdates(locationProvider, 10000, 1,
                    (LocationListener) (new LocationListener() {
                        public void onStatusChanged(String provider, int status, Bundle arg2) {
                            switch (status) {
                                // GPS状态为可见时
                                case LocationProvider.AVAILABLE:
                                    Log.i(TAG, "当前GPS状态为可见状态");
                                    break;
                                // GPS状态为服务区外时
                                case LocationProvider.OUT_OF_SERVICE:
                                    Log.i(TAG, "当前GPS状态为服务区外状态");
                                    break;
                                // GPS状态为暂停服务时
                                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                                    break;
                            }
                        }

                        public void onProviderEnabled(String provider) {
                            Location location = locationManager.getLastKnownLocation(provider);
                            if (null != location) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                            Log.d(TAG, "onProviderEnabled----location.latitude: " + latitude);
                            Log.d(TAG, "onProviderEnabled----location.longitude:  " + longitude);
                        }

                        public void onProviderDisabled(String provider) {
                        }

                        public void onLocationChanged(Location loc) {
                            Log.i(TAG, "onLocationChanged");
                            latitude = loc.getLatitude();
                            longitude = loc.getLongitude();
                            speed = loc.getSpeed();
                            Log.d(TAG, "onLocationChanged---location.latitude: " + latitude);
                            Log.d(TAG, "onLocationChanged---location.speed: " + speed);
                            Log.d(TAG, "onLocationChanged---location.longitude:  " + longitude);
                        }
                    }));

            //添加卫星状态改变监听
            locationManager.addGpsStatusListener(gpsStatusListener);
            //传感器，获取方位角
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            aSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            mSensorManager.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(myListener, mSensor, SensorManager.SENSOR_DELAY_GAME);

            calculateOrientation();
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

    /**
     * 获取gps卫星个数
     *
     * @return
     */
    public int getCurrentGpsCount() {
        return gpsCount;
    }

    private GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                //卫星状态改变
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    //获取当前状态
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    //获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    //获取所有的卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    //卫星颗数统计
                    StringBuilder sb = new StringBuilder();
                    while (iters.hasNext() && gpsCount <= maxSatellites) {
                        gpsCount++;
                        GpsSatellite s = iters.next();
                        //卫星的信噪比
                        float snr = s.getSnr();
                        sb.append("第").append(gpsCount).append("颗").append("：").append(snr).append("\n");
                    }
                    break;
                default:
                    break;
            }
        }
    };

    final SensorEventListener myListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            calculateOrientation();
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /**
     * 计算详细方位角，以正北为0度
     */
    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, values);

        // 要经过一次数据格式的转换，转换为度
        values[0] = (float) Math.toDegrees(values[0]);
        Log.i(TAG, "方位角--->" + values[0]);
        //values[1] = (float) Math.toDegrees(values[1]);
        //values[2] = (float) Math.toDegrees(values[2]);

        direction = (int) values[0];
    }


    private LocationUtils(Context context) {
        mContext = context.getApplicationContext();
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
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
