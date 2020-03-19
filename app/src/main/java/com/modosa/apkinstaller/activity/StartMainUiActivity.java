package com.modosa.apkinstaller.activity;

import android.app.Activity;
import android.os.Bundle;

import com.modosa.apkinstaller.util.OpUtil;

public class StartMainUiActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpUtil.startMainUiActivity(this);
        finish();
    }
}
