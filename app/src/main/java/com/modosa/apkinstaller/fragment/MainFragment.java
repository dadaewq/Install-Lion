package com.modosa.apkinstaller.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.modosa.apkinstaller.activity.MainUiActivity;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.OpUtil;
import com.modosa.apkinstaller.util.shell.ShizukuShell;
import com.modosa.apkinstaller.util.shell.SuShell;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import moe.shizuku.api.ShizukuApiConstants;
import moe.shizuku.api.ShizukuService;

import static com.modosa.apkinstaller.util.OpUtil.getEnabledComponentState;
import static com.modosa.apkinstaller.util.OpUtil.setComponentState;

/**
 * @author dadaewq
 */
public class MainFragment extends PreferenceFragmentCompat {
    public final static int INSTALLER_SIZE = 6;
    public static final int RESULT_REFRESH_1 = 5201;
    private static final int REQUEST_CODE_0 = 2330;
    private static final int REQUEST_CODE_1 = 2331;
    private static final int REQUEST_CODE_2 = 2332;
    private final String dsmScopes = "dsm-delegation-install-uninstall-app";
    private SwitchPreferenceCompat[] enableInstallerSwitchPreferenceCompats;
    private ComponentName[] installerComponentNames;
    private Preference icebox_supported;
    private Preference icebox_permission;
    private Preference shizuku_service;
    private Preference shizuku_permission;
    private Preference[] avInstallerPreferences;
    private Preference getOwnerPackageNameAndSDKVersion;
    private Preference getDelegatedScopes;
    private Preference dpm_settings;
    private InstallerPreferenceArrayList[] preferenceArrayLists;
    private boolean issdkge26;
    private boolean issdkge23;
    private Context context;
    private MyHandler mHandler;
    private String command;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MyHandler(this);
        issdkge26 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        issdkge23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        init();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_main_ui, rootKey);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == MainUiActivity.REQUEST_REFRESH && resultCode == RESULT_REFRESH_1) {
            onResume();
        }
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
        context = getActivity();
        Executors.newSingleThreadExecutor().execute(() -> {
            Message msg = mHandler.obtainMessage();
            msg.arg1 = 9;
            mHandler.sendMessage(msg);
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void init() {

        installerComponentNames = new ComponentName[INSTALLER_SIZE];
        enableInstallerSwitchPreferenceCompats = new SwitchPreferenceCompat[INSTALLER_SIZE];
        preferenceArrayLists = new InstallerPreferenceArrayList[INSTALLER_SIZE];
        avInstallerPreferences = new Preference[INSTALLER_SIZE];

        String[] installerClassName = new String[INSTALLER_SIZE];
        String[] keySwitchPreferenceEnable = new String[INSTALLER_SIZE];
        String[] keyPreferenceAvinstall = new String[INSTALLER_SIZE];


        for (int i = 1; i < INSTALLER_SIZE; i++) {
            int finalI = i;

            preferenceArrayLists[i] = new InstallerPreferenceArrayList();
            installerClassName[i] = context.getPackageName() + ".activity" + ".Install" + i + "Activity";
            keySwitchPreferenceEnable[i] = "enable" + i;
            keyPreferenceAvinstall[i] = "avInstall" + i;

            installerComponentNames[i] = new ComponentName(context.getPackageName(), installerClassName[i]);
            enableInstallerSwitchPreferenceCompats[i] = getPreferenceManager().findPreference(keySwitchPreferenceEnable[i]);
            avInstallerPreferences[i] = getPreferenceManager().findPreference(keyPreferenceAvinstall[i]);
            preferenceArrayLists[i].add(avInstallerPreferences[i]);

            enableInstallerSwitchPreferenceCompats[i].setOnPreferenceClickListener(preference -> {
                boolean newState = !getEnabledComponentState(context, installerComponentNames[finalI]);

                refreshInstallerStatus(enableInstallerSwitchPreferenceCompats[finalI]);
                setVisibleInstallerPreference(enableInstallerSwitchPreferenceCompats[finalI], newState);
                enableInstallerSwitchPreferenceCompats[finalI].setChecked(newState);
                setComponentState(context, installerComponentNames[finalI], newState);
                return true;
            });

        }


        // 1-IceBox
        icebox_supported = getPreferenceScreen().findPreference("icebox_supported");
        icebox_permission = getPreferenceScreen().findPreference("icebox_permission");
        preferenceArrayLists[1].add(icebox_supported);
        preferenceArrayLists[1].add(icebox_permission);

        icebox_permission.setOnPreferenceClickListener(preference -> {
            try {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{IceBox.SDK_PERMISSION},
                        REQUEST_CODE_0);
            } catch (Exception e) {
                Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
            }
            return true;
        });


        // 2-DSM
        getOwnerPackageNameAndSDKVersion = getPreferenceScreen().findPreference("getOwnerPackageNameAndSDKVersion");
        getDelegatedScopes = getPreferenceScreen().findPreference("getDelegatedScopes");
        preferenceArrayLists[2].add(getOwnerPackageNameAndSDKVersion);
        preferenceArrayLists[2].add(getDelegatedScopes);

        getDelegatedScopes.setOnPreferenceClickListener(preference -> {
            try {
                DSMClient.requestScopes((Activity) context, REQUEST_CODE_1, dsmScopes);
            } catch (Exception e) {
                Toast.makeText(context, e + "", Toast.LENGTH_SHORT).show();
            }
            return true;
        });


        // 3-Shizuku
        shizuku_service = getPreferenceScreen().findPreference("shizuku_service");
        shizuku_permission = getPreferenceScreen().findPreference("shizuku_permission");
        preferenceArrayLists[3].add(shizuku_service);
        preferenceArrayLists[3].add(shizuku_permission);

        assert shizuku_permission != null;
        shizuku_permission.setOnPreferenceClickListener(preference -> {
            try {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{ShizukuApiConstants.PERMISSION},
                        REQUEST_CODE_2);
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
                    msg.arg1 = 6;
                } else {
                    msg.arg1 = -6;
                }
                mHandler.sendMessage(msg);
            });
            return true;
        });


        // 5-DPM
        dpm_settings = getPreferenceScreen().findPreference("dpm_settings");
        preferenceArrayLists[5].add(dpm_settings);
        assert dpm_settings != null;
        dpm_settings.setOnPreferenceClickListener(preference -> {
            if (isDeviceOwner()) {
                ((MyListener) Objects.requireNonNull(getActivity())).swtichIsMainFragment(false);
            } else {
                OpUtil.showToast0(context, R.string.title_not_deviceowner);
            }
            return true;
        });


        //对Android6.0以下和Android8.0以下的做出提示

        String tipNotSupport;
        if (!issdkge26) {
            tipNotSupport = getString(R.string.summary_av_no) + getString(R.string.tip_ltsdk26);
            avInstallerPreferences[2].setSummary(tipNotSupport);
        }
        if (!issdkge23) {
            tipNotSupport = getString(R.string.summary_av_no) + getString(R.string.tip_ltsdk23);
            avInstallerPreferences[1].setSummary(tipNotSupport);
            avInstallerPreferences[3].setSummary(tipNotSupport);
        }
    }

    private int getIndexOfenableInstallerSwitchPreferenceCompats(SwitchPreferenceCompat enableInstallerSwitchPreferenceCompat) {
        int i;
        for (i = 1; i < INSTALLER_SIZE; i++) {
            if (enableInstallerSwitchPreferenceCompat.equals(enableInstallerSwitchPreferenceCompats[i])) {
                return i;
            }
        }
        return i;
    }

    private void setVisibleInstallerPreference(SwitchPreferenceCompat enableInstallerSwitchPreferenceCompat, boolean enable) {
        int indexOfenableInstallerSwitchPreferenceCompats = getIndexOfenableInstallerSwitchPreferenceCompats(enableInstallerSwitchPreferenceCompat);

        if (enableInstallerSwitchPreferenceCompats[5].equals(enableInstallerSwitchPreferenceCompat) && isDeviceOwner()) {
            dpm_settings.setVisible(true);
            avInstallerPreferences[5].setVisible(false);
            return;
        }
        for (Object preference : preferenceArrayLists[indexOfenableInstallerSwitchPreferenceCompats]) {
            ((Preference) preference).setVisible(enable);
        }

    }

    private void refreshInstallerStatus(SwitchPreferenceCompat enableInstallerSwitchPreferenceCompat) {
        int indexOfenableInstallerSwitchPreferenceCompats = getIndexOfenableInstallerSwitchPreferenceCompats(enableInstallerSwitchPreferenceCompat);


        switch (indexOfenableInstallerSwitchPreferenceCompats) {

            case 1:
                // 1-IceBox
                if (issdkge23) {

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
                    icebox_permission.setSummary(permission0 ? R.string.summary_permission_yes : R.string.summary_permission_no);
                    avInstallerPreferences[1].setSummary(R.string.summary_av_no);
                    if (IceBox.SilentInstallSupport.SUPPORTED.equals(state)) {
                        avInstallerPreferences[1].setSummary(R.string.summary_av_ok_installer);
                    } else {
                        if (IceBox.SilentInstallSupport.PERMISSION_REQUIRED.equals(state)) {
                            icebox_permission.setSummary(getString(R.string.summary_permission_no) + getString(R.string.click2requestpermission));
                        }
                        avInstallerPreferences[1].setSummary(getString(R.string.summary_av_no) + status);
                    }


                }
                break;
            case 2:

                // 2-DSM
                if (issdkge26) {

                    int ownerSdkVersion = DSMClient.getOwnerSDKVersion(context);

                    if (ownerSdkVersion != -1) {
                        String ownerPackageName = DSMClient.getOwnerPackageName(context);
                        String ownerPackageLable = AppInfoUtil.getApplicationLabel(context, ownerPackageName);

                        if (AppInfoUtil.UNINSTALLED.equals(ownerPackageLable)) {
                            ownerPackageLable = ownerPackageName;
                        }
                        getOwnerPackageNameAndSDKVersion.setSummary(ownerPackageLable + " - " + DSMClient.getOwnerSDKVersion(context));
                    } else {
                        getOwnerPackageNameAndSDKVersion.setSummary(R.string.summary_notexist);
                    }


                    if (issdkge26) {

                        getDelegatedScopes.setSummary(R.string.summary_permission_no);
                        avInstallerPreferences[2].setSummary(R.string.summary_av_no);

                        if (ownerSdkVersion != -1) {
                            List<String> scopes = DSMClient.getDelegatedScopes(context);
                            StringBuilder stringBuilder = new StringBuilder();
                            for (String scope : scopes) {
                                stringBuilder.append(scope);
                            }
                            getDelegatedScopes.setSummary(getString(R.string.summary_permission_no) + getString(R.string.click2requestpermission));
                            String sInstall = "install";

                            if (stringBuilder.toString().contains(sInstall)) {
                                getDelegatedScopes.setSummary(R.string.summary_permission_yes);
                                avInstallerPreferences[2].setSummary(R.string.summary_av_ok_installer);
                            }
                        } else {
                            getDelegatedScopes.setSummary(R.string.summary_permission_no);
                            avInstallerPreferences[2].setSummary(R.string.summary_av_no);
                        }

                    }

                }
                break;
            case 3:

                // 3-Shizuku
                if (issdkge23) {


                    Intent launchShizujuIntent = context.getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");

                    boolean isShizukuExist = (launchShizujuIntent != null);
                    boolean isShizukuRunningService = true;

                    try {
                        ShizukuService.getVersion();
                    } catch (Exception e) {
                        //Shizuku的Exception如果为IllegalStateException则说明服务没有在运行
//                        Log.e("Exception", e.getClass() + "");
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
                        shizuku_permission.setSummary(R.string.summary_permission_yes);
                    } else {
                        if (isShizukuExist) {
                            shizuku_permission.setSummary(getString(R.string.summary_permission_no) + getString(R.string.click2requestpermission));
                        } else {
                            shizuku_permission.setSummary(R.string.summary_permission_no);
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
                                    avInstallerPreferences[3].setSummary(getString(R.string.summary_av_no) + getString(R.string.summary_permission_no));
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
                }
                break;
            case 4:

                // 4-Root
                //暂无需刷新的状态

                break;
            case 5:
                // 5-DPM
                if (!issdkge23) {
                    enableInstallerSwitchPreferenceCompats[5].setSummaryOn(R.string.tip_ltsdk23);
                }
                if (!isDeviceOwner()) {
//                    avInstallerPreferences[5].setVisible(false);
//                } else {
                    command = OpUtil.getCommand(context);
                    avInstallerPreferences[5].setSummary(String.format(getString(R.string.summary_clicktocopycmd), command));
                    avInstallerPreferences[5].setOnPreferenceClickListener(v -> {
                        copyCommand();
                        return true;
                    });
                }

                break;
            default:

        }
    }

    private void refreshStatus() {

        for (int i = 1; i < INSTALLER_SIZE; i++) {
            boolean enable = getEnabledComponentState(context, installerComponentNames[i]);
            setVisibleInstallerPreference(enableInstallerSwitchPreferenceCompats[i], enable);

            if (enable) {
                refreshInstallerStatus(enableInstallerSwitchPreferenceCompats[i]);
            }

            enableInstallerSwitchPreferenceCompats[i].setChecked(enable);
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


            switch (msg.arg1) {
                case -6:
                    mainFragment.avInstallerPreferences[4].setTitle(R.string.installer_error_root_no_root);
                    OpUtil.showToast0(mainFragment.context, R.string.installer_error_root_no_root);

                    break;
                case 6:
                    mainFragment.avInstallerPreferences[4].setTitle(R.string.title_show_root_yes);
                    OpUtil.showToast0(mainFragment.context, R.string.title_show_root_yes);

                    break;
                case 9:
                    mainFragment.refreshStatus();
                default:

            }
        }
    }

    private static class InstallerPreferenceArrayList extends ArrayList<Preference> {
    }
}
