package com.modosa.apkinstaller.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.modosa.apkinstaller.R;

import java.io.File;


/**
 * @author dadaewq
 */
public class Install2Activity extends AbstractInstallActivity {
    private final boolean ltsdk26 = Build.VERSION.SDK_INT < Build.VERSION_CODES.O;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (ltsdk26) {
            deleteCache();
        }
    }

    @Override
    public void startInstall(String apkPath) {
        if (ltsdk26) {
            showToast1(R.string.tip_ltsdk26);
            finish();
        } else {
            Log.d("Start install", apkPath + "");
            if (apkPath != null) {
                apkFile = new File(apkPath);
                String authority = getPackageName() + ".FILE_PROVIDER";
                Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, apkFile);
                new Thread(() -> {
                    showToast0(String.format(getString(R.string.tip_start_install), apkinfo[0]));
                    try {
                        DSMClient.installApp(this, installuri, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (show_notification) {
                            Log.e("packagename", apkinfo[1]);
                            Intent intent = new Intent()
                                    .setComponent(new ComponentName(this, NotifyActivity.class))
                                    .putExtra("channelId", "2")
                                    .putExtra("channelName", getString(R.string.name_install2))
                                    .putExtra("realPath", apkPath)
                                    .putExtra("packageName", apkinfo[1])
                                    .putExtra("contentTitle", String.format(getString(R.string.tip_install_over), apkinfo[0]))
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                    showToast1(R.string.tip_install_end);
                    deleteCache();
                    finish();
                }).start();
            } else {
                showToast0(R.string.tip_failed_read);
                finish();
            }
        }
    }

    @Override
    protected void startUninstall(String pkgName) {
        if (ltsdk26) {
            showToast1(R.string.tip_ltsdk26);
            finish();
        } else {
            Log.d("Start uninstall", pkgName);
            new Thread(() -> {
                showToast0(String.format(getString(R.string.tip_start_uninstall), uninstallPackageLable));
                try {
                    DSMClient.uninstallApp(this, pkgName);
                } catch (Exception e) {
                    showToast1(e.toString());
                }
                //Todo show result
            }).start();
        }
    }

}