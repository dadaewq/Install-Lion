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
            final File apkFile = new File(apkPath);
            String authority = getPackageName() + ".FILE_PROVIDER";
            Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, apkFile);
            apkinfo = getApkPkgInfo(apkPath);

            new Thread(() -> {
                showToast(getString(R.string.install_start) + apkinfo[1]);
                try {
                    DSMClient.installApp(this, installuri, null);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (show_notification) {
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"));
                        Log.e("packagename", apkinfo[0]);

                        intent.putExtra("channelId", "2");
                        intent.putExtra("channelName", getString(R.string.name_install2));
                        intent.putExtra("packageName", apkinfo[0]);
                        intent.putExtra("packageLable", apkinfo[1]);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
                showToast(getString(R.string.install_end));


                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                finish();
            }).start();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
    }

}