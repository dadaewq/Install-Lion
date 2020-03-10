package com.modosa.apkinstaller.activity;

import android.content.ComponentName;
import android.content.Intent;
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
public class Install1Activity extends AbstractInstallActivity {

    private Disposable mSubscribe;

    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            apkFile = new File(apkPath);
            String authority = getPackageName() + ".FILE_PROVIDER";
            Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, apkFile);
            showToast0(String.format(getString(R.string.tip_start_install), apkinfo[0]));

            disposeSafety();

            mSubscribe = Single.fromCallable(() -> IceBox.installPackage(this, installuri))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((Boolean success) -> {
                        Toast.makeText(this, success ? String.format(getString(R.string.tip_success_install), apkinfo[0]) : String.format(getString(R.string.tip_failed_install), apkinfo[0]), Toast.LENGTH_SHORT).show();

                        if (show_notification) {
                            Log.e("packagename", apkinfo[1]);
                            Intent intent = new Intent()
                                    .setComponent(new ComponentName(this, NotifyActivity.class))
                                    .putExtra("packageName", apkinfo[1])
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (success) {
                                intent.putExtra("channelId", "1")
                                        .putExtra("channelName", getString(R.string.name_install1))
                                        .putExtra("contentTitle", String.format(getString(R.string.tip_success_install), apkinfo[0]));
                            } else {
                                intent.putExtra("channelId", "4")
                                        .putExtra("channelName", getString(R.string.channalname_fail))
                                        .putExtra("realPath", apkPath)
                                        .putExtra("contentTitle", String.format(getString(R.string.tip_failed_install), apkinfo[0]));
                            }

                            startActivity(intent);
                        }
                        deleteCache();
                    }, Throwable::printStackTrace);

            finish();
        } else {
            showToast0(R.string.tip_failed_read);
            finish();
        }
    }

    @Override
    protected void startUninstall(String pkgName) {
        Log.d("Start uninstall", pkgName);
        showToast0(String.format(getString(R.string.tip_start_uninstall), uninstallPackageLable));
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
