package com.modosa.apkinstaller.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.utils.apksource.ApkSource;
import com.modosa.apkinstaller.utils.installer.ApkSourceBuilder;
import com.modosa.apkinstaller.utils.installer.SAIPackageInstaller;
import com.modosa.apkinstaller.utils.installer.shizuku.ShizukuSAIPackageInstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author dadaewq
 */
public class Install3Activity extends AbstractInstallActivity implements SAIPackageInstaller.InstallationStatusListener {

    private long mOngoingSessionId;
    private File apkFile;


    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            apkFile = new File(apkPath);
            apkinfo = getApkPkgInfo(apkPath);

            ArrayList<File> files = new ArrayList<>();
            files.add(apkFile);
            new Thread(() -> {
                showToast(getString(R.string.install_start) + apkinfo[1]);
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }

            ).start();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
    }

    private void installPackages(List<File> apkFiles) {
        Context mContext = getApplication();
        SAIPackageInstaller mInstaller = ShizukuSAIPackageInstaller.getInstance(mContext);
        mInstaller.addStatusListener(this);
        ApkSource apkSource = new ApkSourceBuilder()
                .fromApkFiles(apkFiles)
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
    }


    @Override
    public void onStatusChanged(long installationID, SAIPackageInstaller.InstallationStatus status, @Nullable String packageNameOrErrorDescription) {
        if (installationID != mOngoingSessionId) {
            return;
        }
        Log.e("status", status + "");
        switch (status) {
            case QUEUED:
            case INSTALLING:
                break;
            case INSTALLATION_SUCCEED:
                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                showToast(getString(R.string.success_install));
                if (show_notification) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"));
                    Log.e("packagename", apkinfo[0]);

                    intent.putExtra("channelId", "3");
                    intent.putExtra("channelName", getString(R.string.name_install3));
                    intent.putExtra("packageName", apkinfo[0]);
                    intent.putExtra("packageLable", apkinfo[1]);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                break;
            case INSTALLATION_FAILED:
                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                showToast(getString(R.string.failed_install));
                break;
            default:
                finish();
        }
    }

}
