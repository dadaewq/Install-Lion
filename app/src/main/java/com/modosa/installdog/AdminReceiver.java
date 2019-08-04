package com.modosa.installdog;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }
}