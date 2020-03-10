package com.modosa.apkinstaller.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

public final class AppInfoUtil {
    public static final String UNINSTALLED = "IL^&UninstalledPN*@!128`+=：:,.[";

    public static String getApplicationLabel(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(pkgName, 0);
        } catch (Exception ignore) {
        }

        if (applicationInfo != null) {
            return applicationInfo.loadLabel(pm).toString();
        } else {
            try {
                return (pm.getApplicationInfo(pkgName, Build.VERSION.SDK_INT > Build.VERSION_CODES.M ? MATCH_UNINSTALLED_PACKAGES : GET_UNINSTALLED_PACKAGES).loadLabel(pm).toString());
            } catch (PackageManager.NameNotFoundException ignore) {
                return UNINSTALLED;
            } catch (Exception e) {
                e.printStackTrace();
                return pkgName;
            }
        }
    }

    public static String[] getApplicationVersion(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(pkgName, 0);
        } catch (Exception ignore) {
        }

        if (applicationInfo != null) {
            return getApkVersion(context, applicationInfo.sourceDir);
        }
        return null;


    }


    public static Bitmap getApplicationIcon(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(pkgName, 0);
        } catch (Exception ignore) {
        }

        if (applicationInfo != null) {
            return drawable2Bitmap(applicationInfo.loadIcon(pm));
        } else {
            try {
                return drawable2Bitmap(pm.getApplicationInfo(pkgName, Build.VERSION.SDK_INT > Build.VERSION_CODES.M ? MATCH_UNINSTALLED_PACKAGES : GET_UNINSTALLED_PACKAGES).loadIcon(pm));
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    public static String[] getApkInfo(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);

        if (pkgInfo != null) {
            pkgInfo.applicationInfo.sourceDir = apkPath;
            pkgInfo.applicationInfo.publicSourceDir = apkPath;

            return new String[]{
                    pm.getApplicationLabel(pkgInfo.applicationInfo).toString(),
                    pkgInfo.packageName,
                    pkgInfo.versionName,
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.P ? Integer.toString(pkgInfo.versionCode) : Long.toString(pkgInfo.getLongVersionCode()), FileSizeUtil.getAutoFolderOrFileSize(apkPath)
            };
        } else {
            return null;
        }
    }

    public static String[] getApkVersion(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);

        if (pkgInfo != null) {
            pkgInfo.applicationInfo.sourceDir = apkPath;
            pkgInfo.applicationInfo.publicSourceDir = apkPath;

            return new String[]{
                    pkgInfo.versionName,
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.P ? Integer.toString(pkgInfo.versionCode) : Long.toString(pkgInfo.getLongVersionCode()), FileSizeUtil.getAutoFolderOrFileSize(apkPath)
            };
        } else {
            return null;
        }
    }

    public static Bitmap getApkIcon(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo applicationInfo = pkgInfo.applicationInfo;
            pkgInfo.applicationInfo.sourceDir = apkPath;
            pkgInfo.applicationInfo.publicSourceDir = apkPath;
            return drawable2Bitmap(applicationInfo.loadIcon(pm));
        }
        return null;
    }

    private static Bitmap drawable2Bitmap(Drawable drawable) {

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            return bd.getBitmap();
        }

        Bitmap bitmap = Bitmap
                .createBitmap(
                        drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }


}
