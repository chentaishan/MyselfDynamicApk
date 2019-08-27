package com.example.myselfdynamicapk;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {
    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            HookHelper.hookAMS();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}