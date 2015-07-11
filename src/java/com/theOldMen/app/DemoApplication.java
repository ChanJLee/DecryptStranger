package com.theOldMen.app;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.baidu.mapapi.SDKInitializer;

import java.io.File;

public class DemoApplication extends Application {

    private static Context s_context;


    @Override
	public void onCreate() {
		super.onCreate();
		// 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
		SDKInitializer.initialize(this);
        s_context = getApplicationContext();

        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/TheOldMen";
        File root = new File(rootPath);
        root.mkdirs();// 没有根目录创建根目录

    }

    public static Context getContext(){
        return s_context;
    }

}