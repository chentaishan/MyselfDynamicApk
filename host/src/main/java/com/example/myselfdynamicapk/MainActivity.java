package com.example.myselfdynamicapk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String PLUG_APP_PATH = "";
    private String APK_NAME = "plugin-debug.apk";
    private String APK_PATH ="";
    private ImageView mImageview;
    private Button mClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        PLUG_APP_PATH = Environment.getDataDirectory()+"/data/" +getPackageName()+ File.separator;
        APK_PATH =PLUG_APP_PATH+APK_NAME;
        initView();



    }

    private void initView() {
        mImageview = (ImageView) findViewById(R.id.imageview);
        mClick = (Button) findViewById(R.id.click);
        mClick.setOnClickListener(this);
    }


    /**
     * 加载资源
     */
    private void loadPlugResource() {

        String[] apkInfo = getUninstallApkInfo(this, PLUG_APP_PATH  + APK_NAME);
        String appName = apkInfo[0];
        String pkgName = apkInfo[1];
        Resources resource = getPluginResources(APK_PATH);
        try {
            int resid = getRecourceIdFromPlugApk(APK_PATH, pkgName);
            mImageview.setBackgroundDrawable(resource.getDrawable(resid));
        } catch (Exception e) {
            e.printStackTrace();

        }

    }

    /**
     * 加载apk获得内部资源
     *
     * @param apkPath apk路径
     * @throws Exception
     */
    private int getRecourceIdFromPlugApk(String apkPath, String apkPackageName) throws Exception {
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建
        Log.v("zxy", optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex
        //参数：1、包含dex的apk文件或jar文件的路径，2、apk、jar解压缩生成dex存储的目录，3、本地library库目录，一般为null，4、父ClassLoader
        DexClassLoader dexClassLoader = new DexClassLoader(apkPath, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());
        Class<?> clazz = dexClassLoader.loadClass(apkPackageName + ".R$mipmap");//通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id
        Field field = clazz.getDeclaredField("test");//得到名为test的这张图片字段
        int resId = field.getInt(R.id.class);//得到图片id
        return resId;
    }

    /**
     * @param apkPath
     * @return 得到对应插件的Resource对象
     * 通过得到AssetManager中的内部的方法addAssetPath，
     * 将未安装的apk路径传入从而添加进assetManager中，
     * 然后通过new Resource把assetManager传入构造方法中，进而得到未安装apk对应的Resource对象。
     */
    private Resources getPluginResources(String apkPath) {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);//反射调用方法addAssetPath(String path)
            //第二个参数是apk的路径：Environment.getExternalStorageDirectory().getPath()+File.separator+"plugin"+File.separator+"apkplugin.apk"
            //将未安装的Apk文件的添加进AssetManager中，第二个参数为apk文件的路径带apk名
            addAssetPath.invoke(assetManager, apkPath);
            Resources superRes = this.getResources();
            Resources mResources = new Resources(assetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
            return mResources;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取未安装apk的信息
     *
     * @param context
     * @param apkPath apk文件的path
     * @return
     */
    private String[] getUninstallApkInfo(Context context, String apkPath) {
        String[] info = new String[2];
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            String versionName = pkgInfo.versionName;//版本号
            Drawable icon = pm.getApplicationIcon(appInfo);//图标
            String appName = pm.getApplicationLabel(appInfo).toString();//app名称
            String pkgName = appInfo.packageName;//包名
            info[0] = appName;
            info[1] = pkgName;
        }
        return info;
    }

    private static final String TAG = "MainActivity";
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.click:
                // TODO 19/08/25
                loadPlugResource();

                try {
                    final String[] uninstallApkInfo = getUninstallApkInfo(this, APK_PATH);

                    final String result = runPlugApkMethod(APK_PATH, uninstallApkInfo[1]);

                    Log.d(TAG, "onClick: "+result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }


    /**
     * @param apkPath apk路径
     * @throws Exception
     */
    private String runPlugApkMethod(String apkPath,String apkPackageName) throws Exception {
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);//在应用安装目录下创建一个名为app_dex文件夹目录,如果已经存在则不创建
        Log.v("zxy", optimizedDirectoryFile.getPath().toString());// /data/data/com.example.dynamicloadapk/app_dex
        //参数：1、包含dex的apk文件或jar文件的路径，2、apk、jar解压缩生成dex存储的目录，3、本地library库目录，一般为null，4、父ClassLoader
        DexClassLoader dexClassLoader = new DexClassLoader(apkPath, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());
//        //通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id
//        Class<?> clazz = dexClassLoader.loadClass(apkPackageName + ".R$mipmap");
//        Field field = clazz.getDeclaredField("test");//得到名为test的这张图片字段
//        int resId = field.getInt(R.id.class);//得到图片id

        // 使用DexClassLoader加载类
        Class libProvierClazz = dexClassLoader.loadClass(apkPackageName+".MainActivity");
        //通过反射运行sayHello方法
        Object obj=libProvierClazz.newInstance();
        Method method=libProvierClazz.getMethod("sayHello");
        return (String)method.invoke(obj);

    }
}
