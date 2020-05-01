package com.modosa.apkinstaller.util;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.Install1Activity;
import com.modosa.apkinstaller.activity.Install2Activity;
import com.modosa.apkinstaller.activity.Install3Activity;
import com.modosa.apkinstaller.activity.Install4Activity;
import com.modosa.apkinstaller.activity.Install5Activity;
import com.modosa.apkinstaller.activity.LaunchAppActivity;

import java.io.File;

/**
 * @author dadaewq
 */
public class NotifyUtil {
    public final static String CHANNEL_ID_FAIL = "21";
    private final Context context;
    private String channelId;
    private String channelName;
    private String versionName = "";
    private String contentTitle = "";
    private NotificationManager notificationManager;
    private Bitmap largeIcon;

    public NotifyUtil(Context context) {
        this.context = context;
    }


    public void sendNotification(String channelId, String contentTitle, String packageName, String realPath, boolean isTemp, boolean enableAnotherinstaller) {

        this.channelId = channelId;
        this.channelName = getChannelName(channelId);
        this.contentTitle = contentTitle;


        int notificationId = (int) System.currentTimeMillis();

        PendingIntent deleteFilePendingIntent = null;
        PendingIntent runAppPendingIntent = null;
        PendingIntent installApkPendingIntent = null;


//        如果使用 AppInfoUtils.getApkIcon()在InstallActivity用Bundle传LargeIcon
//        LargeIcon = getIntent().getParcelableExtra("LargeIcon");

        String[] version;

        if (CHANNEL_ID_FAIL.equals(channelId) || Install2Activity.CHANNEL_ID.equals(channelId)) {
            largeIcon = AppInfoUtil.getApkIconBitmap(context, realPath);
            version = AppInfoUtil.getApkVersion(context, realPath);
            if (enableAnotherinstaller) {
//                if(CHANNEL_ID_FAIL.equals(channelId)){
                installApkPendingIntent = getInstallApkPendingIntent(context, notificationId, realPath, isTemp);
//                }
            } else {
                if (isTemp) {
                    OpUtil.deleteSingleFile(new File(realPath));
                }
            }

        } else {
            largeIcon = AppInfoUtil.getApplicationIconBitmap(context, packageName);
            runAppPendingIntent = getRunAppPendingIntent(context, notificationId, packageName);
            if (!isTemp) {
                deleteFilePendingIntent = getDeleteFilePendingIntent((Activity) context, notificationId, realPath);

//                deleteFilePendingIntent=runAppPendingIntent;
            }
            version = AppInfoUtil.getApplicationVersion(context, packageName);

        }
        if (version != null) {
            versionName = version[0];
        }

        notifyLiveStart(deleteFilePendingIntent, runAppPendingIntent, installApkPendingIntent, notificationId);
    }

    private String getChannelName(String channelId) {
        String channelName = "";
        switch (channelId) {
            case Install1Activity.CHANNEL_ID:
                channelName = context.getString(R.string.name_install1);
                break;
            case Install2Activity.CHANNEL_ID:
                channelName = context.getString(R.string.name_install2);
                break;
            case Install3Activity.CHANNEL_ID:
                channelName = context.getString(R.string.name_install3);
                break;
            case Install4Activity.CHANNEL_ID:
                channelName = context.getString(R.string.name_install4);
                break;
            case Install5Activity.CHANNEL_ID:
                channelName = context.getString(R.string.name_install5);
                break;
            case CHANNEL_ID_FAIL:
                channelName = context.getString(R.string.channalname_fail);
                break;
            default:
        }
        return channelName;
    }

    private void notifyLiveStart(PendingIntent deleteFilePendingIntent, PendingIntent runAppPendingIntent, PendingIntent installApkPendingIntent, int notificationId) {

        NotificationChannel channel;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (notificationManager == null) {
                notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            //是否在桌面icon右上角展示小红点
            channel.enableLights(true);
            //是否在长按桌面图标时显示此渠道的通知
            channel.setShowBadge(true);

            notificationManager.createNotificationChannel(channel);
        }

        if (notificationManager == null) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                //设置通知栏标题
                .setContentTitle(contentTitle)
                //通知产生的时间，会在通知信息里显示
                .setWhen(System.currentTimeMillis())
                //设置小图标（通知栏没有下拉的图标）
                .setSmallIcon(Install4Activity.CHANNEL_ID.equals(channelId) ?
                        R.drawable.ic_settings_black_24dp :
                        R.drawable.ic_filter_vintage_black_24dp)
                //设置点击通知后自动删除通知
                .setAutoCancel(true);
//                .setContentIntent(null);

        if (deleteFilePendingIntent != null) {
            builder.addAction(0, context.getString(R.string.click_delete_apk), deleteFilePendingIntent);
        }

        if (runAppPendingIntent != null) {
            builder.addAction(0, context.getString(R.string.click_launch), runAppPendingIntent);
        }

        if (installApkPendingIntent != null) {
            builder.addAction(0, context.getString(R.string.title_installWithAnother), installApkPendingIntent);
        }

        //设置通知栏显示内容
        builder.setContentText(versionName);

        if (largeIcon != null) {
            //设置右侧大图标
            builder.setLargeIcon(largeIcon);
        }


        Notification notification = builder.build();
//        .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
//
//        .setOngoing(false)//ture，设置他为一个正在进行的通知,通常是用来表示一个后台任务,以某种方式正在等待,如一个文件下载,同步操作
//
//        .setDefaults(Notification.DEFAULT_VIBRATE)//向通知添加声音、闪灯和振动效果


        notificationManager.notify(notificationId, notification);

    }


    private PendingIntent getRunAppPendingIntent(Context context, int notificationId, String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent = new Intent(context, LaunchAppActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("notificationId", notificationId)
                        .putExtra(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Intent.EXTRA_PACKAGE_NAME : "android.intent.extra.PACKAGE_NAME", packageName);

                return PendingIntent.getActivity(context, notificationId + 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private PendingIntent getDeleteFilePendingIntent(Activity context, int notificationId, String realPath) {

        Intent intent = new Intent(context, LaunchAppActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("notificationId", notificationId)
                .putExtra("realPath", realPath);

        try {
            return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private PendingIntent getInstallApkPendingIntent(Context context, int notificationId, String realPath, boolean isTemp) {

        Intent intent = new Intent(context, LaunchAppActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("notificationId", notificationId)
                .putExtra("realPath", realPath)
                .putExtra("isTemp", isTemp);

        try {
            return PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
