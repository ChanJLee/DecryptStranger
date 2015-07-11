package com.theOldMen.ShackActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.theOldMen.Activity.R;


/**
 * Created by 李嘉诚 on 2015/4/13.
 * 最后修改时间: 2015/4/13
 */
public class TheOldMenShakeActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);


        setContentView(R.layout.the_old_men_shake_activity);

        if(bundle == null){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.m_container, new TheOldMenShakeFragment()).commit();
        }
    }

}
