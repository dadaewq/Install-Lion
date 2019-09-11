package com.modosa.apkinstaller.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.modosa.apkinstaller.R;


/**
 * @author dadaewq
 */
public class ManageActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
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
    }

    private void enable() {
        ComponentName ctMain = new ComponentName(this, MainActivity.class);
        PackageManager pm = getPackageManager();
        int state = pm.getComponentEnabledSetting(ctMain);
        boolean isEnabled = (state == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || state == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
        try {
            if (isEnabled) {
                Toast.makeText(getApplicationContext(), getString(R.string.success_enable), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplication(), MainActivity.class);
                startActivity(intent);
            } else {
                pm.setComponentEnabledSetting(ctMain, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (Exception e0) {
            e0.printStackTrace();
        }
        finish();
    }
}





















