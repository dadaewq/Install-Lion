package com.modosa.apkinstaller.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * @author dadaewq
 */
public class ApkInfo {
    private final String apkSourcePath;
    private final Context context;

    public ApkInfo(Context context, String apkSourcePath) {
        this.context = context;
        this.apkSourcePath = apkSourcePath;
    }

    public String[] getApkPkgInfo() {
        if (apkSourcePath == null) {
            return null;
        } else {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkSourcePath, PackageManager.GET_ACTIVITIES);
            if (pkgInfo != null) {
                pkgInfo.applicationInfo.sourceDir = apkSourcePath;
                pkgInfo.applicationInfo.publicSourceDir = apkSourcePath;

                return new String[]{pkgInfo.packageName, pm.getApplicationLabel(pkgInfo.applicationInfo).toString() + "_" + pkgInfo.versionName + "(" + pkgInfo.versionCode + ")"};
            }
            return null;
        }
    }


}
