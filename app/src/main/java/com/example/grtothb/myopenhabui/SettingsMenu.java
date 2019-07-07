package com.example.grtothb.myopenhabui;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.example.grtothb.myopenhabui.AlarmAction.MyBroadcastReceiver;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


public class SettingsMenu extends AppCompatActivity {

    public static final String KEEP_ALIVE_SRV_NOTIFICATION_CH_ID = "MyOpenHabUINotification";
    public static final String KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID = "MyOpenHabUIAlarmNotification";

    private static LogToFileSwitchChangeListener logFileSwitchListener = null;
    private static KeepAliveViaAlarmSwitchChangeListener KeepAliveAlarmListener = null;


    // Variables for myDeviceAdminReceiver
    static final int RESULT_ENABLE = 1;
    DevicePolicyManager devicePolicyMan;
    ComponentName compName;

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_menu);

        // Register routines for UI elements
        Switch mySwitch = findViewById(R.id.LogToFileSwtchID);
        if(logFileSwitchListener == null) {
            logFileSwitchListener = new LogToFileSwitchChangeListener();
        }
        mySwitch.setChecked(logFileSwitchListener.logToFileSwitchState);
        mySwitch.setOnCheckedChangeListener(logFileSwitchListener);

        Switch mySwitch2 = findViewById(R.id.KeepAliveViaAlarmsID);
        if (KeepAliveAlarmListener == null){
            KeepAliveAlarmListener = new KeepAliveViaAlarmSwitchChangeListener();
        }
        KeepAliveAlarmListener.SwitchState = MyBroadcastReceiver.isAlarmOn();
        mySwitch2.setChecked(KeepAliveAlarmListener.SwitchState);
        mySwitch2.setOnCheckedChangeListener(KeepAliveAlarmListener);

        // Get Alarm Interval value
        EditText myTextEntry = findViewById(R.id.IntevalID);
        myTextEntry.setText(MyBroadcastReceiver.getInterval().toString());


        // Prepare to work with the DPM
        devicePolicyMan = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, DeviceAdminReceiver.class);

        // check if DeviveAdminReceiver is registered
        boolean active = devicePolicyMan.isAdminActive(compName);
        if (!active) {
            // Not activated, activate it
            // Launch the activity to have the user enable our admin.
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "MyOpenHabUI needs Admin access to be able to lock the screen.");
            startActivityForResult(intent, RESULT_ENABLE);
        }
    }

    // --------------------------------------------------------------------------------------------
    //
    // --------------------------------------------------------------------------------------------
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.e("myDeviceAdminReceiver", "Admin enabled!");
                } else {
                    Log.e("myDeviceAdminReceiver", "Admin enable FAILED!");
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        boolean SwitchState = false;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                SwitchState = true;
                // Create Notification Channel for the App's keepAliveAlarms
                createNotificationChannel(KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID);

                // Get Alarm Interval value
                EditText myTextEntry = findViewById(R.id.IntevalID);
                long interval = Long.parseLong(myTextEntry.getText().toString());
                // Start Keep Alive Alarms
                Intent intent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                intent.setAction(MyBroadcastReceiver.BCASTRCV_TRGNXTALARM);
                intent.putExtra(MyBroadcastReceiver.BCASTRCV_PARAM_PKG_NAME, "com.example.grtothb.myopenhabui");
                intent.putExtra(MyBroadcastReceiver.BCASTRCV_PARAM_INTERVAL, interval);
                sendBroadcast(intent);
            } else {
                SwitchState = false;
                // Stop Keep Alive Alarms
                Intent intent = new Intent(getApplicationContext(), MyBroadcastReceiver.class);
                intent.setAction(MyBroadcastReceiver.BCASTRCV_STPALARM);
                sendBroadcast(intent);
                // delete notification channel
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    Objects.requireNonNull(notificationManager).deleteNotificationChannel(KEEP_ALIVE_ALARM_NOTIFICATION_CH_ID);
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
        boolean logToFileSwitchState = false;

        @SuppressWarnings("ResultOfMethodCallIgnored")
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
                if ( !logDirectory.exists() ) //noinspection ResultOfMethodCallIgnored
                    logDirectory.mkdir();
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
            Objects.requireNonNull(notificationManager).createNotificationChannel(channel);
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
