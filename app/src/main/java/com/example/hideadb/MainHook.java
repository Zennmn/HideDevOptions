package com.example.hideadb;

import android.provider.Settings;
import android.util.Log;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    private static final String TAG = "HideDevOptions";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded in process: " + param.getProcessName());

        try {
            var getIntGlobal = Settings.Global.class.getDeclaredMethod(
                "getInt",
                android.content.ContentResolver.class,
                String.class,
                int.class
            );
            hook(getIntGlobal).intercept(this::interceptGlobalGetInt);
        } catch (NoSuchMethodException e) {
            log(Log.ERROR, TAG, "Failed to hook Settings.Global.getInt", e);
        }

        try {
            var getIntSecure = Settings.Secure.class.getDeclaredMethod(
                "getInt",
                android.content.ContentResolver.class,
                String.class,
                int.class
            );
            hook(getIntSecure).intercept(this::interceptSecureGetInt);
        } catch (NoSuchMethodException e) {
            log(Log.ERROR, TAG, "Failed to hook Settings.Secure.getInt", e);
        }

        try {
            var getStringGlobal = Settings.Global.class.getDeclaredMethod(
                "getString",
                android.content.ContentResolver.class,
                String.class
            );
            hook(getStringGlobal).intercept(this::interceptGlobalGetString);
        } catch (NoSuchMethodException e) {
            log(Log.ERROR, TAG, "Failed to hook Settings.Global.getString", e);
        }

        try {
            var getStringSecure = Settings.Secure.class.getDeclaredMethod(
                "getString",
                android.content.ContentResolver.class,
                String.class
            );
            hook(getStringSecure).intercept(this::interceptSecureGetString);
        } catch (NoSuchMethodException e) {
            log(Log.ERROR, TAG, "Failed to hook Settings.Secure.getString", e);
        }
    }

    private Object interceptGlobalGetInt(XposedInterface.Chain chain) throws Throwable {
        String key = (String) chain.getArgs().get(1);
        if (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.equals(key)
                || Settings.Global.ADB_ENABLED.equals(key)) {
            return 0;
        }
        return chain.proceed();
    }

    private Object interceptSecureGetInt(XposedInterface.Chain chain) throws Throwable {
        String key = (String) chain.getArgs().get(1);
        if ("development_settings_enabled".equals(key)
                || "adb_enabled".equals(key)) {
            return 0;
        }
        return chain.proceed();
    }

    private Object interceptGlobalGetString(XposedInterface.Chain chain) throws Throwable {
        String key = (String) chain.getArgs().get(1);
        if (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED.equals(key)
                || Settings.Global.ADB_ENABLED.equals(key)) {
            return "0";
        }
        return chain.proceed();
    }

    private Object interceptSecureGetString(XposedInterface.Chain chain) throws Throwable {
        String key = (String) chain.getArgs().get(1);
        if ("development_settings_enabled".equals(key)
                || "adb_enabled".equals(key)) {
            return "0";
        }
        return chain.proceed();
    }
}
