package com.modosa.apkinstaller.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.Install1Activity;
import com.modosa.apkinstaller.activity.Install2Activity;
import com.modosa.apkinstaller.activity.Install3Activity;
import com.modosa.apkinstaller.activity.Install4Activity;
import com.modosa.apkinstaller.activity.Install5Activity;
import com.modosa.apkinstaller.activity.Install6Activity;
import com.modosa.apkinstaller.activity.MainUiActivity;
import com.modosa.apkinstaller.fragment.MainFragment;
import com.modosa.apkinstaller.fragment.SettingsFragment;
import com.modosa.apkinstaller.receiver.AdminReceiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author dadaewq
 */
public class OpUtil {

    public final static String SP_KEY_CONFIRM_PROMPT = "ConfirmPrompt";
    public static final String MODOSA_ACTION_PICK_FILE = "modosa.action.GO_PICK_FILE";
    public static final String MODOSA_ACTION_GO_OPEN_DOCUMENT = "modosa.action.GO_OPEN_DOCUMENT";
    public static final String MODOSA_ACTION_GO_GET_FILE = "modosa.action.GO_GET_FILE";
    public static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final int REQUEST_REFRESH_WRITE_PERMISSION = 0x2330;

    public static void startMainUiActivity(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(context, MainUiActivity.class);
        context.startActivity(intent);
    }

    public static void requestWritePermission(Activity activity) {
        try {
            ActivityCompat.requestPermissions(activity, new String[]{WRITE_PERMISSION}, REQUEST_REFRESH_WRITE_PERMISSION);
        } catch (Exception e) {
            showToast1(activity, "" + e);
        }
    }

    public static boolean checkWritePermission(Context context) {
        int checkSelfPermission = ContextCompat.checkSelfPermission(context, WRITE_PERMISSION);
        return (checkSelfPermission == 0);
    }

    public static void launchCustomTabsUrl(Context context, String url) {
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build();

            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (Exception e) {
            showToast1(context, "" + e);
        }
    }

    public static void startMyClass(Context context, Class myClass) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setClass(context, myClass);
            context.startActivity(intent);
        } catch (Exception e) {
            showToast1(context, "" + e);
        }
    }


    public static AlertDialog createDialogConfirmPrompt(Context context) {

        View view = View.inflate(context, R.layout.confirmprompt_doublecheckbox, null);

        CheckBox checkBox1 = view.findViewById(R.id.confirm_checkbox1);
        CheckBox checkBox2 = view.findViewById(R.id.confirm_checkbox2);
        checkBox1.setText(R.string.checkbox1_instructions_before_use);
        checkBox2.setText(R.string.checkbox2_instructions_before_use);
        checkBox2.setEnabled(false);
        checkBox1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkBox2.setChecked(false);
            checkBox2.setEnabled(isChecked);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.title_instructions_before_use)
                .setView(view)
                .setPositiveButton(android.R.string.cancel, null)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> {
                    boolean hasBothConfirm = false;
                    if (checkBox1.isChecked() && checkBox2.isChecked()) {
                        hasBothConfirm = true;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SP_KEY_CONFIRM_PROMPT, hasBothConfirm).apply();
                });

        return builder.create();

    }

    public static void showDialogConfirmPrompt(Context context, AlertDialog alertDialog) {

        OpUtil.showAlertDialog(context, alertDialog);

        Button button = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        button.setEnabled(false);

        CountDownTimer timer = new CountDownTimer(20000, 1000) {
            final String oK = context.getString(android.R.string.ok);

            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                button.setText(oK + "(" + millisUntilFinished / 1000 + "s" + ")");
            }

            @Override
            public void onFinish() {
                button.setText(oK);
                button.setEnabled(true);
            }
        };
        //调用 CountDownTimer 对象的 start() 方法开始倒计时，也不涉及到线程处理
        timer.start();
    }

    public static File createApkFromUri(Context context, InputStream is) {

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

            if (is != null) {
                tempFile = new File(cacheDir, System.currentTimeMillis() + ".apk");
                if (Build.VERSION.SDK_INT >= 26) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
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
            SharedPreferences spGetPreferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
            if (spGetPreferenceManager.getBoolean(SettingsFragment.SP_KEY_EASTER_EGG, false)) {
                window.setBackgroundDrawableResource(R.drawable.alertdialog_background_transparent);
            } else {
                window.setBackgroundDrawableResource(R.drawable.alertdialog_background);
            }
        }
        if (!((Activity) context).isFinishing()) {
            alertDialog.show();
        }
    }

    public static void setButtonTextColor(Context context, AlertDialog alertDialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.rBackground, null));
            alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(context.getResources().getColor(R.color.rBackground, null));
        } else {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.rBackground));
            alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(context.getResources().getColor(R.color.rBackground));
        }
    }

    public static Uri getMyContentUriForFile(Context context, File file) {

        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".FILE_PROVIDER", file);
    }

    public static void startAnotherInstaller(Context context, File installApkFile, boolean istemp) {
        String anotherInstallerName = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsFragment.SP_KEY_ANOTHER_INSTALLER_NAME, "").trim();

        Intent systemIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(getMyContentUriForFile(context, installApkFile), "application/vnd.android.package-archive")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent anotherIntent = (Intent) systemIntent.clone();
        boolean useSystemIntent = true;
        if (!"".equals(anotherInstallerName)) {
            anotherIntent.setPackage(anotherInstallerName);
            try {
                context.startActivity(anotherIntent);
                useSystemIntent = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (useSystemIntent) {
            try {
                context.startActivity(systemIntent);
            } catch (Exception e) {
                e.printStackTrace();
                if (istemp) {
                    deleteSingleFile(installApkFile);
                }
            }
        }

    }

    public static ComponentName[] getInstallerComponentNames(Context context) {
        ComponentName[] installerComponentNames = new ComponentName[MainFragment.INSTALLER_SIZE];

        installerComponentNames[1] = new ComponentName(context, Install1Activity.class);
        installerComponentNames[2] = new ComponentName(context, Install2Activity.class);
        installerComponentNames[3] = new ComponentName(context, Install3Activity.class);
        installerComponentNames[4] = new ComponentName(context, Install4Activity.class);
        installerComponentNames[5] = new ComponentName(context, Install5Activity.class);
        installerComponentNames[6] = new ComponentName(context, Install6Activity.class);
        return installerComponentNames;
    }


    public static boolean getEnabledComponentState(Context context, ComponentName componentName) {
        PackageManager pm = context.getPackageManager();
        //PackageManager.COMPONENT_ENABLED_STATE_DEFAULT 需要AndroidManifest.xml里的<activity>不为android:enabled="false"
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


    /**
     * 只有是文件且存在时删除
     */
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

    public static ArrayList<String> convertToList(String origin, String s) {
        return origin == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(origin.split(s)));
    }

    public static ArrayList<String> convertToList(String[] origin) {
        return origin == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(origin));
    }

    public static String listToString(List<String> stringList, String s) {

        if (stringList == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String s1 : stringList) {
            if (!"".equals(s1)) {
                sb.append(s1);
                sb.append(s);
            }

        }

        return sb.toString();
    }


    public static void showToast0(Context context, final String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast0(Context context, final int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }

    private static void showToast1(Context context, final String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void showToast1(Context context, final int stringId) {
        Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
    }


}
