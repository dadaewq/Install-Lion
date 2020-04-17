package com.modosa.apkinstaller.base;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.modosa.apkinstaller.fragment.SettingsFragment;

@SuppressWarnings("WeakerAccess")
public class Application extends android.app.Application {


    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences spGetPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
        if (!spGetPreferenceManager.getBoolean(SettingsFragment.SP_KEY_DISABLE_BUG_REPORT, false)) {
            String scretCode = null;


            AppCenter.start(this, scretCode, Analytics.class, Crashes.class);
        }
        int nightMode = spGetPreferenceManager.getInt(SettingsFragment.SP_KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);
        AppCompatDelegate.setDefaultNightMode(nightMode);

    }

}
