package com.theOldMen.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by cheng on 2015/5/1.
 */
public class SettingPreferenceUtils {
    public static String getPreferenceString(Context context, String key, String defaultValue) {
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key, defaultValue);
    }

    public static void setPreferenceString(Context context, String key, String value) {
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(key, value).commit();
    }
}
