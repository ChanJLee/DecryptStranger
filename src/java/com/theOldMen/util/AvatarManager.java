package com.theOldMen.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.theOldMen.io.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by chan on 15-7-13.
 */
public class AvatarManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////

    static private DiskLruCache s_diskLruCache;

    static {
        File file = new File(Environment.getExternalStorageDirectory() + "/theOldMen/avatar");

        if (!file.exists())
            file.mkdir();

        try {
            s_diskLruCache = DiskLruCache.open(file, 1, 1, 10 * 1024 * 1024);
        } catch (IOException e) {
        }
    }

    public static Bitmap getCacheAvatar(String key) {

        Bitmap bitmap = null;
        synchronized (s_diskLruCache) {
            try {
                DiskLruCache.Snapshot snapshot = s_diskLruCache.get(key);

                InputStream is = snapshot.getInputStream(0);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (Exception e) {}
        }
        return bitmap;
    }

    public static void addCacheAvatar(String key,byte[] bytes){

        synchronized (s_diskLruCache){
            DiskLruCache.Editor editor = null;
            try {
                editor = s_diskLruCache.edit(key);
                if(editor == null) return;

                OutputStream os = editor.newOutputStream(0);
                os.write(bytes);

                os.flush();
                os.close();

                editor.commit();

                s_diskLruCache.flush();
            } catch (Exception e) {}
        }
    }
}
