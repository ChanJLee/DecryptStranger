package com.theOldMen.util;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.easemob.chat.EMMessage;
import com.easemob.chat.TextMessageBody;
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

    //根据消息内容和消息类型获取消息内容提示
    public static String getMessageDigest(EMMessage message, Context context) {

        String digest = "";

        switch (message.getType()) {

            // 位置消息
            case LOCATION:
                if (message.direct == EMMessage.Direct.RECEIVE)
                {
                    digest = getString(context, R.string.location_recv);

                    digest = String.format(digest, message.getFrom());
                    return digest;

                }
                else
                {
                    digest = getString(context, R.string.location_prefix);
                }
                break;

            // 图片消息
            case IMAGE:
                digest = getString(context, R.string.picture);
                break;

            // 语音消息
            case VOICE:
                digest = getString(context, R.string.voice);
                break;

            // 视频消息
            case VIDEO:
                digest = getString(context, R.string.video);
                break;

            // 文本消息
            case TXT:
                if(!message.getBooleanAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL,false)){
                    TextMessageBody txtBody = (TextMessageBody) message.getBody();
                    digest = txtBody.getMessage();
                }else{
                    TextMessageBody txtBody = (TextMessageBody) message.getBody();
                    digest = getString(context, R.string.voice_call) + txtBody.getMessage();
                }
                break;

            //普通文件消息
            case FILE:
                digest = getString(context, R.string.file);
                break;

            //未知类型
            default:
                System.err.println("error, unknow type");
                return "";
        }

        return digest;
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
