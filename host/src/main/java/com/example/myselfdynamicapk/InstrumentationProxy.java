package com.example.myselfdynamicapk;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class InstrumentationProxy extends Instrumentation {
    private Instrumentation mInstrumentation;
    private PackageManager mPackageManager;
    public InstrumentationProxy(Instrumentation instrumentation, PackageManager packageManager) {
        mInstrumentation = instrumentation;
        mPackageManager = packageManager;
    }
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        List<ResolveInfo> infos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        if (infos == null || infos.size() == 0) {
            intent.putExtra(HookHelper.TARGET_INTENT_NAME, intent.getComponent().getClassName());//1
            intent.setClassName(who, "com.example.liuwangshu.pluginactivity.StubActivity");//2
        }
        try {
            Method execMethod = Instrumentation.class.getDeclaredMethod("execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
            return (ActivityResult) execMethod.invoke(mInstrumentation, who, contextThread, token,
                    target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        String intentName = intent.getStringExtra(HookHelper.TARGET_INTENT_NAME);
        if (!TextUtils.isEmpty(intentName)) {
            return super.newActivity(cl, intentName, intent);
        }
        return super.newActivity(cl, className, intent);
    }

    public static void hookInstrumentation(Context context) throws Exception {
        Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
        Field mMainThreadField  =FieldUtil.getField(contextImplClass,"mMainThread");//1
        Object activityThread = mMainThreadField.get(context);//2
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Field mInstrumentationField=FieldUtil.getField(activityThreadClass,"mInstrumentation");//3
        FieldUtil.setField(activityThreadClass,activityThread,"mInstrumentation",new InstrumentationProxy((Instrumentation) mInstrumentationField.get(activityThread),
                context.getPackageManager()));
    }
}
