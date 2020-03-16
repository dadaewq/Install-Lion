package com.modosa.apkinstaller.base;


import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.modosa.apkinstaller.fragment.SettingsFragment;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        int nightMOde = PreferenceManager.getDefaultSharedPreferences(this).getInt(SettingsFragment.SP_KEY_NIGHT_MODE, 520);

        if (nightMOde == AppCompatDelegate.MODE_NIGHT_NO || nightMOde == AppCompatDelegate.MODE_NIGHT_YES || nightMOde == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(nightMOde);
        }
    }

}
