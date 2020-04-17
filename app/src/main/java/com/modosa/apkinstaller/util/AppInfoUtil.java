package com.modosa.apkinstaller.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.modosa.apkinstaller.R;

import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;
import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;

/**
 * @author dadaewq
 */
public final class AppInfoUtil {

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
            } catch (Exception e) {
                e.printStackTrace();
                return context.getString(R.string.unknown);
            }
        }
    }

    private static String getActivityLabel(Context context, ComponentName componentName) {
        PackageManager pm = context.getPackageManager();

        ActivityInfo activityInfo = null;
        try {
            activityInfo = pm.getActivityInfo(componentName, 0);
        } catch (Exception ignore) {
        }

        if (activityInfo != null) {
            return activityInfo.loadLabel(pm).toString();
        } else {
            try {
                return (pm.getActivityInfo(componentName, Build.VERSION.SDK_INT > Build.VERSION_CODES.M ? MATCH_UNINSTALLED_PACKAGES : GET_UNINSTALLED_PACKAGES).loadLabel(pm).toString());
            } catch (Exception e) {
                e.printStackTrace();
                return context.getString(R.string.unknown);
            }
        }
    }

    public static String getCustomInstallerLable(Context context, String getCustomInstaller) {
        String installerLable = context.getString(R.string.unknown);
        if (getCustomInstaller.contains("/")) {
            String[] names = getCustomInstaller.split("/");
            if (names.length == 2 && !"".equals(names[0]) && !"".equals(names[1])) {
                if (names[1].length() > 1 && names[1].startsWith(".")) {
                    names[1] = names[0] + names[1];
                }
                installerLable = getActivityLabel(context, new ComponentName(names[0], names[1]));
            }

        } else {
            installerLable = getApplicationLabel(context, getCustomInstaller);
        }
        return installerLable;
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


    public static Drawable getApplicationIconDrawable(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = pm.getApplicationInfo(pkgName, 0);
        } catch (Exception ignore) {
        }

        if (applicationInfo != null) {
            return applicationInfo.loadIcon(pm);
        } else {
            try {
                return pm.getApplicationInfo(pkgName, Build.VERSION.SDK_INT > Build.VERSION_CODES.M ? MATCH_UNINSTALLED_PACKAGES : GET_UNINSTALLED_PACKAGES).loadIcon(pm);
            } catch (Exception ignore) {
                return pm.getDefaultActivityIcon();
            }
        }
    }

    static Bitmap getApplicationIconBitmap(Context context, String pkgName) {
        return drawable2Bitmap(getApplicationIconDrawable(context, pkgName));
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


    static String[] getApkVersion(Context context, String apkPath) {
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

    public static Drawable getApkIconDrawable(Context context, String apkPath) {
        Drawable icon = null;
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo applicationInfo = pkgInfo.applicationInfo;
            pkgInfo.applicationInfo.sourceDir = apkPath;
            pkgInfo.applicationInfo.publicSourceDir = apkPath;
            icon = applicationInfo.loadIcon(pm);
        }
        if (icon == null) {
            icon = pm.getDefaultActivityIcon();
        }
        return icon;
    }

    static Bitmap getApkIconBitmap(Context context, String apkPath) {
        return drawable2Bitmap(getApkIconDrawable(context, apkPath));
    }

    private static Bitmap drawable2Bitmap(Drawable drawable) {

        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            bitmap = bd.getBitmap();
        }

        if (bitmap == null && drawable != null) {
            bitmap = Bitmap
                    .createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                    : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }


}
