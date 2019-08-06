package com.modosa.apkinstaller.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.modosa.apkinstaller.R;


public class ManageActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.e("onCreate", "onCreate");
    }

    @Override
    public void onStart() {
        super.onStart();
//        Log.e("onStart", "onStart");
        try {
            enable();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            enable();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        Log.e("onResume", "onResume");
    }

    private void enable() {
//        Log.e("enable", "enable");
        ComponentName comptName = new ComponentName(this, MainActivity.class);
        PackageManager pm = getPackageManager();
        int state = pm.getComponentEnabledSetting(comptName);
        boolean isenabled = (state == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || state == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
//        Log.e("--state-", state + "");
        try {
            if (isenabled) {
//                Log.e("-state--1-", state + "");
                Toast.makeText(getApplicationContext(), getString(R.string.success_enable), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplication(), MainActivity.class);
                startActivity(intent);
            } else {
//                Log.e("-state--0-", state + "");
                pm.setComponentEnabledSetting(comptName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (Exception e0) {
            e0.printStackTrace();
        }
        finish();
    }
}





















