package com.modosa.apkinstaller.base;


import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.modosa.apkinstaller.fragment.SettingsFragment;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        int nightMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(SettingsFragment.SP_KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);
        AppCompatDelegate.setDefaultNightMode(nightMode);

    }

}
