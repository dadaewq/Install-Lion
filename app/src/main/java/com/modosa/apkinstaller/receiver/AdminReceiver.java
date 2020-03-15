package com.modosa.apkinstaller.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.OpUtil;

/**
 * @author dadaewq
 */
public class AdminReceiver extends DeviceAdminReceiver {

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), AdminReceiver.class);
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        OpUtil.showToast1(context, R.string.tip_start_activating);
    }
}
