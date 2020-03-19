package com.modosa.apkinstaller.activity;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dadaewq
 */
public class Install1Activity extends AbstractInstallerActivity {
    public final static String CHANNEL_ID = "1";
    private Disposable mSubscribe;

    @Override
    public void startInstall(String getinstallApkPath) {
        Log.d("Start install", getinstallApkPath + "");
        if (getinstallApkPath != null) {
            installApkFile = new File(getinstallApkPath);
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
                            showNotificationWithdeleteCache(CHANNEL_ID, success);

                        }, Throwable::printStackTrace);
            } else {
                Toast.makeText(this, String.format(getString(R.string.tip_failed_install), apkinfo[0]), Toast.LENGTH_SHORT).show();
                showNotificationWithdeleteCache(CHANNEL_ID, false);
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

}
