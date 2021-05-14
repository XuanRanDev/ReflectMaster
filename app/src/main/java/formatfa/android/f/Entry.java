package formatfa.android.f;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import formatfa.reflectmaster.MainActivity;


public class Entry implements IXposedHookLoadPackage {

    public static final String PACKAGE_NAME = "com.XuanRan.ReflectMaster";
    public static final String HOOK_PACKAGE = "package";
    public static final String PACKAGENAME = "formatfa.android.f.reflectmaster";
    public static String id;
    public static String register;
    public static int statu;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        XSharedPreferences xSharedPreferences = new XSharedPreferences(PACKAGE_NAME,HOOK_PACKAGE);
        xSharedPreferences.reload();

        String[] hook_package = xSharedPreferences.getString(MainActivity.KEY,"").split(",");

        boolean flag = false;

        for (String app_packageName : hook_package){

            if (app_packageName.equals(lpparam.packageName)){
                flag = true;
                break;
            }
        }

        if (!flag) {
            return;
        }

        initFloatWindows(xSharedPreferences);


        LogUtils.loge("the aim app had hook");
        XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, "onCreate", Bundle.class, new onCreate_Hook(lpparam));
        XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, "onResume", new onResume_Hook());
        XposedHelpers.findAndHookMethod("android.app.Dialog", lpparam.classLoader, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                int keyCode = (int) param.args[0];
                Dialog dialog = (Dialog) param.thisObject;


                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    FWindow win = new FWindow(Registers.nowAct, dialog);
                    //win.setDialog(dialog);
                }
            }
        });


        XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                int keyCode = (int) param.args[0];


                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    Registers.nowAct = (Activity) param.thisObject;
                    FWindow win = new FWindow(lpparam, param);

                }
            }
        });


        //自定义hook

        String data = xSharedPreferences.getString("script2", null);
        XposedBridge.log("diy script :" + data);
        if (data != null) {
            JSONArray ja = new JSONArray(data);
            for (int i = 0; i < ja.length(); i += 1) {
                JSONObject object = ja.getJSONObject(i);


                String packagename = object.getString("packagename");
                if (!lpparam.packageName.equals(packagename)) {
                    XposedBridge.log("no:" + packagename.toString());
                    continue;
                }
                ;
                String name = object.getString("name");

                //判断是before还是after
                int mode = 0;
                if (name.startsWith("af"))
                    mode = 1;
                name = name.substring(2);
                String code = object.getString("code");
                String[] names = name.split(" ");
                ;
                Object[] objects = new Object[names.length - 2 + 1];

                for (int e = 0; e < objects.length - 1; e += 1) {
                    objects[e] = getCls(lpparam.classLoader, names[e + 2]);
                }

                //before
                objects[objects.length - 1] = new DiyHookCallBack(lpparam, code, mode);
                XposedBridge.log("find and hook:" + names[1]);

                XposedHelpers.findAndHookMethod(names[0], lpparam.classLoader, names[1], objects);


            }


        }

    }

    /**
     * @param xSharedPreferences SharedPreference
     */
    private void initFloatWindows(XSharedPreferences xSharedPreferences) {
        Registers.isUseWindowSearch = xSharedPreferences.getBoolean("windowsearch", false);
        Registers.isFloating = xSharedPreferences.getBoolean("float", true);
        Registers.newThread = xSharedPreferences.getBoolean("newthread", false);
        id = xSharedPreferences.getString("fid", "");
        statu = xSharedPreferences.getInt("statu", 0);
        register = xSharedPreferences.getString("register", "");
        XposedBridge.log("aim hooked");
        Registers.windowSize = xSharedPreferences.getInt("width", 700);
        Registers.rotate = xSharedPreferences.getBoolean("rotate", true);
        XposedBridge.log("set Window size:" + Registers.windowSize);
    }


    private Class getCls(ClassLoader loader, String conciaType) throws ClassNotFoundException {


        XposedBridge.log("getCls:" + conciaType);
        switch (conciaType) {
            case "int":
                return int.class;

            case "boolean":
                return boolean.class;

            case "long":
                return long.class;

            case "byte":
                return byte.class;


            default:
                return loader.loadClass(conciaType);


        }


    }


}
