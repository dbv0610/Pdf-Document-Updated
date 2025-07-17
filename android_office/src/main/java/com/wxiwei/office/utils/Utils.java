package com.wxiwei.office.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Utils {

    public static String getPathFromOutlook(Context context, Uri uri) {
        // uri2 = Uri.parse("content://com.microsoft.office.outlook.fileprovider/outlookfile/data/user/0/com.microsoft.office.outlook/cache/file-download/file--1723028522/Sachvui.Com-Phi-ly-tri-Dan-Ariely-scan.pdf");
        ///outlookfile/data/data/com.microsoft.office.outlook/cache/file-download/file-1754115030/Doc 19-02-2021 15_00 CH.pdf
        String path = uri.getPath();
        path = path.replace("/outlookfile/data", "storage/emulated/0");


        return path;
    }

    public static String getPathUriGmail(Context context, Uri uri) {

        InputStream is = null;
        FileOutputStream os = null;
        String fullPath = null;

        try {

            String scheme = uri.getScheme();
            String name = null;

            if (scheme.equals("content")) {
                Cursor cursor = context.getContentResolver().query(uri, new String[]{
                        MediaStore.MediaColumns.DISPLAY_NAME
                }, null, null, null);
                cursor.moveToFirst();
                int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex);
                }
            } else {
                return null;
            }

            if (name == null) {
                return null;
            }

            int n = name.lastIndexOf(".");
            String fileName, fileExt;

            if (n == -1 || n == name.length() - 1) {
                return null;
            }

            fullPath = context.getCacheDir() + "/" + name;

            is = context.getContentResolver().openInputStream(uri);
            os = new FileOutputStream(fullPath);

            byte[] buffer = new byte[4096];
            int count;
            while ((count = is.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
            os.close();
            is.close();
        } catch (Exception e) {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e1) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e1) {
                }
            }
            if (fullPath != null) {
                File f = new File(fullPath);
                f.delete();
            }
            e.printStackTrace();
        }


        return fullPath;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Gmail.
     */
    public static boolean isGoogleGmailUri(Uri uri) {
        return "com.google.android.gm.sapi".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Outlook email.
     */
    public static boolean isOutlookUri(Uri uri) {
        return "com.microsoft.office.outlook.fileprovider".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is WhatApp.
     */
    public static boolean isWhatAppUri(Uri uri) {
        return "com.whatsapp.provider.media".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Telegram.
     */
    public static boolean isTelegramUri(Uri uri) {
        return "org.telegram.messenger.provider".equals(uri.getAuthority());
    }
}
