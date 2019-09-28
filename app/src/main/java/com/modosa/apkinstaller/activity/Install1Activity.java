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
            final File apkFile = new File(apkPath);
            String authority = getPackageName() + ".FILE_PROVIDER";
            Uri installuri = FileProvider.getUriForFile(getApplicationContext(), authority, apkFile);
            apkinfo = getApkPkgInfo(apkPath);
            showToast(getString(R.string.install_start) + apkinfo[1]);


            disposeSafety();

            mSubscribe = Single.fromCallable(() -> IceBox.installPackage(this, installuri))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((Boolean success) -> {
                        Toast.makeText(this, success ? getString(R.string.success_install) : getString(R.string.failed_install), Toast.LENGTH_SHORT).show();
                        if (istemp) {
                            deleteSingleFile(apkFile);
                        }
                        if (success && show_notification) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"));
                            Log.e("packagename", apkinfo[0]);
                            intent.putExtra("channelId", "1");
                            intent.putExtra("channelName", getString(R.string.name_install1));
                            intent.putExtra("packageName", apkinfo[0]);
                            intent.putExtra("packageLable", apkinfo[1]);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }, Throwable::printStackTrace);

            finish();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
    }


    private void disposeSafety() {
        if (mSubscribe != null && !mSubscribe.isDisposed()) {
            mSubscribe.dispose();
        }
        mSubscribe = null;
    }

}
