package com.yalantis.ucrop.task;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yalantis.ucrop.model.ExifInfo;

public class BitmapWorkerResult2 {
    public Bitmap mBitmapResult;
    public ExifInfo mExifInfo;
    public String mImageInputPath;
    public String mImageOutputPath;
    public Exception mBitmapWorkerException;

    public BitmapWorkerResult2(@NonNull Bitmap bitmapResult, @NonNull ExifInfo exifInfo,
                               @NonNull String imageInputPath, @Nullable String imageOutputPath) {
        mBitmapResult = bitmapResult;
        mExifInfo = exifInfo;
        mImageInputPath = imageInputPath;
        mImageOutputPath = imageOutputPath;
    }

    public BitmapWorkerResult2(@NonNull Exception bitmapWorkerException) {
        mBitmapWorkerException = bitmapWorkerException;
    }
}