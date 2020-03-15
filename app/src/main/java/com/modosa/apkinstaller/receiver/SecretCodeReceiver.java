package com.modosa.apkinstaller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.modosa.apkinstaller.activity.MainUiActivity;

/**
 * @author dadaewq
 */
public class SecretCodeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        if ("android.provider.Telephony.SECRET_CODE".equals(intent.getAction())) {
            Intent settingsIntent1 = new Intent(Intent.ACTION_MAIN)
                    .setClass(context.getApplicationContext(), MainUiActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent1);
        }

    }
}
