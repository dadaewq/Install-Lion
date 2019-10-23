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
            showToast0(String.format(getString(R.string.start_install), apkinfo[0]));

            disposeSafety();

            mSubscribe = Single.fromCallable(() -> IceBox.installPackage(this, installuri))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((Boolean success) -> {
                        Toast.makeText(this, success ? String.format(getString(R.string.success_install), apkinfo[0]) : String.format(getString(R.string.failed_install0), apkinfo[0]), Toast.LENGTH_SHORT).show();
                        deleteCache();
                        if (success && show_notification) {
                            Log.e("packagename", apkinfo[1]);
                            Intent intent = new Intent()
                                    .setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"))
                                    .putExtra("channelId", "1")
                                    .putExtra("channelName", getString(R.string.name_install1))
                                    .putExtra("packageName", apkinfo[1])
                                    .putExtra("packageLable", apkinfo[0])
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }, Throwable::printStackTrace);

            finish();
        } else {
            showToast0(getString(R.string.failed_read));
            finish();
        }
    }

    @Override
    protected void startUninstall(String pkgName) {
        Log.d("Start uninstall", pkgName);
        showToast0(String.format(getString(R.string.start_uninstall), packageLable));
        disposeSafety();

        mSubscribe = Single.fromCallable(() -> IceBox.uninstallPackage(this, pkgName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Boolean success) -> Toast.makeText(this, success ? String.format(getString(R.string.success_uninstall), packageLable) : String.format(getString(R.string.failed_uninstall0), packageLable), Toast.LENGTH_SHORT).show(), Throwable::printStackTrace);
        finish();

    }


    private void disposeSafety() {
        if (mSubscribe != null && !mSubscribe.isDisposed()) {
            mSubscribe.dispose();
        }
        mSubscribe = null;
    }

}
