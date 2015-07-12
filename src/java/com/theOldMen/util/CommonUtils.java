package com.theOldMen.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
            NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();

            //如果获得了 检测能否能联网
            if (networkInfo != null) {
                return networkInfo.isAvailable();
            }
        }

        //返回false
        return false;
    }

    //检测是否有sd卡
    public static boolean isExitsSdcard() {

        return (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED
        ));
    }
}
