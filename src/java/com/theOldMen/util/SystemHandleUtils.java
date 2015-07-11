package com.theOldMen.util;

/**
 * Created by cheng on 2015/5/5.
 */
public class SystemHandleUtils {
    public static boolean existSDCard() {
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else
            return false;
    }
}
