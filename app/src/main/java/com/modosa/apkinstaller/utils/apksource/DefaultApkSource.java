package com.modosa.apkinstaller.utils.apksource;


import com.modosa.apkinstaller.utils.filedescriptor.FileDescriptor;

import java.io.InputStream;
import java.util.List;

public class DefaultApkSource implements ApkSource {

    private final FileDescriptor mCurrentApk;

    public DefaultApkSource(List<FileDescriptor> apkFileDescriptors) {
        mCurrentApk = apkFileDescriptors.remove(0);
    }


    @Override
    public InputStream openApkInputStream() throws Exception {
        return mCurrentApk.open();
    }

    @Override
    public long getApkLength() {
        return mCurrentApk.length();
    }

    @Override
    public String getApkName() {
        return mCurrentApk.name();
    }
}
