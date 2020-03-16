package com.modosa.apkinstaller.activity;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.fragment.DPMSettingsFragment;
import com.modosa.apkinstaller.fragment.MainFragment;
import com.modosa.apkinstaller.util.OpUtil;

import java.util.ArrayList;

/**
 * @author dadaewq
 */
public class MainUiActivity extends AppCompatActivity implements MainFragment.MyListener {

    private final static String CONFIRM_PROMPT = "ConfirmPrompt";
    private static final String TAG_MAINUI = "mainUi";
    private static final String TAG_DPM = "dpm";
    private long exitTime = 0;
    private DevicePolicyManager devicePolicyManager;
    private boolean isMain = true;
    private FragmentManager fragmentManager;
    private SharedPreferences spGetPreferenceManager;
    private AlertDialog alertDialogConfirmPrompt;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fragmentManager = getSupportFragmentManager();

        Fragment fragmentMainUi = fragmentManager.findFragmentByTag(TAG_MAINUI);
        Fragment fragmentDpm = fragmentManager.findFragmentByTag(TAG_DPM);

        if (fragmentMainUi == null && fragmentDpm == null) {
            fragmentManager.beginTransaction().replace(R.id.framelayout, new MainFragment(), TAG_MAINUI).commit();
        } else if (fragmentDpm != null) {
            isMain = false;

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(!isMain);
                actionBar.setTitle(R.string.title_dpmSettings);
            }
        }


        init();
        confirmPrompt();
    }

    private void init() {
        spGetPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
    }

    private void confirmPrompt() {
        if (!spGetPreferenceManager.getBoolean(CONFIRM_PROMPT, false)) {
            showDialogConfirmPrompt();
        }
    }

    private void showMyToast0(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    private void showMyToast0(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_ui, menu);
        return true;
    }

    @Override
    public boolean isDeviceOwner() {
        return devicePolicyManager.isDeviceOwnerApp(getPackageName());
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        super.onPrepareOptionsMenu(menu);
//
//        if (isDeviceOwner()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                menu.findItem(R.id.RebootDevice).setVisible(true);
//            }
//        }
//        return true;
//    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                swtichIsMainFragment(true);
                break;
            case R.id.InstallFromGetContent:
                select();
                break;
            case R.id.Settings:
                Intent settingsIntent = new Intent(Intent.ACTION_VIEW)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setClass(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
//            case R.id.hideIcon:
//                showDialoghideIcon();
//                break;
//            case R.id.clearAllowedList:
//                showDialogclearAllowedList();
//                break;
//            case R.id.clearCache:
//                showDialogclearCache();
//                break;
//            case R.id.RebootDevice:
//                devicePolicyManager.reboot(adminComponentName);
//                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (alertDialogConfirmPrompt != null) {
            alertDialogConfirmPrompt.dismiss();
        }
    }

    private void showDialogConfirmPrompt() {

        View view = View.inflate(this, R.layout.confirm_doublecheckbox, null);

        CheckBox checkBox1 = view.findViewById(R.id.confirm_checkbox1);
        CheckBox checkBox2 = view.findViewById(R.id.confirm_checkbox2);
        checkBox1.setText(R.string.checkbox1_instructions_before_use);
        checkBox2.setText(R.string.checkbox2_instructions_before_use);
        checkBox2.setEnabled(false);
        checkBox1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkBox2.setChecked(false);
            checkBox2.setEnabled(isChecked);
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.title_instructions_before_use)
                .setView(view)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    boolean hasBothConfirm = false;
                    if (checkBox1.isChecked() && checkBox2.isChecked()) {
                        hasBothConfirm = true;
                    }
                    spGetPreferenceManager.edit().putBoolean(CONFIRM_PROMPT, hasBothConfirm).apply();
                });

        alertDialogConfirmPrompt = builder.create();
        OpUtil.showAlertDialog(this, alertDialogConfirmPrompt);
        alertDialogConfirmPrompt.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.rBackground));
        alertDialogConfirmPrompt.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.rBackground));

    }

    @Override
    public void swtichIsMainFragment(boolean isMain) {

        this.isMain = isMain;

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(!isMain);
//        invalidateOptionsMenu();
        if (isMain) {
            actionBar.setTitle(R.string.app_name);
            fragmentManager.beginTransaction().replace(R.id.framelayout, new MainFragment(), TAG_MAINUI).commit();
        } else {
            actionBar.setTitle(R.string.title_dpmSettings);
            fragmentManager.beginTransaction().replace(R.id.framelayout, new DPMSettingsFragment(), TAG_DPM).commit();
        }

    }

//    private void showDialoghideIcon() {
//
//        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
//        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
//        checkBox.setText(R.string.hideIcon);
//
//        ComponentName mainComponentName = new ComponentName(this, "com.modosa.apkinstaller.activity.MainActivity");
//        PackageManager pm = getPackageManager();
//        boolean isEnabled = (pm.getComponentEnabledSetting(mainComponentName) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(mainComponentName) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
//
//        checkBox.setChecked(!isEnabled);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle(R.string.hideIcon)
//                .setMessage(R.string.message_hideIcon)
//                .setView(checkBoxView)
//                .setNeutralButton(android.R.string.no, null)
//                .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.setComponentState(this, mainComponentName,
//                        !checkBox.isChecked()));
//
//        AlertDialog alertDialog = builder.create();
//
//
//        OpUtil.showAlertDialog(this, alertDialog);
//
//    }
//
//    private void showDialogclearAllowedList() {
//
//        View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
//        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
//        checkBox.setText(R.string.checkbox_clearAllowedList);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle(R.string.clearAllowedList)
//                .setMessage(R.string.message_clearAllowedList)
//                .setView(checkBoxView)
//                .setNeutralButton(android.R.string.no, null)
//                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
//                    getSharedPreferences("allowsource", Context.MODE_PRIVATE).edit().clear().apply();
//                });
//
//        AlertDialog alertDialog = builder.create();
//
//
//        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));
//
//        OpUtil.showAlertDialog(this, alertDialog);
//
//        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
//
//    }

    @Override
    public void onBackPressed() {
        if (isMain) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - exitTime) < 2000) {
                super.onBackPressed();
            } else {
                showMyToast0(R.string.tip_exit);
                exitTime = currentTime;
            }
        } else {
            swtichIsMainFragment(true);
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

//    private void showDialogclearCache() {
//        String cachePath = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath();
//        String cacheSize = FileSizeUtil.getAutoFolderOrFileSize(cachePath);
//        if (EMPTY_SIZE.equals(cacheSize)) {
//            showMyToast0(R.string.tip_empty_cache);
//        } else {
//            Log.e("cacheSize", cacheSize);
//            AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                    .setTitle(R.string.clearCache)
//                    .setMessage(String.format(getString(R.string.message_clearCache), cacheSize))
//                    .setNeutralButton(android.R.string.no, null)
//                    .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.deleteDirectory(cachePath));
//
//            AlertDialog alertDialog = builder.create();
//            OpUtil.showAlertDialog(this, alertDialog);
//        }
//    }

    private void startPickFile(int which, ArrayList<ComponentName> componentNameArrayList) {

        Intent intent = new Intent(OpUtil.MODOSA_ACTION_PICK_FILE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("cn.bavelee.pokeinstalleri");
        intent.setComponent(componentNameArrayList.get(which));
        startActivity(intent);

    }

    private void getAvinstaller(ArrayList<ComponentName> list, ComponentName componentName) {
        if (OpUtil.getComponentState(this, componentName)) {
            list.add(componentName);
        }
    }


    private void select() {

        ComponentName[] installerComponentNames = new ComponentName[MainFragment.installerSize];
        ArrayList<ComponentName> componentNameArrayList = new ArrayList<>();
        for (int i = 1; i < MainFragment.installerSize; i++) {
            installerComponentNames[i] = new ComponentName(getPackageName(), getPackageName() + ".activity" + ".Install" + i + "Activity");

            getAvinstaller(componentNameArrayList, installerComponentNames[i]);
        }


        if (componentNameArrayList.size() != 0) {

            String[] items = new String[componentNameArrayList.size()];

            ArrayList<String> namelist = new ArrayList<>();
            for (ComponentName componentName : componentNameArrayList) {
                if (componentName == installerComponentNames[1]) {
                    namelist.add(getString(R.string.name_install1));
                } else if (componentName == installerComponentNames[2]) {
                    namelist.add(getString(R.string.name_install2));
                } else if (componentName == installerComponentNames[3]) {
                    namelist.add(getString(R.string.name_install3));
                } else if (componentName == installerComponentNames[4]) {
                    namelist.add(getString(R.string.name_install4));
                } else if (componentName == installerComponentNames[5]) {
                    namelist.add(getString(R.string.name_install5));
                }
            }

            for (int i = 0; i < namelist.size(); i++) {
                items[i] = namelist.get(i);
            }

            if (items.length == 1) {
                showMyToast0(items[0]);
                startPickFile(0, componentNameArrayList);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setItems(items, (dialog, which) -> {
                    showMyToast0(items[which]);
                    startPickFile(which, componentNameArrayList);
                });
//                AlertDialog
                alertDialogConfirmPrompt = builder.create();

                OpUtil.showAlertDialog(this, alertDialogConfirmPrompt);
            }

        }
    }
}

