package com.modosa.apkinstaller.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.fragment.MainFragment;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.OpUtil;

import java.io.File;

/**
 * @author dadaewq
 */
public class Install6Activity extends AbstractInstallerActivity {

    @Override
    public void startInstall(String getinstallApkPath) {
        installApkFile = new File(getinstallApkPath);
        boolean needDelete = istemp;
        istemp = false;
        Log.d("Start install", getinstallApkPath + "");

        boolean prepareInstall = false;
        String getCustomInstaller = PreferenceManager.getDefaultSharedPreferences(this).getString(MainFragment.SP_KEY_CUSTOM_INSTALLER, "").trim();
        if (!"".equals(getCustomInstaller)) {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(OpUtil.getMyContentUriForFile(this, installApkFile), "application/vnd.android.package-archive")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (getCustomInstaller.contains("/")) {
                try {
                    String[] names = getCustomInstaller.split("/");
                    if (names[1].length() > 1 && names[1].startsWith(".")) {
                        names[1] = names[0] + names[1];
                    }
                    intent.setComponent(new ComponentName(names[0], names[1]));
                    prepareInstall = true;
                } catch (Exception ignore) {
                }
            } else {
                intent.setPackage(getCustomInstaller);
                prepareInstall = true;
            }

            if (prepareInstall) {
                try {
                    String installerLable = AppInfoUtil.getCustomInstallerLable(this, getCustomInstaller);
                    showMyToast0(String.format(getString(R.string.tip_use_to_install), installerLable, apkinfo[0]));
                    startActivity(intent);
                    needDelete = false;
                } catch (Exception e) {
                    showMyToast1(String.format(getString(R.string.tip_invalid__custom_installer), getCustomInstaller));
                    e.printStackTrace();
                }
            } else {
                showMyToast1(String.format(getString(R.string.tip_invalid__custom_installer), getCustomInstaller));
            }
        } else {
            showMyToast1(R.string.tip_not_set_custom_installer);
        }
        if (needDelete) {
            OpUtil.deleteSingleFile(installApkFile);
        }
        finish();
    }

    @Override
    protected void startUninstall(String uninstallPkgname) {
    }
}
