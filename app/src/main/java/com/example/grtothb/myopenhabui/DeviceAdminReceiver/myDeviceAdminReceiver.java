package com.example.grtothb.myopenhabui.DeviceAdminReceiver;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


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



public class myDeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {

    public final String ACTION_LOCK_SCREEN = "LCK_SCRN";

    DevicePolicyManager devicePolMan;
    public myDeviceAdminReceiver() {
        super();
        devicePolMan = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (devicePolMan == null) {
            devicePolMan = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        }

        if (intent.getAction() == ACTION_LOCK_SCREEN) {
            devicePolMan.lockNow();
        }
        super.onReceive(context, intent);
    }



    @Override
    public void onEnabled(Context context, Intent intent) {
        Log.e("myDeviceAdminReceiver:", "Admin Enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        Log.e("myDeviceAdminReceiver:", "Admin Disable Requested");
        return "Admin Disable Requested";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Log.e("myDeviceAdminReceiver:", "Admin Disabled");
    }
}
