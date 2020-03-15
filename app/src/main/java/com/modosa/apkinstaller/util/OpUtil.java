package com.modosa.apkinstaller.util;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.receiver.AdminReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author dadaewq
 */
public class OpUtil {

    public static final String MODOSA_ACTION_PICK_FILE = "modosa.action.PICK_FILE";

    public static File createApkFromUri(Context context, Uri uri) {

        // getExternalCacheDir 此方法会确保此路径存在
        File cacheDir = context.getExternalCacheDir();
        File tempFile = null;

        //如果cacheDir是个文件则删除
        deleteSingleFile(cacheDir);
        try {
            assert cacheDir != null;
            if (!cacheDir.exists()) {
                //只用单独创建"cache"文件夹即可
                cacheDir.mkdir();
            }

            tempFile = new File(cacheDir, System.currentTimeMillis() + ".apk");

            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                OutputStream fos = new FileOutputStream(tempFile);
                byte[] buf = new byte[4096 * 1024];
                int ret;
                while ((ret = is.read(buf)) != -1) {
                    fos.write(buf, 0, ret);
                    fos.flush();
                }
                fos.close();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tempFile;
    }


    public static String getCommand(Context context) {
        ComponentName componentName = AdminReceiver.getComponentName(context);
        StringBuilder stringBuilder;

        String cPackageName = componentName.getPackageName();
        String cClassName = componentName.getClassName();

        if (cClassName.startsWith(cPackageName)) {
            stringBuilder = new StringBuilder(cClassName);
            stringBuilder.insert(cPackageName.length(), "/");
        } else {
            stringBuilder = new StringBuilder(componentName.flattenToString());
        }
        return "adb shell dpm set-device-owner " + stringBuilder.toString();
    }

    public static void showAlertDialog(Context context, AlertDialog alertDialog) {
        Window window = alertDialog.getWindow();
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(R.color.Background, null)));
            } else {

                window.setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(R.color.Background)));
            }
        }
        alertDialog.show();
    }


    public static boolean getComponentState(Context context, ComponentName componentName) {
        PackageManager pm = context.getPackageManager();
        return (pm.getComponentEnabledSetting(componentName) == (PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || pm.getComponentEnabledSetting(componentName) == (PackageManager.COMPONENT_ENABLED_STATE_ENABLED));
    }

    public static void setComponentState(Context context, ComponentName componentName, boolean isenable) {
        PackageManager pm = context.getPackageManager();
        int flag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if (isenable) {
            flag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        }

        pm.setComponentEnabledSetting(componentName, flag, PackageManager.DONT_KILL_APP);
    }


    //只有是文件且存在时删除
    public static void deleteSingleFile(File file) {
        if (file != null && file.exists() && file.isFile() && file.delete()) {
            Log.e("-DELETE-", "==>" + file.getAbsolutePath() + " OK！");
        }
    }


    public static void deleteDirectory(String filePath) {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }

        File dirFile = new File(filePath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出

        deleteSingleFile(dirFile);

        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();

        if (files == null) {
            Log.e("files", "null");

        } else {
            Log.e("files", files.length + "");
            for (File file : files) {
                // 删除子文件
                if (file.isFile()) {
                    deleteSingleFile(file);

                }
                // 删除子目录
                else if (file.isDirectory()) {
                    deleteDirectory(file.getAbsolutePath());
                }
            }

            // 删除空文件夹
            dirFile.delete();

        }
    }

    public static void showToast0(Context context, final String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast0(Context context, final int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }

    public static void showToast1(Context context, final String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast1(Context context, final int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }


}
