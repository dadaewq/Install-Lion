package com.modosa.apkinstaller.util;


import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

/**
 * @author dadaewq
 * @Used android  Get the size of a folder or file  In units of B, KB, MB, GB
 * @
 * @modifiedFrom https://github.com/haiyuKing/FileSizeUtilDemo/blob/master/app/src/main/java/com/why/project/filesizeutildemo/utils/FileSizeUtil.java
 */
class FileSizeUtil {

    private static final int SIZETYPE_B = 1;//获取文件大小单位为B的double值
    private static final int SIZETYPE_KB = 2;//获取文件大小单位为KB的double值
    private static final int SIZETYPE_MB = 3;//获取文件大小单位为MB的double值
    private static final int SIZETYPE_GB = 4;//获取文件大小单位为GB的double值

    /**
     * 获取指定文件或指定文件夹的的指定单位的大小
     *
     * @param filePath 文件路径
     * @param sizeType 获取大小的类型1为B、2为KB、3为MB、4为GB
     * @return double值的大小
     */
    public static double getFolderOrFileSize(String filePath, int sizeType) {
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
            Log.e("获取文件夹大小", "获取失败!");
        }
        return FormetFileSize(blockSize, sizeType);
    }

    /**
     * 调用此方法自动计算指定文件或指定文件夹的大小
     *
     * @param filePath 文件路径
     * @return 计算好的带B、KB、MB、GB的字符串
     */
    static String getAutoFolderOrFileSize(String filePath) {
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
            Log.e("获取文件大小", "获取失败!");
        }
        return FormetFileSize(blockSize);
    }

    /**
     * 获取指定文件的大小
     *
     * @param file 指定文件
     * @return longValue
     * @throws Exception FileNotFound
     */
    private static long getFileSize(File file) throws Exception {
        long size;
        if (file.exists()) {
            FileInputStream fis;
            fis = new FileInputStream(file);
            size = fis.available();
            fis.close();
        } else {
            Log.e("获取文件大小", "文件不存在!");
            throw new Exception("FileNotFound");
        }

        return size;
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
            Log.e("获取文件夹大小", "文件夹不存在!");
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
    private static String FormetFileSize(long fileSize) {
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
    private static double FormetFileSize(long fileSize, int sizeType) {
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
                break;
        }
        return fileSizeLong;
    }
}