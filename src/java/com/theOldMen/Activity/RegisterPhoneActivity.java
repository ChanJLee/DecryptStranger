package com.theOldMen.Activity;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.theOldMen.connection.SmackImpl;
import com.theOldMen.tools.T;

import java.util.regex.Pattern;


public class RegisterPhoneActivity extends Activity{

    private EditText accountEdit;
    private EditText passwordEdit;
	private Button registComplete;
    private int registResult = -1;
    private ConnectionOutTimeProcess registOutTimeProcess;
    private Thread registThread = null;

    private static final long REGIST_SLEEP_OUT_TIME =5*1000L;//5s

    private static final int REGIST_NETWORKFAIL = 0;
    private static final int REGIST_SUCESS = 1;
    private static final int REGIST_EXIST = 2;
    private static final int REGIST_FAIL = 3;
    private static final int REGIST_OUT_TIME = 4;

    private String mAccount = null;
    private String mPassword = null;

    //正在处理的窗口
    private ProgressDialog 		m_progressDialog	= null;

    private void showProgressDialog() {

        //产生一个显示正在确定你的位置的处理对话框
        //设置点击组件外部范围为无效
        //设置风格为SPINNER
        m_progressDialog = new ProgressDialog(this);
        m_progressDialog.setCanceledOnTouchOutside(false);
        m_progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        m_progressDialog.setMessage("正在注册...");

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

    private void dismissProgressDialog(){

        if(m_progressDialog != null && m_progressDialog.isShowing())
            m_progressDialog.dismiss();
    }


    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            dismissProgressDialog();

            switch (msg.what) {
                case REGIST_NETWORKFAIL:

                    T.showShort(RegisterPhoneActivity.this, "连接失败！请检查网络连接...");
                    break;
                case REGIST_SUCESS:
                    T.showShort(RegisterPhoneActivity.this, "注册成功!");
                    startLogin();
                    break;
                case REGIST_EXIST:
                    T.showShort(RegisterPhoneActivity.this, "用户名已存在!");
                    break;
                case REGIST_FAIL:
                    T.showShort(RegisterPhoneActivity.this, "未知错误,注册失败！");
                    break;
                case REGIST_OUT_TIME:
                    if (registOutTimeProcess != null
                            && registOutTimeProcess.running)
                        registOutTimeProcess.stop();
                    registThread = null; //重置注册线程
                    T.showShort(RegisterPhoneActivity.this, "注册超时！请检查网络连接...");
                    break;
                default:
                    break;
            }
        }

    };

    //启动LoginActivity
    private void startLogin(){
        Intent x = new Intent();
        x.putExtra("account",mAccount);
        x.putExtra("password",mPassword);
        setResult(LoginActivity.GET_LOGIN_DATA,x);

        finish();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register_phone);
		findView();
		init();
	}

	private void findView(){
        accountEdit = (EditText) findViewById(R.id.register_account);
        passwordEdit = (EditText) findViewById(R.id.register_password);
		registComplete = (Button) findViewById(R.id.register_complete);
	}


    private void init() {

        registOutTimeProcess = new ConnectionOutTimeProcess();

        registComplete.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                mAccount = accountEdit.getText().toString().trim();
                mPassword = passwordEdit.getText().toString().trim();
                Pattern p = Pattern
                        .compile("[a-zA-Z0-9]+");
                if (TextUtils.isEmpty(mAccount)) {
                    Toast.makeText(RegisterPhoneActivity.this, "用户名为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!p.matcher(mAccount).matches()) {
                    Toast.makeText(RegisterPhoneActivity.this, "用户名格式不正确", Toast.LENGTH_SHORT).show();
                    return;

                }

                if (TextUtils.isEmpty(mPassword)) {
                    Toast.makeText(RegisterPhoneActivity.this, "密码为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                showProgressDialog();

                if (registOutTimeProcess != null && !registOutTimeProcess.running)
                    registOutTimeProcess.start();

                registThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        registResult = new SmackImpl(getApplicationContext()).regist(mAccount, mPassword);
                        registOutTimeProcess.stop();
                        handler.sendEmptyMessage(registResult);
                    }
                });

                registThread.start();
            }
        });
    }

    class ConnectionOutTimeProcess implements Runnable {
        public boolean running = false;
        private long startTime = 0L;
        private Thread thread = null;

        ConnectionOutTimeProcess() {
        }

        public void run() {
            while (true) {
                if (!this.running)
                    return;
                if (System.currentTimeMillis() - this.startTime > REGIST_SLEEP_OUT_TIME) {
                    handler.sendEmptyMessage(REGIST_OUT_TIME);
                }
                try {
                    Thread.sleep(10L);
                } catch (Exception localException) {
                }
            }
        }

        public void start() {
            try {
                this.thread = new Thread(this);
                this.running = true;
                this.startTime = System.currentTimeMillis();
                this.thread.start();
            } finally {
            }
        }

        public void stop() {
            try {
                this.running = false;
                this.thread = null;
                this.startTime = 0L;
            } finally {
            }
        }
    }

}

