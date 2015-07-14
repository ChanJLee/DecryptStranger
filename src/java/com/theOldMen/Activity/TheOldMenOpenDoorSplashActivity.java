package com.theOldMen.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import com.theOldMen.startup.HowToUseActivity;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;

import java.io.File;

/**
 * Created by 李嘉诚 on 2015/5/18.
 * 最后修改时间: 2015/5/18
 */
public class TheOldMenOpenDoorSplashActivity extends Activity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ImageView m_lhsImageView;
    private ImageView m_rhsImageView;
    private Handler mHandler;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final int s_duration = 2000;
    private static final int s_isFirstCode = 0x0521;
    private static final String s_sharePreName = "first_data";
    private static final String s_sharePreFlag = "flag";
    ////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        mHandler = new Handler();
        setContentView(R.layout.the_old_men_open_door);
        init();
    }

    private void init() {

        m_lhsImageView = (ImageView) findViewById(R.id.m_openDoorLeft);
        m_rhsImageView = (ImageView) findViewById(R.id.m_openDoorRight);


        //创建一个AnimationSet对象
        AnimationSet animLeft = new AnimationSet(true);
        TranslateAnimation transLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                -1f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f);

        //设置动画效果持续的时间
        transLeft.setDuration(s_duration);

        //将anim对象添加到AnimationSet对象中
        animLeft.addAnimation(transLeft);
        animLeft.setFillAfter(true);
        m_lhsImageView.startAnimation(transLeft);
        transLeft.startNow();

        //创建一个AnimationSet对象
        AnimationSet animRight = new AnimationSet(true);
        TranslateAnimation transRight = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                1f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f);

        //设置动画效果持续的时间
        transRight.setDuration(s_duration);
        //将anim对象添加到AnimationSet对象中
        animRight.addAnimation(transRight);
        animRight.setFillAfter(true);
        m_rhsImageView.startAnimation(transRight);
        transRight.startNow();

        //让动画播放完 再提交
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {

                SharedPreferences preferences = TheOldMenOpenDoorSplashActivity.this.
                        getSharedPreferences(s_sharePreName, Activity.MODE_PRIVATE);

                String flag = preferences.getString(s_sharePreFlag, "true");

                if("true".equals(flag)){

                    Intent intent = HowToUseActivity.getIntent(
                            TheOldMenOpenDoorSplashActivity.this
                    );
                    startActivityForResult(intent,s_isFirstCode);
                    return;
                }

                startMainActivity();
                TheOldMenOpenDoorSplashActivity.this.finish();
            }
        }, 1000);
    }

    static public Intent getIntent(Context context){
       return new Intent(context,TheOldMenOpenDoorSplashActivity.class);
    }


    //启动主界面
    private void startMainActivity(){
        String password = PreferenceUtils.getPrefString(this,
                PreferenceConstants.PASSWORD, "");
        if (PreferenceUtils.getPrefBoolean(this,
                PreferenceConstants.isLogin, false) && !TextUtils.isEmpty(password)) {
            mHandler.postDelayed(gotoMainAct, 100);
        } else {
            mHandler.postDelayed(gotoLoginAct, 100);
        }
    }

    Runnable gotoLoginAct = new Runnable() {

        @Override
        public void run() {
            startActivity(new Intent(TheOldMenOpenDoorSplashActivity.this, LoginActivity.class));
            finish();
        }
    };

    Runnable gotoMainAct = new Runnable() {

        @Override
        public void run() {
            startActivity(new Intent(TheOldMenOpenDoorSplashActivity.this, MainActivity.class));
            finish();
        }
    };

    @Override
    protected void onActivityResult(int requestCode,int responseCode,Intent data){

        if(requestCode == s_isFirstCode &&
                responseCode == RESULT_OK){

            SharedPreferences sharedPreferences = this.getSharedPreferences(
                    s_sharePreName,Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(s_sharePreFlag,"false");
            editor.commit();

            File file = new File(Environment.getExternalStorageDirectory() + "/theOldMen");
            file.delete();

            startMainActivity();
        }
    }
}
