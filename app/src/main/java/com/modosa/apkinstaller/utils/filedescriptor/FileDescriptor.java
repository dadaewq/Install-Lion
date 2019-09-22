package com.modosa.apkinstaller.utils.filedescriptor;

import java.io.InputStream;

public interface FileDescriptor {

    String name();

    long length();

    InputStream open() throws Exception;

}
