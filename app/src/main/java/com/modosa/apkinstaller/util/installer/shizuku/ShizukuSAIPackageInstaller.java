package com.modosa.apkinstaller.util.installer.shizuku;

import android.content.Context;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.installer.ShellSAIPackageInstaller;
import com.modosa.apkinstaller.util.shell.Shell;
import com.modosa.apkinstaller.util.shell.ShizukuShell;

public class ShizukuSAIPackageInstaller extends ShellSAIPackageInstaller {
    private static ShizukuSAIPackageInstaller sInstance;

    private ShizukuSAIPackageInstaller(Context c) {
        super(c);
        sInstance = this;
    }

    public static ShizukuSAIPackageInstaller getInstance(Context c) {
        synchronized (ShizukuSAIPackageInstaller.class) {
            return sInstance != null ? sInstance : new ShizukuSAIPackageInstaller(c);
        }
    }

    @Override
    public Shell getShell() {
        return ShizukuShell.getInstance();
    }

    @Override
    public String getInstallerName() {
        return "Shizuku";
    }

    @Override
    public String getShellUnavailableMessage() {
        return getContext().getString(R.string.installer_error_shizuku_unavailable);
    }
}
