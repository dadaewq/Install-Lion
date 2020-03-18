package com.modosa.apkinstaller.activity;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.NotifyUtil;
import com.modosa.apkinstaller.util.OpUtil;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dadaewq
 */
public class Install1Activity extends AbstractInstallerActivity {

    private Disposable mSubscribe;
    private String installApkPath;

    @Override
    public void startInstall(String getinstallApkPath) {
        Log.d("Start install", getinstallApkPath + "");
        if (getinstallApkPath != null) {
            installApkPath = getinstallApkPath;
            installApkFile = new File(installApkPath);
            String authority = getPackageName() + ".FILE_PROVIDER";
            Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, installApkFile);
            showMyToast0(String.format(getString(R.string.tip_start_install), apkinfo[0]));

            disposeSafety();

            //查询如果为SUPPORTED才开始安装，避免因为没有安装等情况导致installPackage没有返回值
            //（疑似部分设备即使安装了查询下也是NOT_INSTALLED）
            if (IceBox.SilentInstallSupport.SUPPORTED.equals(IceBox.querySupportSilentInstall(this))) {
                mSubscribe = Single.fromCallable(() -> IceBox.installPackage(this, installuri))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((Boolean success) -> {
                            Toast.makeText(this, success ? String.format(getString(R.string.tip_success_install), apkinfo[0]) : String.format(getString(R.string.tip_failed_install), apkinfo[0]), Toast.LENGTH_SHORT).show();
                            showNotificationWithdeleteCache(success);

                        }, Throwable::printStackTrace);
            } else {
                Toast.makeText(this, String.format(getString(R.string.tip_failed_install), apkinfo[0]), Toast.LENGTH_SHORT).show();
                showNotificationWithdeleteCache(false);
            }
            finish();
        } else {
            showMyToast0(R.string.tip_failed_read);
            finish();
        }
    }

    @Override
    protected void startUninstall(String pkgName) {
        Log.d("Start uninstall", pkgName);
        showMyToast0(String.format(getString(R.string.tip_start_uninstall), uninstallPackageLable));
        disposeSafety();

        mSubscribe = Single.fromCallable(() -> IceBox.uninstallPackage(this, pkgName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Boolean success) -> Toast.makeText(this, success ? String.format(getString(R.string.tip_success_uninstall), uninstallPackageLable) : String.format(getString(R.string.tip_failed_uninstall), uninstallPackageLable), Toast.LENGTH_SHORT).show(), Throwable::printStackTrace);
        finish();

    }


    private void disposeSafety() {
        if (mSubscribe != null && !mSubscribe.isDisposed()) {
            mSubscribe.dispose();
        }
        mSubscribe = null;
    }

    private void showNotificationWithdeleteCache(boolean success) {
        if (success) {
            isInstalledSuccess = true;
            deleteCache();
            if (show_notification) {
                Log.e("packagename", apkinfo[1]);
                new NotifyUtil(this).sendSuccessNotification("1", String.format(getString(R.string.tip_success_install), apkinfo[0]), apkinfo[1]);
            }

        } else {
            isInstalledSuccess = false;

            if (show_notification) {
                Log.e("packagename", apkinfo[1]);
                new NotifyUtil(this).sendFailNotification("21", String.format(getString(R.string.tip_failed_install), apkinfo[0]), apkinfo[1], installApkPath, istemp && !enableAnotherinstaller);
            } else {
                if (!enableAnotherinstaller) {
                    deleteCache();
                }
            }
            if (enableAnotherinstaller) {
                OpUtil.startAnotherInstaller(this, installApkFile, istemp);
            }

        }
    }
}
