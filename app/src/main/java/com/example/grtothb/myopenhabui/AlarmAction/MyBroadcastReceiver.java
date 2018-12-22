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

import java.util.Objects;

import static android.content.Context.ALARM_SERVICE;

public class MyBroadcastReceiver extends BroadcastReceiver {

    // Intent parameters for BroadCastReceiver
    public static final String BCASTRCV_PARAM_PKG_NAME = "com.example.grtothb.myopenhabui.bcastrcv.param.pkgName";
    public static final String BCASTRCV_PARAM_INTERVAL = "com.example.grtothb.myopenhabui.bcastrcv.param.interval";
    private static final String BCASTRCV_PARAM_ALMSIR_TEMP = "com.example.grtothb.myopenhabui.bcastrcv.param.alarmsirene.temp";
    private static final String BCASTRCV_PARAM_NROFRELAUNCHES = "com.example.grtothb.myopenhabui.bcastrcv.param.numberOfRelaunches";
    private static final String BCASTRCV_PARAM_NROFYCYCLES = "com.example.grtothb.myopenhabui.bcastrcv.param.numberOfCycles";

    // Intent actions for BroadCastReceiver
    public static final String BCASTRCV_TRGNXTALARM = "com.example.grtothb.myopenhabui.bcastrcv.action.TriggerNextAlarm";
    public static final String BCASTRCV_STPALARM = "com.example.grtothb.myopenhabui.bcastrcv.action.StopAlarms";

    private static final int KEEP_ALIVE_ALARM_NOTIFICATION_ID = 118;
    private static final String msg = "MyBcastRcv:";

    private static Integer NumberOfRelaunches = 0;
    private static Integer NumberOfCycles = 0;
    private static String AlarmSireneTemperature = "-.- °C";
    private static NotificationCompat.Builder KeepAliveAlarm_NotificationBuilder = null;

    // TODO: Verify if StopAlarm can stop alarming via PendingIntent
    // NOTE: PendingIntent is set to static to make sure StopAlarm will use the right PendingIntent to stop the alarm
    private static PendingIntent pending_alarm_intent = null;

    private String PkgName = "com.example.grtothb.myopenhabui";
    private static long interval = 30000;

    private final String HttpReqTag = "AlarmSireneTag";
    private RequestQueue HttpReqQueue = null;
    private StringRequest stringRequest = null;

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    public static boolean isAlarmOn() {
        return (pending_alarm_intent != null);
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    public static Long getInterval() {
        return interval;
    }


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
    private String msToTimeStr(long time_in_ms){
        long day = time_in_ms / (24*60*60*1000);
        long hour = (time_in_ms - day*24*60*60*1000)/(60*60*1000);
        long min = (time_in_ms - day*24*60*60*1000 - hour*60*60*1000) / (60*1000);
        long sec = (time_in_ms - day*24*60*60*1000 - hour*60*60*1000 - min*60*1000)/1000;

        String ret_str = "";
        if (day > 0) ret_str = day + " d";
        if (hour > 0) ret_str = ret_str + hour + "h";
        if (min > 0) ret_str = ret_str + min + "m";
        if (sec > 0) ret_str = ret_str + sec + "s";
        return ret_str;
     }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void TriggerNextAlarm (Context context) {
        // NOTE: Order of the operations below is important as it needs to be ensured that in case the App is started in a new
        // process the right info from this call MyBroadcastReceiver is delivered by the static methods

        //TODO: Verify if setting up the next Alarm and setting pending_alarm_intent before relaunch helps to provide the right state info for relaunch
        //TODO: doesn't work as expected see, Log.e line in MyOpenHabUI.java
        //NOTE: this is probably necessary to ensure that the relaunched app gets the correct value for pending_alarm_intent
        // Setup next Alarm
        Intent Alarm_intent = new Intent (context, MyBroadcastReceiver.class);
        Alarm_intent.setAction(BCASTRCV_TRGNXTALARM);
        // Set params,
        // NOTE: add also static variables as params to save state in case BroadCastReceiver will be executed in new process
        Alarm_intent.putExtra(BCASTRCV_PARAM_PKG_NAME, PkgName);
        Alarm_intent.putExtra(BCASTRCV_PARAM_INTERVAL, interval);
        Alarm_intent.putExtra(BCASTRCV_PARAM_NROFRELAUNCHES, NumberOfRelaunches);
        Alarm_intent.putExtra(BCASTRCV_PARAM_NROFYCYCLES, NumberOfCycles);
        Alarm_intent.putExtra(BCASTRCV_PARAM_ALMSIR_TEMP, AlarmSireneTemperature);

        // TODO: Verify if FLAG_UPDATE_CURRENT is OK
        pending_alarm_intent = PendingIntent.getBroadcast(
                context, 1, Alarm_intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Check foreground App
        fgAppChecker fg_appChecker = new fgAppChecker();
        String foreGroundAppName = fg_appChecker.getForegroundApp(context);
        if ( PkgName.equalsIgnoreCase(foreGroundAppName) ) {
            Log.e(msg, "Running in foreground, Pid: " + android.os.Process.myPid() + ", Uid: " + android.os.Process.myUid());
        }
        else {
            Log.e(msg, "Running Package: " + foreGroundAppName + " Relaunch Package: " + PkgName);
            Log.e(msg, "Relaunch, Pid: " + android.os.Process.myPid() + ", Uid: " + android.os.Process.myUid());
            NumberOfRelaunches++;
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(PkgName);
            context.startActivity(LaunchIntent);
        }

        // get actual temp every 15 min and at the very beginning
        final int _15MIN_IN_MS = 15*60*1000;
        if ( (Objects.equals(AlarmSireneTemperature, "-.- °C")) || ((((NumberOfCycles+1) * interval) / _15MIN_IN_MS) - ((NumberOfCycles * interval) / _15MIN_IN_MS)) != 0 ) {
            getAlarmSireneTemp();
        }

        // Update notification
        String notification_string = "Cycle: " + NumberOfCycles.toString() + "/" + msToTimeStr(NumberOfCycles*interval) + " Relaunches: " + NumberOfRelaunches.toString() + " Temp: " + AlarmSireneTemperature;
        Log.e(msg, "NotificationString: " + notification_string);

        // Create NotificationBuilder in case it not available (due to first run or Bcastreceiver executing in new process)
        if (KeepAliveAlarm_NotificationBuilder == null)
            setupNotification(context);

        if (KeepAliveAlarm_NotificationBuilder != null) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            KeepAliveAlarm_NotificationBuilder.setContentText(notification_string);
            Notification updateNotification = KeepAliveAlarm_NotificationBuilder.build();
            notificationManager.notify(KEEP_ALIVE_ALARM_NOTIFICATION_ID, updateNotification);
        }
        else
        {
            Log.e(msg, " ERROR: KeepAliveAlarm_NotificationBuilder is null!");
        }


        // Start next Alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager != null)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, pending_alarm_intent);
        else
            Log.e(msg, "ERROR: alarmManager = null");
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
                Log.e(msg, "ERROR: alarmManager stopAlarming = null");
        }
        if (HttpReqQueue != null) {
            HttpReqQueue.cancelAll(HttpReqTag);
        }
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void getAlarmSireneTemp() {
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
        if (intent != null) {

            // Get Params
            String tmp_str = intent.getStringExtra(BCASTRCV_PARAM_PKG_NAME);
            if (tmp_str != null)
                PkgName = tmp_str;
            interval = intent.getLongExtra(BCASTRCV_PARAM_INTERVAL, 30000); //in milliseconds

            tmp_str = intent.getStringExtra(BCASTRCV_PARAM_ALMSIR_TEMP);
            if (tmp_str != null)
                AlarmSireneTemperature = tmp_str;
            NumberOfCycles = intent.getIntExtra(BCASTRCV_PARAM_NROFYCYCLES, NumberOfCycles);
            NumberOfRelaunches = intent.getIntExtra(BCASTRCV_PARAM_NROFRELAUNCHES, NumberOfRelaunches);
            NumberOfCycles++;

            switch (Objects.requireNonNull(intent.getAction())) {
                case BCASTRCV_TRGNXTALARM:
                    TriggerNextAlarm(context);
                    break;
                case BCASTRCV_STPALARM:
                    stopAlarming(context);
                    break;
                default:
                    // Invalid action
                    stopAlarming(context);
                    break;
            }
        }
        Log.e(msg, "KeepAliveAlarmOnReceive cycle: " + NumberOfCycles.toString() + " Relaunches: " + NumberOfRelaunches.toString());
    }

}
