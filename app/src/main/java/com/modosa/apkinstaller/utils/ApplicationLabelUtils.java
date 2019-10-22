package com.modosa.apkinstaller.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import static android.content.pm.PackageManager.GET_UNINSTALLED_PACKAGES;

public final class ApplicationLabelUtils {
    public static final String UNINSTALLED = "IL^&UninstalledPN*@!128`+=：:,.[";

    public static String getApplicationLabel(Context context, PackageManager packageManager, ApplicationInfo applicationInfo, String pkgName) {

        if (pkgName == null) {
            return "";
        }

        String name;

        PackageManager pm = packageManager == null ? context.getPackageManager() : packageManager;
        if (applicationInfo != null) {
            name = applicationInfo.loadLabel(pm).toString();
            return name;
        } else {
            try {
                name = pm.getApplicationInfo(pkgName, GET_UNINSTALLED_PACKAGES).loadLabel(pm).toString();
                return name;
            } catch (PackageManager.NameNotFoundException ignore) {
                return UNINSTALLED;
            } catch (Exception e) {
                e.printStackTrace();
                return pkgName;
            }
        }
    }

}
