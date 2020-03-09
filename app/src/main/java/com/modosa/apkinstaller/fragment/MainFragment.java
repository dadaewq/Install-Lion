package com.modosa.apkinstaller.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.Install1Activity;
import com.modosa.apkinstaller.activity.Install2Activity;
import com.modosa.apkinstaller.activity.Install3Activity;
import com.modosa.apkinstaller.util.shell.ShizukuShell;

import java.util.List;

import moe.shizuku.api.ShizukuApiConstants;
import moe.shizuku.api.ShizukuService;

import static com.modosa.apkinstaller.util.OpUtil.getComponentState;
import static com.modosa.apkinstaller.util.OpUtil.setComponentState;

/**
 * @author dadaewq
 */
public class MainFragment extends PreferenceFragmentCompat {
    private SwitchPreferenceCompat needconfirm;
    private SwitchPreferenceCompat show_notification;
    private SwitchPreferenceCompat enable1;
    private SwitchPreferenceCompat enable2;
    private SwitchPreferenceCompat enable3;
    private SharedPreferences sharedPreferences;
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
    private Preference getOwnerPackageNameAndSDKVersion;
    private Preference getDelegatedScopes;
    private boolean ifsdkge26;
    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ifsdkge26 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        sharedPreferences = getPreferenceManager().getSharedPreferences();
        init();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_install1, rootKey);
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
    public void onResume() {
        super.onResume();
        refreshStatus();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void init() {

        ctInstall1 = new ComponentName(context, Install1Activity.class);
        ctInstall2 = new ComponentName(context, Install2Activity.class);
        ctInstall3 = new ComponentName(context, Install3Activity.class);

        needconfirm = findPreference("needconfirm");
        show_notification = findPreference("show_notification");


        Preference manualAuthorize = getPreferenceScreen().findPreference("manualAuthorize");
        assert manualAuthorize != null;
        manualAuthorize.setOnPreferenceClickListener(preference -> {
            manualuthorize();
            return true;
        });

        //IceBox
        enable1 = getPreferenceManager().findPreference("enable1");
        assert enable1 != null;
        enable1.setOnPreferenceClickListener(preference -> {
            setComponentState(context, ctInstall1, !getComponentState(context, ctInstall1));
            enable1.setChecked(getComponentState(context, ctInstall1));
            return true;
        });
        enable1.setChecked(getComponentState(context, ctInstall1));

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

        avInstall1 = getPreferenceScreen().findPreference("avInstall1");


        //Shizuku
        enable3 = getPreferenceManager().findPreference("enable3");
        assert enable3 != null;
        enable3.setOnPreferenceClickListener(preference -> {
            setComponentState(context, ctInstall3, !getComponentState(context, ctInstall3));
            enable3.setChecked(getComponentState(context, ctInstall3));
            return true;
        });
        enable3.setChecked(getComponentState(context, ctInstall3));
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

        avInstall3 = getPreferenceScreen().findPreference("avInstall3");

        //DSM
        enable2 = getPreferenceManager().findPreference("enable2");
        avInstall2 = getPreferenceScreen().findPreference("avInstall2");
        if (ifsdkge26) {
            enable2.setOnPreferenceClickListener(preference -> {
                setComponentState(context, ctInstall2, !getComponentState(context, ctInstall2));
                enable2.setChecked(getComponentState(context, ctInstall2));
                return true;
            });
            enable2.setChecked(getComponentState(context, ctInstall2));

            getOwnerPackageNameAndSDKVersion = getPreferenceScreen().findPreference("getOwnerPackageNameAndSDKVersion");
            getDelegatedScopes = getPreferenceScreen().findPreference("getDelegatedScopes");
            assert getDelegatedScopes != null;
            getDelegatedScopes.setOnPreferenceClickListener(preference -> {
                try {
                    DSMClient.requestScopes((Activity) context, 0x52, "dsm-delegation-install-uninstall-app");
                } catch (Exception e) {
                    Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

        } else {
            enable2.setEnabled(false);
            enable2.setSummaryOff(R.string.tip_ltsdk26);
            avInstall2.setSummary(R.string.av_no);
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


        ComponentName componentName = new ComponentName(targetPackage, targetPackage + ".applications.Mana4geApplications");
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
                Toast.makeText(context, getString(R.string.av_no) + "\n" + e2, Toast.LENGTH_SHORT).show();
            }
        }

    }


    private void refreshStatus() {
        show_notification.setChecked(sharedPreferences.getBoolean("needconfirm", true));
        needconfirm.setChecked(sharedPreferences.getBoolean("needconfirm", true));

        //IceBox
        enable1.setChecked(getComponentState(context, ctInstall1));

        IceBox.SilentInstallSupport state = IceBox.querySupportSilentInstall(context);

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

        boolean permission0 = (ContextCompat.checkSelfPermission(context, IceBox.SDK_PERMISSION) == PackageManager.PERMISSION_GRANTED);
        icebox_supported.setSummary(status);
        icebox_permission.setSummary(permission0 ? R.string.permission_yes : R.string.permission_no);
        avInstall1.setSummary(R.string.av_no);
        String s_SUPPORTED = "SUPPORTED";
        if (s_SUPPORTED.equals(state + "")) {
            avInstall1.setSummary(R.string.av_ok_installer);
        } else {
            if (IceBox.SilentInstallSupport.PERMISSION_REQUIRED == state) {
                icebox_permission.setSummary(getString(R.string.permission_no) + getString(R.string.click2requestpermission));
            }
            avInstall1.setSummary(getString(R.string.av_no) + status);
        }


        //Shizuku
        enable3.setChecked(getComponentState(context, ctInstall3));

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
            shizuku_service.setSummary(R.string.av_yes);
        } else {
            shizuku_service.setSummary(R.string.av_no);
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
            avInstall3.setSummary(R.string.av_yes);
        } else {
            if (hasPermission) {
                if (isShizukuRunningService) {
                    if (isShizukuExist) {
                        avInstall3.setSummary(getString(R.string.av_no) + getString(R.string.go_shizuku));
                        avInstall3.setOnPreferenceClickListener(preference -> {
                            try {
                                startActivity(launchShizujuIntent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return true;
                        });
                    } else {
                        avInstall3.setSummary(R.string.av_no);
                    }
                } else {
                    avInstall3.setSummary(R.string.av_no);
                }
            } else {
                avInstall3.setSummary(getString(R.string.av_no) + getString(R.string.permission_no));
            }

        }


        if (ifsdkge26) {
            //DSM
            enable2.setChecked(getComponentState(context, ctInstall2));

            String OwnerPkgname = DSMClient.getOwnerPackageName(context);
            getDelegatedScopes.setSummary(" ");
            if (OwnerPkgname != null) {
                getOwnerPackageNameAndSDKVersion.setSummary(OwnerPkgname + " " + DSMClient.getOwnerSDKVersion(context));
                List<String> scopes = DSMClient.getDelegatedScopes(context);
                StringBuilder stringBuilder = new StringBuilder();
                for (String scope : scopes) {
                    stringBuilder.append(scope);
                }
                getDelegatedScopes.setSummary(getString(R.string.permission_no) + getString(R.string.click2requestpermission));
                String s_install = "install";

                if (stringBuilder.toString().contains(s_install)) {
                    getDelegatedScopes.setSummary(R.string.permission_yes);
                    avInstall2.setSummary(R.string.av_ok_installer);
                }
            } else {
                getOwnerPackageNameAndSDKVersion.setSummary(R.string.notexist);
                getDelegatedScopes.setSummary(R.string.permission_no);
                avInstall2.setSummary(R.string.av_no);
            }
        } else {
            if (getComponentState(context, ctInstall2)) {
                setComponentState(context, ctInstall2, false);
            }
            enable2.setChecked(false);
        }

    }


}
