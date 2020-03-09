package com.modosa.apkinstaller.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.OpUtil;

/**
 * @author dadaewq
 */
public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private long exitTime = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updatePermissionState();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0x2330);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length > 0) {
            Log.e("onRequestPermissit", requestCode + " | " + permissions[0] + " | " + grantResults[0]);
        } else {
            Log.e("wrong requestCode", requestCode + "");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.HideIcon:
                showDialogHideIcon();
                break;
            case R.id.ClearAllowedList:
                showDialogClearAllowedList();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showDialogHideIcon() {

        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.HideIcon);

        ComponentName mainComponentName = new ComponentName(this, "com.modosa.apkinstaller.activity.MainActivity");
        PackageManager pm = getPackageManager();
        boolean isEnabled = (pm.getComponentEnabledSetting(mainComponentName) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(mainComponentName) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));

        checkBox.setChecked(!isEnabled);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.HideIcon)
                .setMessage(R.string.message_HideIcon)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.setComponentState(this, mainComponentName,
                        !checkBox.isChecked()));

        AlertDialog alertDialog = builder.create();


        OpUtil.showAlertDialog(this, alertDialog);

    }

    private void showDialogClearAllowedList() {

        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.checkbox_ClearAllowedList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.ClearAllowedList)
                .setMessage(R.string.message_ClearAllowedList)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    SharedPreferences.Editor editor = getSharedPreferences("allowsource", Context.MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();
                });

        AlertDialog alertDialog = builder.create();


        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));

        OpUtil.showAlertDialog(this, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            long intervals = 2000;
            if ((System.currentTimeMillis() - exitTime) > intervals) {
                Toast.makeText(getApplicationContext(), getString(R.string.tip_exit), Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionState();
    }

    private void updatePermissionState() {
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean judge = (permissionRead == 0);
        if (!judge) {
            requestPermission();
        }
    }
}

