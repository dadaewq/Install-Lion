package com.modosa.apkinstaller.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.modosa.apkinstaller.R;

import java.io.File;


/**
 * @author dadaewq
 */
public class Install2Activity extends AbstractInstallActivity {

    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            apkFile = new File(apkPath);
            String authority = getPackageName() + ".FILE_PROVIDER";
            Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, apkFile);
            new Thread(() -> {
                showToast0(String.format(getString(R.string.start_install), apkinfo[0]));
                try {
                    DSMClient.installApp(this, installuri, null);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (show_notification) {
                        Log.e("packagename", apkinfo[1]);
                        Intent intent = new Intent()
                                .setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"))
                                .putExtra("channelId", "2")
                                .putExtra("channelName", getString(R.string.name_install2))
                                .putExtra("packageName", apkinfo[1])
                                .putExtra("packageLable", apkinfo[0])
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
                showToast1(getString(R.string.install_end));
                deleteCache();
                finish();
            }).start();
        } else {
            showToast0(getString(R.string.failed_read));
            finish();
        }
    }

    @Override
    protected void startUninstall(String pkgName) {
        Log.d("Start uninstall", pkgName);
        new Thread(() -> {
            showToast0(String.format(getString(R.string.start_uninstall), packageLable));
            try {
                DSMClient.uninstallApp(this, pkgName);
            } catch (Exception e) {
                showToast1(e.toString());
            }
            //Todo show result
        }).start();

    }

}