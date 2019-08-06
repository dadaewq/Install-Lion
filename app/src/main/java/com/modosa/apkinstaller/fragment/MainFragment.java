package com.modosa.apkinstaller.fragment;

import android.app.AlertDialog;
import android.content.ComponentName;
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
import com.modosa.apkinstaller.activity.MainActivity;

import java.util.List;
import java.util.Objects;


public class MainFragment extends PreferenceFragment {
    private SwitchPreference hide_icon;
    private SwitchPreference enable1;
    private SwitchPreference enable2;
    private SharedPreferences sharedPreferences;
    private ComponentName comptName;
    private ComponentName install1;
    private ComponentName install2;
    private Preference icebox_supported;
    private Preference icebox_permission;
    private Preference av_install1;
    private Preference getOwnerPackageName;
    private Preference getOwnerSDKVersion;
    private Preference getDelegatedScopes;
    private Preference av_install2;
    private boolean av_DSM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_install1);
        av_DSM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        sharedPreferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (sharedPreferences.getBoolean("first", true)) {
            editor.putBoolean("first", true);
        }
        editor.apply();
        if (av_DSM) {
            addPreferencesFromResource(R.xml.pref_install2);
        }
        initialize(av_DSM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
//        setHasOptionsMenu(true);
        Objects.requireNonNull(view).setBackgroundResource(android.R.color.white);
        return view;
    }
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        menu.add("Menu 1a").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//        menu.add("Menu 1b").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
////        inflater.inflate(R.menu.menu, menu);
//    }

    private void initialize(Boolean av_DSM) {
        comptName = new ComponentName(this.getActivity(), MainActivity.class);
        install1 = new ComponentName(this.getActivity(), Install1Activity.class);
        install2 = new ComponentName(this.getActivity(), Install2Activity.class);
        if (sharedPreferences.getBoolean("first", true)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("first", false);
            editor.putBoolean("hide_icon", false);
            editor.putBoolean("enable1", true);
            editor.putBoolean("enable2", true);
            editor.apply();
            if (!av_DSM) {
                PackageManager pm = this.getActivity().getPackageManager();
                    pm.setComponentEnabledSetting(install2,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
            }
        }

        hide_icon = (SwitchPreference) findPreference("hide_icon");
        hide_icon.setOnPreferenceClickListener(preference -> {
            showRestartDialog();
            return true;
        });

        enable1 = (SwitchPreference) getPreferenceManager().findPreference("enable1");
        Objects.requireNonNull(enable1).setOnPreferenceChangeListener((preference, o) -> {
            changstate("enable1", install1, !sharedPreferences.getBoolean("enable1", true));
            return true;
        });
        icebox_supported = getPreferenceScreen().findPreference("icebox_supported");
        icebox_permission = getPreferenceScreen().findPreference("icebox_permission");
        av_install1 = getPreferenceScreen().findPreference("av_install1");
        av_install1.setOnPreferenceClickListener(preference -> {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{IceBox.SDK_PERMISSION},
                    0x333);
            return true;
        });
        updateComponentName("enable1");
        if (av_DSM) {
            enable2 = (SwitchPreference) getPreferenceManager().findPreference("enable2");
            Objects.requireNonNull(enable2).setOnPreferenceChangeListener((preference, o) -> {
                changstate("enable2", install2, !sharedPreferences.getBoolean("enable2", true));
                return true;
            });
            getOwnerPackageName = getPreferenceScreen().findPreference("getOwnerPackageName");
            getOwnerSDKVersion = getPreferenceScreen().findPreference("getOwnerSDKVersion");
            getDelegatedScopes = getPreferenceScreen().findPreference("getDelegatedScopes");
            av_install2 = getPreferenceScreen().findPreference("av_install2");
            av_install2.setOnPreferenceClickListener(preference -> {
                DSMClient.requestScopes(getActivity(), "dsm-delegation-install-uninstall-app");
                return true;
            });
            updateComponentName("enable2");
        }
    }

    private void printStatus(boolean av_DSM) {
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
        av_install1.setSummary(getString(R.string.av_no));
        if ("SUPPORTED".equals(state + "")) {
            av_install1.setSummary(getString(R.string.av_ok));
        }
        if (av_DSM) {
            getOwnerPackageName.setSummary(DSMClient.getOwnerPackageName(getActivity()));
            getOwnerSDKVersion.setSummary(DSMClient.getOwnerSDKVersion(getActivity()) + "");
            List<String> scopes = DSMClient.getDelegatedScopes(getActivity());
            StringBuilder stringBuilder = new StringBuilder();
            for (String scope : scopes) {
                stringBuilder.append(scope);
            }
            getDelegatedScopes.setSummary(" ");
            av_install2.setSummary(getString(R.string.av_no));
            if (stringBuilder.toString().contains("install")) {
                getDelegatedScopes.setSummary(stringBuilder.toString());
                av_install2.setSummary(getString(R.string.av_ok));
            }
        }
    }

    private void showRestartDialog() {
        new AlertDialog.Builder(getActivity()).setCancelable(false)
                .setTitle(getString(R.string.hide_title))
                .setMessage("\n"+getString(R.string.hide_tip))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("hide_icon", false);
                    editor.apply();
                    hide_icon.setChecked(false);
                })
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    changstate("hide_icon", comptName, false);
                    android.os.Process.killProcess(android.os.Process.myPid());
                })
                .show();
    }

    private void changstate(String s, ComponentName c, boolean value) {
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
        if (!"hide_icon".equals(s)) {
            updateComponentName(s);
        }
    }

    private void updateComponentName(String s) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        PackageManager pm = this.getActivity().getPackageManager();
        switch (s) {
            case "enable1":
                boolean isenabled1 = (pm.getComponentEnabledSetting(install1) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(install1) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                if (isenabled1 != sharedPreferences.getBoolean("enable1", true)) {
                    editor.putBoolean("enable1", isenabled1);
                    editor.apply();
                    enable1.setChecked(isenabled1);
                }
                break;
            case "enable2":
                boolean isenabled2 = (pm.getComponentEnabledSetting(install2) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(install2) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
                if (isenabled2 != sharedPreferences.getBoolean("enable2", true)) {
                    editor.putBoolean("enable2", isenabled2);
                    editor.commit();
                    enable2.setChecked(isenabled2);
                }
                break;
                default:
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        printStatus(av_DSM);
    }

}
