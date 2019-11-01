package com.example.grtothb.myopenhabui;

import android.app.Application;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.grtothb.myopenhabui.AlarmAction.MyBroadcastReceiver;

public class MyOpenHabUI extends Application {
    private static MyOpenHabUI sInstance;
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();

        //TODO: Remove this test code line
        Log.d("MyOpenHabUI", "Alarm state: " + MyBroadcastReceiver.isAlarmOn() + " PID: " + android.os.Process.myPid() + " UID:" + android.os.Process.myUid());

        sInstance = this;
    }

    public static synchronized MyOpenHabUI getsInstance() {
        return sInstance;
    }

    public RequestQueue getmRequestQueue() {
        // lazy initialize the request queue, the queue instance will be
        // created when it is accessed for the first time
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
            Log.d("MyOpenHabUI", "New VolleyRequestQ, PID: " + android.os.Process.myPid() + " UID: " + android.os.Process.myUid());
        }
        return mRequestQueue;
    }
}

