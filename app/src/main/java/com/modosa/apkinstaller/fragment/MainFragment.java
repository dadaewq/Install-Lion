package com.modosa.apkinstaller.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.OpUtil;
import com.modosa.apkinstaller.util.shell.ShizukuShell;
import com.modosa.apkinstaller.util.shell.SuShell;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import moe.shizuku.api.ShizukuApiConstants;
import moe.shizuku.api.ShizukuService;

import static com.modosa.apkinstaller.util.OpUtil.getComponentState;
import static com.modosa.apkinstaller.util.OpUtil.setComponentState;

/**
 * @author dadaewq
 */
public class MainFragment extends PreferenceFragmentCompat {
    public final static int installerSize = 6;
    private SwitchPreferenceCompat needconfirm;
    private SwitchPreferenceCompat show_notification;
    private SwitchPreferenceCompat[] enableInstallerSwitchPreferenceCompats;
    private SharedPreferences sharedPreferences;
    private ComponentName[] installerComponentNames;
    private Preference icebox_supported;
    private Preference icebox_permission;
    private Preference shizuku_service;
    private Preference shizuku_permission;
    private Preference[] avInstallerPreferences;
    private Preference getOwnerPackageNameAndSDKVersion;
    private Preference getDelegatedScopes;
    private boolean issdkge26;
    private boolean issdklt23;
    private Context context;
    private MyHandler mHandler;
    private String command;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
        issdkge26 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        issdklt23 = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
        sharedPreferences = getPreferenceManager().getSharedPreferences();
        init();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_main_ui, rootKey);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove all Runnable and Message.
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();
//        refreshStatus();

        Executors.newSingleThreadExecutor().execute(() -> {
            Message msg = mHandler.obtainMessage();
            msg.arg2 = 666;
            mHandler.sendMessage(msg);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void init() {

        installerComponentNames = new ComponentName[installerSize];
        enableInstallerSwitchPreferenceCompats = new SwitchPreferenceCompat[installerSize];
        avInstallerPreferences = new Preference[installerSize];
        for (int i = 1; i < installerSize; i++) {
            int finalI = i;

            installerComponentNames[i] = new ComponentName(context.getPackageName(), context.getPackageName() + ".activity" + ".Installer" + i + "Activity");
            enableInstallerSwitchPreferenceCompats[i] = getPreferenceManager().findPreference("enable" + i);
            avInstallerPreferences[i] = getPreferenceManager().findPreference("avInstall" + i);

            enableInstallerSwitchPreferenceCompats[i].setOnPreferenceClickListener(preference -> {
                setComponentState(context, installerComponentNames[finalI], !getComponentState(context, installerComponentNames[finalI]));
                enableInstallerSwitchPreferenceCompats[finalI].setChecked(getComponentState(context, installerComponentNames[finalI]));
                return true;
            });


        }


        needconfirm = findPreference("needconfirm");
        show_notification = findPreference("show_notification");

        Preference manualAuthorize = getPreferenceScreen().findPreference("manualAuthorize");
        assert manualAuthorize != null;
        manualAuthorize.setOnPreferenceClickListener(preference -> {
            manualuthorize();
            return true;
        });


        // 1-IceBox
        icebox_supported = getPreferenceScreen().findPreference("icebox_supported");
        icebox_permission = getPreferenceScreen().findPreference("icebox_permission");
        assert icebox_permission != null;
        icebox_permission.setOnPreferenceClickListener(preference -> {
            try {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{IceBox.SDK_PERMISSION},
                        0x2331);
            } catch (Exception e) {
                Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
            }

            return true;
        });


        // 2-DSM
        getOwnerPackageNameAndSDKVersion = getPreferenceScreen().findPreference("getOwnerPackageNameAndSDKVersion");
        if (issdkge26) {

            getDelegatedScopes = getPreferenceScreen().findPreference("getDelegatedScopes");

        } else {
            enableInstallerSwitchPreferenceCompats[2].setEnabled(false);
            avInstallerPreferences[2].setSummary(getString(R.string.summary_av_no) + getString(R.string.tip_ltsdk26));
        }


        // 3-Shizuku
        shizuku_service = getPreferenceScreen().findPreference("shizuku_service");

        shizuku_permission = getPreferenceScreen().findPreference("shizuku_permission");
        assert shizuku_permission != null;
        shizuku_permission.setOnPreferenceClickListener(preference -> {
            try {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{ShizukuApiConstants.PERMISSION},
                        0x2333);
            } catch (Exception e) {
                Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
            }

            return true;
        });


        // 4-Root
        avInstallerPreferences[4].setOnPreferenceClickListener(preference -> {

            Executors.newSingleThreadExecutor().execute(() -> {
                Message msg = mHandler.obtainMessage();
                if (SuShell.getInstance().isAvailable()) {
                    msg.arg1 = 2;
                } else {
                    msg.arg1 = -1;
                }
                mHandler.sendMessage(msg);
            });
            return true;
        });


        // 5-DPM
        Preference dpm_settings = getPreferenceScreen().findPreference("dpm_settings");

        assert dpm_settings != null;
        dpm_settings.setOnPreferenceClickListener(preference -> {
            if (isDeviceOwner()) {
                ((MyListener) Objects.requireNonNull(getActivity())).swtichIsMainFragment(false);
            } else {
                OpUtil.showToast0(context, R.string.title_not_deviceowner);
            }
            return true;
        });


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
                Toast.makeText(context, getString(R.string.summary_av_no) + "\n" + e2, Toast.LENGTH_SHORT).show();
            }
        }

    }


    private void refreshStatus() {

        Log.e("refreshStatus", "refreshStatus: ");
        show_notification.setChecked(sharedPreferences.getBoolean("show_notification", false));
        needconfirm.setChecked(sharedPreferences.getBoolean("needconfirm", true));

        for (int i = 1; i < installerSize; i++) {
            enableInstallerSwitchPreferenceCompats[i].setChecked(getComponentState(context, installerComponentNames[i]));
        }


        // 1-IceBox
        IceBox.SilentInstallSupport state = IceBox.querySupportSilentInstall(context);

        String status;

        String[] languages = getResources().getStringArray(R.array.querySupportSilentInstall);
        switch (state) {
            case SUPPORTED:
                status = languages[0];
                break;
            case NOT_INSTALLED:
                status = languages[1];
                break;
            case NOT_DEVICE_OWNER:
                status = languages[2];
                break;
            case PERMISSION_REQUIRED:
                status = languages[3];
                break;
            case SYSTEM_NOT_SUPPORTED:
                status = languages[4];
                break;
            case UPDATE_REQUIRED:
                status = languages[5];
                break;
            default:
                status = languages[6];
        }

        boolean permission0 = (ContextCompat.checkSelfPermission(context, IceBox.SDK_PERMISSION) == PackageManager.PERMISSION_GRANTED);
        icebox_supported.setSummary(status);
        icebox_permission.setSummary(permission0 ? R.string.permission_yes : R.string.permission_no);
        avInstallerPreferences[1].setSummary(R.string.summary_av_no);
        String s_SUPPORTED = "SUPPORTED";
        if (s_SUPPORTED.equals(state + "")) {
            avInstallerPreferences[1].setSummary(R.string.summary_av_ok_installer);
        } else {
            if (IceBox.SilentInstallSupport.PERMISSION_REQUIRED == state) {
                icebox_permission.setSummary(getString(R.string.permission_no) + getString(R.string.click2requestpermission));
            }
            avInstallerPreferences[1].setSummary(getString(R.string.summary_av_no) + status);
        }


        // 2-DSM
        int OwnerSDKVersion = DSMClient.getOwnerSDKVersion(context);

        if (OwnerSDKVersion != -1) {
            String OwnerPackageName = DSMClient.getOwnerPackageName(context);
            String OwnerPackageLable = AppInfoUtil.getApplicationLabel(context, OwnerPackageName);

            if (AppInfoUtil.UNINSTALLED.equals(OwnerPackageLable)) {
                OwnerPackageLable = OwnerPackageName;
            }
            getOwnerPackageNameAndSDKVersion.setSummary(OwnerPackageLable + " - " + DSMClient.getOwnerSDKVersion(context));
        } else {
            getOwnerPackageNameAndSDKVersion.setSummary(R.string.notexist);
        }


        if (issdklt23) {
            for (int i = 1; i < installerSize; i++) {
                if (getComponentState(context, installerComponentNames[i])) {
                    setComponentState(context, installerComponentNames[i], false);
                }
                enableInstallerSwitchPreferenceCompats[i].setChecked(false);
            }
        } else {

            if (issdkge26) {

                getDelegatedScopes.setOnPreferenceClickListener(preference -> {
                    try {
                        DSMClient.requestScopes((Activity) context, 0x52, "dsm-delegation-install-uninstall-app");
                    } catch (Exception e) {
                        Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
                if (OwnerSDKVersion != -1) {
                    List<String> scopes = DSMClient.getDelegatedScopes(context);
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String scope : scopes) {
                        stringBuilder.append(scope);
                    }
                    getDelegatedScopes.setSummary(getString(R.string.permission_no) + getString(R.string.click2requestpermission));
                    String s_install = "install";

                    if (stringBuilder.toString().contains(s_install)) {
                        getDelegatedScopes.setSummary(R.string.permission_yes);
                        avInstallerPreferences[2].setSummary(R.string.summary_av_ok_installer);
                    }
                } else {
                    getDelegatedScopes.setSummary(R.string.permission_no);
                    avInstallerPreferences[2].setSummary(R.string.summary_av_no);
                }

            } else {
                if (getComponentState(context, installerComponentNames[2])) {
                    setComponentState(context, installerComponentNames[2], false);
                }
                enableInstallerSwitchPreferenceCompats[2].setChecked(false);
            }

        }


        // 3-Shizuku
        Intent launchShizujuIntent = context.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");

        boolean isShizukuExist = (launchShizujuIntent != null);
        boolean isShizukuRunningService = true;

        try {
            ShizukuService.getVersion();
        } catch (Exception e) {
            Log.e("Exception", e.getClass() + "");
            if (e.getClass() == IllegalStateException.class) {
                isShizukuRunningService = false;
            }
        }


        if (isShizukuRunningService) {
            shizuku_service.setSummary(R.string.summary_av_yes);
        } else {
            shizuku_service.setSummary(R.string.summary_av_no);
        }

        boolean hasPermission = ContextCompat.checkSelfPermission(context, ShizukuApiConstants.PERMISSION) == 0;

        if (hasPermission) {
            shizuku_permission.setSummary(R.string.permission_yes);
        } else {
            if (isShizukuExist) {
                shizuku_permission.setSummary(getString(R.string.permission_no) + getString(R.string.click2requestpermission));
            } else {
                shizuku_permission.setSummary(R.string.permission_no);
            }
        }

        boolean avShizuku = ShizukuShell.getInstance().isAvailable();
        if (avShizuku) {
            avInstallerPreferences[3].setSummary(R.string.summary_av_ok_installer);
        } else {
            if (isShizukuRunningService) {
                if (hasPermission) {
                    avInstallerPreferences[3].setSummary(getString(R.string.summary_av_no) + getString(R.string.unknown));
                } else {
                    if (isShizukuExist) {
                        avInstallerPreferences[3].setOnPreferenceClickListener(preference -> {
                            try {
                                startActivity(launchShizujuIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return true;
                        });
                        avInstallerPreferences[3].setSummary(getString(R.string.summary_av_no) + getString(R.string.summary_go_shizuku));
                    } else {
                        avInstallerPreferences[3].setSummary(getString(R.string.summary_av_no) + getString(R.string.permission_no));
                    }
                }
            } else {
                if (isShizukuExist) {
                    avInstallerPreferences[3].setOnPreferenceClickListener(preference -> {
                        try {
                            startActivity(launchShizujuIntent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return true;
                    });
                    avInstallerPreferences[3].setSummary(getString(R.string.summary_av_no) + getString(R.string.summary_go_shizuku));
                } else {
                    avInstallerPreferences[3].setSummary(getString(R.string.summary_av_no) + getString(R.string.summary_check_shizuku_service));
                }
            }
        }

        // 4-Root
        //暂无需刷新的状态

        // 5-DSM
        if (isDeviceOwner()) {
            avInstallerPreferences[5].setTitle(R.string.title_is_deviceowner);
            avInstallerPreferences[5].setSummary(null);
            avInstallerPreferences[5].setOnPreferenceClickListener(v -> false);

        } else {
            command = OpUtil.getCommand(context);
            avInstallerPreferences[5].setTitle(R.string.title_not_deviceowner);
            avInstallerPreferences[5].setSummary(String.format(getString(R.string.summary_clicktocopycmd), command));
            avInstallerPreferences[5].setOnPreferenceClickListener(v -> {
                copyCommand();
                return false;
            });
        }

    }

    private boolean isDeviceOwner() {
        return ((MyListener) Objects.requireNonNull(getActivity())).isDeviceOwner();
    }

    private void copyCommand() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, command);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
        OpUtil.showToast0(context, command);
    }

    public interface MyListener {
        /**
         * switch Fragment
         */
        void swtichIsMainFragment(boolean isMain);

        boolean isDeviceOwner();
    }

    private static class MyHandler extends Handler {

        private final WeakReference<MainFragment> wrFragment;

        MyHandler(MainFragment fragment) {
            this.wrFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (wrFragment.get() == null) {
                return;
            }
            MainFragment mainFragment = wrFragment.get();

            if (msg.arg2 == 666) {
                mainFragment.refreshStatus();
            }
            switch (msg.arg1) {
                case 2:
                    mainFragment.avInstallerPreferences[4].setTitle(R.string.title_show_root_yes);
                    OpUtil.showToast0(mainFragment.getActivity(), R.string.title_show_root_yes);
                    break;
                case -1:
                    mainFragment.avInstallerPreferences[4].setTitle(R.string.title_show_root_no_click2request);
                    OpUtil.showToast0(mainFragment.getActivity(), R.string.installer_error_root_no_root);
                    break;
                default:

            }
        }
    }
}
