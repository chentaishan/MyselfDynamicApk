package com.example.myselfdynamicapk;

import android.os.Build;
import android.os.Handler;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class HookHelper {

    public static final String TARGET_INTENT = "target_intent";
    public static final String TARGET_INTENT_NAME = "name";

    public static void hookAMS() throws Exception {
        Object defaultSingleton=null;
        if (Build.VERSION.SDK_INT >= 26) {//1
            Class<?> activityManageClazz = Class.forName("android.app.ActivityManager");
            //获取activityManager中的IActivityManagerSingleton字段
            defaultSingleton=  FieldUtil.getField(activityManageClazz ,null,"IActivityManagerSingleton");
        } else {
            Class<?> activityManagerNativeClazz = Class.forName("android.app.ActivityManagerNative");
            //获取ActivityManagerNative中的gDefault字段
            defaultSingleton=  FieldUtil.getField(activityManagerNativeClazz,null,"gDefault");
        }
        Class<?> singletonClazz = Class.forName("android.util.Singleton");
        Field mInstanceField= FieldUtil.getField(singletonClazz ,"mInstance");//2
        //获取iActivityManager
        Object iActivityManager = mInstanceField.get(defaultSingleton);//3
        Class<?> iActivityManagerClazz = Class.forName("android.app.IActivityManager");
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { iActivityManagerClazz }, new IActivityManagerProxy(iActivityManager));
        mInstanceField.set(defaultSingleton, proxy);
    }

    public static void hookHandler() throws Exception {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object currentActivityThread= FieldUtil.getField(activityThreadClass ,null,"sCurrentActivityThread");//1
        Field mHField = FieldUtil.getField(activityThreadClass,"mH");//2
        Handler mH = (Handler) mHField.get(currentActivityThread);//3
        FieldUtil.setField(Handler.class,mH,"mCallback",new HCallback(mH));
    }
}