package com.modosa.apkinstaller.fragment;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.AboutActivity;
import com.modosa.apkinstaller.activity.MainUiActivity;
import com.modosa.apkinstaller.activity.ManageAllowSourceActivity;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.FileSizeUtil;
import com.modosa.apkinstaller.util.OpUtil;
import com.modosa.apkinstaller.util.ResultUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author dadaewq
 */
@SuppressWarnings("ConstantConditions")
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    public static final String SP_KEY_NIGHT_MODE = "MODE_NIGHT";
    public static final String SP_KEY_ANOTHER_INSTALLER_NAME = "anotherInstallerName";
    public static final String SP_KEY_DISABLE_BUG_REPORT = "disableBugReport";
    public static final String SP_KEY_EASTER_EGG = "Easter Egg";
    public static final String SP_KEY_ENABLE_ANOTHER_INSTALLER = "enableAnotherInstaller";
    private Context context;
    private SharedPreferences spGetPreferenceManager;
    private MyHandler mHandler;
    private AlertDialog alertDialog;
    private String cachePath;
    private String cacheSize;
    private Preference clearCache;
    private Preference ignoreBatteryOptimization;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
        init();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_setings, rootKey);
    }

    private void findPreferencesAndSetListner() {
        spGetPreferenceManager = getPreferenceManager().getSharedPreferences();

        findPreference("installWithAnotherAfterFail").setOnPreferenceClickListener(this);
        findPreference("setAppTheme").setOnPreferenceClickListener(this);
        findPreference("hideIcon").setOnPreferenceClickListener(this);
        findPreference("manageAllowedList").setOnPreferenceClickListener(this);
        findPreference("uninstallApp").setOnPreferenceClickListener(this);
        findPreference("bugReport").setOnPreferenceClickListener(this);
        findPreference("instructions_before_use").setOnPreferenceClickListener(this);
        findPreference("help").setOnPreferenceClickListener(this);
        findPreference("about").setOnPreferenceClickListener(this);

        clearCache = findPreference("clearCache");
        assert clearCache != null;
        clearCache.setOnPreferenceClickListener(this);
        cachePath = Objects.requireNonNull(context.getExternalCacheDir()).getAbsolutePath();

        Preference manualAuthorize = findPreference("manualAuthorize");
        assert manualAuthorize != null;
        manualAuthorize.setOnPreferenceClickListener(this);
        if (ResultUtil.isMiui()) {
            manualAuthorize.setSummary(R.string.summary_manualAuthorize);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkNeedIgnoreBatteryOptimization()) {
            ignoreBatteryOptimization = findPreference("ignoreBatteryOptimization");
            assert ignoreBatteryOptimization != null;
            ignoreBatteryOptimization.setVisible(true);
            ignoreBatteryOptimization.setOnPreferenceClickListener(this);
        }

    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == MainUiActivity.REQUEST_REFRESH && resultCode == Activity.RESULT_OK) {
            ignoreBatteryOptimization.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        context = getActivity();
        Executors.newSingleThreadExecutor().execute(() -> {
            Message msg = mHandler.obtainMessage();
            msg.arg1 = 9;
            mHandler.sendMessage(msg);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove all Runnable and Message.
        mHandler.removeCallbacksAndMessages(null);
    }

    private void init() {
        findPreferencesAndSetListner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {

            case "installWithAnotherAfterFail":
                showDialogInstallWithAnotherAfterFail();
                break;
            case "setAppTheme":
                showDialogSetAppTheme();
                break;
            case "hideIcon":
                showDialogHideIcon();
                break;
            case "manageAllowedList":

                Intent settingsIntent = new Intent(Intent.ACTION_VIEW)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setClass(context, ManageAllowSourceActivity.class);
                try {
                    startActivity(settingsIntent);
                } catch (Exception e) {
                    OpUtil.showToast0(context, "" + e);
                }

                break;
            case "clearCache":
                Executors.newSingleThreadExecutor().execute(() -> {
                    Message msg = mHandler.obtainMessage();
                    msg.arg1 = 6;
                    mHandler.sendMessage(msg);
                });
                break;
            case "uninstallApp":
                uninstallApp();
                break;
            case "manualAuthorize":
                manualuthorize();
                break;
            case "bugReport":
                showDialogBugReport();
                break;
            case "ignoreBatteryOptimization":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ignoreBatteryOptimization();
                }
                break;
            case "instructions_before_use":
                alertDialog = OpUtil.createDialogConfirmPrompt(context);
                OpUtil.showDialogConfirmPrompt(context, alertDialog);
                break;
            case "help":
                OpUtil.launchCustomTabsUrl(context, "https://dadaewq.gitee.io/tutorials/install-lion.html");
                break;
            case "about":
                OpUtil.startMyClass(context, AboutActivity.class);
                break;
            default:
        }
        return true;
    }

    private void showDialogInstallWithAnotherAfterFail() {
        View view = View.inflate(context, R.layout.install_by_another, null);
        CheckBox checkBox = view.findViewById(R.id.confirm_checkbox);
        EditText editText = view.findViewById(R.id.editText);

        editText.setText(spGetPreferenceManager.getString(SP_KEY_ANOTHER_INSTALLER_NAME, ""));
        boolean enable = spGetPreferenceManager.getBoolean(SP_KEY_ENABLE_ANOTHER_INSTALLER, false);
        checkBox.setChecked(enable);


        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_installWithAnotherAfterFail)
                .setView(view)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    SharedPreferences.Editor editor = spGetPreferenceManager.edit();
                    editor.putBoolean(SP_KEY_ENABLE_ANOTHER_INSTALLER, checkBox.isChecked());
                    if (checkBox.isChecked()) {
                        ((SwitchPreferenceCompat) Objects.requireNonNull(findPreference("show_notification"))).setChecked(true);
                    }
                    String packageName = editText.getText().toString().replace(" ", "");
                    editor.putString(SP_KEY_ANOTHER_INSTALLER_NAME, packageName);
                    if (!"".equals(packageName)) {
                        OpUtil.showToast0(context, AppInfoUtil.getCustomInstallerLable(context, packageName));
                    }
                    editor.apply();
                });

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);

    }

    private void showDialogSetAppTheme() {

        View view = View.inflate(context, R.layout.app_theme, null);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        int radioButtonId = 0;
        int getNightMode = spGetPreferenceManager.getInt(SP_KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);
        AtomicBoolean isValid = new AtomicBoolean(true);
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
                isValid.set(false);
        }
        if (isValid.get()) {
            ((RadioButton) view.findViewById(radioButtonId)).setChecked(true);
        }
        isValid.set(true);
        RadioButton radioButton = view.findViewById(R.id.MODE_NIGHT_FOLLOW_SYSTEM);
        radioButton.setOnLongClickListener(view1 -> {
            spGetPreferenceManager.edit().putBoolean(SP_KEY_EASTER_EGG, !spGetPreferenceManager.getBoolean(SP_KEY_EASTER_EGG, false)).apply();
            OpUtil.showToast0(context, SP_KEY_EASTER_EGG);
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            return true;
        });
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int nightMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED;
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
                    isValid.set(false);
            }

            if (isValid.get()) {
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

        View checkBoxView = View.inflate(context, R.layout.hideicon_checkbox, null);
        CheckBox checkBox1 = checkBoxView.findViewById(R.id.confirm_checkbox1);
        CheckBox checkBox2 = checkBoxView.findViewById(R.id.confirm_checkbox2);
        checkBox1.setText(R.string.hideIcon);
        checkBox2.setText(R.string.hideIconByTranslucent);

        ComponentName launchMainUiComponentName1 = new ComponentName(context, MainUiActivity.class.getName().replace(MainUiActivity.class.getSimpleName(), "MainActivity"));
        ComponentName launchMainUiComponentName2 = new ComponentName(context, MainUiActivity.class.getName().replace(MainUiActivity.class.getSimpleName(), "TransparentActivity"));
        PackageManager pm = context.getPackageManager();

        boolean isDisabledMain = !OpUtil.getEnabledComponentState(context, launchMainUiComponentName1);
        boolean isEnabledTranslucent = pm.getComponentEnabledSetting(launchMainUiComponentName2) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        checkBox1.setChecked(isDisabledMain);
        checkBox2.setChecked(isEnabledTranslucent);
        if (!isDisabledMain) {
            checkBox2.setEnabled(false);
        }

        checkBox1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkBox2.setEnabled(isChecked);
            if (!isChecked) {
                checkBox2.setChecked(false);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.hideIcon)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    OpUtil.setComponentState(context, launchMainUiComponentName1,
                            !checkBox1.isChecked());

                    OpUtil.setComponentState(context, launchMainUiComponentName2,
                            checkBox2.isChecked());
                });

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);
    }

    private void showDialogUninstall(String item, ComponentName componentName) {

        EditText editText = new EditText(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage(item + "\n" + context.getString(R.string.message_uninstallPackageName))
                .setView(editText)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null);
        alertDialog = builder.create();

        OpUtil.showAlertDialog(context, alertDialog);

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            String packageName = editText.getText().toString().trim();
            if (!"".equals(packageName)) {
                Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + packageName))
                        .setComponent(componentName);
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    OpUtil.showToast1(context, "" + e);
                }
            }
        });
    }

    private void uninstallApp() {
        OpUtil.pickFileToInstall(context, new OpUtil.PickInstaller() {
            @Override
            public void startPicked(String item, ComponentName componentName) {
                showDialogUninstall(item, componentName);
            }

            @Override
            public void showPickDialog(Context context, String[] items, ArrayList<ComponentName> componentNameArrayList) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setTitle(R.string.title_pickUninstaller)
                        .setItems(items, (dialog, which) -> showDialogUninstall(items[which], componentNameArrayList.get(which)));

                alertDialog = builder.create();

                OpUtil.showAlertDialog(context, alertDialog);
            }
        });
    }

    private void manualuthorize() {
        String targetPackage = "com.android.settings";
        Intent intentManageAppPermissions, intentManageAll;
        intentManageAppPermissions = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Intent.EXTRA_PACKAGE_NAME : "android.intent.extra.PACKAGE_NAME", context.getPackageName());
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

    private void showDialogBugReport() {

        View checkBoxView = View.inflate(context, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.checkbox_bugReport);
        checkBox.setChecked(spGetPreferenceManager.getBoolean(SP_KEY_DISABLE_BUG_REPORT, false));
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_bugReport)
                .setMessage(R.string.message_bugReport)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> spGetPreferenceManager.edit().putBoolean(SP_KEY_DISABLE_BUG_REPORT, checkBox.isChecked()).apply());


        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);
    }


    @SuppressLint("BatteryLife")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void ignoreBatteryOptimization() {
        try {
            startActivityForResult(new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + context.getPackageName())
            ), MainUiActivity.REQUEST_REFRESH);
        } catch (Exception e) {
            OpUtil.showToast0(context, "" + e);
        }
    }


    private static class MyHandler extends Handler {

        private final WeakReference<SettingsFragment> wrFragment;

        MyHandler(SettingsFragment fragment) {
            this.wrFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (wrFragment.get() == null) {
                return;
            }
            SettingsFragment settingsFragment = wrFragment.get();


            settingsFragment.cacheSize = FileSizeUtil.getAutoFolderOrFileSize(settingsFragment.cachePath);

            switch (msg.arg1) {
                case 9:
                    settingsFragment.clearCache.setSummary(String.format(settingsFragment.getString(R.string.summary_clearCache), settingsFragment.cacheSize));
                    break;
                case 6:
                    OpUtil.deleteDirectory(settingsFragment.cachePath);
                    settingsFragment.clearCache.setSummary(String.format(settingsFragment.getString(R.string.summary_clearCache), FileSizeUtil.getAutoFolderOrFileSize(settingsFragment.cachePath)));
                    OpUtil.showToast0(settingsFragment.context, R.string.tip_success_clear_cache);
                    break;
                default:

            }
        }
    }
}
