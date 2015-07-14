package com.theOldMen.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.theOldMen.Activity.R;
import com.theOldMen.CircleImage.CircleImageView;
import com.theOldMen.adapter.ExpressionAdapter;
import com.theOldMen.adapter.ExpressionPagerAdapter;
import com.theOldMen.connection.SmackImpl;
import com.theOldMen.db.ChatProvider;
import com.theOldMen.db.RosterProvider;
import com.theOldMen.handWrite.HandWriteActivity;
import com.theOldMen.layout.KugouLayout;
import com.theOldMen.service.IConnectionStatusCallback;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.tools.T;
import com.theOldMen.util.CommonUtils;
import com.theOldMen.util.MIMEFileUtil;
import com.theOldMen.util.SmileUtils;
import com.theOldMen.widget.ExpandGridView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 李嘉诚 on 2015/5/4.
 * 最后修改时间: 2015/5/4
 */

@SuppressWarnings("deprecation")
public class TheOldMenChatMainActivity extends Activity implements IConnectionStatusCallback{
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static final String s_deleteExpression       = "delete_expression";
    public static final int s_baiduMapCode              = 05252;
    private static final String s_imageType             = "image/*";
    private static final String s_bar                   = "&";
    public static final String s_latitude               = "latitude";
    public static final String s_longitude              = "longitude";

    //表情的大小
    public static final short s_expressionResSize       = 35;
    public static final short s_emojiPageSize           = 20;
    private static final int s_imageCode                = 525100;
    private static final int s_cameraCode               = 525101;
    private static final int s_videoCode                = 525102;
    private static final int s_fileCode                 = 525103;
    private static final int s_minLength                = 1000;
    private static final int s_handWriteCode            = 950521;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private View m_recordingContainer                   = null;
    private ImageView m_micImage                        = null;
    private TextView m_recordingHint                    = null;
    private ListView m_listView                         = null;
    private EditText m_editTextContent                  = null;
    private View m_buttonSetModeKeyboard                = null;
    private View m_buttonSetModeVoice                   = null;
    private RelativeLayout m_editTextLayout             = null;
    private View m_buttonSend                           = null;
    private View m_buttonPressToSpeak                   = null;
    private ViewPager m_expressionViewpager             = null;
    private LinearLayout m_emojiIconContainer           = null;
    private LinearLayout m_btnContainer                 = null;
    private ImageView m_ivEmoticonsNormal               = null;
    private ImageView m_ivEmoticonsChecked              = null;
    private Button m_btnMore                            = null;
    private View m_more                                 = null;
    private List<String> m_listRes                      = null;
    private PowerManager.WakeLock m_wakeLock            = null;
    private InputMethodManager m_manager                = null;
    private SpannableString m_span                      = new SpannableString(" ");
    private AnimationDrawable m_animationDrawable       = null;
    private MediaRecorder m_recorder                    = null;
    private String m_voiceFileName                      = null;
    private String m_capPicPath                         = null;
    private Thread picThread                            = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    static {
        File root = new File(SmackImpl.FILE_ROOT_PATH);
        root.mkdirs();// 没有根目录创建根目录
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////
    //对方ID 昵称 头像
    private static String m_toId ;
    private static String m_toName;
    private static Bitmap m_toAvatar;

    //本人ID 昵称 头像
    private static String m_myId;
    private static String m_myName;
    private static Bitmap m_myAvatar;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // 查询字段
    private static final String[] PROJECTION_FROM = new String[] {
            ChatProvider.ChatConstants._ID,
            ChatProvider.ChatConstants.DATE,
            ChatProvider.ChatConstants.DIRECTION,
            ChatProvider.ChatConstants.JID,
            ChatProvider.ChatConstants.MESSAGE,
            ChatProvider.ChatConstants.DELIVERY_STATUS
    };

    // 联系人数据监听，主要是监听对方在线状态
    private ContentObserver mContactObserver = new ContactObserver();

    // Main服务
    private XXService mXxService;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //获得服务
            mXxService = ((XXService.XXBinder) service).getService();
            mXxService.registerConnectionStatusCallback(TheOldMenChatMainActivity.this);

            // 如果没有连接上，则重新连接xmpp服务器
            if (!mXxService.isAuthenticated()) {

                //获得帐号密码  然后登录
                String usr = PreferenceUtils.getPrefString(TheOldMenChatMainActivity.this,
                        PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        TheOldMenChatMainActivity.this, PreferenceConstants.PASSWORD, "");

                mXxService.Login(usr, password);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }

    };

    /**
     * 解绑服务
     */
    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            //L.e("Service wasn't bound!");
        }
    }

    /**
     * 绑定服务
     */
    private void bindXMPPService() {
        Intent mServiceIntent = new Intent(this, XXService.class);
        Uri chatURI = Uri.parse(m_toId); //JabberId
        mServiceIntent.setData(chatURI);
        bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.the_old_men_new_chat_activity_layout);

        //做一些初始化工作
        init();

        setChatWindowAdapter();// 初始化对话数据
        getContentResolver().registerContentObserver(
                RosterProvider.CONTENT_URI, true, mContactObserver);// 开始监听联系人数据库

    }

    ////////////////////////////////////////////////
    @Override
    protected void onResume() {
        super.onResume();
        updateContactStatus();// 更新联系人状态
    }

    // 查询联系人数据库字段
    private static final String[] STATUS_QUERY = new String[] {
            RosterProvider.RosterConstants.STATUS_MESSAGE };

    private void updateContactStatus() {
        Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI,
                STATUS_QUERY, RosterProvider.RosterConstants.USER_ID + " = ?",
                new String[]{m_toId}, null);
        int MSG_IDX = cursor
                .getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

        if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            String status_message = cursor.getString(MSG_IDX);

        }
        cursor.close();
    }

    /**
     * 联系人数据库变化监听
     *
     */
    private class ContactObserver extends ContentObserver {
        public ContactObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            //L.d("ContactObserver.onChange: " + selfChange);
            updateContactStatus();// 联系人状态变化时，刷新界面
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hasWindowFocus())
            unbindXMPPService();// 解绑服务
        getContentResolver().unregisterContentObserver(mContactObserver);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 窗口获取到焦点时绑定服务，失去焦点将解绑
        if (hasFocus)
            bindXMPPService();
        else
            unbindXMPPService();
    }

    /**
     * 设置聊天的Adapter
     */
    private void setChatWindowAdapter() {
        String selection = ChatProvider.ChatConstants.JID + "='" + m_toId + "'";
        // 异步查询数据库
        new AsyncQueryHandler(getContentResolver()) {

            @Override
            protected void onQueryComplete(int token, Object cookie,
                                           Cursor cursor) {

                ListAdapter adapter = new ChatAdapter(TheOldMenChatMainActivity.this,
                        cursor, PROJECTION_FROM);
                m_listView.setAdapter(adapter);
                m_listView.setSelection(adapter.getCount() - 1);

            }
        }.startQuery(0,null,ChatProvider.CONTENT_URI, PROJECTION_FROM,
                       selection, null,null);
   }


    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        // TODO Auto-generated method stub

    }

    ///////////////////////////////////////////////

    @SuppressLint("NewApi")
    private void init() {

        //初始化视图
        initView();
        setView();
    }

    private KugouLayout m_kuGouLayoutFront;
    private KugouLayout m_kuGouLayoutBack;

    private void initView() {

        m_kuGouLayoutFront = (KugouLayout) findViewById(R.id.m_theOldMenChatFront);
        m_kuGouLayoutFront.setContentView(R.layout.the_old_men_chat_main_activity);
        m_kuGouLayoutFront.setAnimType(KugouLayout.REBOUND_ANIM);
        m_kuGouLayoutBack = (KugouLayout) findViewById(R.id.m_theOldMenChatBack);
        m_kuGouLayoutBack.setContentView(R.layout.main);
        m_kuGouLayoutFront.setLayoutCloseListener(new KugouLayout.LayoutCloseListener() {
            @Override
            public void onLayoutClose() {
                finish();
            }
        });

        m_kuGouLayoutFront.addHorizontalScrollableView(findViewById(R.id.more));

        //初始化views
        m_recordingContainer            = findViewById(R.id.recording_container);
        m_micImage                      = (ImageView)findViewById(R.id.mic_image);
        m_recordingHint                 = (TextView)findViewById(R.id.recording_hint);
        m_listView                      = (ListView)findViewById(R.id.list);
        m_editTextContent               = (EditText) findViewById(R.id.et_sendmessage);
        m_buttonSetModeKeyboard         =  findViewById(R.id.m_setModeKeyBoardButton);
        m_editTextLayout                = (RelativeLayout)findViewById(R.id.edittext_layout);
        m_buttonSetModeVoice            =  findViewById(R.id.m_setModeVoiceButton);
        m_buttonSend                    =  findViewById(R.id.btn_send);
        m_buttonPressToSpeak            =  findViewById(R.id.btn_press_to_speak);
        m_expressionViewpager           = (ViewPager)findViewById(R.id.vPager);
        m_emojiIconContainer            = (LinearLayout)findViewById(R.id.ll_face_container);
        m_btnContainer                  = (LinearLayout)findViewById(R.id.ll_btn_container);
        m_ivEmoticonsNormal             = (ImageView)findViewById(R.id.iv_emoticons_normal);
        m_ivEmoticonsChecked            = (ImageView)findViewById(R.id.iv_emoticons_checked);
        m_btnMore                       = (Button)findViewById(R.id.m_chatMoreButton);

        //正常的表情按钮显示 非正常的不显示
        m_ivEmoticonsNormal.setVisibility(View.VISIBLE);
        m_ivEmoticonsChecked.setVisibility(View.INVISIBLE);

        //更多内容的view 类型为linear layout
        m_more =  findViewById(R.id.more);

        //输入文字框 设置背景
        m_editTextLayout.setBackgroundResource(R.drawable.input_bar_bg_normal);

        // 表情list
        m_listRes = getExpressionRes(s_expressionResSize);

        // 初始化表情viewpager
        List<View> views = new ArrayList<View>();
        //获得表情的子view
        View gv1 = getGridChildView(1);
        View gv2 = getGridChildView(2);
        views.add(gv1);
        views.add(gv2);

        //设置表情页
        m_expressionViewpager.setAdapter(new ExpressionPagerAdapter(views));

        //让输入栏获得焦点
        m_editTextLayout.requestFocus();

        m_buttonPressToSpeak.setOnTouchListener(new PressToSpeakListen());

    }


    //设置视图监听器
    private void setView() {

        findViewById(R.id.m_chatPaintImageView).setOnClickListener(m_clickListener);

        //设置监听器
        m_ivEmoticonsNormal.setOnClickListener(m_clickListener);
        m_ivEmoticonsChecked.setOnClickListener(m_clickListener);
        m_buttonSend.setOnClickListener(m_clickListener);

        findViewById(R.id.m_recorderBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        m_manager   = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        //软键盘隐藏
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //
        m_wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "the_old_men");

        getBaseInfo();

        //用户点击了中间部分 就回复初始状态
        m_listView.setOnTouchListener(new View.OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //隐藏软键盘
                hideKeyboard();

                //恢复初始的样子
                m_more.setVisibility(View.GONE);
                m_ivEmoticonsNormal.setVisibility(View.VISIBLE);
                m_ivEmoticonsChecked.setVisibility(View.INVISIBLE);
                m_emojiIconContainer.setVisibility(View.GONE);
                m_btnContainer.setVisibility(View.GONE);
                return false;
            }
        });

        //当输入栏获得焦点之后  改变背景
        m_editTextContent.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    m_editTextLayout
                            .setBackgroundResource(R.drawable.input_bar_bg_active);
                } else {
                    m_editTextLayout
                            .setBackgroundResource(R.drawable.input_bar_bg_normal);
                }

            }
        });

        m_editTextContent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //点击后先更换背景
                m_editTextLayout.setBackgroundResource(R.drawable.input_bar_bg_active);

                //更多按钮消失
                m_more.setVisibility(View.GONE);

                //表情按钮出现
                m_ivEmoticonsNormal.setVisibility(View.VISIBLE);

                //点中的表情按钮消失
                m_ivEmoticonsChecked.setVisibility(View.INVISIBLE);

                //表情容器消失
                m_emojiIconContainer.setVisibility(View.GONE);

                //发送图片 视频的组件消失
                m_btnContainer.setVisibility(View.GONE);
            }
        });

        // 监听文字框
        m_editTextContent.addTextChangedListener(new TextWatcher() {
            ////////////////////////////////////////////////////////////////////////////////////////
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            ////////////////////////////////////////////////////////////////////////////////////////
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                //如果有文字  那么更多按钮不显示 而显示发送按钮
                if (!TextUtils.isEmpty(s)) {
                    m_btnMore.setVisibility(View.GONE);
                    m_buttonSend.setVisibility(View.VISIBLE);
                }
                //反之
                else {
                    m_btnMore.setVisibility(View.VISIBLE);
                    m_buttonSend.setVisibility(View.GONE);
                }
            }
            ////////////////////////////////////////////////////////////////////////////////////////
        });

        m_btnMore.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //隐藏键盘
                hideKeyboard();

                //显示更多内容
                m_more.setVisibility(View.VISIBLE);
                //隐藏表情
                m_emojiIconContainer.setVisibility(View.GONE);
                //显示按钮
                m_btnContainer.setVisibility(View.VISIBLE);
            }
        });

        m_buttonSetModeKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hideKeyboard();
                m_buttonSetModeKeyboard.setVisibility(View.GONE);
                m_buttonSetModeVoice.setVisibility(View.VISIBLE);

                findViewById(R.id.btn_press_to_speak).setVisibility(View.GONE);
                findViewById(R.id.edittext_layout).setVisibility(View.VISIBLE);
                m_more.setVisibility(View.GONE);
                m_btnMore.setVisibility(View.VISIBLE);
                m_buttonSend.setVisibility(View.GONE);
            }
        });

        m_buttonSetModeVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hideKeyboard();
                m_buttonSetModeKeyboard.setVisibility(View.VISIBLE);
                m_buttonSetModeVoice.setVisibility(View.GONE);

                findViewById(R.id.btn_press_to_speak).setVisibility(View.VISIBLE);
                findViewById(R.id.edittext_layout).setVisibility(View.GONE);
                m_more.setVisibility(View.GONE);
                m_btnMore.setVisibility(View.GONE);
                m_buttonSend.setVisibility(View.GONE);
            }
        });

        m_micImage.setImageResource(R.anim.voice_animation);
        m_animationDrawable = (AnimationDrawable) m_micImage.getDrawable();

        findViewById(R.id.btn_take_picture).setOnClickListener(m_clickListener);
        findViewById(R.id.btn_picture).setOnClickListener(m_clickListener);
        findViewById(R.id.btn_video).setOnClickListener(m_clickListener);
        findViewById(R.id.btn_file).setOnClickListener(m_clickListener);
        findViewById(R.id.btn_location).setOnClickListener(m_clickListener);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    //获得表情包的集合
    public List<String> getExpressionRes(short size) {

        //返回客户的集合
        List<String> result = new ArrayList<String>();

        for (short i = 1; i <= size; ++i) {
            //文件名
            String filename = "ee_" + i;
            result.add(filename);
        }

        return result;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private View getGridChildView(int idx) {

        //计算表情的开始 表情的结束 下标
        int begin = (idx - 1) * s_emojiPageSize;
        int end = Math.min(idx * s_emojiPageSize, m_listRes.size());

        //如果下标不合理
        if (begin < 0 || begin >= end || end < 0) return null;

        //生成表情一页的视图
        View view = View.inflate(this, R.layout.expression_gridview, null);
        ExpandGridView gv = (ExpandGridView) view.findViewById(R.id.gridview);

        //存放返回的表情列表
        List<String> list = new ArrayList<String>();

        //添加分页的子表情
        list.addAll(m_listRes.subList(begin, end));
        list.add(s_deleteExpression);

        //表情的适配器
        final ExpressionAdapter expressionAdapter = new ExpressionAdapter(this,
                1, list);

        //设置网格布局的适配器
        gv.setAdapter(expressionAdapter);

        //设置网格布局的触碰监听器
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                //获得表情的文件名
                String filename = expressionAdapter.getItem(position);

                try {
                    // 文字输入框可见时，才可输入表情
                    // 按住说话可见，不让输入表情
                    if (m_buttonSetModeKeyboard.getVisibility() != View.VISIBLE) {

                        if (!filename.equals(s_deleteExpression)) {

                            // 不是删除键，显示表情
                            // 这里用的反射，所以混淆的时候不要混淆SmileUtils这个类
                            @SuppressWarnings("rawtypes")
                            Class clz = Class.forName("com.theOldMen.util.SmileUtils");

                            //获得域
                            Field field = clz.getField(filename);

                            //获得当前的表情
                            //因为获得的是静态域 get(null) 会返回值
                            m_editTextContent.append(SmileUtils.getSmiledText(
                                    TheOldMenChatMainActivity.this, (String) field.get(null)));
                        }

                        // 删除文字或者表情
                        else {

                            //如果文本是空的 那么直接返回
                            if (TextUtils.isEmpty(m_editTextContent.getText())) return;

                            // 获取光标的位置
                            int selectionStart = m_editTextContent
                                    .getSelectionStart();
                            //如果光标不合法 直接退出
                            if (selectionStart <= 0) return;

                            //获得输入框中的内容
                            String body = m_editTextContent.getText().toString();
                            //获得子串
                            String sub = body.substring(0, selectionStart);

                            // 获取最后一个表情的位置
                            int idx = sub.lastIndexOf("[");

                            //如果当前删除的是一个表情
                            if (idx != -1) {

                                //检测是否为一个表情
                                CharSequence cs = sub.substring(idx, selectionStart);

                                //如果是表情
                                if (SmileUtils.containsKey(cs.toString())) {

                                    //就从输入框中删除
                                    m_editTextContent.getEditableText()
                                            .delete(idx, selectionStart);
                                    return;
                                }
                            }

                            //如果删除的是一个字符
                            m_editTextContent.getEditableText()
                                    .delete(selectionStart - 1,
                                            selectionStart);
                        }
                    }
                } catch (Exception e) {}
            }
        });

        return view;
    }

    //隐藏软键盘
    private void hideKeyboard() {
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                m_manager.hideSoftInputFromWindow(getCurrentFocus()
                        .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    class ChatAdapter extends SimpleCursorAdapter {

        private Context mContext;
        private LayoutInflater mInflater;
        ////////////////////////////////////////////////////////////////////////////////////////////
        private static final int s_picWidth  = 300;
        private static final int s_picHeight = 400;
        ////////////////////////////////////////////////////////////////////////////////////////////


        public ChatAdapter(Context context, Cursor cursor, String[] from) {
            super(context, 0, cursor, from, null);
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Cursor cursor = this.getCursor();
            cursor.moveToPosition(position);

            //读取信息
            String message = cursor.getString(cursor
                    .getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
            int come = cursor.getInt(cursor
                    .getColumnIndex(ChatProvider.ChatConstants.DIRECTION));// 消息来自
            boolean from_me = (come == ChatProvider.ChatConstants.OUTGOING);

            ViewHolder viewHolder = null;
            Message chatMsg = Message.analyseMsgBody(message);

            if (convertView == null || convertView.getTag(R.string.app_name + come) == null) {
                viewHolder = new ViewHolder();


                if (come == ChatProvider.ChatConstants.OUTGOING) {
                    chatMsg.m_isOut = true;
                    convertView = mInflater.inflate(R.layout.chatting_item_msg_text_right, null);
                    viewHolder.content = (TextView) convertView.findViewById(R.id.r_content);
                    viewHolder.avatar = (CircleImageView) convertView.findViewById(R.id.r_userhead);
                    viewHolder.voice = (ImageView) convertView.findViewById(R.id.m_rightVoiceImageView);

                    if(m_myAvatar == null)
                        viewHolder.avatar.setImageResource(R.drawable.avatar);
                    else
                        viewHolder.avatar.setImageBitmap(m_myAvatar);

                    viewHolder.content.setTag(chatMsg);

                } else {
                    chatMsg.m_isOut = false;
                    convertView = mInflater.inflate(R.layout.chatting_item_msg_text_left, null);
                    viewHolder.content = (TextView) convertView.findViewById(R.id.l_content);
                    viewHolder.avatar = (CircleImageView) convertView.findViewById(R.id.l_userhead);
                    viewHolder.voice = (ImageView) convertView.findViewById(R.id.m_leftVoiceImageView);
                    if(m_toAvatar == null)
                        viewHolder.avatar.setImageResource(R.drawable.avatar);
                    else
                        viewHolder.avatar.setImageBitmap(m_toAvatar);
                    viewHolder.content.setTag(chatMsg);
                }

                convertView.setTag(R.string.app_name + come,viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag(R.string.app_name + come);
            }
            //语音播放提示标签为隐藏
            viewHolder.voice.setVisibility(View.GONE);


            if (from_me
                    && !PreferenceUtils.getPrefBoolean(mContext,
                    PreferenceConstants.SHOW_MY_HEAD, true)) {
                viewHolder.avatar.setVisibility(View.GONE);
            }

            //设置发送信息的内容
            chatMsg.m_isOut = (come == ChatProvider.ChatConstants.OUTGOING);
            viewHolder.content.setTag(chatMsg);

            if(Message.s_video.equals(chatMsg.m_type) ||  //如果是图片 或者视频
                    Message.s_pic.equals(chatMsg.m_type)) {
                try {

                    //根据类型以及来源得到图片的路径
                    String picPath = (come == ChatProvider.ChatConstants.OUTGOING) ?
                            (Message.s_video.equals(chatMsg.m_type) ? (Environment.getExternalStorageDirectory()
                                    + "/theOldMen/" + chatMsg.m_time) : chatMsg.filePath) :
                            SmackImpl.FILE_ROOT_PATH + "/" + chatMsg.m_time;

                    final String toPath = picPath;

                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), Uri.fromFile(new File(picPath)));

                    if (bitmap != null) {
                        m_span.setSpan(new ImageSpan(Bitmap.createScaledBitmap(bitmap
                                        , s_picWidth, s_picHeight, true)),
                                m_span.length() - 1,
                                m_span.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        m_span.setSpan(BitmapFactory.decodeResource(getResources(), R.drawable.avatar),
                                m_span.length() - 1,
                                m_span.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (picThread == null) {
                            picThread = new Thread() {
                                @Override
                                public void run() {
                                    Bitmap bitmap2 = null;

                                    for (int time = 0; time <= 100; time++) {
                                        if (bitmap2 == null) {
                                            try {
                                                Thread.sleep(100);
                                                bitmap2 = MediaStore.Images.Media.getBitmap(
                                                        getContentResolver(), Uri.fromFile(
                                                                new File(toPath)));
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            try {
                                                bitmap2 = MediaStore.Images.Media.getBitmap(
                                                        getContentResolver(), Uri.fromFile(
                                                                new File(toPath)));
                                                m_span.setSpan(new ImageSpan(Bitmap.createScaledBitmap(bitmap2
                                                                , s_picWidth, s_picHeight, true)),
                                                        m_span.length() - 1,
                                                        m_span.length(),
                                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                                break;
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            };
                            picThread.start();
                        }

                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                viewHolder.content.setText(m_span);
            }
            //如果是文件
            else if (Message.s_file.equals(chatMsg.m_type)) {
                String picPath = (come == ChatProvider.ChatConstants.OUTGOING)?chatMsg.filePath:
                        SmackImpl.FILE_ROOT_PATH  + "/" +  chatMsg.m_time;

                String typeArgs[]  = MIMEFileUtil.getMIMEType(new File(picPath)).split("/");

                if("audio".equals(typeArgs[0]))
                    chatMsg.m_data = ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_album_black_48dp)).getBitmap();
                else if("video".equals(typeArgs[0]))
                    chatMsg.m_data = ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_videocam_black_48dp)).getBitmap();
                else if("image".equals(typeArgs[0]))
                    chatMsg.m_data = ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_image_black_48dp)).getBitmap();
                else
                    chatMsg.m_data = ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_folder_black_48dp)).getBitmap();


                m_span.setSpan(
                        new ImageSpan((Bitmap) chatMsg.m_data),
                        m_span.length() - 1,
                        m_span.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                File file = new File(picPath);
                if (file != null) {
                    viewHolder.content.setText(file.getName() + "\n");
                    viewHolder.content.append(m_span);
                } else viewHolder.content.setText(m_span);
            }else if (Message.s_location.equals(chatMsg.m_type)) {

                chatMsg.m_data =  ((BitmapDrawable)getResources().getDrawable(R.drawable.location_msg)).getBitmap();

                m_span.setSpan(
                        new ImageSpan((Bitmap) chatMsg.m_data),
                        m_span.length() - 1,
                        m_span.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                viewHolder.content.setText(m_span);
            } else if (Message.s_voice.equals(chatMsg.m_type)) {
                viewHolder.content.setText( chatMsg.msg);
                viewHolder.voice.setVisibility(View.VISIBLE);
                chatMsg.m_extraData = convertView;
            }
            //如果是文本类型
            else
                viewHolder.content.setText(convertNormalStringToSpannableString(chatMsg.msg));

            viewHolder.content.setOnClickListener(m_messageListener);

            return convertView;
        }


        private final Pattern EMOTION_URL = Pattern.compile("\\[(\\S+?)\\]");
        private final int s_smileMinShort = 6;

        private CharSequence convertNormalStringToSpannableString(String message) {

            String hackTxt;

            if (message.startsWith("[") && message.endsWith("]")) {
                hackTxt = message + " ";
            } else {
                hackTxt = message;
            }

            SpannableString value = SpannableString.valueOf(hackTxt);

            Matcher localMatcher = EMOTION_URL.matcher(value);

            while (localMatcher.find()) {

                String smile = localMatcher.group(0);

                int begin = localMatcher.start();
                int end = localMatcher.end();

                if ((end - begin) < s_smileMinShort) {

                    ImageSpan span = SmileUtils.getImageSpan(mContext, smile);

                    if (span != null)
                        value.setSpan(span, begin, end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            return value;
        }


        private class ViewHolder {
            TextView content;
            CircleImageView avatar;
            ImageView voice;

        }

    }


    private long m_startTime;
    private long m_endTime;

    private void startRecordVoice(){

        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/TheOldMen";
        File root = new File(rootPath);
        root.mkdirs();// 没有根目录创建根目录

        // 设置音频文件输出的路径
        m_voiceFileName = rootPath + "/" + System.currentTimeMillis() + ".amr";

        //L.i("voicePath:::" , m_voiceFileName);

        m_recorder = new MediaRecorder();
        m_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        m_recorder.setOutputFile(m_voiceFileName);
        m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        //最长录制一分钟
        m_recorder.setMaxDuration(1000 * 60);

        try {
            m_recorder.prepare();
        } catch (IOException e) {
        }

        m_recorder.start();
        m_startTime = System.currentTimeMillis();
    }

    private long stopRecordVoice(){

        m_endTime = System.currentTimeMillis();

        try{
            m_recorder.stop();

        }catch (Exception e){

            m_recorder.reset();
            m_recorder.release();
            return 0;
        }

        m_recorder.reset();
        m_recorder.release();

        return m_endTime - m_startTime;
    }

    @Override
    public void onPause(){

        super.onPause();
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    //按下说话的监听器
    private class PressToSpeakListen implements View.OnTouchListener {

        @SuppressLint({"ClickableViewAccessibility", "Wakelock"})
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                //按下
                case MotionEvent.ACTION_DOWN:

                    if (!CommonUtils.isExitsSdcard()) {
                        Toast.makeText(TheOldMenChatMainActivity.this, "发送语音需要sdcard支持！",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    try {

                        m_animationDrawable.start();
                        startRecordVoice();

                        //设置视图被按下
                        v.setPressed(true);

                        //添加一个wake lock
                        m_wakeLock.acquire();

                        m_recordingContainer.setVisibility(View.VISIBLE);
                        m_recordingHint.setText("手指上滑，取消发送");

                        m_recordingHint.setBackgroundColor(Color.TRANSPARENT);

                        return true;
                    } catch (Exception e) {

                        //设置视图没有被点击
                        v.setPressed(false);

                        //如果已经上了锁 那么就释放它
                        if (m_wakeLock.isHeld())
                            m_wakeLock.release();

                        m_recordingContainer.setVisibility(View.INVISIBLE);
                        Toast.makeText(TheOldMenChatMainActivity.this, "录音失败",
                                Toast.LENGTH_SHORT).show();

                        stopRecordVoice();

                        return false;
                    }

                    //移动
                case MotionEvent.ACTION_MOVE: {

                    //如果上滑过了
                    if (event.getY() < 0) {
                        m_recordingHint
                                .setText("松开手指，取消发送");
                        m_recordingHint
                                .setBackgroundResource(R.drawable.recording_text_hint_bg);
                    }

                    //提示用户取消发送的方式
                    else {
                        m_recordingHint
                                .setText("手指上滑，取消发送");
                        m_recordingHint.setBackgroundColor(Color.TRANSPARENT);
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP:

                    m_animationDrawable.stop();
                    //获得录音的长度
                    long length = stopRecordVoice();

                    //按钮设置为没有按下
                    v.setPressed(false);

                    //提示取消
                    m_recordingContainer.setVisibility(View.INVISIBLE);

                    //取消lock
                    if (m_wakeLock.isHeld())
                        m_wakeLock.release();

                    //如果向上滑动了 那么就取消发送
                    if (event.getY() < 0) {

                        return true;
                    }

                    //如果不是
                    if (length > 0 && length > s_minLength) {

                        //发送声音
                        sendVoice(m_voiceFileName, length);
                    } else {
                        Toast.makeText(getApplicationContext(), "录音时间太短",
                                Toast.LENGTH_SHORT).show();
                    }

                    return true;
                default:

                    //其他情况 隐藏视图 取消录音
                    m_recordingContainer.setVisibility(View.GONE);
                    stopRecordVoice();
                    return true;
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private View.OnClickListener m_clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            int id = view.getId();

            // 点击发送按钮(发文字和表情)
            if (id == R.id.btn_send) {

                Editable editable = m_editTextContent.getText();
                sendText(editable.subSequence(0,editable.length()));
            }

            //拍照
            else if (id == R.id.btn_take_picture) {
                 selectPicFromCamera();
            }

            //发送图片
            else if (id == R.id.btn_picture) {
                  selectPicFromLocal(); // 点击图片图标
            }

            //设置位置
            else if (id == R.id.btn_location) {

                // 位置
                startActivityForResult(new Intent(
                                TheOldMenChatMainActivity.this,
                                BaiduMapActivity.class),
                        s_baiduMapCode);

            }

            else if (id == R.id.iv_emoticons_normal) { // 点击显示表情框

                m_more.setVisibility(View.VISIBLE);
                m_ivEmoticonsNormal.setVisibility(View.INVISIBLE);
                m_ivEmoticonsChecked.setVisibility(View.VISIBLE);
                m_btnContainer.setVisibility(View.GONE);
                m_emojiIconContainer.setVisibility(View.VISIBLE);
                hideKeyboard();
            }

            else if (id == R.id.iv_emoticons_checked) { // 点击隐藏表情框

                m_ivEmoticonsNormal.setVisibility(View.VISIBLE);
                m_ivEmoticonsChecked.setVisibility(View.INVISIBLE);
                m_btnContainer.setVisibility(View.VISIBLE);
                m_emojiIconContainer.setVisibility(View.GONE);
                m_more.setVisibility(View.GONE);

            }

            else if (id == R.id.btn_video) {
                // 点击摄像图标
                Intent intent = new Intent(TheOldMenChatMainActivity.this,
                        ImageGridActivity.class);
                startActivityForResult(intent,s_videoCode);
            }

            else if (id == R.id.btn_file) { // 点击文件图标
                 selectFileFromLocal();
            }

            // 点击语音电话图标
            else if (id == R.id.m_chatPaintImageView) {

                Intent x = HandWriteActivity.getIntent(TheOldMenChatMainActivity.this);
                startActivityForResult(x,s_handWriteCode);
            }
        }
    };

    private View.OnClickListener m_messageListener = new View.OnClickListener(){
        private AnimationDrawable m_drawable                =  null;
        //语音操作对象
        private MediaPlayer m_player                        = null;


        ////////////////////////////////////////////////////////////////////////////////////////////
        private void playRecordVoice(String fileName, final View view, final Message msg) {

            if (m_player == null)
                m_player = new MediaPlayer();

            try {
                //L.i("@@@@@@@@@@@@@@@@@@@@",fileName);
                m_player.setDataSource(fileName);
                m_player.prepare();
                m_player.setLooping(false);
                //L.i("@@@@@@@@@@@@@@@@@@@@",fileName);

                ImageView v = null;
                if (msg.m_isOut) {
                    v = (ImageView) view.findViewById(R.id.m_rightVoiceImageView);

                    v.setImageResource(R.anim.voice_to_icon);
                    m_drawable = (AnimationDrawable) v.getDrawable();
                } else {
                    v = (ImageView) view.findViewById(R.id.m_leftVoiceImageView);

                    v.setImageResource(R.anim.voice_from_icon);
                    m_drawable = (AnimationDrawable) v.getDrawable();
                }

                //当语音播放结束后
                m_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {

                        //暂停动画播放
                        m_drawable.stop();

                        //判断语音的去向  重新设置语音的背景
                        if (msg.m_isOut)

                            ((ImageView) view.findViewById(R.id.m_rightVoiceImageView)).
                                    setImageResource(R.drawable.chatto_voice_playing);
                        else
                            ((ImageView) view.findViewById(R.id.m_leftVoiceImageView)).
                                setBackgroundResource(R.drawable.chatfrom_voice_playing);

                        //释放播放器资源
                        m_player.release();
                        m_player = null;
                    }
                });

                //开始动画播放
                m_drawable.start();
                //开始语音播放
                m_player.start();
            } catch (Exception e) {}
        }
        ////////////////////////////////////////////////////////////////////////////////////////////

        //说好一起到白头 而你却偷偷焗了油

        @Override
        public void onClick(View v) {

            Message msg = (Message)(v.getTag());

            if(msg == null) return;

            if(Message.s_file.equals(msg.m_type) ||
                    Message.s_video.equals(msg.m_type)) {

                String fileName = (Message.s_file.equals(msg.m_type))?msg.m_time :msg.filePath;

                String filePath = msg.m_isOut?(Message.s_file.equals(msg.m_type)?msg.filePath:msg.receive):
                        SmackImpl.FILE_ROOT_PATH + "/" + fileName;

                Intent x = MIMEFileUtil.getOpenMIMEFileIntent(new File(filePath));
                startActivity(x);
            }

            else if(Message.s_voice.equals(msg.m_type)) {
                //L.i("voiceFilePath*************",msg.filePath);

                String fileName = null ;
                String[] pathStrings = msg.filePath.split("/");
                if (pathStrings!=null && pathStrings.length>0) {
                    fileName = pathStrings[pathStrings.length-1];
                }
                String voicePath =(msg.m_isOut)?msg.filePath:
                        SmackImpl.FILE_ROOT_PATH + "/" + fileName;

                //L.i("voiceFilePath*************",voicePath);
                playRecordVoice(voicePath, (View) msg.m_extraData, msg);
            }

            else if(Message.s_location.equals(msg.m_type)) {

                String location = msg.filePath;
                //L.i("location*************",location);
                String[] args   = location.split(s_bar);

                Intent x        = new Intent(TheOldMenChatMainActivity.this, BaiduMapPreviewActivity.class);

                x.putExtra(s_latitude, args[0]);
                x.putExtra(s_longitude, args[1]);

                startActivity(x);
            }

            else if(Message.s_pic.equals(msg.m_type)){
                String picPath = (msg.m_isOut)?msg.filePath:
                        SmackImpl.FILE_ROOT_PATH + "/" + msg.m_time;

                Intent x = MIMEFileUtil.getOpenMIMEFileIntent(new File(picPath));
                startActivity(x);
            }
        }
    };
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private void selectFileFromLocal() {

        Intent intent = null;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

        } else {

            intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }

        startActivityForResult(intent, s_fileCode);
    }

    private void selectPicFromLocal() {

        if (!CommonUtils.isExitsSdcard()) {

            Toast.makeText(getApplicationContext(), "SD卡不存在",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT,null);
        intent.setType(s_imageType);

        startActivityForResult(intent, s_imageCode);
    }

    private void selectPicFromCamera() {

        if (!CommonUtils.isExitsSdcard()) {
            Toast.makeText(getApplicationContext(), "SD卡不存在",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File tempFile = new File(Environment.getExternalStorageDirectory() + "/theOldMen/"
                , System.currentTimeMillis() + "send_pic.jpg");

        m_capPicPath = tempFile.toString();

        try {
            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }
        } catch (IOException e) {
            //L.i("Createpic", "create temp file failed");
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(tempFile));

//        intent.putExtra("picPath",path);
        startActivityForResult(intent, s_cameraCode);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        boolean isCamera = true;
        if (requestCode == s_cameraCode) {
            if (resultCode == RESULT_OK) {

//                Bitmap photo = data.getParcelableExtra("data");
                Uri sendUri = Uri.fromFile(new File(m_capPicPath));
                sendPic(isCamera,sendUri);
            }
        }

        else if (requestCode == s_imageCode) {
            if (resultCode == RESULT_OK) {

                //获得uri
                Uri uri = data.getData();
                try {

                    sendPic(!isCamera,uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        else if(requestCode == s_videoCode){
            if(resultCode == RESULT_OK){

                //视频路径
                String path = data.getStringExtra("path");

                //视频的截图
                Bitmap bitmap        = null;

                try {

                    //创建一个缩略图
                    bitmap = ThumbnailUtils.createVideoThumbnail(path, 3);

                    //如果返回的是null 那么我们就用默认的图标
                    if (bitmap == null) {

                        bitmap = BitmapFactory.decodeResource(getResources(),
                                R.drawable.app_panel_video_icon);
                    }
                    else
                        bitmap = Bitmap.createBitmap(bitmap);
                } catch (Exception e) {}
                String bitmapPath = Environment.getExternalStorageDirectory() + "/theOldMen/" +
                        System.currentTimeMillis() + "_video.jpg";
                File videoFile = new File(bitmapPath);
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(videoFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                sendVideo(path, bitmapPath);
            }
        }

        else if(requestCode == s_fileCode &&
                resultCode == RESULT_OK &&
                data != null){

                    Uri uri = data.getData();
                    if (uri != null) {

                        String filePath = null;

                        if ("content".equalsIgnoreCase(uri.getScheme())) {

                            String[] projection = { "_data" };
                            Cursor cursor = null;

                            try {

                                cursor = getContentResolver().query(uri, projection, null,
                                        null, null);
                                int column_index = cursor.getColumnIndexOrThrow("_data");

                                if (cursor.moveToFirst()) {
                                    filePath = cursor.getString(column_index);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if ("file".equalsIgnoreCase(uri.getScheme())) {
                            filePath = uri.getPath();
                        }

                        //检测文件是否存在
                        File file = new File(filePath);

                        if (file == null || !file.exists()) {
                            Toast.makeText(getApplicationContext(), "文件不存在", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        //如果文件过大
                        if (file.length() > 10 * 1024 * 1024) {

                            Toast.makeText(getApplicationContext(), "文件不能大于10M",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        //发送消息
                        sendFile(filePath);
                    }
        }

        else if(requestCode == s_baiduMapCode &&
                resultCode == RESULT_OK){

            sendLoc(data.getDoubleExtra(BaiduMapActivity.s_latitude,0.0),
                    data.getDoubleExtra(BaiduMapActivity.s_longitude,0.0),
                    data.getStringExtra(BaiduMapActivity.s_address));
        }
        else if(requestCode == s_handWriteCode &&
                resultCode == RESULT_OK){

            String filePath = data.getStringExtra(HandWriteActivity.s_fileNameTag);

            if(TextUtils.isEmpty(filePath)) return;

            String[] pathStrings = filePath.split("/"); // 文件名
            String fileName = null ;

            if (pathStrings !=  null && pathStrings.length  >   0) {
                fileName = pathStrings[pathStrings.length - 1];
            }

            Message sendChatMsg = new Message(m_toId, "发送图片",
                    Message.s_pic,fileName, filePath ,false);

            sendFileto(filePath);

            // 发送消息
            mXxService.sendMessage(m_toId,Message.toJson(sendChatMsg));
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////


    static public Intent getChatIntent(Context context,String toId,String fromId,String toName,
                                   String fromName,String toAvatarFileName,String fromAvatarFileName) {
        Intent x = new Intent(context, TheOldMenChatMainActivity.class);

        m_toId = toId;
        m_toName = toName;
        m_myId = fromId;
        m_myName = fromName;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outHeight = options.outWidth = s_defaultAvatarSize;

  /*      Bitmap bitmap = AvatarManager.getCacheAvatar(toAvatarFileName);
        m_toAvatar = Bitmap.createScaledBitmap(
                bitmap,
                s_defaultAvatarSize,
                s_defaultAvatarSize,
                false
        );
        bitmap.recycle();

        bitmap = AvatarManager.getCacheAvatar(fromAvatarFileName);
        m_myAvatar = Bitmap.createScaledBitmap(
                bitmap,
                s_defaultAvatarSize,
                s_defaultAvatarSize,
                false
        );
        bitmap.recycle();*/

        m_toAvatar = BitmapFactory.decodeFile(toAvatarFileName, options);
        m_myAvatar = BitmapFactory.decodeFile(fromAvatarFileName, options);
        return x;
    }

    private static final short s_defaultAvatarSize = 100;

    static public Intent getChatIntent(Context context,String toId) {
        Intent x = new Intent(context, TheOldMenChatMainActivity.class);

        m_toId = toId;

        return x;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //获取对方的ID网名 头像 以及自己用户的头像
    private void getBaseInfo(){

        ((TextView)  findViewById(R.id.m_friendNamePreviewTextView)).setText(m_toName);
    }

    //发送文字
    private void sendText(CharSequence text) {

        if (mXxService != null) {

            Message chatMsg = new Message(m_toId, text.toString());
            mXxService.sendMessage(m_toId,Message.toJson(chatMsg));
            //L.i("!!!!!!!!!!!!!!!!!!!!!!!m_toId",m_toId);
            if (!mXxService.isAuthenticated()){
                T.showShort(this, "消息已经保存随后发送");
            }
        }

        //清空输入框
        m_editTextContent.setText("");
    }

    private void sendPic(boolean isCamera , Uri uri){

        String filePath = null;
        if(isCamera)
            filePath = uri.getPath();
        else {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex("_data");
            filePath = cursor.getString(columnIndex);

            cursor.close();
            if (filePath == null || filePath.equals("null")) {
                Toast toast = Toast.makeText(this, "找不到图片", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Intent x = HandWriteActivity.getIntent(TheOldMenChatMainActivity.this,filePath);
        startActivityForResult(x, s_handWriteCode);
    }

    private void sendVideo(String videoPath,String bitmapPath){


        String bitmap[] = bitmapPath.split("/");
        String bitmapName = bitmap[bitmap.length-1];

        String video[] = videoPath.split("/");
        String videoName = video[video.length-1];

        Message sendChatMsg = new Message(m_toId, "发送小视屏",
                Message.s_video,bitmapName, videoName, videoPath);

        sendFileto(bitmapPath);
        sendFileto(videoPath);

        // 发送消息
        mXxService.sendMessage(m_toId,Message.toJson(sendChatMsg));
    }

    //发送文件
    private void sendFile(String filePath) {


        String[] pathStrings = filePath.split("/"); // 文件名
        String fileName = null ;
        if (pathStrings!=null && pathStrings.length>0) {
            fileName = pathStrings[pathStrings.length-1];
        }

        Message sendChatMsg = new Message(m_toId, "发送文件",
                Message.s_file,fileName, filePath ,false);

        sendFileto(filePath);

        // 发送消息
        mXxService.sendMessage(m_toId,Message.toJson(sendChatMsg));

    }

    //发送定位消息
    private void sendLoc(double latitude,double longitude,String address){

        String data = latitude + s_bar + longitude + s_bar + address;
        Message chatMsg = new Message(m_toId, "发送坐标",
                Message.s_location,"", data ,false);
        mXxService.sendMessage(m_toId,Message.toJson(chatMsg));
    }


    public void sendFileto(String path) {


        try {
            String sendUserId = m_toId + "/" + PreferenceUtils.getPrefString(this,PreferenceConstants.RESSOURCE
                    ,SmackImpl.IDENTITY_NAME);
            mXxService.sendFile(sendUserId, new java.io.File(path));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendVoice(String filePath,long length){

        String voiceName = (length / 1000) + "\"" + (length % 1000) + "    ";

        Message chatMsg = new Message(m_toId,voiceName,Message.s_voice,length+"",filePath , false);
        sendFileto(filePath);
        mXxService.sendMessage(m_toId,Message.toJson(chatMsg));

    }
}
