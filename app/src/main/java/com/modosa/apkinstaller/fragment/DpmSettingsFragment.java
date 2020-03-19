package com.modosa.apkinstaller.fragment;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.MainUiActivity;
import com.modosa.apkinstaller.receiver.AdminReceiver;
import com.modosa.apkinstaller.util.OpUtil;

import java.util.List;

import static com.modosa.apkinstaller.util.OpUtil.showToast0;

/**
 * @author dadaewq
 */
public class DpmSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    public static final int RESULT_REFRESH_2 = 5202;
    private final String sp_key_orgName = "orgName";
    private final String sp_key_confirmWarning = "confirmWarning";
    private final String[] userRestrictionsKeys = {UserManager.DISALLOW_INSTALL_APPS, UserManager.DISALLOW_UNINSTALL_APPS, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES};
    private SwitchPreferenceCompat[] switchPreferences;
    private SwitchPreferenceCompat enableBackupService;
    private SharedPreferences spGetPreferenceManager;
    private boolean isOpUserRestrictionSuccess = false;
    private DevicePolicyManager devicePolicyManager;
    private UserManager userManager;
    private ComponentName adminComponentName;
    private ComponentName[] allMatchComponentName;
    private CharSequence[] allMatchAppInfo;
    private ComponentName defaultApp;
    private Context context;
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
        setPreferencesFromResource(R.xml.pref_dpm_settings, rootKey);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onResume() {
        super.onResume();
        refreshSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == MainUiActivity.REQUEST_REFRESH && resultCode == RESULT_REFRESH_2) {
            onResume();
        }
    }

    private void findPreferencesAndSetListner() {
        spGetPreferenceManager = getPreferenceManager().getSharedPreferences();

        switchPreferences = new SwitchPreferenceCompat[userRestrictionsKeys.length];
        switchPreferences[0] = findPreference("DISALLOW_INSTALL_APPS");
        switchPreferences[1] = findPreference("DISALLOW_UNINSTALL_APPS");
        switchPreferences[2] = findPreference("DISALLOW_INSTALL_UNKNOWN_SOURCES");

        Preference setOrganizationName = findPreference("SetOrganizationName");
        Preference setLockScreenInfo = findPreference("SetLockScreenInfo");
        enableBackupService = findPreference("EnableBackupService");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            enableBackupService.setEnabled(false);
            setOrganizationName.setEnabled(false);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            setLockScreenInfo.setEnabled(false);
        }

        Preference lockDefaultLauncher = findPreference("LockDefaultLauncher");
        Preference lockDefaultPackageInstaller = findPreference("LockDefaultPackageInstaller");

        Preference setUserRestrictions = findPreference("SetUserRestrictions");

        Preference deactivateDeviceOwner = findPreference("DeactivateDeviceOwner");

        setLockScreenInfo.setOnPreferenceClickListener(this);
        setOrganizationName.setOnPreferenceClickListener(this);
        lockDefaultLauncher.setOnPreferenceClickListener(this);
        lockDefaultPackageInstaller.setOnPreferenceClickListener(this);
        setUserRestrictions.setOnPreferenceClickListener(this);
        deactivateDeviceOwner.setOnPreferenceClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Preference rebootDevice = findPreference("RebootDevice");
            assert rebootDevice != null;
            rebootDevice.setVisible(true);
            rebootDevice.setOnPreferenceClickListener(this);
        }

    }

    private boolean isDeviceOwner() {
        return devicePolicyManager.isDeviceOwnerApp(context.getPackageName());
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void init() {
        findPreferencesAndSetListner();

        adminComponentName = AdminReceiver.getComponentName(context);

        userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

//        int i=0;
//        for (String key : userManager.getUserRestrictions().keySet()) {
//            i++;
//            Log.e("Bundle " + i, key + "");
//        }

        refreshSwitch();
        if (isDeviceOwner()) {
            for (int i = 0; i < userRestrictionsKeys.length; i++) {
                int finalI = i;

                switchPreferences[i].setOnPreferenceClickListener(v -> {
                    boolean is = switchPreferences[finalI].isChecked();
                    switchPreferences[finalI].setChecked(is);
                    opUserRestrictionFromSwitch(userRestrictionsKeys[finalI], is);
                    return true;
                });
            }

            enableBackupService.setOnPreferenceClickListener(v -> {
                setEnableBackupServiceEnabled(enableBackupService.isChecked());
                return true;
            });

        }
    }

    private ComponentName getDefaultApplication(Intent intent) {

        final ResolveInfo res = context.getPackageManager().resolveActivity(intent, 0);

        if (res != null && res.activityInfo != null && !"android".equals(res.activityInfo.packageName)) {
            return new ComponentName(res.activityInfo.packageName, res.activityInfo.name);
        }
        return null;
    }

    private void queryIntentActivities(Intent intent) {

        defaultApp = getDefaultApplication(intent);

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);
        int size = list.size();
        allMatchComponentName = new ComponentName[size];
        allMatchAppInfo = new CharSequence[size];


        for (int i = 0; i < size; i++) {
            String packageName = list.get(i).activityInfo.packageName;
            String activityName = list.get(i).activityInfo.name;
            allMatchComponentName[i] = new ComponentName(packageName, activityName);
            allMatchAppInfo[i] = list.get(i).activityInfo.loadLabel(packageManager) + "(" + packageName + ")";

            if (allMatchComponentName[i].equals(defaultApp)) {
                allMatchAppInfo[i] = new StringBuilder("* ").append(allMatchAppInfo[i]);
            }
        }
    }

    private void addUserRestrictions(String key) {
        devicePolicyManager.addUserRestriction(adminComponentName, key);
    }

    private void clearUserRestrictions(String key) {
        devicePolicyManager.clearUserRestriction(adminComponentName, key);
    }

    private boolean checkUserRestriction(String key) {
        return userManager.hasUserRestriction(key);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isEnableBackupServiceEnabled() {
        return devicePolicyManager.isBackupServiceEnabled(adminComponentName);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setEnableBackupServiceEnabled(boolean isEnable) {
        devicePolicyManager.setBackupServiceEnabled(adminComponentName, isEnable);
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showDialogSetOrganizationName() {
        String getOrgName = spGetPreferenceManager.getString(sp_key_orgName, "");
        devicePolicyManager.setOrganizationName(adminComponentName, getOrgName);
        final EditText editOraName = new EditText(context);
        editOraName.setText(getOrgName);
        SharedPreferences.Editor editorGetPreferenceManager = spGetPreferenceManager.edit();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_dialog_SetOrganizationName)
                .setView(editOraName)
                .setNeutralButton(R.string.close, null)
                .setNegativeButton(R.string.clear, (dialog, which) -> {
                    editorGetPreferenceManager.putString(sp_key_orgName, null).apply();
                    devicePolicyManager.setOrganizationName(adminComponentName, null);
                })
                .setPositiveButton(R.string.bt_set, (dialog, which) -> {
                    String getEditName = editOraName.getText().toString() + "";
                    editorGetPreferenceManager.putString(sp_key_orgName, getEditName).apply();
                    devicePolicyManager.setOrganizationName(adminComponentName, getEditName);
                    OpUtil.showToast0(context, getEditName);
                });

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showDialogSetLockScreenInfo() {

        final EditText editInfo = new EditText(context);
        CharSequence getLockScreenInfo = devicePolicyManager.getDeviceOwnerLockScreenInfo();
        if (getLockScreenInfo == null) {
            editInfo.setHint("");
        } else {
            editInfo.setText(getLockScreenInfo);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_dialog_SetLockScreenInfo)
                .setView(editInfo)
                .setNeutralButton(R.string.close, null)
                .setNegativeButton(R.string.clear, (dialog, which) -> devicePolicyManager.setDeviceOwnerLockScreenInfo(adminComponentName, null))
                .setPositiveButton(R.string.bt_set, (dialog, which) -> devicePolicyManager.setDeviceOwnerLockScreenInfo(adminComponentName, editInfo.getText().toString()));

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);
    }

    private void showWarningLockDefaultApplication(String lockpreferenceKey) {
        View checkBoxView = View.inflate(context, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.checkbox_lockdefault);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_lockdefault)
                .setMessage(R.string.message_lockdefault)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    spGetPreferenceManager.edit()
                            .putBoolean(sp_key_confirmWarning, true)
                            .apply();
                    showDialogLockDefaultApplication(lockpreferenceKey);
                });

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void showDialogLockDefaultApplication(String preferenceKey) {
        Intent intent;
        IntentFilter filter;
        int titleid;
        switch (preferenceKey) {
            case "LockDefaultLauncher":
                intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .addCategory(Intent.CATEGORY_DEFAULT);

                filter = new IntentFilter(Intent.ACTION_MAIN);
                filter.addCategory(Intent.CATEGORY_HOME);
                filter.addCategory(Intent.CATEGORY_DEFAULT);

                titleid = R.string.title_LockDefaultLauncher;
                break;
            case "LockDefaultPackageInstaller":
                intent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse("content://test/0/test.apk"), "application/vnd.android.package-archive");

                filter = new IntentFilter(Intent.ACTION_VIEW);
                filter.addCategory(Intent.CATEGORY_DEFAULT);
                filter.addDataScheme(ContentResolver.SCHEME_CONTENT);
                try {
                    filter.addDataType("application/vnd.android.package-archive");
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    e.printStackTrace();
                }

                titleid = R.string.title_dialog_LockDefaultPackageInstaller;
                break;
            default:
                return;
        }
        queryIntentActivities(intent);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(titleid)
                .setItems(allMatchAppInfo, (dialog, which) -> {
                    if (defaultApp != null) {
                        devicePolicyManager.clearPackagePersistentPreferredActivities(adminComponentName, defaultApp.getPackageName());
                    }
                    devicePolicyManager.addPersistentPreferredActivity(adminComponentName, filter, allMatchComponentName[which]);
                    showToast0(context, allMatchAppInfo[which].toString());
                })
                .setNeutralButton(R.string.close, null)
                .setPositiveButton(R.string.clear_lock, (dialog, which) -> {
                    if (defaultApp != null) {
                        devicePolicyManager.clearPackagePersistentPreferredActivities(adminComponentName, defaultApp.getPackageName());
                    }
                });

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showDialogSetUserRestrictions() {

        final EditText keyOfRestriction = new EditText(context);
        keyOfRestriction.setHint(R.string.hint_restrictionkey);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_SetUserRestrictions)
                .setView(keyOfRestriction)
                .setNeutralButton(R.string.close, null)
                .setNegativeButton(R.string.clear, null)
                .setPositiveButton(R.string.bt_add, null);

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> opUserRestrictionFromDialog(keyOfRestriction.getText().toString().trim(), AlertDialog.BUTTON_POSITIVE));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> opUserRestrictionFromDialog(keyOfRestriction.getText().toString().trim(), AlertDialog.BUTTON_NEGATIVE));
        alertDialog.setOnDismissListener(dialog -> {
            if (isOpUserRestrictionSuccess) {
                isOpUserRestrictionSuccess = false;
                refreshSwitch();
            }
        });
    }

    private void showDialogDeactivateDeviceOwner() {

        View checkBoxView = View.inflate(context, R.layout.confirm_checkbox, null);
        CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
        checkBox.setText(R.string.checkbox_deactivate);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.title_deactivate_deviceowner)
                .setMessage(R.string.message_deactivate)
                .setView(checkBoxView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (clearDeviceOwner()) {
                        spGetPreferenceManager.edit().putBoolean(sp_key_confirmWarning, false).apply();
                        showToast0(context, R.string.tip_success_deactivate);
                        ((MainFragment.MyListener) getActivity()).swtichIsMainFragment(true);
                    } else {
                        showToast0(context, R.string.tip_failed_deactivate);
                    }
                });

        alertDialog = builder.create();
        OpUtil.showAlertDialog(context, alertDialog);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void refreshSwitch() {
        for (int i = 0; i < userRestrictionsKeys.length; i++) {
            switchPreferences[i].setChecked(checkUserRestriction(userRestrictionsKeys[i]));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enableBackupService.setChecked(isEnableBackupServiceEnabled());
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "SetLockScreenInfo":
                showDialogSetLockScreenInfo();
                break;
            case "SetOrganizationName":
                showDialogSetOrganizationName();
                break;
            case "LockDefaultLauncher":
            case "LockDefaultPackageInstaller":
                if (spGetPreferenceManager.getBoolean(sp_key_confirmWarning, false)) {
                    showDialogLockDefaultApplication(preference.getKey());
                } else {
                    showWarningLockDefaultApplication(preference.getKey());
                }
                break;
            case "SetUserRestrictions":
                showDialogSetUserRestrictions();
                break;
            case "DeactivateDeviceOwner":
                showDialogDeactivateDeviceOwner();
                break;
            case "RebootDevice":
                devicePolicyManager.reboot(adminComponentName);
                break;
            default:
                break;
        }
        return true;
    }

    private void opUserRestrictionFromSwitch(String key, boolean isAdd) {
        if (isAdd) {
            addUserRestrictions(key);
        } else {
            clearUserRestrictions(key);
        }
    }

    private void opUserRestrictionFromDialog(String key, int whichButton) {
        if ("".equals(key)) {
            showToast0(context, R.string.tip_empty_key);
        } else {
            isOpUserRestrictionSuccess = false;

            try {
                if (whichButton == AlertDialog.BUTTON_POSITIVE) {
                    addUserRestrictions(key);
                    if (checkUserRestriction(key)) {
                        isOpUserRestrictionSuccess = true;
                    }
                } else if (whichButton == AlertDialog.BUTTON_NEGATIVE) {
                    clearUserRestrictions(key);
                    if (!checkUserRestriction(key)) {
                        isOpUserRestrictionSuccess = true;
                    }
                }
            } catch (Exception ignore) {
                isOpUserRestrictionSuccess = false;
            }

            Log.e(key, "" + checkUserRestriction(key));

            showToast0(context, isOpUserRestrictionSuccess ? "Success" : "Fail");
        }
    }

    private boolean clearDeviceOwner() {
        if (isDeviceOwner()) {
            devicePolicyManager.clearDeviceOwnerApp(context.getPackageName());
            return !isDeviceOwner();
        } else {
            return false;
        }
    }


}
