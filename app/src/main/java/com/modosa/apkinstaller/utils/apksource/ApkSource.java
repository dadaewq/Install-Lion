package com.modosa.apkinstaller.utils.apksource;

import java.io.InputStream;

public interface ApkSource extends AutoCloseable {

    boolean nextApk();

    InputStream openApkInputStream() throws Exception;

    long getApkLength();

    String getApkName();

    @Override
    default void close() {

    }
}
