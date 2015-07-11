package com.theOldMen.util;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.theOldMen.Activity.R;
import com.theOldMen.net.Constant;

import java.util.List;

/**
 * Created by 李嘉诚 on 2015/5/5.
 * 最后修改时间: 2015/5/5
 */
public class CommonUtils {

    //检测网络是否可用
    public static boolean isNetWorkConnected(Context context) {

        if (context != null) {

            //获得链接管理器
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            //获取网络信息
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();

            //如果获得了 检测能否能联网
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }

        //返回false
        return false;
    }

    //检测是否有sd卡
    public static boolean isExitsSdcard() {

        return (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)
               );
    }


    //根据资源ID 获得资源的String 内容
    static String getString(Context context, int resId){

        return context.getResources().getString(resId);
    }


    public static String getTopActivity(Context context) {

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);

        if (runningTaskInfos != null)
            return runningTaskInfos.get(0).topActivity.getClassName();
        else
            return "";
    }
}
