package com.example.grtothb.myopenhabui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.example.grtothb.myopenhabui.AlarmAction.MyBroadcastReceiver;

import java.io.File;
import java.io.IOException;


public class SettingsMenu extends AppCompatActivity {

    public static final String KEEP_ALIVE_SRV_NOTIFICATION_CH_ID = "MyOpenHabUINotification";
    public static final String KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID = "MyOpenHabUIAlarmNotification";

    private static LogToFileSwitchChangeListener logFileSwitchListener = null;
    private static KeepAliveViaAlarmSwitchChangeListener KeepAliveAlarmListener = null;

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_menu);

        // Register routines for UI elements
        Switch mySwitch = (Switch) findViewById(R.id.LogToFileSwtchID);
        if(logFileSwitchListener == null) {
            logFileSwitchListener = new LogToFileSwitchChangeListener();
        }
        mySwitch.setChecked(logFileSwitchListener.logToFileSwitchState);
        mySwitch.setOnCheckedChangeListener(logFileSwitchListener);

        Switch mySwitch2 = (Switch) findViewById(R.id.KeepAliveViaAlarmsID);
        if (KeepAliveAlarmListener == null){
            KeepAliveAlarmListener = new KeepAliveViaAlarmSwitchChangeListener();
        }
        mySwitch2.setChecked(KeepAliveAlarmListener.SwitchState);
        mySwitch2.setOnCheckedChangeListener(KeepAliveAlarmListener);
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private class KeepAliveViaAlarmSwitchChangeListener implements CompoundButton.OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        public boolean SwitchState = false;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                SwitchState = true;
                // Create Notification Channel for the App's keepAliveAlarms
                createNotificationChannel(KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID);
                // Setup notification
                Intent intent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                intent.setAction(MyBroadcastReceiver.BCASTRCV_SETUP_NOTIFICATION);
                sendBroadcast(intent);

                // Get Alarm Interval value
                EditText myTextEntry = (EditText) findViewById(R.id.IntevalID);
                long interval = Long.parseLong(myTextEntry.getText().toString());
                // Start Keep Alive Alarms
                Intent intent2 = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                intent2.setAction(MyBroadcastReceiver.BCASTRCV_TRGNXTALARM);
                intent2.putExtra(MyBroadcastReceiver.BCASTRCV_PARAM_PKG_NAME, "com.example.grtothb.myopenhabui");
                intent2.putExtra(MyBroadcastReceiver.BCASTRCV_INTERVAL, interval);
                sendBroadcast(intent2);
            } else {
                SwitchState = false;
                // Stop Keep Alive Alarms
                Intent intent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                intent.setAction(MyBroadcastReceiver.BCASTRCV_STPALARM);
                sendBroadcast(intent);
                // delete notification channel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    notificationManager.deleteNotificationChannel(KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID);
                }
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    private class LogToFileSwitchChangeListener implements CompoundButton.OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param buttonView The compound button view whose state has changed.
         * @param isChecked  The new checked state of buttonView.
         */
        private Process logcat_process = null;
        public boolean logToFileSwitchState = false;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                logToFileSwitchState = true;
                // Turn on logging to file
                // Setup logcat to log into file
                File appDirectory = new File( Environment.getExternalStorageDirectory() + "/MyOpenHabUI" );
                File logDirectory = new File( appDirectory + "/log" );
                File logFile = new File( logDirectory, "logcat" + System.currentTimeMillis() + ".txt" );

                // create app folder
                if ( !appDirectory.exists() ) appDirectory.mkdir();
                // create log folder
                if ( !logDirectory.exists() ) logDirectory.mkdir();
                // clear the previous logcat and then write the new one to the file
                try {
                    logcat_process = Runtime.getRuntime().exec("logcat -c");
                    //process = Runtime.getRuntime().exec("logcat -f " + logFile);
                    logcat_process = Runtime.getRuntime().exec("logcat *:E -f " + logFile);
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            } else {
                logToFileSwitchState = false;
               // Turn off logging to file
                // clear the previous logcat and stop it
                try {
                    // clear/flush the buffer
                    Runtime.getRuntime().exec("logcat -c");
                    logcat_process.destroy();
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Create notification channel
    // --------------------------------------------------------------------------------------------
    private void createNotificationChannel(String ID) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MyOpenHabUI Notification Channel";
            String description = "MyOpenHabUI Notification's Channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    public void onClickStrtSrv(View view) {
        // Start Keep Alive Service button pressed
        // Create Notification Channel for the App's keepAliveService
        createNotificationChannel(KEEP_ALIVE_SRV_NOTIFICATION_CH_ID);
        // Start KeepAliveService
        KeepAliveService.startKeepAliveService(this, "com.example.grtothb.myopenhabui", 10000);
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    public void onClickStopSrv(View view) {
        // Stop Keep Alive Service
        KeepAliveService.stopKeepAliveService(this,"com.example.grtothb.myopenhabui","param2" );
        // delete notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.deleteNotificationChannel(KEEP_ALIVE_SRV_NOTIFICATION_CH_ID);
        }
    }



    //TODO: Add OnClickStartKeepAliveWorker routine
    //TODO: Add dependency in build.gradle for Androidx Workmanager
    //TODO: Add buttons to activity_settings_menu layout
    //TODO: Add KeepAliveWorker class, see MyAppWithNotification
    //TODO: Update CreateNotificationChannel routine so it can be used for both KeepAliveService and KeepAliveWorker
}
