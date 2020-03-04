package com.modosa.apkinstaller.util.installer;

import com.modosa.apkinstaller.util.apksource.ApkSource;

class QueuedInstallation {

    private final ApkSource mApkSource;
    private final long mId;

    QueuedInstallation(ApkSource apkSource, long id) {
        mApkSource = apkSource;
        mId = id;
    }

    public long getId() {
        return mId;
    }

    ApkSource getApkSource() {
        return mApkSource;
    }
}
