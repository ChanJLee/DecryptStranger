package com.theOldMen.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.gc.materialdesign.views.ProgressBarDeterminate;

/**
 * Created by 李嘉诚 on 2015/5/18.
 * 最后修改时间: 2015/5/18
 */
public class TheOldMenSplashActivity extends Activity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static final int s_processTag = 0521;
    static final int s_finishTag  = 0525;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ProgressBarDeterminate m_processBar;
    ////////////////////////////////////////////////////////////////////////////////////////////////


    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.the_old_men_splash);

        init();
    }

    private void init(){

        m_processBar =
                (ProgressBarDeterminate) findViewById(R.id.m_splashProcessBar);
        m_processBar.setMax(100);

        new Thread(new Runnable() {
            @Override
            public void run() {

                handleBackground();
            }
        }).start();
    }

    //后台处理的代码
    private void handleBackground(){

        //demo 代码 开始
        for(int i = 0;i < 100;++i){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Message message = m_handler.obtainMessage();
            message.what = s_processTag;
            message.arg1 = i;
            m_handler.sendMessage(message);
        }
        //demo 代码 结束


        Message message = m_handler.obtainMessage();
        message.what = s_finishTag;
        m_handler.sendMessage(message);
    }

    private Handler m_handler = new Handler(){
        @Override
        public void handleMessage(Message message){

            if(message.what == s_processTag){
                m_processBar.setProgress(message.arg1);
            }
            else if(message.what == s_finishTag){

                startActivity(TheOldMenOpenDoorSplashActivity.getIntent(TheOldMenSplashActivity.this));
                finish();
            }
            else super.handleMessage(message);
        }
    };
}
