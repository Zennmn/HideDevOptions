package com.example.hideadb;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class MainHook extends XposedModule {

    private static final String TAG = "HideDevOptions";
    private static final String DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled";
    private static final String ADB_ENABLED = "adb_enabled";
    private static final String ADB_WIFI_ENABLED = "adb_wifi_enabled";
    private static final String SETTINGS_AUTHORITY = "settings";
    private static final String SETTINGS_VALUE_COLUMN = Settings.NameValueTable.VALUE;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded in process: " + param.getProcessName());

        hookSettingsReads(Settings.Global.class);
        hookSettingsReads(Settings.Secure.class);
        hookSettingsReads(Settings.System.class);
        hookContentResolverReads();
        hookSystemPropertyReads();
    }

    private void hookSettingsReads(Class<?> settingsClass) {
        hookMethod(settingsClass, "getString", this::interceptGetString, false,
                ContentResolver.class, String.class);
        hookMethod(settingsClass, "getStringForUser", this::interceptGetString, true,
                ContentResolver.class, String.class, int.class);

        hookMethod(settingsClass, "getInt", this::interceptGetInt, false,
                ContentResolver.class, String.class, int.class);
        hookMethod(settingsClass, "getInt", this::interceptGetInt, false,
                ContentResolver.class, String.class);
        hookMethod(settingsClass, "getIntForUser", this::interceptGetInt, true,
                ContentResolver.class, String.class, int.class, int.class);
        hookMethod(settingsClass, "getIntForUser", this::interceptGetInt, true,
                ContentResolver.class, String.class, int.class);
    }

    private void hookContentResolverReads() {
        hookMethod(ContentResolver.class, "call", this::interceptResolverCall, false,
                Uri.class, String.class, String.class, Bundle.class);
        hookMethod(ContentResolver.class, "call", this::interceptResolverCall, true,
                String.class, String.class, String.class, Bundle.class);

        hookMethod(ContentResolver.class, "query", this::interceptResolverQuery, false,
                Uri.class, String[].class, Bundle.class, android.os.CancellationSignal.class);
        hookMethod(ContentResolver.class, "query", this::interceptResolverQuery, false,
                Uri.class, String[].class, String.class, String[].class, String.class);
        hookMethod(ContentResolver.class, "query", this::interceptResolverQuery, false,
                Uri.class, String[].class, String.class, String[].class, String.class,
                android.os.CancellationSignal.class);
    }

    @SuppressLint("PrivateApi")
    private void hookSystemPropertyReads() {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            hookMethod(systemPropertiesClass, "get", this::interceptSystemProperty, true,
                    String.class);
            hookMethod(systemPropertiesClass, "get", this::interceptSystemProperty, true,
                    String.class, String.class);
            hookMethod(systemPropertiesClass, "getBoolean", this::interceptSystemProperty, true,
                    String.class, boolean.class);
            hookMethod(systemPropertiesClass, "getInt", this::interceptSystemProperty, true,
                    String.class, int.class);
            hookMethod(systemPropertiesClass, "getLong", this::interceptSystemProperty, true,
                    String.class, long.class);
        } catch (ClassNotFoundException e) {
            log(Log.ERROR, TAG, "Failed to find android.os.SystemProperties", e);
        }
    }

    private void hookMethod(
            Class<?> ownerClass,
            String methodName,
            XposedInterface.Hooker hooker,
            boolean optional,
            Class<?>... parameterTypes
    ) {
        try {
            var method = ownerClass.getDeclaredMethod(methodName, parameterTypes);
            deoptimize(method);
            hook(method).intercept(hooker);
        } catch (NoSuchMethodException e) {
            if (!optional) {
                log(Log.ERROR, TAG, "Failed to find " + ownerClass.getName() + "." + methodName, e);
            }
        }
    }

    private Object interceptGetInt(XposedInterface.Chain chain) throws Throwable {
        if (shouldHideSetting(chain)) {
            return 0;
        }
        return chain.proceed();
    }

    private Object interceptGetString(XposedInterface.Chain chain) throws Throwable {
        if (shouldHideSetting(chain)) {
            return "0";
        }
        return chain.proceed();
    }

    private Object interceptResolverCall(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        Object target = args.get(0);
        String method = (String) args.get(1);
        String key = (String) args.get(2);
        if (isSettingsTarget(target) && isSettingsGetMethod(method) && shouldHideSetting(key)) {
            Bundle result = new Bundle();
            result.putString(SETTINGS_VALUE_COLUMN, "0");
            return result;
        }
        return chain.proceed();
    }

    private Object interceptResolverQuery(XposedInterface.Chain chain) throws Throwable {
        List<Object> args = chain.getArgs();
        Uri uri = (Uri) args.get(0);
        if (!isSettingsUri(uri)) {
            return chain.proceed();
        }

        String key = findQueriedSettingKey(uri, args);
        if (!shouldHideSetting(key)) {
            return chain.proceed();
        }

        String[] projection = (String[]) args.get(1);
        return buildSettingsCursor(projection, key);
    }

    private Object interceptSystemProperty(XposedInterface.Chain chain) throws Throwable {
        String key = (String) chain.getArgs().get(0);
        if (!shouldHideSystemProperty(key)) {
            return chain.proceed();
        }

        Method method = (Method) chain.getExecutable();
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) {
            return "ro.adb.secure".equals(key);
        }
        if (returnType == int.class) {
            return systemPropertyIntReplacement(key);
        }
        if (returnType == long.class) {
            return systemPropertyLongReplacement(key);
        }

        Object original = shouldSanitizeUsbProperty(key) ? chain.proceed() : null;
        return systemPropertyReplacement(key, original instanceof String ? (String) original : null);
    }

    private boolean shouldHideSetting(XposedInterface.Chain chain) {
        Object key = chain.getArgs().get(1);
        return shouldHideSetting(key);
    }

    private boolean shouldHideSetting(Object key) {
        return DEVELOPMENT_SETTINGS_ENABLED.equals(key)
                || ADB_ENABLED.equals(key)
                || ADB_WIFI_ENABLED.equals(key);
    }

    private boolean isSettingsTarget(Object target) {
        if (target instanceof Uri) {
            return isSettingsUri((Uri) target);
        }
        return SETTINGS_AUTHORITY.equals(target);
    }

    private boolean isSettingsUri(Uri uri) {
        return uri != null && SETTINGS_AUTHORITY.equals(uri.getAuthority());
    }

    private boolean isSettingsGetMethod(String method) {
        return method != null && method.startsWith("GET_");
    }

    private String findQueriedSettingKey(Uri uri, List<Object> args) {
        String keyFromUri = findSettingKeyInUri(uri);
        if (shouldHideSetting(keyFromUri)) {
            return keyFromUri;
        }

        if (args.size() > 2 && args.get(2) instanceof Bundle) {
            Bundle queryArgs = (Bundle) args.get(2);
            return findSettingKeyInArgs(queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS));
        }

        if (args.size() > 3 && args.get(3) instanceof String[]) {
            return findSettingKeyInArgs((String[]) args.get(3));
        }

        return null;
    }

    private String findSettingKeyInUri(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (segments.size() >= 2) {
            return segments.get(segments.size() - 1);
        }
        return null;
    }

    private String findSettingKeyInArgs(String[] selectionArgs) {
        if (selectionArgs == null) {
            return null;
        }
        for (String arg : selectionArgs) {
            if (shouldHideSetting(arg)) {
                return arg;
            }
        }
        return null;
    }

    private MatrixCursor buildSettingsCursor(String[] projection, String key) {
        String[] columns = projection != null ? projection : new String[] {"name", SETTINGS_VALUE_COLUMN};
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            if ("_id".equals(column)) {
                row[i] = 0;
            } else if ("name".equals(column)) {
                row[i] = key;
            } else if (SETTINGS_VALUE_COLUMN.equals(column)) {
                row[i] = "0";
            }
        }

        MatrixCursor cursor = new MatrixCursor(columns, 1);
        cursor.addRow(row);
        return cursor;
    }

    private boolean shouldHideSystemProperty(String key) {
        return "ro.debuggable".equals(key)
                || "ro.adb.secure".equals(key)
                || "init.svc.adbd".equals(key)
                || "service.adb.tcp.port".equals(key)
                || "persist.adb.tcp.port".equals(key)
                || "persist.service.adb.tcp.port".equals(key)
                || "persist.service.adb.enable".equals(key)
                || shouldSanitizeUsbProperty(key);
    }

    private boolean shouldSanitizeUsbProperty(String key) {
        return "sys.usb.config".equals(key)
                || "sys.usb.state".equals(key)
                || "persist.sys.usb.config".equals(key);
    }

    private String systemPropertyReplacement(String key, String original) {
        if ("ro.adb.secure".equals(key)) {
            return "1";
        }
        if ("init.svc.adbd".equals(key)) {
            return "stopped";
        }
        if ("service.adb.tcp.port".equals(key)
                || "persist.adb.tcp.port".equals(key)
                || "persist.service.adb.tcp.port".equals(key)) {
            return "-1";
        }
        if (shouldSanitizeUsbProperty(key)) {
            return sanitizeUsbConfig(original);
        }
        return "0";
    }

    private int systemPropertyIntReplacement(String key) {
        if ("ro.adb.secure".equals(key)) {
            return 1;
        }
        if ("service.adb.tcp.port".equals(key)
                || "persist.adb.tcp.port".equals(key)
                || "persist.service.adb.tcp.port".equals(key)) {
            return -1;
        }
        return 0;
    }

    private long systemPropertyLongReplacement(String key) {
        if ("ro.adb.secure".equals(key)) {
            return 1L;
        }
        if ("service.adb.tcp.port".equals(key)
                || "persist.adb.tcp.port".equals(key)
                || "persist.service.adb.tcp.port".equals(key)) {
            return -1L;
        }
        return 0L;
    }

    private String sanitizeUsbConfig(String value) {
        if (value == null || value.isEmpty()) {
            return "none";
        }

        String sanitized = value
                .replace(",adb", "")
                .replace("adb,", "")
                .replace("adb", "")
                .replace(",,", ",");
        if (sanitized.startsWith(",")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.endsWith(",")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized.isEmpty() ? "none" : sanitized;
    }
}
