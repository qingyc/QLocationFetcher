package com.qingyc.qlocationlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.*;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.qingyc.qlogger.QLogger;

import java.util.ArrayList;
import java.util.List;


/**
 * 类说明: 定位获取
 *
 * @author qing
 * @time 2018/9/19 15:22
 */
public class QLocationFetcher {

    /**
     * android api
     */
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    /**
     * google service location api  implementation 'com.google.android.gms:play-services-location:16.0.0'
     */
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mFusedLocationCallback;
    private Boolean mAndroidLocateError = false;
    private Boolean mGoogleLocateError = false;

    private static final String TAG = "QLocationFetcher";
    //自动停止定位时间
    private static final int AUTO_STOP_LOCATION_TIME = 20000;

    public Location mLocation = null;
    private Handler mHandler = new Handler();

    public boolean hasStopLocate = false;
    private boolean mToastError = false;
    private ArrayList<LocationCallBack> currentCallBacks = new ArrayList<>();

    public interface LocationCallBack {
        /**
         * 获取定位
         *
         * @param location location
         */
        void onLocationCallBack(Location location);

    }

    private QLocationFetcher() {
    }

    public static QLocationFetcher getInstance() {
        return Singleton.instance;
    }

    public static class Singleton {
        static QLocationFetcher instance = new QLocationFetcher();
    }


    /**
     * @param context
     * @param callBack
     * @param toastError 是否显示提示
     */
    @SuppressLint("CheckResult")
    public void getLocation(final FragmentActivity context,
                            final Boolean toastError,
                            final LocationCallBack callBack) {
        if (currentCallBacks.contains(callBack)) {
            return;
        }
        if (callBack != null) {
            currentCallBacks.add(callBack);
        }
        this.mToastError = toastError;
        requestLocation(context, callBack);
    }

    private void requestLocation(final FragmentActivity context,
                                 final LocationCallBack callBack) {
        hasStopLocate = false;
        mHandler.removeCallbacksAndMessages(null);
        Runnable mStopLocateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mLocation == null && !hasStopLocate) {
                    if (mToastError) {
                        Toast.makeText(context, R.string.toast_locate_fail, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                stopLocate();
            }
        };
        //默认10秒钟定位无反应 取消定位
        mHandler.postDelayed(mStopLocateRunnable, AUTO_STOP_LOCATION_TIME);

        try {
            getAndroidLocation(context);
        } catch (Exception e) {
            if (BuildConfig.DEBUG && e != null) {
                Log.e(TAG, e.toString());
            }
        }
        try {
            ProviderInstaller.installIfNeeded(context);
            //是否开启google定位
            getFusedLocation(context);
        } catch (Exception e) {
            if (BuildConfig.DEBUG && e != null) {
                Log.e(TAG, e.toString());
            }
            mGoogleLocateError = true;
        }

    }

    private void getFusedLocation(FragmentActivity context) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        //获取更新的位置
        // Update UI with location data
        if (mFusedLocationCallback == null) {
            mFusedLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        if (!currentCallBacks.isEmpty()) {
                            if (mAndroidLocateError) {
                                stopLocate();
                            }
                        }
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        mLocation = location;
                        stopLocate();
                        if (location != null) {
                            //   QLogger.e(location.toString());
                        }
                    }
                }
            };
        }

        @SuppressLint("MissingPermission") Task<Void> voidTask = mFusedLocationClient.requestLocationUpdates(new LocationRequest(), mFusedLocationCallback, null);
        voidTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                QLogger.e(" onSuccess");
            }
        });

        voidTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (BuildConfig.DEBUG && e != null) {
                    Log.e(TAG, e.toString());
                }
                if (!currentCallBacks.isEmpty()) {
                    if (mAndroidLocateError) {
                        stopLocate();
                    }
                    mGoogleLocateError = true;
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getAndroidLocation(FragmentActivity context) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            return;
        }


        if (mLocationListener == null) {
            mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    if (location != null) {
                        QLogger.e("onLocationChanged  " + location.toString());
                        mLocation = location;
                        stopLocate();
                    } else {
                        QLogger.e("onLocationChanged  location = null ");
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {
                    QLogger.e("onProviderDisabled == " + provider);
                    if (mGoogleLocateError && !currentCallBacks.isEmpty()) {
                        for (LocationCallBack currentCallBack : currentCallBacks) {
                            currentCallBack.onLocationCallBack(null);
                        }
                    }
                    mAndroidLocateError = true;
                    stopAndroidLocate();

                }
            };
        }
        //默认使用网络定位
        LocationProvider netProvider = mLocationManager.getProvider("network");
        LocationProvider gpsProvider = mLocationManager.getProvider("gps");
        boolean netProviderIsNotAllow = netProvider == null || !mLocationManager.isProviderEnabled(netProvider
                .getName());
        boolean gpsProviderIsNotAllow = gpsProvider == null || !mLocationManager.isProviderEnabled(gpsProvider
                .getName());
        if (gpsProviderIsNotAllow && netProviderIsNotAllow) {
            hasStopLocate = true;
            stopLocate();
            if (mToastError) {
                Toast.makeText(context, context.getString(R.string.toast_check_open_location_service), Toast.LENGTH_SHORT)
                        .show();
            }
            return;
        }
        if (netProviderIsNotAllow) {
            List<String> providers = mLocationManager.getProviders(true);
            //网络定位不可用就使用其他可用的定位
            if (providers != null && providers.size() > 0) {
                for (int i = 0; i < providers.size(); i++) {
                    String provider1 = providers.get(i);
                    boolean providerEnabled = mLocationManager.isProviderEnabled(provider1);
                    if (providerEnabled) {
                        mLocationManager.requestLocationUpdates(provider1, 100, 0, mLocationListener);
                    }
                }
            }
            return;
        }
        mLocationManager.requestLocationUpdates(netProvider.getName(), 100, 0, mLocationListener);
    }


    public void stopLocate() {
        for (LocationCallBack currentCallBack : currentCallBacks) {
            currentCallBack.onLocationCallBack(mLocation);
        }
        currentCallBacks.clear();
        hasStopLocate = true;
        mHandler.removeCallbacksAndMessages(null);
        stopAndroidLocate();
        stopFusedLocate();

    }

    private void stopAndroidLocate() {
        if (mLocationManager != null && mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationManager = null;
            mAndroidLocateError = false;
        }
    }

    private void stopFusedLocate() {
        if (mFusedLocationCallback != null && mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mFusedLocationCallback);
            mFusedLocationCallback = null;
            mAndroidLocateError = false;
        }
    }
}
