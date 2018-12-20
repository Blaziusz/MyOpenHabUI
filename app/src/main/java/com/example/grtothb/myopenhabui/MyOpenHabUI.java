package com.example.grtothb.myopenhabui;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class MyOpenHabUI extends Application {
    private static MyOpenHabUI sInstance;
    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();

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
        }
        return mRequestQueue;
    }
}

