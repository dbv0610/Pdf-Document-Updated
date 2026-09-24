package com.azg.pdf8.utils.crop.util;

import kotlin.jvm.JvmStatic;

@SuppressWarnings("unused") public class Logger {
  private static final String TAG = "SimpleCropView";
  public static boolean enabled = false;

  @JvmStatic  public static void e(String msg) {
    if (!enabled) return;
    android.util.Log.e(TAG, msg);
  }

  @JvmStatic  public static void e(String msg, Throwable e) {
    if (!enabled) return;
    android.util.Log.e(TAG, msg, e);
  }

  @JvmStatic  public static void i(String msg) {
    if (!enabled) return;
    android.util.Log.i(TAG, msg);
  }

  @JvmStatic
  public static void i(String msg, Throwable e) {
    if (!enabled) return;
    android.util.Log.i(TAG, msg, e);
  }
}
