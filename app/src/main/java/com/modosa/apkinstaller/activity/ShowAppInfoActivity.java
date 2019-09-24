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
public class ShowAppInfoActivity extends Activity {
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Log.d("getIntent", getIntent() + "");
//
//        ComponentName getcomponentName = getIntent().getComponent();
//        ComponentName currentcomponentName = new ComponentName(getPackageName(), getClass().getName());
//        Log.d("getcmpName", getcomponentName + "");
//        Log.d("ct_cmpName", currentcomponentName + "");

        try {
            String packagename = getIntent().getExtras().get(Intent.EXTRA_PACKAGE_NAME) + "";
            Log.d("packagename", packagename);

            Intent intent = new Intent(Intent.ACTION_SHOW_APP_INFO);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packagename);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, e + "", Toast.LENGTH_SHORT).show();
        }
        finish();

    }
}
