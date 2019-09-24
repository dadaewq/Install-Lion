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

        try {
            String packagename = getIntent().getExtras().get(Intent.EXTRA_PACKAGE_NAME) + "";
            Log.d("PACKAGE_NAME ==>", packagename);
            Intent intent = getPackageManager().getLaunchIntentForPackage(packagename);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e + "", Toast.LENGTH_LONG).show();
        }

        finish();
    }

}
