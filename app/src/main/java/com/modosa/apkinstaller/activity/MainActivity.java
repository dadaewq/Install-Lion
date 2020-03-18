package com.modosa.apkinstaller.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;


/**
 * @author dadaewq
 */
public class MainActivity extends Activity {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startMainUiActivity();
        finishAndRemoveTask();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void startMainUiActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, MainUiActivity.class);
        startActivity(intent);
    }
}
