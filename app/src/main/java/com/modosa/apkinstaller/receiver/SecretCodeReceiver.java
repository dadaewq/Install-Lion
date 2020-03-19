package com.modosa.apkinstaller.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.TelephonyManager;

import com.modosa.apkinstaller.util.OpUtil;

/**
 * @author dadaewq
 */
public class SecretCodeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        String action = intent.getAction();
        if (Telephony.Sms.Intents.SECRET_CODE_ACTION.equals(action) || TelephonyManager.ACTION_SECRET_CODE.equals(action)) {
            OpUtil.startMainUiActivity(context);
        }


    }
}
