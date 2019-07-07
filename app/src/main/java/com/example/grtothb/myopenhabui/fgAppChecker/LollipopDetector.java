package com.example.grtothb.myopenhabui.fgAppChecker;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

public class LollipopDetector implements Detector {

    private static String foregroundApp = null;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getForegroundApp(final Context context) {
        if(!Utils.hasUsageStatsPermission(context)) {
            Log.e("MyLollipopDetector", "Permission error");
            return null;
        }

        UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Service.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();

        // check the events of the last 30 min
        if (mUsageStatsManager != null){
            UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 1800, time);
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if(event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundApp = event.getPackageName();
                }
            }
        }
        else {
            Log.e("MyLollipopDetector", "mUsageStatsManager = null");
        }
        // return the name of the package for last MOVE_TO_FOREGROUND event or no such event found the last one that was brought to foreground
        return foregroundApp;
    }
}
