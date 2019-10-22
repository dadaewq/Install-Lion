package com.modosa.apkinstaller.fragment;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.Install1Activity;
import com.modosa.apkinstaller.activity.Install2Activity;
import com.modosa.apkinstaller.activity.Install3Activity;
import com.modosa.apkinstaller.utils.shell.ShizukuShell;

import java.util.List;
import java.util.Objects;

/**
 * @author dadaewq
 */
public class MainFragment extends PreferenceFragment {
    private final String S_first = "first";
    private final String shizuku_PERMISSION = "moe.shizuku.manager.permission.API_V23";
    private SwitchPreference hideIcon;
    private SwitchPreference needconfirm;
    private SwitchPreference enable1;
    private SwitchPreference enable2;
    private SwitchPreference enable3;
    private SharedPreferences sharedPreferences;
    private ComponentName ctMain;
    private ComponentName ctInstall1;
    private ComponentName ctInstall2;
    private ComponentName ctInstall3;
    private Preference icebox_supported;
    private Preference icebox_permission;
    private Preference shizuku_service;
    private Preference shizuku_permission;
    private Preference avInstall1;
    private Preference avInstall2;
    private Preference avInstall3;
    private Preference getOwnerPackageName;
    private Preference getOwnerSDKVersion;
    private Preference getDelegatedScopes;
    private boolean avDSM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_install1);
        avDSM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        sharedPreferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (sharedPreferences.getBoolean(S_first, true)) {
            editor.putBoolean(S_first, true);
        }
        editor.apply();
        if (avDSM) {
            addPreferencesFromResource(R.xml.pref_install2);
        }
        addPreferencesFromResource(R.xml.pref_install3);
        initialize(avDSM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Objects.requireNonNull(view).setBackgroundResource(android.R.color.white);
        return view;
    }


    private void initialize(Boolean avDSM) {
        ctMain = new ComponentName(this.getActivity(), "com.modosa.apkinstaller.activity.MainActivity");
        ctInstall1 = new ComponentName(this.getActivity(), Install1Activity.class);
        ctInstall2 = new ComponentName(this.getActivity(), Install2Activity.class);
        ctInstall3 = new ComponentName(this.getActivity(), Install3Activity.class);
        if (sharedPreferences.getBoolean(S_first, true)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("first", false);
            editor.putBoolean("notificationManager.areNotificationsEnabled()", false);
            editor.putBoolean("show_notification", false);
            editor.putBoolean("needconfirm", true);
            editor.putBoolean("enable1", true);
            editor.putBoolean("enable2", true);
            editor.putBoolean("enable3", true);
            editor.apply();
            if (!avDSM) {
                PackageManager pm = this.getActivity().getPackageManager();
                pm.setComponentEnabledSetting(ctInstall2,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }

        hideIcon = (SwitchPreference) findPreference("hide_icon");
        hideIcon.setOnPreferenceClickListener(preference -> {
            if (sharedPreferences.getBoolean("hide_icon", false)) {
                showHideIconDialog();
            } else {
                changeState("hide_icon", ctMain, true);
            }
            return true;
        });
        needconfirm = (SwitchPreference) findPreference("needconfirm");
        enable1 = (SwitchPreference) getPreferenceManager().findPreference("enable1");
        Objects.requireNonNull(enable1).setOnPreferenceChangeListener((preference, o) -> {
            changeState("enable1", ctInstall1, !sharedPreferences.getBoolean("enable1", true));
            return true;
        });
        icebox_supported = getPreferenceScreen().findPreference("icebox_supported");
        icebox_permission = getPreferenceScreen().findPreference("icebox_permission");
        avInstall1 = getPreferenceScreen().findPreference("avInstall1");
        avInstall1.setOnPreferenceClickListener(preference -> {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{IceBox.SDK_PERMISSION},
                    0x333);
            return true;
        });
        updateComponentName("enable1");
        if (avDSM) {
            enable2 = (SwitchPreference) getPreferenceManager().findPreference("enable2");
            Objects.requireNonNull(enable2).setOnPreferenceChangeListener((preference, o) -> {
                changeState("enable2", ctInstall2, !sharedPreferences.getBoolean("enable2", true));
                return true;
            });
            getOwnerPackageName = getPreferenceScreen().findPreference("getOwnerPackageName");
            getOwnerSDKVersion = getPreferenceScreen().findPreference("getOwnerSDKVersion");
            getDelegatedScopes = getPreferenceScreen().findPreference("getDelegatedScopes");
            avInstall2 = getPreferenceScreen().findPreference("avInstall2");
            avInstall2.setOnPreferenceClickListener(preference -> {
                DSMClient.requestScopes(getActivity(), 0x52, "dsm-delegation-install-uninstall-app");
                return true;
            });
            updateComponentName("enable2");
        }
        enable3 = (SwitchPreference) getPreferenceManager().findPreference("enable3");
        Objects.requireNonNull(enable3).setOnPreferenceChangeListener((preference, o) -> {
            changeState("enable3", ctInstall3, !sharedPreferences.getBoolean("enable3", true));
            return true;
        });
        shizuku_service = getPreferenceScreen().findPreference("shizuku_service");
        shizuku_permission = getPreferenceScreen().findPreference("shizuku_permission");
        avInstall3 = getPreferenceScreen().findPreference("avInstall3");
        updateComponentName("enable3");
    }

    private void printStatus(boolean avDSM) {
        needconfirm.setChecked(sharedPreferences.getBoolean("needconfirm", true));
        IceBox.SilentInstallSupport state = IceBox.querySupportSilentInstall(getActivity());
        String status;
        switch (state) {
            case SUPPORTED:
                status = "支持";
                break;
            case NOT_INSTALLED:
                status = "未安装冰箱 IceBox";
                break;
            case NOT_DEVICE_OWNER:
                status = "冰箱 IceBox 不是设备管理员";
                break;
            case PERMISSION_REQUIRED:
                status = "未取得权限";
                break;
            case SYSTEM_NOT_SUPPORTED:
                status = "当前系统版本不支持静默安装";
                break;
            case UPDATE_REQUIRED:
                status = "冰箱 IceBox 版本过低";
                break;
            default:
                status = "未知";
        }
        int permission0 = ContextCompat.checkSelfPermission(getActivity(), IceBox.SDK_PERMISSION);
        icebox_supported.setSummary(status);
        icebox_permission.setSummary(permission0 == PackageManager.PERMISSION_GRANTED ? "已授权" : "未授权");
        avInstall1.setSummary(R.string.av_nonono);
        String s_SUPPORTED = "SUPPORTED";
        String s_SR = "PERMISSION_REQUIRED";
        if (s_SUPPORTED.equals(state + "")) {
            avInstall1.setSummary(R.string.av_ok);
        } else if (s_SR.equals(state + "")) {
            avInstall1.setSummary(R.string.av_no);
        }
        if (avDSM) {
            String OwnerPkgname = DSMClient.getOwnerPackageName(getActivity());
            getDelegatedScopes.setSummary(" ");
            if (OwnerPkgname != null) {
                getOwnerPackageName.setSummary(OwnerPkgname);
                getOwnerSDKVersion.setSummary(DSMClient.getOwnerSDKVersion(getActivity()) + "");
                List<String> scopes = DSMClient.getDelegatedScopes(getActivity());
                StringBuilder stringBuilder = new StringBuilder();
                for (String scope : scopes) {
                    stringBuilder.append(scope);
                }
                avInstall2.setSummary(R.string.av_no);
                String s_install = "install";

                if (stringBuilder.toString().contains(s_install)) {
                    getDelegatedScopes.setSummary(stringBuilder.toString());
                    avInstall2.setSummary(R.string.av_ok);
                }
            } else {
                getOwnerPackageName.setSummary(R.string.notexist);
                avInstall2.setSummary(R.string.av_nonono);
            }
        }

        boolean avShizuku_service = false;

        boolean permission1 = ContextCompat.checkSelfPermission(getActivity(), shizuku_PERMISSION) == 0;
        shizuku_permission.setSummary(permission1 ? "已授权" : "未授权");
        if (permission1) {
            avShizuku_service = ShizukuShell.getInstance().isAvailable();
        }
        String status1 = avShizuku_service ? getString(R.string.av_okokok) : getString(R.string.av_nonono);
        shizuku_service.setSummary(status1);
        avInstall3.setOnPreferenceClickListener(preference -> {
            ActivityCompat.requestPermissions(MainFragment.this.getActivity(),
                    new String[]{shizuku_PERMISSION},
                    0x332);
            return true;
        });
        if (avShizuku_service) {
            avInstall3.setSummary(R.string.av_ok);
        } else {
            if (permission1) {
                avInstall3.setSummary(getString(R.string.av_nonono) + getString(R.string.go_shizuku));
                avInstall3.setOnPreferenceClickListener(preference -> {
                    try {
                        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                });
            } else {
                avInstall3.setSummary(R.string.av_no);
            }

        }

    }

    private void showHideIconDialog() {
        new AlertDialog.Builder(getActivity()).setCancelable(false)
                .setTitle(R.string.dialog_title)
                .setMessage("\n" + getString(R.string.hide_tip))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("hide_icon", false);
                    editor.apply();
                    hideIcon.setChecked(false);
                })
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    changeState("hide_icon", ctMain, false);
                    android.os.Process.killProcess(android.os.Process.myPid());
                })
                .show();
    }

    private void changeState(String s, ComponentName c, boolean value) {
        PackageManager pm = this.getActivity().getPackageManager();
        if (value) {
            pm.setComponentEnabledSetting(c,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            pm.setComponentEnabledSetting(c,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
        String s_hide_icon = "hide_icon";
        if (!s_hide_icon.equals(s)) {
            updateComponentName(s);
        }
    }

    private void updateComponentName(String s) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        PackageManager pm = this.getActivity().getPackageManager();
        String s_enable1 = "enable1";
        String s_enable2 = "enable2";
        String s_enable3 = "enable3";
        switch (s) {
            case "enable1":
                boolean isEnabled1 = (pm.getComponentEnabledSetting(ctInstall1) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(ctInstall1) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                if (isEnabled1 != sharedPreferences.getBoolean(s_enable1, true)) {
                    editor.putBoolean("enable1", isEnabled1);
                    editor.apply();
                    enable1.setChecked(isEnabled1);
                }
                break;
            case "enable2":
                boolean isEnabled2 = (pm.getComponentEnabledSetting(ctInstall2) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(ctInstall2) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                if (isEnabled2 != sharedPreferences.getBoolean(s_enable2, true)) {
                    editor.putBoolean("enable2", isEnabled2);
                    editor.commit();
                    enable2.setChecked(isEnabled2);
                }
                break;
            case "enable3":
                boolean isEnabled3 = (pm.getComponentEnabledSetting(ctInstall3) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(ctInstall3) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                if (isEnabled3 != sharedPreferences.getBoolean(s_enable3, true)) {
                    editor.putBoolean("enable3", isEnabled3);
                    editor.commit();
                    enable3.setChecked(isEnabled3);
                }
                break;
            default:
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        printStatus(avDSM);
    }

}
