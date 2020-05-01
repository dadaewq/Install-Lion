package com.modosa.apkinstaller.activity;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

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

    private boolean needDelete = false;


    @Override
    public void startInstall(String getinstallApkPath) {
        startOperate(true, getinstallApkPath);
    }

    @Override
    protected void startUninstall(String uninstallPkgname) {
        startOperate(false, uninstallPkgname);
    }


    private void startOperate(boolean isInstall, String param) {

        if (isInstall) {
            installApkFile = new File(param);
            needDelete = istemp;
            istemp = false;
            Log.d("Start install", param + "");
        } else {
            Log.d("Start uninstall", param + "");
        }

        boolean prepareOperate;
        String getCustomInstaller = PreferenceManager.getDefaultSharedPreferences(this).getString(MainFragment.SP_KEY_CUSTOM_INSTALLER, "").trim();
        if (!"".equals(getCustomInstaller)) {
            Intent startIntent;
            if (isInstall) {
                startIntent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(OpUtil.getMyContentUriForFile(this, installApkFile), "application/vnd.android.package-archive")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                startIntent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + param))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            Pair<Boolean, Intent> pair = OpUtil.getStartIntentPair(getCustomInstaller, startIntent);
            prepareOperate = pair.first;
            startIntent = pair.second;


            if (prepareOperate) {
                try {
                    String installerLable = AppInfoUtil.getCustomInstallerLable(this, getCustomInstaller);
                    if (isInstall) {
                        showMyToast0(String.format(getString(R.string.tip_use_to_install), installerLable, apkinfo[0]));
                    } else {
                        showMyToast0(String.format(getString(R.string.tip_use_to_uninstall), installerLable, uninstallPackageLable));
                    }
                    Log.e("intent0", ": " + startIntent);
                    startActivity(startIntent);
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

        if (isInstall && needDelete) {
            OpUtil.deleteSingleFile(installApkFile);
        }
        finish();
    }

}
