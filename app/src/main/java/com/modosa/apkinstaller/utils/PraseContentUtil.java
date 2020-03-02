package com.modosa.apkinstaller.utils;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * @author dadaewq
 */
public class PraseContentUtil {
    public static File getSomeFileFromReferrerAndUri(String referrer, Uri uri) {
        String authority = uri.getAuthority();
        File file;
        String path = "";
        if (authority != null) {
//            showDetail(uri);
            String uriPath = uri.getPath();
            String getExternalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory("") + "";

            String getExternalRootDir = "/Android/data/" + referrer;
            String getExternalFilesDir = "/Android/data/" + referrer + "/files";
            String getExternalCacheDir = "/Android/data/" + referrer + "/cache";
            @SuppressLint("SdCardPath")
            String getStorageIsolationDir = "/Android/data/" + referrer + "/sdcard";


            String getfirstPathSegment = uri.getPathSegments().get(0);
            String getExcludeFirstPathSegment = uriPath.substring(getfirstPathSegment.length() + 1);
            String getLastPathSegment = uri.getLastPathSegment();

            switch (referrer) {
                case "com.tencent.mm":
                    if ("com.tencent.mm.external.fileprovider".equals(authority)) {

                        path = getExternalStoragePublicDirectory + getExcludeFirstPathSegment;
                        file = new File(path);
                        if (file.exists()) {
                            return file;
                        }

                        path = getExternalStoragePublicDirectory + getStorageIsolationDir + getExcludeFirstPathSegment;
                    }
                    break;
                case "com.tencent.mobileqq":
                    if ("com.tencent.mobileqq.fileprovider".equals(authority)) {

                        path = getExcludeFirstPathSegment;
                        file = new File(path);
                        if (file.exists()) {
                            return file;
                        }

                        int indexTencent = path.indexOf(uri.getPathSegments().get(4)) - 1;
                        StringBuilder stringBuilder = new StringBuilder(path)
                                .insert(indexTencent, getStorageIsolationDir);
                        path = stringBuilder.toString();

                    }

                    break;
                case "com.coolapk.market":
                    if ("com.coolapk.market.fileprovider".equals(authority)) {

                        switch (getfirstPathSegment) {
                            case "files_root":
                                path = getExternalStoragePublicDirectory + getExternalRootDir + getExcludeFirstPathSegment;
                                break;
                            case "external_files_path":
                                path = getExternalStoragePublicDirectory + getExternalFilesDir + "/Download" + getExcludeFirstPathSegment;
                                break;
                            case "gdt_sdk_download_path":
                                path = getExternalStoragePublicDirectory + "/GDTDOWNLOAD" + getExcludeFirstPathSegment;
                                break;
                            case "external_storage_root":
                                path = getExternalStoragePublicDirectory + getExcludeFirstPathSegment;
                                break;

                            default:
                        }


                        file = new File(path);
                        if (file.exists()) {
                            return file;
                        } else {
                            path = getExternalStoragePublicDirectory + getStorageIsolationDir + getExcludeFirstPathSegment;
                            file = new File(path);
                            if (file.exists()) {
                                return file;
                            } else {
                                path = getExternalStoragePublicDirectory + getStorageIsolationDir + "/GDTDOWNLOAD" + getExcludeFirstPathSegment;
                                Log.e("path", path);
                            }

                        }

                    }
                    break;
                case "com.coolapk.market.vn":
                    if ("com.coolapk.market.vn.fileProvider".equals(authority)) {

                        switch (getfirstPathSegment) {
                            case "files_root":
                                path = getExternalStoragePublicDirectory + getExternalRootDir + getExcludeFirstPathSegment;
                                break;
                            case "external_storage_root":
                                path = getExternalStoragePublicDirectory + getExcludeFirstPathSegment;
                                break;
                            default:
                        }

                        file = new File(path);
                        if (file.exists()) {
                            return file;
                        } else {
                            path = getExternalStoragePublicDirectory + getStorageIsolationDir + getExcludeFirstPathSegment;

                        }
                    }
                    break;
                default:
                    return null;
            }
        }

        file = new File(path);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }

    }

//    public static void showDetail(Uri uri) {
//        Log.e("--Uri--", uri + "");
//        Log.e("--getPath--", "[" + uri.getPath() + "]");
//        Log.e("--getLastPathSegment--", "[" + uri.getLastPathSegment() + "]");
//        Log.e("--getQuery--", "[" + uri.getQuery() + "]");
//        Log.e("--getScheme--", "[" + uri.getScheme() + "]");
//        Log.e("--getEncodedPath--", "[" + uri.getEncodedPath() + "]");
//        Log.e("--getAuthority--", "[" + uri.getAuthority() + "]");
//        Log.e("--getEncodedAuthority--", "[" + uri.getEncodedAuthority() + "]");
//        Log.e("--getEncodedFragment--", "[" + uri.getEncodedFragment() + "]");
//        Log.e("--getUserInfo--", uri.getUserInfo() + "");
//        Log.e("--getHost--", uri.getHost() + "");
//        Log.e("--getPathSegments--", uri.getPathSegments() + "");
//        Log.e("--getSchemeSpecificPart", uri.getSchemeSpecificPart() + "");
//        Log.e("--getPort--", uri.getPort() + "");
//        Log.e("-getQueryParameterNames", uri.getQueryParameterNames() + "");
//        Log.e("--isAbsolute--", uri.isAbsolute() + "");
//        Log.e("--isHierarchical--", uri.isHierarchical() + "");
//        Log.e("--isOpaque--", uri.isOpaque() + "");
//        Log.e("--isRelative--", uri.isRelative() + "");
//    }
}
