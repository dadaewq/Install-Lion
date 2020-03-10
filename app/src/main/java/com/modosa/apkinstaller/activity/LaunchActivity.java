package com.modosa.apkinstaller.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;


/**
 * @author dadaewq
 */
public class LaunchActivity extends Activity {

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init();
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void init() {
        if (getIntent().hasExtra(Intent.EXTRA_PACKAGE_NAME)) {
            String packagename = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME) + "";
            Log.d("PACKAGE_NAME ==>", packagename);
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(packagename);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, e + "", Toast.LENGTH_LONG).show();
            }
        }
    }
}
