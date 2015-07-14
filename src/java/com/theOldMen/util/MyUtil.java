package com.theOldMen.util;

/**
 * Created by jz on 2015/5/11.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.TypedValue;

import com.theOldMen.exception.MyException;


public class MyUtil {
    private static final Pattern EMOTION_URL = Pattern.compile("\\[(\\S+?)\\]");

    public static void verifyJabberID(String jid)
            throws MyException {
        if (jid != null) {
            Pattern p = Pattern
                    .compile("(?i)[a-z0-9\\-_\\.]++@[a-z0-9\\-_]++(\\.[a-z0-9\\-_]++)++");
            Matcher m = p.matcher(jid);

            if (!m.matches()) {
                throw new MyException(
                        "Configured Jabber-ID is incorrect!");
            }
        } else {
            throw new MyException("Jabber-ID wasn't set!");
        }
    }

    public static void verifyJabberID(Editable jid)
            throws MyException {
        verifyJabberID(jid.toString());
    }

    public static int tryToParseInt(String value, int defVal) {
        int ret;
        try {
            ret = Integer.parseInt(value);
        } catch (NumberFormatException ne) {
            ret = defVal;
        }
        return ret;
    }

    public static int getEditTextColor(Context ctx) {
        TypedValue tv = new TypedValue();
        boolean found = ctx.getTheme().resolveAttribute(
                android.R.attr.editTextColor, tv, true);
        if (found) {
            // SDK 11+
            return ctx.getResources().getColor(tv.resourceId);
        } else {
            // SDK < 11
            return ctx.getResources().getColor(
                    android.R.color.primary_text_light);
        }
    }

    public static String splitJidAndServer(String account) {
        if (!account.contains("@"))
            return account;
        String[] res = account.split("@");
        String userName = res[0];
        return userName;
    }


    public static String getUserAvatarPath(String user){
        String userName = user.split("@")[0];
        return Environment.getExternalStorageDirectory() + "/theOldMen/"+
                userName +"_avatar.jpg";
    }

    public static String getUserAvatarKey(String user){
        return user.split("@")[0];
    }
}
