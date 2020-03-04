package com.modosa.apkinstaller.util.filedescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class NormalFileDescriptor implements FileDescriptor {

    private final File mFile;

    public NormalFileDescriptor(File file) {
        mFile = file;
    }


    @Override
    public long length() {
        return mFile.length();
    }

    @Override
    public InputStream open() throws Exception {
        return new FileInputStream(mFile);
    }
}
