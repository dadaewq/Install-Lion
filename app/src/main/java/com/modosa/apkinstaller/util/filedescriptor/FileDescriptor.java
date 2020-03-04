package com.modosa.apkinstaller.util.filedescriptor;

import java.io.InputStream;

public interface FileDescriptor {

    long length();

    InputStream open() throws Exception;

}
