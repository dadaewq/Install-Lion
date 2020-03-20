package com.modosa.apkinstaller.util;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.core.app.NotificationCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.activity.Install1Activity;
import com.modosa.apkinstaller.activity.Install2Activity;
import com.modosa.apkinstaller.activity.Install3Activity;
import com.modosa.apkinstaller.activity.Install4Activity;
import com.modosa.apkinstaller.activity.Install5Activity;

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

    public void sendSuccessNotification(String channelId, String contentTitle, String packageName) {
        sendFailNotification(channelId, contentTitle, packageName, null, false);

    }

    public void sendFailNotification(String channelId, String contentTitle, String packageName, String realPath, boolean shouldDelete) {

        this.channelId = channelId;
        this.channelName = getChannelName(channelId);
        this.contentTitle = contentTitle;

        int id = (int) System.currentTimeMillis();

        PendingIntent clickIntent;

//        如果使用 AppInfoUtils.getApkIcon()在InstallActivity用Bundle传LargeIcon
//        LargeIcon = getIntent().getParcelableExtra("LargeIcon");

        String[] version;

        if (realPath != null || "2".equals(channelId)) {
            largeIcon = AppInfoUtil.getApkIconBitmap(context, realPath);
            clickIntent = null;
            version = AppInfoUtil.getApkVersion(context, realPath);
            if (shouldDelete && realPath != null) {
                OpUtil.deleteSingleFile(new File(realPath));
            }

        } else {
            largeIcon = AppInfoUtil.getApplicationIconBitmap(context, packageName);
            clickIntent = getContentIntent((Activity) context, id, packageName);
            version = AppInfoUtil.getApplicationVersion(context, packageName);

        }
        if (version != null) {
            versionName = version[0];
        }

        notifyLiveStart(clickIntent, id);
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

    private void notifyLiveStart(PendingIntent pendingIntent, int id) {

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
                .setSmallIcon("4".equals(channelId) ?
                        R.drawable.ic_settings_black_24dp :
                        R.drawable.ic_filter_vintage_black_24dp)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        //设置通知栏显示内容
        if (pendingIntent == null) {
            builder.setContentText(versionName);
        } else {
            builder.setContentText(String.format(context.getString(R.string.click_run), versionName));
        }

        if (largeIcon != null) {
            //设置右侧大图标
            builder.setLargeIcon(largeIcon);
        }
        //设置点击通知后自动删除通知

        Notification notification = builder.build();
//        .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
//
//        .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
//
//        .setOngoing(false)//ture，设置他为一个正在进行的通知,通常是用来表示一个后台任务,以某种方式正在等待,如一个文件下载,同步操作
//
//        .setDefaults(Notification.DEFAULT_VIBRATE)//向通知添加声音、闪灯和振动效果


        notificationManager.notify(id, notification);

    }

    private PendingIntent getContentIntent(Activity context, int id, String packageName) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            return PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
