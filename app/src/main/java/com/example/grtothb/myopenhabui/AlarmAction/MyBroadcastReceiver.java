package com.example.grtothb.myopenhabui.AlarmAction;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.example.grtothb.myopenhabui.MainActivity;
import com.example.grtothb.myopenhabui.MyOpenHabUI;
import com.example.grtothb.myopenhabui.R;
import com.example.grtothb.myopenhabui.SettingsMenu;
import com.example.grtothb.myopenhabui.fgAppChecker.fgAppChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.SystemClock.sleep;

public class MyBroadcastReceiver extends BroadcastReceiver {

    // Intent parameters for BroadCastReceiver
    public static final String BCASTRCV_PARAM_PKG_NAME = "com.example.grtothb.myopenhabui.bcastrcv.param.pkgName";
    public static final String BCASTRCV_PARAM_INTERVAL = "com.example.grtothb.myopenhabui.bcastrcv.param.interval";
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
    private static String AlarmSystemArmedState = "---";
    private static Integer openFKs = 0; // Number of open Fensterkontakte
    private static Integer numOfFKs = 0; // Number of the Fensterkontakte
    private static Integer disabledFKs = 0; // Number of Fensterkontakte disabled for Alarm
    private static Integer numOfFKs4disable = 0; // Mumber of Fensterkontalte that can be disabled for Alarm
    private static NotificationCompat.Builder KeepAliveAlarm_NotificationBuilder = null;
    private static boolean alarmingOn = false;

    // TODO: Verify if StopAlarm can stop alarming via PendingIntent
    // NOTE: PendingIntent is set to static to make sure StopAlarm will use the right PendingIntent to stop the alarm
    private static PendingIntent pending_alarm_intent = null;

    private String PkgName = "com.example.grtothb.myopenhabui";
    private static long interval = 60000;

    private final String HttpReqTag = "AlarmSireneTag";
    private final String HttpReqTagAlarmState = "AlarmArmedTag";
    private final String JsonReqTag = "HM_FKsTag";
    private static RequestQueue HttpReqQueue = null;
    private static StringRequest stringRequest = null;
    private static StringRequest stringRequestAlarmState = null;
    private static JsonObjectRequest jsonRequest = null;
    private static JsonObjectRequest jsonReqDisabledFKs = null;

    // --------------------------------------------------------------------------------------------
    // Constructor only for debug purposes
    // --------------------------------------------------------------------------------------------
    public MyBroadcastReceiver() {
        super();
        Log.d(msg, "MyBcasRcv Constructor, PID: " + android.os.Process.myPid() + " UID: " + android.os.Process.myUid());
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    public static boolean isAlarmOn() {
        return alarmingOn;
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
        // TODO: Check if the order of the operations below is important as it needs to be ensured that in case the App is started in a new
        // process the right info from this call MyBroadcastReceiver is delivered by the static methods

        // Check foreground App
        fgAppChecker fg_appChecker = new fgAppChecker();
        String foreGroundAppName = fg_appChecker.getForegroundApp(context);
        if ( PkgName.equalsIgnoreCase(foreGroundAppName) ) {
            Log.e(msg, "Running in foreground, Pid: " + android.os.Process.myPid() + ", Uid: " + android.os.Process.myUid());
        }
        else {
            Log.d(msg, "Running Package: " + foreGroundAppName + " Relaunch Package: " + PkgName);
            Log.d(msg, "Relaunch, Pid: " + android.os.Process.myPid() + ", Uid: " + android.os.Process.myUid());
            NumberOfRelaunches++;
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage(PkgName);
            if (LaunchIntent != null) {
                LaunchIntent.putExtra(BCASTRCV_PARAM_PKG_NAME, "TestString");
                context.startActivity(LaunchIntent);
            }
        }

        // 2019-12-01 Use % (modulo) operator to make code more readable
        // WARNING: Using modulo assumes the interval set for KeepAlive Alarm is in a way (e.g.: 60000 and not 30123) that 15 min and 12 hour values and their multiplies are achieved
        // Previous version of the code was more difficult to read, worked however w/ any KeepAlive Alarm interval values
        final int _12H_IN_MS =  12*60*60*1000;
        // final int _1H_IN_MS = 1*60*60*1000;
        final int _15MIN_IN_MS = 15*60*1000;

        // 2019-11-27 : delete cache every 12 hour and restart webview timers (see: https://bartsimons.me/android-webview-cpu-usage-fix/)
        // 2020-06-28 : delete cache every 12 hour or when it is marked for deletion and restart webview timers (see: https://bartsimons.me/android-webview-cpu-usage-fix/)
        if ((((NumberOfCycles*interval) % _12H_IN_MS) == 0) || MyOpenHabUI.getsInstance().getTriggerCacheDel()) {
            // 2022-12-10 BT - do not delete cache
            //deleteCache(context);
            // 2019-12-01 restart webview timers
            MainActivity myMainActivity = MyOpenHabUI.getsInstance().getMainActivityInstance();
            if (myMainActivity != null)
            {
                myMainActivity.wv_basic_ui.pauseTimers();
                //2022-12-10: Add short wait after pauseTimers
                sleep(50);
                myMainActivity.wv_basic_ui.resumeTimers();
                myMainActivity.wv_paper_ui.pauseTimers();

                sleep(50);
                myMainActivity.wv_paper_ui.resumeTimers();
            }
        }

        // get actual temp every 15 min and at the very beginning
        if ( (Objects.equals(AlarmSireneTemperature, "-.- °C")) || (((NumberOfCycles * interval) % _15MIN_IN_MS) == 0) ) {
            getAlarmSireneTemp();
        }

        // get number of open Fensterkontakte in every cycle
        getNumOfOpenFK();
        // get number of disabled Fensterkontakte in every cycle
        getNumOfDisabledFK();

        // get Alarm State in every cycle
        getAlarmSystemArmedState();

        // Update notification
        String notification_title = "MyOpenHabUI Alarm: " + msToTimeStr(NumberOfCycles*interval) + " ReLau: " + NumberOfRelaunches.toString() + " ALARM: " + AlarmSystemArmedState;
        String notification_string = "OpenFKs:" + openFKs.toString() + "/" + numOfFKs + ", DisabledFKs:" + disabledFKs.toString() + "/" + numOfFKs4disable + ", Temp: " + AlarmSireneTemperature;
        Log.d(msg, "NotificationString: " + notification_string);

        // Create NotificationBuilder in case it not available (due to first run or Bcastreceiver executing in new process)
        if (KeepAliveAlarm_NotificationBuilder == null)
            setupNotification(context);

        if (KeepAliveAlarm_NotificationBuilder != null) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            KeepAliveAlarm_NotificationBuilder.setContentText(notification_string);
            KeepAliveAlarm_NotificationBuilder.setContentTitle(notification_title);
            Notification updateNotification = KeepAliveAlarm_NotificationBuilder.build();
            notificationManager.notify(KEEP_ALIVE_ALARM_NOTIFICATION_ID, updateNotification);
        }
        else
        {
            Log.e(msg, " ERROR: KeepAliveAlarm_NotificationBuilder is null!");
        }

        // Check if the screen is ON
        PowerManager powerMan = (PowerManager) context.getSystemService(POWER_SERVICE);
        // If screen is Off turn it ON
        if (!powerMan.isInteractive()) {
            PowerManager.WakeLock mWakeL = powerMan.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK| PowerManager.ACQUIRE_CAUSES_WAKEUP, "my_ohui:wakeup_tag");
            // turn on screen
            mWakeL.acquire(5*1000L);
        }

        // Setup next Alarm
        Intent Alarm_intent = new Intent (context, MyBroadcastReceiver.class);
        Alarm_intent.setAction(BCASTRCV_TRGNXTALARM);
        // Set params,
        // NOTE: add also static variables as params to save state in case BroadCastReceiver will be executed in new process
        Alarm_intent.putExtra(BCASTRCV_PARAM_PKG_NAME, PkgName);
        Alarm_intent.putExtra(BCASTRCV_PARAM_INTERVAL, interval);
        Alarm_intent.putExtra(BCASTRCV_PARAM_NROFRELAUNCHES, NumberOfRelaunches);
        Alarm_intent.putExtra(BCASTRCV_PARAM_NROFYCYCLES, NumberOfCycles);

        // TODO: Verify if FLAG_UPDATE_CURRENT is OK
        pending_alarm_intent = PendingIntent.getBroadcast(
                context, 1, Alarm_intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
            if (alarmManager != null) {
                alarmManager.cancel(pending_alarm_intent);
                pending_alarm_intent = null;
            }
            else
                Log.e(msg, "ERROR: alarmManager stopAlarming = null");
        }
        // Remove notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(KEEP_ALIVE_ALARM_NOTIFICATION_ID);

        if (HttpReqQueue != null) {
            HttpReqQueue.cancelAll(HttpReqTag);
            HttpReqQueue.cancelAll(JsonReqTag);
        }
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void getAlarmSystemArmedState() {
        String url ="http://192.168.1.50:8080/rest/items/Alarm_Armed/state";

        if (HttpReqQueue == null){
            HttpReqQueue = MyOpenHabUI.getsInstance().getmRequestQueue();
        }

        if (stringRequestAlarmState == null) {
            // Request a string response from the provided URL.
            stringRequestAlarmState = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            AlarmSystemArmedState = response;
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    AlarmSystemArmedState = "- Err -";
                }
            });
            stringRequestAlarmState.setTag(HttpReqTagAlarmState);
        }
        // Add the request to the RequestQueue.
        // 2019-11-25: disable cache for the request
        stringRequestAlarmState.setShouldCache(false);
        HttpReqQueue.add(stringRequestAlarmState);
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
        // 2019-11-25: disable cache for the request
        stringRequest.setShouldCache(false);
        HttpReqQueue.add(stringRequest);
    }


    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void getNumOfOpenFK() {
        String url ="http://192.168.1.50:8080/rest/items/HM_FKs";

        if (HttpReqQueue == null){
            HttpReqQueue = MyOpenHabUI.getsInstance().getmRequestQueue();
        }

        if (jsonRequest == null) {
            // Request a string response from the provided URL.
            jsonRequest = new JsonObjectRequest(Request.Method.GET, url,null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            openFKs = 0;
                            try {
                                JSONArray jsonArray = response.getJSONArray("members");
                                numOfFKs = jsonArray.length();
                                if (response.getString("state").equals("OPEN")) {
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject jsonObj = jsonArray.getJSONObject(i);
                                        if (jsonObj.getString("state").equals("OPEN")) openFKs++;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                openFKs = -1;
                                numOfFKs = -1;
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(msg, "getHM-FKs error:" + error.toString());
                    openFKs = -1;
                    numOfFKs = -1;
                }
            });
            jsonRequest.setTag(JsonReqTag);
        }

        // Add the request to the RequestQueue.
        // 2019-11-25: disable cache for the request
        jsonRequest.setShouldCache(false);
        HttpReqQueue.add(jsonRequest);
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void getNumOfDisabledFK() {
        String url ="http://192.168.1.50:8080/rest/items/FK_Enablers";

        if (HttpReqQueue == null){
            HttpReqQueue = MyOpenHabUI.getsInstance().getmRequestQueue();
        }

        if (jsonReqDisabledFKs == null) {
            // Request a string response from the provided URL.
            jsonReqDisabledFKs = new JsonObjectRequest(Request.Method.GET, url,null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            disabledFKs = 0;
                            try {
                                JSONArray jsonArray = response.getJSONArray("members");
                                numOfFKs4disable = jsonArray.length();
                                if (response.getString("state").equals("OFF")) {
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject jsonObj = jsonArray.getJSONObject(i);
                                        if (jsonObj.getString("state").equals("OFF")) disabledFKs++;
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                disabledFKs = -1;
                                numOfFKs4disable = -1;
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(msg, "getHM-FKs error:" + error.toString());
                    disabledFKs = -1;
                    numOfFKs4disable = -1;
                }
            });
            jsonReqDisabledFKs.setTag(JsonReqTag);
        }

        // Add the request to the RequestQueue.
        // 2019-11-25: disable cache for the request
        jsonReqDisabledFKs.setShouldCache(false);
        HttpReqQueue.add(jsonReqDisabledFKs);
    }

    // --------------------------------------------------------------------------------------------
    // 2019-11-27 : Not tested yet
    // --------------------------------------------------------------------------------------------
    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            Log.e(msg, " ERROR: deleteCache" + dir.toString());
            deleteDir(dir);
        } catch (Exception e) { e.printStackTrace();}
    }

    // --------------------------------------------------------------------------------------------
    // 2019-11-27 : Not tested yet
    // --------------------------------------------------------------------------------------------
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
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

        Log.d(msg, "onReceive start");
        //TODO: Verify if setting up the next Alarm and setting the alarming Status alarmingOn before checking for relaunch
        // helps to provide the right state info for te relaunched app
        //TODO: I tried it but didn't work as expected see, Log.e() line in MyOpenHabUI.java
        //NOTE: this is probably necessary to ensure that the relaunched app gets the correct value for alarmingOn

        // check if the right app is running in the foreground
        if (intent != null) {

            // Get Params
            String tmp_str = intent.getStringExtra(BCASTRCV_PARAM_PKG_NAME);
            if (tmp_str != null)
                PkgName = tmp_str;
            interval = intent.getLongExtra(BCASTRCV_PARAM_INTERVAL, 30000); //in milliseconds

            NumberOfCycles = intent.getIntExtra(BCASTRCV_PARAM_NROFYCYCLES, NumberOfCycles);
            NumberOfRelaunches = intent.getIntExtra(BCASTRCV_PARAM_NROFRELAUNCHES, NumberOfRelaunches);
            NumberOfCycles++;

            switch (Objects.requireNonNull(intent.getAction())) {
                case BCASTRCV_TRGNXTALARM:
                    alarmingOn = true;
                    TriggerNextAlarm(context);
                    break;
                case BCASTRCV_STPALARM:
                    stopAlarming(context);
                    alarmingOn = false;
                    break;
                default:
                    // Invalid action
                    stopAlarming(context);
                    alarmingOn = false;
                    break;
            }
        }
        Log.d(msg, "KeepAliveAlarmOnReceive cycle: " + NumberOfCycles.toString() + " Relaunches: " + NumberOfRelaunches.toString());
    }

}
