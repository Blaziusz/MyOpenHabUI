package com.example.grtothb.myopenhabui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.example.grtothb.myopenhabui.AlarmAction.MyBroadcastReceiver;
import com.example.grtothb.myopenhabui.fgAppChecker.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    private static final String msg = "MyOpenHabUI: ";
    private static final String paper_ui_url = "http://192.168.1.50:8080/paperui/index.html#/inbox/search";
    private static final String basic_ui_url = "http://192.168.1.50:8080/basicui/app?sitemap=alarm";
    private static final String NOTIFICATION_CH_ID = "MyOpenHabUINotification";


    // Power management
    private PowerManager.WakeLock mWakeLock;

    // --------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

/*
        // Debug Infos
        Log.e("MainAct - DBG", "Calling Activity: " + getCallingActivity());
        Log.e("MainAct - DBG", "Calling Package: " + getCallingPackage());
        Log.e("MainAct - DBG", "Component Name: " + getComponentName());
        Log.e("MainAct - DBG", "LifeCycle: " + getLifecycle().getCurrentState().toString());
        Log.e("MainAct - DBG", "PID: " + android.os.Process.myPid());

        Log.e("MainAct - DBG", "Get Intent Package: " + getIntent().getPackage());
        Log.e("MainAct - DBG", "Get Intent ComponentName: " + getIntent().getComponent());
        Log.e("MainAct - DBG", "Get Intent Type: " + getIntent().getType());
        Log.e("MainAct - DBG", "Get Intent Action: " + getIntent().getAction());
        Log.e("MainAct - DBG", "Get Intent TesSTR: " + getIntent().getStringExtra(MyBroadcastReceiver.BCASTRCV_PARAM_PKG_NAME));

*/

        // get permissions for usage stats
        requestUsageStatsPermission();

        // hide action bar
        if (getWindow().requestFeature(Window.FEATURE_ACTION_BAR)) {
            ActionBar myActionBar = getSupportActionBar();
            if (myActionBar != null) {
                myActionBar.hide();
            }
            else {
                android.app.ActionBar myAppActionBar = getActionBar();
                if (myAppActionBar != null) {
                    myAppActionBar.hide();
                }
            }
        }

        // Create Notification Channel for the App's keepAliveService
        // TODO: move to start service? it is called everytime the MainActivity is displayed
        createNotificationChannel();

        // Show Activity
        setContentView(R.layout.activity_main);
        // Set PowerManager settings
        PowerManager pm = (PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
        if (pm != null){
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "my_ohui:pm_tag");
            //mWakeLock.acquire(10*60*1000L /*10 minutes*/);
            // Keep CPU on 6 weeks (42 days) long
            mWakeLock.acquire(42*24*60*60*1000L);

        }
        else {
            Log.e(msg, "Error: PowerManager = null");
        }
        // Load the OpenHab Web pages
        WebView wv_paper_ui = findViewById(R.id.paper_ui_webview);
        loadWebViewDatafinal(wv_paper_ui, paper_ui_url, "paper_ui");
        WebView wv_basic_ui = findViewById(R.id.basic_ui_webview);
        loadWebViewDatafinal(wv_basic_ui, basic_ui_url, "basic_ui");
    }

    // --------------------------------------------------------------------------------------------
    // Create app's notification channel
    // --------------------------------------------------------------------------------------------
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MyOpenHabUI Notification Channel";
            String description = "MyOpenHabUI Notification's Channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CH_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            } else {
                Log.e(msg, "Error: notificationManager = null");
            }
        }
    }


    // --------------------------------------------------------------------------------------------
    // Load Webpages
    //
    // 2019-11-03: Cache still seems to cause performance increase => Webview started with no cache
    //
    // 2019-11-01: Use different cache for the 2 web pages: paper_ui and basic_ui. It seems to be
    //             that the CPU load of ~85% after 1 week of operation is caused by webview cache
    //             after clearing cache of the app in settings the CPU load falls back to 1-2%.
    // --------------------------------------------------------------------------------------------
    private void loadWebViewDatafinal(WebView wv, String url, String FileID) {
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(true);

        try {
            Log.d("WEB_VIEW_JS", "Enabling HTML5-Features");
            Method m1 = WebSettings.class.getMethod("setDomStorageEnabled", Boolean.TYPE);
            m1.invoke(ws, Boolean.TRUE);
            Method m2 = WebSettings.class.getMethod("setDatabaseEnabled", Boolean.TYPE);
            m2.invoke(ws, Boolean.TRUE);
            Method m3 = WebSettings.class.getMethod("setDatabasePath", String.class);
            m3.invoke(ws, "/data/data/" + this.getPackageName() + "/databases/" + FileID + "/");
            Log.d("WEB_VIEW_JS", "Enabled HTML5-Features");
            // start the webview w/o cache
            //Method m4 = WebSettings.class.getMethod("setAppCacheMaxSize", Long.TYPE);
            //m4.invoke(ws, 1024 * 1024 * 8);
            //Method m5 = WebSettings.class.getMethod("setAppCachePath", String.class);
            //m5.invoke(ws, "/data/data/" + this.getPackageName() + "/cache/" + FileID + "/");
            Method m6 = WebSettings.class.getMethod("setAppCacheEnabled", Boolean.TYPE);
            //m6.invoke(ws, Boolean.TRUE);
            // Disable cache
            m6.invoke(ws, Boolean.FALSE);
            wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            Log.d("WEB_VIEW_JS", "Disabled Cache");
        } catch (NoSuchMethodException e) {
            Log.e("WEB_VIEW_JS", "Reflection fail NoSuchMethod", e);
        } catch (InvocationTargetException e) {
            Log.e("WEB_VIEW_JS", "Reflection fail InvocationIssue", e);
        } catch (IllegalAccessException e) {
            Log.e("WEB_VIEW_JS", "Reflection fail", e);
        }
        wv.loadUrl(url);
    }

    // --------------------------------------------------------------------------------------------
    /** Called when the user taps the Menu button */
    // --------------------------------------------------------------------------------------------
    public void switch2SettingsMenu(View view){
        Log.d("MyOpenHabUI:MainAct", "Menu Button pressed");
        Intent intent = new Intent(this, SettingsMenu.class);
        startActivity(intent);
    }

    // --------------------------------------------------------------------------------------------
    @Override
    public void onDestroy(){
        Log.d("MyOpenHabUI:MainAct", "onDestroy");
        // TODO: move this where the app is closed, this is called when getting back from Menu Activity
/*
        // Release wakelock
        if (mWakeLock != null)
            mWakeLock.release();
        // delete Apps's notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.deleteNotificationChannel(NOTIFICATION_CH_ID);
            else
                Log.e(msg,"notificationManager on Destroy = null");
        }
*/
        super.onDestroy();
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void requestUsageStatsPermission() {
        if(Utils.postLollipop() && !Utils.hasUsageStatsPermission(this)) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }


}
