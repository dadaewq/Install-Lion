package com.modosa.apkinstaller.util;


import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;

/**
 * @author dadaewq
 * @Used android  Get the size of a folder or file  In units of B, KB, MB, GB
 * @
 * @modifiedFrom https://github.com/haiyuKing/FileSizeUtilDemo/blob/master/app/src/main/java/com/why/project/filesizeutildemo/utils/FileSizeUtil.java
 */
public class FileSizeUtil {

    private static final int SIZETYPE_B = 1;
    private static final int SIZETYPE_KB = 2;
    private static final int SIZETYPE_MB = 3;
    private static final int SIZETYPE_GB = 4;

    /**
     * 获取指定文件或指定文件夹的的指定单位的大小
     *
     * @param filePath 文件路径
     * @param sizeType 获取大小的类型1为B、2为KB、3为MB、4为GB
     * @return double值的大小
     */
    static double getFolderOrFileSize(String filePath, int sizeType) {
        File file = new File(filePath);
        long blockSize = 0;
        try {
            if (file.isDirectory()) {
                blockSize = getFolderSize(file);
            } else {
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getFolderOrFileSize", "Fail !");
        }
        return formetFileSize(blockSize, sizeType);
    }

    /**
     * 调用此方法自动计算指定文件或指定文件夹的大小
     *
     * @param filePath 文件路径
     * @return 计算好的带B、KB、MB、GB的字符串
     */
    public static String getAutoFolderOrFileSize(String filePath) {
        File file = new File(filePath);
        long blockSize = 0;
        try {
            if (file.isDirectory()) {
                blockSize = getFolderSize(file);
            } else {
                blockSize = getFileSize(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getAutoFolderOrFileSize", "Fail !");
        }
        return formetFileSize(blockSize);
    }

    /**
     * 获取指定文件的大小
     *
     * @param file 指定文件
     * @return longValue
     */
    private static long getFileSize(File file) {
        return file.length();
    }

    /**
     * 获取指定文件夹的大小
     *
     * @param file 指定文件夹
     * @return longValue
     * @throws Exception FolderNotFound
     */
    private static long getFolderSize(File file) throws Exception {
        long size = 0;
        File[] flist = file.listFiles();
        if (flist == null) {
            Log.e("getFolderSize", "notExist!");
            throw new Exception("FolderNotFound");
        } else {
            for (File value : flist) {
                if (value.isDirectory()) {
                    size = size + getFolderSize(value);
                } else {
                    size = size + getFileSize(value);
                }
            }
            return size;
        }

    }

    /**
     * 转换文件大小
     *
     * @param fileSize 文件大小
     * @return 格式化显示
     */
    private static String formetFileSize(long fileSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString;
        String wrongSize = "0B";
        if (fileSize == 0) {
            return wrongSize;
        }
        if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "KB";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    /**
     * 转换文件大小,指定转换的类型
     *
     * @param fileSize 文件大小
     * @param sizeType 指定转换的类型
     * @return 格式化显示
     */
    private static double formetFileSize(long fileSize, int sizeType) {
        DecimalFormat df = new DecimalFormat("#.00");
        double fileSizeLong = 0;
        switch (sizeType) {
            case SIZETYPE_B:
                fileSizeLong = Double.parseDouble(df.format((double) fileSize));
                break;
            case SIZETYPE_KB:
                fileSizeLong = Double.parseDouble(df.format((double) fileSize / 1024));
                break;
            case SIZETYPE_MB:
                fileSizeLong = Double.parseDouble(df.format((double) fileSize / 1048576));
                break;
            case SIZETYPE_GB:
                fileSizeLong = Double.parseDouble(df.format((double) fileSize / 1073741824));
                break;
            default:
        }
        return fileSizeLong;
    }
}