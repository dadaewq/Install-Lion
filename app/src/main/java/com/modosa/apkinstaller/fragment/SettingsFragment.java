package com.modosa.apkinstaller.fragment;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.FileSizeUtil;
import com.modosa.apkinstaller.util.OpUtil;

import java.util.Objects;

/**
 * @author dadaewq
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    public static final String SP_KEY_NIGHT_MODE = "MODE_NIGHT";
    private static final String EMPTY_SIZE = "0B";
    private Context context;
    private SharedPreferences spGetPreferenceManager;

    private AlertDialog alertDialog;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        init();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_setings, rootKey);
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
//    @Override
//    public void onResume() {
//        super.onResume();
//        refreshSwitch();
//    }

    private void findPreferencesAndSetListner() {
        spGetPreferenceManager = getPreferenceManager().getSharedPreferences();

        spGetPreferenceManager = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
//        SwitchPreferenceCompat needconfirm = findPreference("needconfirm");
//        SwitchPreferenceCompat show_notification = findPreference("show_notification");


//        Preference hideIcon = findPreference("hideIcon");
//        Preference clearAllowedList = findPreference("clearAllowedList");
//        Preference clearCache = findPreference("clearCache");
//        Preference manualAuthorize = findPreference("manualAuthorize");


//        hideIcon.setOnPreferenceClickListener(this);
//        clearAllowedList.setOnPreferenceClickListener(this);
//        clearCache.setOnPreferenceClickListener(this);
//        manualAuthorize.setOnPreferenceClickListener(this);

        findPreference("setAppTheme").setOnPreferenceClickListener(this);
        findPreference("hideIcon").setOnPreferenceClickListener(this);
        findPreference("clearAllowedList").setOnPreferenceClickListener(this);
        findPreference("clearCache").setOnPreferenceClickListener(this);
        findPreference("manualAuthorize").setOnPreferenceClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkNeedIgnoreBatteryOptimization()) {
            Preference ignoreBatteryOptimization = findPreference("ignoreBatteryOptimization");
            assert ignoreBatteryOptimization != null;
            ignoreBatteryOptimization.setVisible(true);
            ignoreBatteryOptimization.setOnPreferenceClickListener(this);
        }


    }

    @SuppressLint("ResourceType")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init() {
        findPreferencesAndSetListner();
    }


//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private void refreshSwitch() {
//
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "setAppTheme":
                showDialogSetAppTheme();
                break;
            case "hideIcon":
                showDialogHideIcon();
                break;
            case "clearAllowedList":
                showDialogClearAllowedList();
                break;
            case "clearCache":
                showDialogClearCache();
                break;
            case "manualAuthorize":
                manualuthorize();
                break;
            case "ignoreBatteryOptimization":
                ignoreBatteryOptimization();
                break;
            default:
                break;
        }
        return false;
    }


    private void showDialogSetAppTheme() {

//        MaterialDialog dialog = new MaterialDialog(this, MaterialDialog.getDEFAULT_BEHAVIOR());
//
//        int[] disabledIndices = {1, 3};
//        dialog.title(R.string.title_AppTheme, null);
//        DialogSingleChoiceExtKt.listItemsSingleChoice(dialog, R.array.AppTheme, null, null, 0,
//                true,(materialDialog, index, text) -> {
//                    Toast.makeText(this, "Selected item  " + text + " at index " + index, Toast.LENGTH_SHORT).show();
//                    return null;
//                });


        View view = View.inflate(context, R.layout.app_theme, null);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        int radioButtonId = 520;
        int getNightMode = spGetPreferenceManager.getInt(SP_KEY_NIGHT_MODE, 520);
        switch (getNightMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                radioButtonId = R.id.MODE_NIGHT_NO;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                radioButtonId = R.id.MODE_NIGHT_YES;
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                radioButtonId = R.id.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            default:
        }
        if (getNightMode != 520 && radioButtonId != 520) {
            ((RadioButton) view.findViewById(radioButtonId)).setChecked(true);
        }
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int nightMode = 520;
            switch (checkedId) {
                case R.id.MODE_NIGHT_NO:
                    nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case R.id.MODE_NIGHT_YES:
                    nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                case R.id.MODE_NIGHT_FOLLOW_SYSTEM:
                    nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    break;
                default:
            }
            if (nightMode != 520) {
                spGetPreferenceManager.edit().putInt(SP_KEY_NIGHT_MODE, nightMode).apply();
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
                AppCompatDelegate.setDefaultNightMode(nightMode);
            }

        });


        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(view);

        alertDialog = builder.create();

        OpUtil.showAlertDialog(context, alertDialog);


    }

    private void showDialogHideIcon() {

        View checkBoxView = View.inflate(context, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.hideIcon);

        ComponentName mainComponentName = new ComponentName(context, "com.modosa.apkinstaller.activity.MainActivity");
        PackageManager pm = context.getPackageManager();
        boolean isEnabled = (pm.getComponentEnabledSetting(mainComponentName) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(mainComponentName) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));

        checkBox.setChecked(!isEnabled);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.hideIcon)
                .setMessage(R.string.message_hideIcon)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.setComponentState(context, mainComponentName,
                        !checkBox.isChecked()));

//        AlertDialog
        alertDialog = builder.create();


        OpUtil.showAlertDialog(context, alertDialog);

    }

    private void showDialogClearAllowedList() {

        View checkBoxView = View.inflate(context, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.checkbox_clearAllowedList);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.clearAllowedList)
                .setMessage(R.string.message_clearAllowedList)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> context.getSharedPreferences("allowsource", Context.MODE_PRIVATE).edit().clear().apply());

//        AlertDialog
        alertDialog = builder.create();


        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));

        OpUtil.showAlertDialog(context, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

    }

    private void showDialogClearCache() {
        String cachePath = Objects.requireNonNull(context.getExternalCacheDir()).getAbsolutePath();
        String cacheSize = FileSizeUtil.getAutoFolderOrFileSize(cachePath);
        if (EMPTY_SIZE.equals(cacheSize)) {
            OpUtil.showToast0(context, R.string.tip_empty_cache);
        } else {
            Log.e("cacheSize", cacheSize);
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(R.string.title_clearCache)
                    .setMessage(String.format(getString(R.string.message_clearCache), cacheSize))
                    .setNeutralButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> OpUtil.deleteDirectory(cachePath));

//            AlertDialog
            alertDialog = builder.create();
            OpUtil.showAlertDialog(context, alertDialog);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void manualuthorize() {
        String targetPackage = "com.android.settings";
        Intent intentManageAppPermissions, intentManageAll;
        intentManageAppPermissions = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, context.getPackageName());
//      intentManageAppPermissions.setComponent(new ComponentName("com.android.packageinstaller","com.android.packageinstaller.permission.ui.ManagePermissionsActivity"))

        //MIUI等不能用
//        intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.getPackageName()))
//        ComponentName componentName = new ComponentName(targetPackage, targetPackage + ".applications.InstalledAppDetails");
//        intent2.setComponent(componentName);

//        intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, packageUri);
//
//        intent = new Intent("com.android.settings.APP_OPEN_BY_DEFAULT_SETTINGS", Uri.parse("package:" + context.getPackageName()))


        ComponentName componentName = new ComponentName(targetPackage, targetPackage + ".applications.ManageApplications");
        intentManageAll = new Intent(Intent.ACTION_VIEW)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setComponent(componentName);

        try {
            startActivity(intentManageAppPermissions);
            Log.e("start", "intentManageAppPermissions");
        } catch (Exception e1) {
            e1.printStackTrace();
            Log.e("Wrong intentMAP", e1 + "");
            try {
                startActivity(intentManageAll);
                Log.e("start intent", "intentManageAll");
                Toast.makeText(context, R.string.tip_manualAuthorize, Toast.LENGTH_SHORT).show();
            } catch (Exception e2) {
                e2.printStackTrace();
                OpUtil.showToast0(context, getString(R.string.summary_av_no) + "\n" + e2);
            }
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkNeedIgnoreBatteryOptimization() {
        return !((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(context.getPackageName());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ignoreBatteryOptimization() {
        try {
            startActivity(new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + context.getPackageName())
            ));
        } catch (Exception e) {
            OpUtil.showToast0(context, "" + e);
        }
    }

}
