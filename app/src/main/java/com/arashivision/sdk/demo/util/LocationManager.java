package com.arashivision.sdk.demo.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import java.util.List;


/**
 * Created by dingming on 2017/4/1.
 */

public class LocationManager {

    private static final String TAG = LocationManager.class.getSimpleName();
    private static final String[] GPS_PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private static LocationManager instance;

    private android.location.LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Location mLastGpslocation;
    private Location mLastNetworklocation;

    private LocationManager() {
    }

    public static LocationManager getInstance() {
        if (instance == null) {
            instance = new LocationManager();
        }
        return instance;
    }


    public boolean isRegistered() {
        synchronized (this) {
            return mLocationManager != null;
        }
    }

    public void registerLocation(Context context) {

        synchronized (this) {
            if (isRegistered()) {
                return;
            }

            mLocationListener = new LocationListener() {
                /**
                 * 位置信息变化时触发
                 */
                public void onLocationChanged(Location location) {
                    if (location.getProvider().equals(android.location.LocationManager.GPS_PROVIDER)) {
                        Log.v(TAG, "update gps location " + mLastGpslocation);
                        mLastGpslocation = location;
                    }
                    if (location.getProvider().equals(android.location.LocationManager.NETWORK_PROVIDER)) {
                        Log.v(TAG, "update network location " + mLastNetworklocation);
                        mLastNetworklocation = location;
                    }
                }

                /**
                 * GPS状态变化时触发
                 */
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    switch (status) {
                        // GPS状态为可见时
                        case LocationProvider.AVAILABLE:
                            Log.d(TAG, "当前GPS状态为可见状态");
                            break;
                        // GPS状态为服务区外时
                        case LocationProvider.OUT_OF_SERVICE:
                            Log.d(TAG, "当前GPS状态为服务区外状态");
                            break;
                        // GPS状态为暂停服务时
                        case LocationProvider.TEMPORARILY_UNAVAILABLE:
                            Log.d(TAG, "当前GPS状态为暂停服务状态");
                            break;
                    }
                }

                /**
                 * GPS开启时触发
                 */
                public void onProviderEnabled(String provider) {
                    if (provider.equals(android.location.LocationManager.GPS_PROVIDER)) {
                        Log.d(TAG, "gps provider enabled");
                        mLastGpslocation = getLastLocation(provider);
                        requestLocationUpdates(provider);
                    }
                    if (provider.equals(android.location.LocationManager.NETWORK_PROVIDER)) {
                        Log.d(TAG, "network provider enabled");
                        mLastNetworklocation = getLastLocation(provider);
                        requestLocationUpdates(provider);
                    }
                }

                /**
                 * GPS禁用时触发
                 */
                public void onProviderDisabled(String provider) {
                    if (provider.equals(android.location.LocationManager.GPS_PROVIDER)) {
                        Log.d(TAG, "gps provider disabled");
                        mLastGpslocation = null;
                    }
                    if (provider.equals(android.location.LocationManager.NETWORK_PROVIDER)) {
                        Log.d(TAG, "network provider disabled");
                        mLastNetworklocation = null;
                    }
                }
            };

            mLocationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager != null) {
                if (mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    Log.d(TAG, "get last gps location " + mLastGpslocation);
                    mLastGpslocation = getLastLocation(android.location.LocationManager.GPS_PROVIDER);
                    requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER);
                }
                if (mLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    Log.d(TAG, "get last network location " + mLastNetworklocation);
                    mLastNetworklocation = getLastLocation(android.location.LocationManager.NETWORK_PROVIDER);
                    requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER);
                }
            }
        }
    }

    public void unregisterLocation() {

        synchronized (this) {
            if (!isRegistered()) {
                return;
            }

            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception e) {
                // Fatal Exception: java.lang.NullPointerException: Attempt to read from field 'java.lang.String com.android.server.location.gnss.datacollect.UseGnssAppBean.packageName' on a null object reference
                e.printStackTrace();
            }
            mLocationManager = null;
            mLocationListener = null;
        }
    }

    public Location getCurrentLocation() {
        if (mLastGpslocation != null) {
            Log.d(TAG, "return gps location " + mLastGpslocation);
            return mLastGpslocation;
        }
        if (mLastNetworklocation != null) {
            Log.d(TAG, "return network location " + mLastNetworklocation);
            return mLastNetworklocation;
        }
        Log.d(TAG, "return no location");
        return null;
    }

    @SuppressLint("MissingPermission")
    private synchronized Location getLastLocation(String provider) {
        try {
            if (mLocationManager != null) {
                return mLocationManager.getLastKnownLocation(provider);
            }
        } catch (Exception e) {
            // Fatal Exception: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.location.Location.setLatitude(double)' on a null object reference
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    private synchronized void requestLocationUpdates(String provider) {
        if (mLocationManager != null && mLocationListener != null) {
            try {
                mLocationManager.requestLocationUpdates(provider, 10, 0, mLocationListener);
            } catch (Exception e) {
                //Fatal Exception: java.lang.SecurityException
                //uid 10247 does not have android.permission.ACCESS_COARSE_LOCATION or android.permission.ACCESS_FINE_LOCATION.
                e.printStackTrace();
            }
        }
    }

    public enum LocationType {
        ANY, GPS, NETWORK, PASSIVE
    }

    /**
     * 手机是否支持定位服务功能
     */
    public static boolean isSystemSupportLocationService(Context context, LocationType locationType) {
        try {
            android.location.LocationManager locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                switch (locationType) {
                    case ANY:
                        return !locationManager.getAllProviders().isEmpty();
                    case GPS:
                        return locationManager.getProvider(android.location.LocationManager.GPS_PROVIDER) != null;
                    case NETWORK:
                        return locationManager.getProvider(android.location.LocationManager.NETWORK_PROVIDER) != null;
                    case PASSIVE:
                        return locationManager.getProvider(android.location.LocationManager.PASSIVE_PROVIDER) != null;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 手机是否开启定位服务
     */
    public static boolean isLocationServiceEnable(Context context, LocationType locationType) {
        try {
            android.location.LocationManager locationManager = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                switch (locationType) {
                    case ANY:
                        List<String> allProviders = locationManager.getAllProviders();
                        if (!allProviders.isEmpty()) {
                            for (String provider : allProviders) {
                                if (locationManager.isProviderEnabled(provider)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    case GPS:
                        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
                    case NETWORK:
                        return locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
                    case PASSIVE:
                        return locationManager.isProviderEnabled(android.location.LocationManager.PASSIVE_PROVIDER);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 跳转到系统定位服务设置页
     */
    public static void gotoSystemLocationSetting(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // The Android SDK doc says that the location settings activity
            // may not be found. In that case show the general settings.
            // General settings activity
            intent.setAction(Settings.ACTION_SETTINGS);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
