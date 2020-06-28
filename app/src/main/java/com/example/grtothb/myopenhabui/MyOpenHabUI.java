package com.example.grtothb.myopenhabui;

import android.app.Application;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.grtothb.myopenhabui.AlarmAction.MyBroadcastReceiver;

public class MyOpenHabUI extends Application {
    private static MyOpenHabUI sInstance;
    private RequestQueue mRequestQueue;
    private MainActivity mMainActivityInstance = null;
    private boolean mTriggerCacheDel = false;

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

    public synchronized void setMainActivityInstance(MainActivity mainActivityInst) {
        mMainActivityInstance = mainActivityInst;
    }

    public synchronized MainActivity getMainActivityInstance() {
        return mMainActivityInstance;
    }

    // Added 2020-06-28
    // setTriggerCacheDel triggers the deletion of the cache
    // getTriggerCacheDel automatically resets mTriggerCacheDel if it is true
    public synchronized boolean getTriggerCacheDel() {
        if (mTriggerCacheDel) {
            mTriggerCacheDel = false;
            return true;
        }
        return false;
    }

    public synchronized void setTriggerCacheDel() {
        mTriggerCacheDel = true;
    }
}

