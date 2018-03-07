package com.internalpositioning.find3.find3app;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by zacks on 3/2/2018.
 */

public class AlarmReceiverLife extends BroadcastReceiver {
    private static PowerManager.WakeLock wakeLock;

    private static final String TAG = "AlarmReceiverLife";
    static Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "Recurring alarm");

        // get data
        String familyName = intent.getStringExtra("familyName");
        String deviceName = intent.getStringExtra("deviceName");
        String locationName = intent.getStringExtra("locationName");
        String serverAddress = intent.getStringExtra("serverAddress");
        boolean allowGPS = intent.getBooleanExtra("allowGPS",false);
        Log.d(TAG,"familyName: "+ familyName);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "WakeLock");
        wakeLock.acquire();
        Intent scanService = new Intent(context, ScanService.class);
        scanService.putExtra("familyName",familyName);
        scanService.putExtra("deviceName",deviceName);
        scanService.putExtra("locationName",locationName);
        scanService.putExtra("serverAddress",serverAddress);
        scanService.putExtra("allowGPS",allowGPS);
        try {
            context.startService(scanService);
        } catch (Exception e) {
            Log.w(TAG,e.toString());
        }
        Log.d(TAG,"Releasing wakelock");
        if (wakeLock != null) wakeLock.release();
        wakeLock = null;
    }


}
