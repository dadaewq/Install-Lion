package com.modosa.apkinstaller.activity;

import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.NotifyUtil;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dadaewq
 */
public class Installer1Activity extends AbstractInstallerActivity {

    private Disposable mSubscribe;

    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            installApkFile = new File(apkPath);
            String authority = getPackageName() + ".FILE_PROVIDER";
            Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, installApkFile);
            showMyToast0(String.format(getString(R.string.tip_start_install), apkinfo[0]));

            disposeSafety();

            mSubscribe = Single.fromCallable(() -> IceBox.installPackage(this, installuri))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((Boolean success) -> {
                        Toast.makeText(this, success ? String.format(getString(R.string.tip_success_install), apkinfo[0]) : String.format(getString(R.string.tip_failed_install), apkinfo[0]), Toast.LENGTH_SHORT).show();

                        if (show_notification) {
                            Log.e("packagename", apkinfo[1]);
                            if (success) {
                                deleteCache();
                                new NotifyUtil(this).sendNotification("1", String.format(getString(R.string.tip_success_install), apkinfo[0]), apkinfo[1]);
                            } else {
                                new NotifyUtil(this).sendNotification("21", String.format(getString(R.string.tip_failed_install), apkinfo[0]), apkinfo[1], apkPath, istemp);
                            }
                        } else {
                            deleteCache();
                        }

                    }, Throwable::printStackTrace);

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
