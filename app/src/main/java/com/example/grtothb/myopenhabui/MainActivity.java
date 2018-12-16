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

import com.example.grtothb.myopenhabui.fgAppChecker.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    private static final String msg = "MyOpenHabUI: ";
    private static final String paper_ui_url = "http://192.168.1.50:8080/paperui/index.html#/inbox/search";
    private static final String basic_ui_url = "http://192.168.1.50:8080/basicui/app?sitemap=alarm";
    public static final String NOTIFICATION_CH_ID = "MyOpenHabUINotification";

    public static WebView wv_paper_ui = null;
    public static WebView wv_basic_ui = null;

    // Power management
    private PowerManager.WakeLock mWakeLock;

    // --------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        PowerManager pm = (PowerManager) getSystemService(getBaseContext().POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "my_ohui:pm_tag");
        mWakeLock.acquire();
        // Load the OpenHab Web pages
        wv_paper_ui = (WebView) findViewById(R.id.paper_ui_webview);
        loadWebViewDatafinal(wv_paper_ui, paper_ui_url);
        wv_basic_ui = (WebView) findViewById(R.id.basic_ui_webview);
        loadWebViewDatafinal(wv_basic_ui, basic_ui_url);
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
            notificationManager.createNotificationChannel(channel);
        }
    }


    // --------------------------------------------------------------------------------------------
    // Load Webpages
    // --------------------------------------------------------------------------------------------
    private void loadWebViewDatafinal(WebView wv, String url) {
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setAllowFileAccess(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            try {
                Log.d("WEB_VIEW_JS", "Enabling HTML5-Features");
                Method m1 = WebSettings.class.getMethod("setDomStorageEnabled", new Class[]{Boolean.TYPE});
                m1.invoke(ws, Boolean.TRUE);
                Method m2 = WebSettings.class.getMethod("setDatabaseEnabled", new Class[]{Boolean.TYPE});
                m2.invoke(ws, Boolean.TRUE);
                Method m3 = WebSettings.class.getMethod("setDatabasePath", new Class[]{String.class});
                m3.invoke(ws, "/data/data/" + this.getPackageName() + "/databases/");
                Method m4 = WebSettings.class.getMethod("setAppCacheMaxSize", new Class[]{Long.TYPE});
                m4.invoke(ws, 1024 * 1024 * 8);
                Method m5 = WebSettings.class.getMethod("setAppCachePath", new Class[]{String.class});
                m5.invoke(ws, "/data/data/" + this.getPackageName() + "/cache/");
                Method m6 = WebSettings.class.getMethod("setAppCacheEnabled", new Class[]{Boolean.TYPE});
                m6.invoke(ws, Boolean.TRUE);
                Log.d("WEB_VIEW_JS", "Enabled HTML5-Features");
            } catch (NoSuchMethodException e) {
                Log.e("WEB_VIEW_JS", "Reflection fail", e);
            } catch (InvocationTargetException e) {
                Log.e("WEB_VIEW_JS", "Reflection fail", e);
            } catch (IllegalAccessException e) {
                Log.e("WEB_VIEW_JS", "Reflection fail", e);
            }
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
        if (mWakeLock != null)
            mWakeLock.release();
        // delete Apps's notification channel
        Log.d("MyOpenHabUI:MainAct", "onDestroy");
        // TODO: move this stopService, this is called when getting back from Menu Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(NOTIFICATION_CH_ID);
        }
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
