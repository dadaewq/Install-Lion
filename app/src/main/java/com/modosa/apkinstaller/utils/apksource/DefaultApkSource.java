package com.modosa.apkinstaller.utils.apksource;

import com.modosa.apkinstaller.utils.filedescriptor.FileDescriptor;

import java.io.InputStream;
import java.util.List;

public class DefaultApkSource implements ApkSource {

    private final List<FileDescriptor> mApkFileDescriptors;
    private FileDescriptor mCurrentApk;

    public DefaultApkSource(List<FileDescriptor> apkFileDescriptors) {
        mApkFileDescriptors = apkFileDescriptors;
    }

    @Override
    public boolean nextApk() {
        if (mApkFileDescriptors.size() == 0) {
            return false;
        }

        mCurrentApk = mApkFileDescriptors.remove(0);
        return true;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return mCurrentApk.open();
    }

    @Override
    public long getApkLength() {
        return mCurrentApk.length();
    }

}
