package com.example.grtothb.myopenhabui.fgAppChecker;

import android.content.Context;

public class fgAppChecker {

    private Detector detector;

    public fgAppChecker() {
        if(Utils.postLollipop())
            detector = new LollipopDetector();
        else
            detector = new PreLollipopDetector();
    }

    public String getForegroundApp(Context context) {
        return detector.getForegroundApp(context);
    }

}
