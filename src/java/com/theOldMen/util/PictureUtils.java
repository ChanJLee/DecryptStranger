package com.theOldMen.util;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by cheng on 2015/4/24.
 */
public class PictureUtils {

    private static String TAG = "PictureUtils";
    private static final String IMAGE_TYPE = "image/*";

    /**
    * 方法名: cleanImageView(ImageView imageView)
    * 功能: 清除ImageView控件Bitmap占用的系统资源
    * 参数: ImageView imageView - 需要清除Bitmap资源的imageView
    * 返回值: 无
    */
    public static void cleanImageView(ImageView imageView) {
        if (!(imageView.getDrawable() instanceof BitmapDrawable)) {
            return;
        }
        //清除Bitmap占用的系统资源
        BitmapDrawable bitmapDrawable = (BitmapDrawable)imageView.getDrawable();
        bitmapDrawable.getBitmap().recycle();

        imageView.setImageBitmap(null);
    }

    /**
    * 方法名: openSystemGalleryIntent()
    * 功能: 创建打开系统相册的隐式Intent
    * 参数: 无
    * 返回值: Intent 创建的隐式Intent
    */
    public static Intent createOpenSystemGalleryIntent() {
        //隐式intent, ACTION_PICK表明执行从数据中返回选择项的操作
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        //设置数据以及数据类型（image）
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        return intent;
    }

    /**
     * 方法名: createOpenSystemCameraIntent()
     * 功能: 创建打开系统照相机的隐式Intent
     * 参数: 无
     * 返回值: Intent 创建的隐式Intent
     */
    public static Intent createOpenSystemCameraIntent(String name) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File tempFile = new File(Environment.getExternalStorageDirectory(),
                name);
        try {
            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }
        } catch (IOException e) {
              Log.d(TAG, "create temp file failed");
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(tempFile));
        return intent;
    }

    /**
    * 方法名: createCropPicIntent(Uri uri, int outputX, int outputY)
    * 功能: 创建剪切图片的隐式Intent
    * 参数: Uri uri - intent.getData()获得的Uri
    *      int outputX - 输出图片沿x轴宽度
    *      int outputY - 输出图片沿y轴高度
    *      String saveFileName - 输出图像文件的文件名
    * 返回值: Intent intent - 创建的隐式Intent
    */
    public static Intent createCropPicIntent(Uri uri, int outputX, int outputY, String saveFileName) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri(saveFileName));
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        intent.putExtra("circleCrop", "true"); //可能没有用
        return intent;
    }

    private static Uri getTempUri(String photoName) {
        return Uri.fromFile(getTempFile(photoName));
    }

    private static File getTempFile(String photoName) {
        if (isSDCARDMounted()) {
            File f = new File(Environment.getExternalStorageDirectory(), photoName);
            try {
                f.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "create temp file error", e);
            }
            return f;
        } else {
            return null;
        }
    }

    public static byte[] FileToByte(File file){
        byte[] buffer = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return buffer;
    }

    private static boolean isSDCARDMounted() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED))
            return true;
        return false;
    }
}
