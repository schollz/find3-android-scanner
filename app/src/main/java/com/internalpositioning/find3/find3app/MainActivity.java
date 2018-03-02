package com.internalpositioning.find3.find3app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // wifi scanning
    private WifiManager wifi;
    private WifiScanReceiver wifiReceiver;

    // bluetooth scanning
    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();

    // logging
    private final String TAG = "MainActivity";

    // post data request queue
    RequestQueue queue;
    private JSONObject jsonBody = new JSONObject();
    private JSONObject bluetoothResults = new JSONObject();
    private JSONObject wifiResults = new JSONObject();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE}, 1);
        } else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        queue = Volley.newRequestQueue(this);
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    bluetoothResults = new JSONObject();
                    wifiResults = new JSONObject();
                    BTAdapter.startDiscovery();
                    if (wifi.startScan()) {
                        Log.d(TAG, "started wifi scan");
                    } else {
                        Log.w(TAG, "started wifi scan false?");
                    }
                    Log.d(TAG, "started discovery");
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    // your code here
                                    Log.d(TAG,"timer off, trying to send data");
                                    sendData();
                                }
                            },
                            10000
                    );
                } else {
                    Log.d("alarmCheck", "ALARM SET TO FALSE");
                }
            }
        });


        // setup wifi
        wifi = wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false) {
            wifi.setWifiEnabled(true);
        }
        wifiReceiver = new WifiScanReceiver();

    }

    protected void onPause() {
        unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(
                wifiReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        );
        super.onResume();
    }

    // bluetooth reciever
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getAddress();
                TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                rssi_msg.setText(rssi_msg.getText() + "bluetooth: " + name + " => " + rssi + "dBm\n");
                try {
                    bluetoothResults.put(name,rssi);
                } catch (Exception e) {
                    Log.e(TAG,e.toString());
                }
            }
        }
    };

    // wifi reciever
    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> wifiScanList = wifi.getScanResults();
            for (int i = 0; i < wifiScanList.size(); i++) {
                String name = wifiScanList.get(i).BSSID;
                int rssi = wifiScanList.get(i).level;
                TextView rssi_msg = (TextView) findViewById(R.id.textOutput);
                rssi_msg.setText(rssi_msg.getText() + "wifi: " + name + " => " + rssi + "dBm\n");
                Log.d(TAG,"wifi: " + name + " => " + rssi + "dBm");
                try {
                    wifiResults.put(name,rssi);
                } catch (Exception e) {
                    Log.e(TAG,e.toString());
                }
            }
        }
    }


    public void sendData() {
        try {
            String URL = "http://192.168.0.23:8003/data";
            jsonBody.put("f", "testfamily");
            jsonBody.put("d", "testdevice");
            jsonBody.put("l", "");
            jsonBody.put("t", System.currentTimeMillis());
            JSONObject sensors = new JSONObject();
            sensors.put("wifi",wifiResults);
            sensors.put("bluetooth",bluetoothResults);
            jsonBody.put("s",sensors);
            final String mRequestBody = jsonBody.toString();
            Log.d(TAG,mRequestBody);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("LOG_VOLLEY", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("LOG_VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {

                        responseString = String.valueOf(response.statusCode);

                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
