package com.theOldMen.handWrite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.theOldMen.Activity.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by 李嘉诚 on 2015/6/5.
 * 最后修改时间: 2015/6/5
 */
public class HandWriteActivity extends Activity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static final String s_fileNameTag = "fileName";
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private String          m_fileName      = null;
    private Bitmap          m_bitmap        = null;

    private PanelView       m_panelView     = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.the_old_men_hand_write_activity);

        init();
    }

    static public Intent getIntent(Context context,String filePath){

        Intent result = new Intent(context,HandWriteActivity.class);

        result.putExtra(s_fileNameTag, filePath);

        return result;
    }

    static public Intent getIntent(Context context){

        String filePath = Environment.getExternalStorageDirectory() +
                "/theOldMen/" +
                System.currentTimeMillis() +
                "send_pic.jpg";

        return getIntent(context,filePath);
    }

    @SuppressLint("HandlerLeak")
    private void init(){

        m_fileName      = getIntent().getStringExtra(s_fileNameTag);
        m_bitmap        = BitmapFactory.decodeFile(m_fileName);
        m_panelView     = (PanelView) findViewById(R.id.m_handWritePanelView);

        if(m_bitmap != null)
            m_bitmap = m_bitmap.copy(Bitmap.Config.ARGB_8888,true);

        m_panelView.setBackGround(m_bitmap);

        findViewById(R.id.m_handWriteBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent x = new Intent();
                x.putExtra(s_fileNameTag,"");
                setResult(RESULT_OK,x);
            }
        });

        findViewById(R.id.m_handWriteSendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //先保存到本地
                saveBitmapToDisk();

                Intent x = new Intent();
                x.putExtra(s_fileNameTag,m_fileName);
                setResult(RESULT_OK, x);

                finish();
            }
        });
    }

    private void saveBitmapToDisk(){

        m_bitmap = m_panelView.getBackGround();

        try {

            FileOutputStream os = new FileOutputStream(m_fileName);
            m_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);

            os.flush();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////
    }
}
