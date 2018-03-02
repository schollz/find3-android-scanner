package com.internalpositioning.find3.find3app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by zacks on 3/2/2018.
 */

public class AlarmReceiverLife extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiverLife";
    static Context context;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v(TAG, "Alarm for life...");

//        Intent ll24Service = new Intent(context, LifeLogService.class);
//        context.startService(ll24Service);
    }
}