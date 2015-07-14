package com.theOldMen.zone;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;
import com.theOldMen.Activity.LoginActivity;
import com.theOldMen.Activity.R;
import com.theOldMen.adapter.PromotedActionsLibrary;
import com.theOldMen.connection.SmackImpl;
import com.theOldMen.net.TheOldMenXmlParser;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.L;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.util.DialogUtil;
import com.theOldMen.view.LDialog.BaseLDialog;
import com.theOldMen.widget.SwipeListView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jdom2.JDOMException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by 李嘉诚 on 2015/4/22.
 * 最后修改时间: 2015/4/22
 */
public class TheOldMenUserZoneActivity extends ActionBarActivity {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String s_url                       =
//            "http://192.168.155.1:8084/TheOldMenWeb/GetUserZoneMsgServlet";
            "http://" + SmackImpl.TEST_IP + ":8080" + "/GetUserZoneMsgServlet";
    private static final int s_getDataTag                   = 0525;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //显示用户发送内容的LIST View
    private SwipeListView m_swipeListView        = null;
    //存放从服务器获得的数据
    private List<DataHolder>         m_data                 = new ArrayList<DataHolder>();
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private XXService mXxService;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            // 开始连接xmpp服务器
            if (!mXxService.isAuthenticated()) {
                String usr = PreferenceUtils.getPrefString(TheOldMenUserZoneActivity.this,
                        PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        TheOldMenUserZoneActivity.this, PreferenceConstants.PASSWORD, "");
                mXxService.Login(usr, password);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }
    };

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.the_old_men_user_zone_activity);

        startService(new Intent(TheOldMenUserZoneActivity.this, XXService.class));
        //初始化
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        bindXMPPService();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindXMPPService();
    }

    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            L.i(LoginActivity.class, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            L.e(LoginActivity.class, "Service wasn't bound!");
        }
    }
    private void bindXMPPService() {
        L.i(LoginActivity.class, "[SERVICE] Unbind");
        bindService(new Intent(TheOldMenUserZoneActivity.this, XXService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }

    private void init(){

        //设置list view 的视图
        m_swipeListView      = (SwipeListView)findViewById(R.id.m_swipeListView);

        //设置适配器
        m_swipeListView.setAdapter(m_adapter);

        getData();


        FrameLayout layout = (FrameLayout) findViewById(R.id.userZoneFrameLayoutContainer);

        PromotedActionsLibrary promotedActionsLibrary = new PromotedActionsLibrary();

        promotedActionsLibrary.setup(getApplicationContext(), layout);


        promotedActionsLibrary.addItem(getResources().getDrawable(R.drawable.ic_autorenew_grey600_24dp), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getData();
            }
        });

        promotedActionsLibrary.addItem(getResources().getDrawable(R.drawable.ic_send_grey600_24dp), new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //按下按钮 就打开发送说说的界面
                Intent intent = getIntent();

                if(intent == null) return;

                //启动界面
                startActivityForResult(TheOldMenPushPhoto.getIntent(
                        TheOldMenUserZoneActivity.this,
                        intent.getStringExtra(s_idTag),
                        intent.getStringExtra(s_nameTag)), s_pushActivityRequestCode);
            }
        });

        promotedActionsLibrary.addMainItem(getResources().getDrawable(R.drawable.ic_add));

        findViewById(R.id.m_userZoneBackButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private static final int s_pushActivityRequestCode = 0521;

    @Override
    protected void onActivityResult(int requestCode,int responseCode,Intent data){

        if(requestCode == s_pushActivityRequestCode &&
                responseCode == RESULT_OK){

            getData();
        }
        else super.onActivityResult(requestCode,responseCode,data);
    }

    //正在处理的窗口
    private ProgressDialog 		m_progressDialog	= null;

    private void showProgressDialog() {

        //产生一个显示正在确定你的位置的处理对话框
        //设置点击组件外部范围为无效
        //设置风格为SPINNER
        m_progressDialog = new ProgressDialog(this);
        m_progressDialog.setCanceledOnTouchOutside(false);
        m_progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        m_progressDialog.setMessage("正在下载数据...");

        //设置用户选择取消后的动作
        //即退出定位窗口 并且不发送定位消息
        m_progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface arg0) {

                if (m_progressDialog.isShowing()) {
                    m_progressDialog.dismiss();
                }

                finish();
            }
        });

        m_progressDialog.show();
    }

    private void getData(){

        showProgressDialog();

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpClient client = new DefaultHttpClient();
                HttpGet   get     = new HttpGet(s_url);

                String xml        = "";

                try {

                    HttpResponse response = client.execute(get);
                    if(response.getStatusLine().getStatusCode() == 200){

                        xml = EntityUtils.toString(response.getEntity(), HTTP.UTF_8);

                        Message msg               = m_getDataHander.obtainMessage();

                        msg.what                  = s_getDataTag;
                        msg.obj                   = xml;

                        //发送报文
                        m_getDataHander.sendMessage(msg);
                    }
                } catch (IOException e) {}
            }
        }).start();
    }

    static final public class DataHolder{
        //用户的网名
        public String m_name    = null;
        //用户的说说文字内容
        public String m_text    = null;
        //用户的图片
        public Bitmap m_image   = null;

        public String m_id      = null;
    }

    //这里可以存储你感兴趣的信息
    static final public class ViewHolder{

        public TextView m_nameTextView;
        public TextView m_contentTextView;
        public ImageView m_photoImageView;
    }

    public static final String s_nameTag = "name";
    public static final String s_idTag = "id";


    public static Intent getIntent(Context context, String id,String name){
        Intent x  = new Intent(context,TheOldMenUserZoneActivity.class);
        x.putExtra(s_idTag, id);
        x.putExtra(s_nameTag, name);
        return x;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private BaseAdapter m_adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return m_data.size();
        }
        @Override
        public Object getItem(int position) {
            return m_data.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("NewApi")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            //保存 数据
            DataHolder dataHolder           = m_data.get(position);
            ViewHolder viewHolder           = null;

            //如果当前的视图是空的
            if(convertView == null){

                //那么膨胀视图
                LayoutInflater inflater = LayoutInflater.from(TheOldMenUserZoneActivity.this);
                convertView             = inflater.inflate(R.layout.user_zone_layout,parent,false);

                viewHolder = new ViewHolder();

                //显示用户名
                viewHolder.m_nameTextView    = (TextView)convertView.findViewById(R.id.m_frontUserNameTextView);

                //显示说说的内容
                viewHolder.m_contentTextView  = (TextView)convertView.findViewById(R.id.m_frontUserInfoTextView);

                //显示发送的图片
                viewHolder.m_photoImageView = (ImageView)convertView.findViewById(R.id.m_frontUserImageView);

                //设置与视图绑定的数据
                convertView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final String friendId = dataHolder.m_id;
            final String friendName = dataHolder.m_name;

            //当点击添加好友的按钮时 发送添加好友的请求
            final ButtonFloat mkFriendButton = (ButtonFloat)convertView.findViewById(R.id.m_mkFriendButton);
            mkFriendButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //弹出对话框
                    BaseLDialog.Builder builder = new BaseLDialog.Builder(TheOldMenUserZoneActivity.this);
                    BaseLDialog baseLDialog =
                            builder.setTitle("发送好友申请")
                                    .setTitleColor("#434343")
                                    .setContent("添加"+friendName+"("+friendId+")"+"为好友?")
                                    .setContentColor(Color.parseColor("blue"))
                                    .setContentSize(20)
                                    .setPositiveButtonText("确定")
                                    .setPositiveColor("#3c78d8")
                                    .setNegativeButtonText("取消")
                                    .setNegativeColor("#cccccc")
                                    .create();
                    baseLDialog.setListeners(new BaseLDialog.ClickListener() {
                        @Override
                        public void onConfirmClick() {

                            makeFriend(friendId);

                            BaseLDialog.Builder builder = new BaseLDialog.Builder(TheOldMenUserZoneActivity.this);
                            BaseLDialog dialog =
                                    builder .setContent("申请发送成功!")
                                            .setContentSize(20)
                                            .setContentColor("#bcb8b7")
                                            .setMode(false)
                                            .setPositiveButtonText("确定")
                                            .setPositiveColor("#3c78d8")
                                            .create();
                            dialog.setListeners(new BaseLDialog.ClickListener() {
                                @Override
                                public void onConfirmClick() {
                                }

                                @Override
                                public void onCancelClick() {
                                }
                            });
                            dialog.show();
                        }
                        @Override
                        public void onCancelClick() {}
                    });
                    DialogUtil.setPopupDialog(TheOldMenUserZoneActivity.this, baseLDialog);
                    baseLDialog.show();
                }
            });

            viewHolder.m_nameTextView.setText(dataHolder.m_name);

            viewHolder.m_contentTextView.setText(dataHolder.m_text);

            viewHolder.m_photoImageView.setImageBitmap(dataHolder.m_image);

            //返回设置好的视图
            return convertView;
        }
    };
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private static final short s_getDataFinishTag = 0x0521;

    private Handler m_getDataHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == s_getDataTag) {

                final String xml = (String) msg.obj;

                if (!TextUtils.isEmpty(xml)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                TheOldMenXmlParser.xmlToConfig(xml, m_data);

                                Message message = m_getDataHander.obtainMessage();
                                message.what = s_getDataFinishTag;

                                m_getDataHander.sendMessage(message);
                            } catch (Exception e) {
                            }
                        }
                    }).start();
                } else {
                    dismissProgressDialog();
                    Toast.makeText(TheOldMenUserZoneActivity.this,
                            "没有获取到数据", Toast.LENGTH_SHORT).show();
                }
            } else if (msg.what == s_getDataFinishTag) {
                dismissProgressDialog();
                m_adapter.notifyDataSetChanged();
            }
        }
    };


    private void dismissProgressDialog(){

        if(m_progressDialog != null && m_progressDialog.isShowing())
            m_progressDialog.dismiss();
    }

    private void makeFriend(final String friendId){
        mXxService.addRosterItem(friendId , "", "");
    }
}
