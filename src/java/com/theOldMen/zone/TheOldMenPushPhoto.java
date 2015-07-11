package com.theOldMen.zone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.gc.materialdesign.views.ButtonFloat;
import com.theOldMen.Activity.R;
import com.theOldMen.connection.SmackImpl;
import com.theOldMen.net.TheOldMenXmlParser;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 李嘉诚 on 2015/4/23.
 * 最后修改时间: 2015/4/23
 */
public class TheOldMenPushPhoto extends ActionBarActivity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String s_imageType                = "image/*";
    private static final int s_imageCode                   = 100;
    private static final int s_cameraCode                  = 101;
    private static final String s_url                      =
//            "http://192.168.155.1:8084/TheOldMenWeb/UserZoneServlet";
            "http://"+ SmackImpl.TEST_IP + ":8080/UserZoneServlet";
    public static final String s_userNameTag               = "userNameTag";
    public static final String s_userIdTag                 = "userIdTag";
    private static final int s_finshCode                   = 1000000;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private ImageView m_photoPreviewImageView              = null;
    private ButtonFloat m_cameraButtonFloat                = null;
    private ButtonFloat m_fromDiskButtonFloat              = null;
    private ButtonFloat m_pushButtonFloat                  = null;
    private String      m_userId                           = null;
    private String      m_userName                         = null;
    private EditText    m_msgEditText                      = null;
    private View        m_processBar                       = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String s_nameTag    = "name";
    public static final String s_idTag      = "id";

    public static Intent getIntent(Context context,String id, String name){

        Intent x = new Intent(context,TheOldMenPushPhoto.class);

        x.putExtra(s_idTag,id);
        x.putExtra(s_nameTag,name);

        return x;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle bundle){

        super.onCreate(bundle);
        setContentView(R.layout.the_old_men_push_photo_activity);

        //初始化
        init();
    }

    @SuppressLint("NewApi")
    private void init(){

        Intent x                = getIntent();

        if(x != null){
            m_userName  = x.getStringExtra(s_nameTag);
            m_userId    = x.getStringExtra(s_idTag);
        }

        m_photoPreviewImageView = (ImageView)findViewById(R.id.m_pushPhotoPreview);
        m_cameraButtonFloat     = (ButtonFloat)findViewById(R.id.m_photoButtonFloat);
        m_fromDiskButtonFloat   = (ButtonFloat)findViewById(R.id.m_getPhotoFromLocButtonFloat);
        m_pushButtonFloat       = (ButtonFloat)findViewById(R.id.m_pushButtonFloat);
        m_msgEditText           = (EditText)findViewById(R.id.m_messageEditText);
        m_processBar            = findViewById(R.id.m_progressBarCircularIndetermininate);


        m_cameraButtonFloat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getPhotoByTaking();
            }
        });

        m_fromDiskButtonFloat.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {getPhotoByDisk();
            }
        });

        m_pushButtonFloat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pushMessage();
            }
        });
    }

    private void getPhotoByTaking(){

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        startActivityForResult(intent, s_cameraCode);
    }

    private void getPhotoByDisk(){

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT,null);
        intent.setType(s_imageType);

        startActivityForResult(intent, s_imageCode);
    }

    private void pushMessage(){

        //
        final TheOldMenUserZoneActivity.DataHolder holder = new TheOldMenUserZoneActivity.DataHolder();

        //设置标志位 可以获得image view 当中的图片
        m_photoPreviewImageView.setDrawingCacheEnabled(true);

        //获得图片
        holder.m_image = Bitmap.createBitmap(m_photoPreviewImageView.getDrawingCache());

        //清空缓存
        m_photoPreviewImageView.setDrawingCacheEnabled(false);

        holder.m_id   = m_userId;
        holder.m_name = m_userName;
        holder.m_text = m_msgEditText.getText().toString();

        //链接网络 在另外一个线程
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                String xml                   = TheOldMenXmlParser.configToXML(holder);

                HttpClient client = new DefaultHttpClient();

                //使用POST作为请求
                HttpPost post = new HttpPost(s_url);

                //设置键值对
                List<NameValuePair> args = new ArrayList<NameValuePair>();
                args.add(new BasicNameValuePair("xml",xml));

                try {

                    //设置字符集
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(args, HTTP.UTF_8);
                    post.setEntity(entity);

                    client.execute(post);
                } catch (Exception e){}


                //发送消息到主线程 通知网络访问结束
                Message msg = m_pushHandler.obtainMessage();
                msg.what    = s_finshCode;

                m_pushHandler.sendMessage(msg);
            }
        });

        thread.start();

        m_processBar.setVisibility(View.VISIBLE);
    }

    private Handler m_pushHandler = new Handler() {

        @Override
        public void handleMessage(Message message) {

            if (message.what == s_finshCode) {
                setResult(RESULT_OK);
                TheOldMenPushPhoto.this.finish();
            } else super.handleMessage(message);
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == s_cameraCode) {
            if (resultCode == RESULT_OK) {

                Bitmap photo = data.getParcelableExtra("data");
                m_photoPreviewImageView.setImageBitmap(photo);
            }
        }

        else if (requestCode == s_imageCode) {
            if (resultCode == RESULT_OK) {

                //获得uri
                Uri uri = data.getData();
                try {

                    //获得图片
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(),
                            uri
                    );

                    //显示图片
                    m_photoPreviewImageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        else super.onActivityResult(requestCode, resultCode, data);
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
}
