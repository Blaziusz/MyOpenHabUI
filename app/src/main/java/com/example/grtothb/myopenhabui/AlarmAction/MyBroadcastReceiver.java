package com.example.grtothb.myopenhabui.AlarmAction;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.grtothb.myopenhabui.MyOpenHabUI;
import com.example.grtothb.myopenhabui.R;
import com.example.grtothb.myopenhabui.SettingsMenu;
import com.example.grtothb.myopenhabui.fgAppChecker.fgAppChecker;

import static android.content.Context.ALARM_SERVICE;

public class MyBroadcastReceiver extends BroadcastReceiver {

    public static final String BCASTRCV_PARAM_PKG_NAME = "com.example.grtothb.myopenhabui.bcastrcv.param.pkgName";
    public static final String BCASTRCV_INTERVAL = "com.example.grtothb.myopenhabui.bcastrcv.param.interval";
    public static final String BCASTRCV_TRGNXTALARM = "com.example.grtothb.myopenhabui.bcastrcv.action.TriggerNextAlarm";
    public static final String BCASTRCV_STPALARM = "com.example.grtothb.myopenhabui.bcastrcv.action.StopAlarms";
    public static final String BCASTRCV_SETUP_NOTIFICATION = "com.example.grtothb.myopenhabui.bcastrcv.action.SetupNotification";

    private static final int KEEP_ALIVE_ALARM_NOTIFICATION_ID = 118;

    private static final String msg = "MyBcastRcv:";

    private static Integer NumberOfRelaunches = 0;
    private static Integer NumberOfCycles = 0;

    private String PkgName = "com.example.grtothb.myopenhabui";
    private long interval = 10000;
    private static PendingIntent pending_alarm_intent = null;
    private static NotificationCompat.Builder KeepAliveAlarm_NotificationBuilder = null;

    private static String AlarmSireneTemperature = "-.- °C";
    private RequestQueue HttpReqQueue = null;
    private StringRequest stringRequest = null;
    private final String HttpReqTag = "AlarmSireneTag";
    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void setupNotification (Context context){
        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(context, SettingsMenu.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        KeepAliveAlarm_NotificationBuilder = new NotificationCompat.Builder(context, SettingsMenu.KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID)
                .setContentTitle("MyOpenHabUI Alarm Notification")
                .setContentText("MyOpenHabUI KeepALiveAlarm")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(resultPendingIntent)
                .setTicker("MyOpenHabUI_Alarm_NotificationTicker");
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void TriggerNextAlarm (Context context) {
        // get actual temp every 15 min and at the very beginning
        final int _15MIN_IN_MS = 15*60*1000;
        if ( (AlarmSireneTemperature == "-.- °C") || ((((NumberOfCycles+1) * interval) / _15MIN_IN_MS) - ((NumberOfCycles * interval) / _15MIN_IN_MS)) != 0 ) {
            getAlarmSireneTemp(context);
        }
        // Check foreground App
        fgAppChecker fg_appChecker = new fgAppChecker();
        String foreGroundAppName = fg_appChecker.getForegroundApp(context);
        if ( PkgName.equalsIgnoreCase(foreGroundAppName) ) {
            Log.e(msg, "Running in foreground");
        }
        else {
            Log.e(msg, "Running Package: " + foreGroundAppName + "Relaunch Package:" + PkgName);
            NumberOfRelaunches++;
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(PkgName);
            context.startActivity(LaunchIntent);
        }
        // Update notification
        String notification_string = "KeepAliveAlarm cycle: " + NumberOfCycles.toString() + " Relaunches: " + NumberOfRelaunches.toString() + " Temp: " + AlarmSireneTemperature;
        Log.e(msg, "NotificationString: " + notification_string);
        if (KeepAliveAlarm_NotificationBuilder != null) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            KeepAliveAlarm_NotificationBuilder.setContentText(notification_string);
            Notification updateNotification = KeepAliveAlarm_NotificationBuilder.build();
            notificationManager.notify(KEEP_ALIVE_ALARM_NOTIFICATION_ID, updateNotification);
        }

        // Start next Alarm
        Intent Alarm_intent = new Intent(context, MyBroadcastReceiver.class);
            Alarm_intent.setAction(BCASTRCV_TRGNXTALARM);
            Alarm_intent.putExtra(BCASTRCV_PARAM_PKG_NAME, PkgName);
            Alarm_intent.putExtra(BCASTRCV_INTERVAL, interval);
        pending_alarm_intent = PendingIntent.getBroadcast(
                context, 1, Alarm_intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager != null)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, pending_alarm_intent);
        else
            Log.e(msg, "alarmManager = null");
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void stopAlarming (Context context) {
        if (pending_alarm_intent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            if (alarmManager != null)
                alarmManager.cancel(pending_alarm_intent);
            else
                Log.e(msg, "alarmManager stopAlarming = null");
        }
        if (HttpReqQueue != null) {
            HttpReqQueue.cancelAll(HttpReqTag);
        }
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void getAlarmSireneTemp(Context context) {
        String url ="http://192.168.1.50:8080/rest/items/AlarmSirene_Temperature/state";

        if (HttpReqQueue == null){
            HttpReqQueue = MyOpenHabUI.getsInstance().getmRequestQueue();
        }

        if (stringRequest == null) {
            // Request a string response from the provided URL.
            stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            AlarmSireneTemperature = response + " °C";
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    AlarmSireneTemperature = "--- Error ---";
                }
            });
            stringRequest.setTag(HttpReqTag);
        }
        // Add the request to the RequestQueue.
        HttpReqQueue.add(stringRequest);
    }


        /**
         * This method is called when the BroadcastReceiver is receiving an Intent
         * broadcast.  During this time you can use the other methods on
         * BroadcastReceiver to view/modify the current result values.  This method
         * is always called within the main thread of its process, unless you
         * explicitly asked for it to be scheduled on a different thread using
         * {@link Context# registerReceiver(BroadcastReceiver, * IntentFilter, String, Handler)}.
         * When it runs on the main thread you should
         * never perform long-running operations in it (there is a timeout of
         * 10 seconds that the system allows before considering the receiver to
         * be blocked and a candidate to be killed). You cannot launch a popup dialog
         * in your implementation of onReceive().
         *
         * <p><b>If this BroadcastReceiver was launched through a &lt;receiver&gt; tag,
         * then the object is no longer alive after returning from this
         * function.</b> This means you should not perform any operations that
         * return a result to you asynchronously. If you need to perform any follow up
         * background work, schedule a {@link JobService} with
         * {@link JobScheduler}.
         * <p>
         * If you wish to interact with a service that is already running and previously
         * bound using {@link Context# bindService(Intent, ServiceConnection, int) bindService()},
         * you can use {@link #peekService}.
         *
         * <p>The Intent filters used in {@link Context#registerReceiver}
         * and in application manifests are <em>not</em> guaranteed to be exclusive. They
         * are hints to the operating system about how to find suitable recipients. It is
         * possible for senders to force delivery to specific recipients, bypassing filter
         * resolution.  For this reason, {@link #onReceive(Context, Intent) onReceive()}
         * implementations should respond only to known actions, ignoring any unexpected
         * Intents that they may receive.
         *
         * @param context The Context in which the receiver is running.
         * @param intent  The Intent being received.
         */
    @Override
    public void onReceive(Context context, Intent intent) {
        // check if the right app is running in the foreground
        NumberOfCycles++;
        Log.e(msg, "KeepAliveAlarmOnReceive cycle: " + NumberOfCycles.toString() + " Relaunches: " + NumberOfRelaunches.toString());

        if (intent != null) {
            switch (intent.getAction()) {
                case BCASTRCV_TRGNXTALARM:
                    PkgName = intent.getStringExtra(BCASTRCV_PARAM_PKG_NAME);
                    interval = intent.getLongExtra(BCASTRCV_INTERVAL, 10000); //in milliseconds
                    TriggerNextAlarm(context);
                    break;
                case BCASTRCV_STPALARM:
                    stopAlarming(context);
                    break;
                case BCASTRCV_SETUP_NOTIFICATION:
                    setupNotification(context);
                    break;
                default:
                    // Invalid action
                    stopAlarming(context);
                    break;
            }
        }
    }
}
