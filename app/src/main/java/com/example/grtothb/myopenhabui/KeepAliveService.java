package com.example.grtothb.myopenhabui;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.grtothb.myopenhabui.fgAppChecker.fgAppChecker;


public class KeepAliveService extends Service {
    private static final String msg = "MyKeepAliveService:";
    int mStartMode;       // indicates how to behave if the service is killed
    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used

    // Parameters for Keep Alive Service
    private static final String SRV_PARAM_PKG_NAME = "com.example.grtothb.myopenhabui.param.pkgName";
    private static final String SRV_PARAM_INTERVAL = "com.example.grtothb.myopenhabui.param.interval";

    private static boolean stopTask;
    private static NotificationCompat.Builder myNotificationBuilder;
    private static final int NOTIFICATION_ID = 111;

    public KeepAliveService() {
        Log.d(msg, "KeepAliveService constructor");
    }

    // --------------------------------------------------------------------------------------------
    // Static methods to start and stop the service
    // --------------------------------------------------------------------------------------------
    public static void startKeepAliveService(Context context, String PkgName, long Interval) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.putExtra(SRV_PARAM_PKG_NAME, PkgName);
        intent.putExtra(SRV_PARAM_INTERVAL, Interval);
        stopTask = false;
        context.startService(intent);
        Log.d(msg, "service start");
    }

    public static void stopKeepAliveService(Context context, String param1, String param2) {
        Intent intent = new Intent(context, KeepAliveService.class);
        intent.putExtra(SRV_PARAM_PKG_NAME, param1);
        intent.putExtra(SRV_PARAM_INTERVAL, param2);
        stopTask = true;
        context.stopService(intent);
        Log.d(msg, "service stop");
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(msg, "onCreate");
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private NotificationCompat.Builder preparations4Notification() {
        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, SettingsMenu.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, SettingsMenu.KEEP_ALIVE_SRV_NOTIFICATION_CH_ID)
                .setContentTitle("MyOpenHabUI Notification")
                .setContentText("MyOpenHabUI KeepALiveService")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(resultPendingIntent)
                .setTicker("MyOpenHabUI_NotificationTicker");
    }


    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private void startKeepAliveThread(final String PkgName, final long interval) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Integer i = 0;
                Integer NumberOfRelaunches = 0;
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
                String foreGroundAppName;
                fgAppChecker fg_appChecker = new fgAppChecker();

                try {
                    while (!stopTask) {
                        i++;
                        Log.e(msg, "Keep Alive Loop, count: " + i.toString() + ", thread: " + this.getId() + "/" + this.getName());

                        //BT - 2018-11-04: checking for Foreground constantly relaunches app when screensave is on on Huawei Tablet
                        // Check if our app is in the foreground
                        //if ( MyOpenHabUI_ActivityLifecycleCallback.isApplicationInForeground() ) {
                        //Log.d(msg, "in foreground");
                        //}
                        // BT 2018-11-24: Testing routine from Didosz
                        //if (isAppOnForeground(getBaseContext(), PkgName)) {
                        foreGroundAppName = fg_appChecker.getForegroundApp(getBaseContext());
                        if ( PkgName.equalsIgnoreCase(foreGroundAppName) ) {
                            Log.e(msg, "Running in foreground");
                        }
                        else {
                            Log.e(msg, "Running Package: " + foreGroundAppName + "Relaunch Package:" + PkgName);
                            NumberOfRelaunches++;
                            Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(PkgName);
                            startActivity(LaunchIntent);
                        }
                        String notification_string = "KeepAliveService cycle: " + i.toString() + " Relaunches: " + NumberOfRelaunches.toString();
                        Log.e(msg, "NotificationString: " + notification_string);
                        myNotificationBuilder.setContentText(notification_string);
                        Notification updateNotification = myNotificationBuilder.build();
                        notificationManager.notify(NOTIFICATION_ID, updateNotification);
                        sleep(interval);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }


    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Log.e(msg, "service starting");

        // Create the Notification Builder for the Foreground Service
        myNotificationBuilder = preparations4Notification();

        Notification notification = myNotificationBuilder.build();
        startForeground(NOTIFICATION_ID, notification);

        if (intent != null) {
            Log.e(msg, "intent received in onStartCommand");
            // Start new thread for service routine
            final String PkgName = intent.getStringExtra(SRV_PARAM_PKG_NAME);
            final long interval = intent.getLongExtra(SRV_PARAM_INTERVAL, 3000); //in milliseconds
            startKeepAliveThread(PkgName,interval);
        }
        mStartMode = START_REDELIVER_INTENT;
        return mStartMode;
    }


    /**
     * This is called if the service is currently running and the user has
     * removed a task that comes from the service's application.  If you have
     * set {@link ServiceInfo#FLAG_STOP_WITH_TASK ServiceInfo.FLAG_STOP_WITH_TASK}
     * then you will not receive this callback; instead, the service will simply
     * be stopped.
     *
     * @param rootIntent The original root Intent that was used to launch
     *                   the task that is being removed.
     */

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e(msg, "onTaskRemoved: " + rootIntent.getPackage());
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.e(msg,"LowMemory received");
    }


    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public boolean onUnbind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
        // All clients have unbound with unbindService()
        //return mAllowRebind;
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");

    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        stopTask = true;
        // stopForeground notification
        stopForeground(true);

        Log.d(msg, "service done");
        super.onDestroy();
    }
}



// --------------------------------------------------------------------------------------------
//
// --------------------------------------------------------------------------------------------
    /*private boolean isAppOnForeground(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            Log.d(msg, "appProcesses List is empty");
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.d(msg, "appProcesses List member: " + appProcess.processName);
            if (((appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) ||
                    (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE)) && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }*/



/*
    private void debugOutOnStartCommand () {
        // The service is starting, due to a call to startService()
        Log.d(msg, "service starting");
        long callingThreadId = Thread.currentThread().getId();
        Log.d(msg, "calling Thread ID: " + callingThreadId);

        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] lstThreads = new Thread[noThreads];
        currentGroup.enumerate(lstThreads);
        for (int i = 0; i < noThreads; i++) Log.d(msg, "Thread No:" + i + " = " + lstThreads[i].getId() + "/" + lstThreads[i].getName());
    }

*/

