package com.anng6.usbkit;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.anng6.usbkit.util.ThemeUtil;

import rikka.material.app.LocaleDelegate;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    public static String TAG = "USBKIT";
    private static App instance;
    private static SharedPreferences pref;
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        AppCompatDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme());

        LocaleDelegate.setDefaultLocale(getLocale());
        var res = getResources();
        var config = res.getConfiguration();
        config.setLocale(LocaleDelegate.getDefaultLocale());
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static Handler getMainHandler() {
        return mainHandler;
    }

    public static App getInstance() {
        return instance;
    }

    public static SharedPreferences getPreferences() {
        return instance.pref;
    }

    public static Locale getLocale(String tag) {
        if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
            return LocaleDelegate.getSystemLocale();
        }
        return Locale.forLanguageTag(tag);
    }

    public static Locale getLocale() {
        var tag = getPreferences().getString("language", null);
        return getLocale(tag);
    }
}
