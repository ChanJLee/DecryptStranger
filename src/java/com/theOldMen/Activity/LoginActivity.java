package com.theOldMen.Activity;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.dd.CircularProgressButton;
import com.gc.materialdesign.views.ButtonFloat;
import com.gc.materialdesign.views.CheckBox;
import com.theOldMen.CircleImage.CircleImageView;
import com.theOldMen.app.DemoApplication;
import com.theOldMen.service.IConnectionStatusCallback;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.tools.T;
import com.theOldMen.util.PictureUtils;
import com.theOldMen.view.LDialog.BaseLDialog;

import org.jivesoftware.smackx.packet.VCard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by jz on 2015/4/15.
 */
public class LoginActivity extends Activity  implements
        IConnectionStatusCallback {

    public static final String LOGIN_ACTION = "com.theOldMen.action.LOGIN";
    public static final long LOGIN_SLEEP_OUT_TIME = 10*1000L;
    private static final int LOGIN_OUT_TIME = 0;

    private Context mContext;
    private CircularProgressButton mLogin;
    private Button register;
    private EditText mAccountEdit;
    private EditText mPasswordEdit;
    private CheckBox mAutoSavePasswordCK;
    private CheckBox mHideLoginCK;
    private String mAccount;
    private String mPassword;
    private CircleImageView mLoginHead;
    private CircleImageView mLoginImage;

    private ConnectionOutTimeProcess mLoginOutTimeProcess;
    private XXService mXxService;

    public final static int GET_LOGIN_DATA = 100;

    private final static int REQUEST_FROM_CAMERA = 0;
    private final static int REQUEST_FROM_GALLERY = 1;
    private final static int REQUEST_FOR_RESULT = 2;

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();

            mXxService.registerConnectionStatusCallback(LoginActivity.this);
            // 开始连接xmpp服务器
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }
    };

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LOGIN_OUT_TIME:
                    if (mLoginOutTimeProcess != null
                            && mLoginOutTimeProcess.running)
                        mLoginOutTimeProcess.stop();
                    T.showShort(LoginActivity.this, R.string.timeout_try_again);
                    break;

                default:
                    break;
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(LoginActivity.this, XXService.class));
        bindXMPPService();

        setContentView(R.layout.activity_login);
        mContext = this;

        findView();
        init();
    }


    private void findView() {

        mLogin = (CircularProgressButton) findViewById(R.id.login);
        register = (Button) findViewById(R.id.register);
        mAccountEdit = (EditText) findViewById(R.id.account);
        mPasswordEdit = (EditText) findViewById(R.id.password);
        mAutoSavePasswordCK = (CheckBox) findViewById(R.id.auto_save_password);
        mHideLoginCK = (CheckBox) findViewById(R.id.hide_login);
        mLoginImage = (CircleImageView)findViewById(R.id.login_image);

    }

    private Boolean getIntentExtra(){
        if(getIntent()!=null){
            Intent intent = getIntent();
            if(intent.getExtras()!=null)
                return true;
        }
        return false;
    }

    private void init() {
        Animation anim = AnimationUtils.loadAnimation(mContext, R.anim.login_anim);

        anim.setFillAfter(true);

        String account = "";
        String password = "";
        if(getIntentExtra()){
            Intent intent = getIntent();
            account = intent.getStringExtra("account");
            password = intent.getStringExtra("psw");
        }
        else {
            account = PreferenceUtils.getPrefString(this,
                    PreferenceConstants.ACCOUNT, "");
            password = PreferenceUtils.getPrefString(this,
                    PreferenceConstants.PASSWORD, "");
        }
        if (!TextUtils.isEmpty(account))
            mAccountEdit.setText(account);
        if (!TextUtils.isEmpty(password))
            mPasswordEdit.setText(password);

        mAutoSavePasswordCK.setChecked(
                PreferenceUtils.getPrefBoolean(
                        this,PreferenceConstants.AUTO_SAVE_PASSWORD,true
                )
        );

        mLogin.setOnClickListener(loginOnClickListener);
        register.setOnClickListener(registerOnClickListener);

        String lastName = PreferenceUtils.getPrefString(this, PreferenceConstants.LASTLOGIN, null);
        if (lastName != null) {

            File file = new File(
                    Environment.getExternalStorageDirectory(),
                    "theOldMen/" + lastName + "_avatar.jpg"
            );

            if (file.exists())
                mLoginImage.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
            else mLoginImage.setImageResource(R.drawable.avatar);

        } else mLoginImage.setImageResource(R.drawable.avatar);
    }


    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);

        } catch (IllegalArgumentException e) {

        }
    }

    private void bindXMPPService() {

        Intent mServiceIntent = new Intent(LoginActivity.this, XXService.class);
        mServiceIntent.setAction(LOGIN_ACTION);
        bindService(mServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }


    private OnClickListener loginOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            mAccount = mAccountEdit.getText().toString();
            mPassword = mPasswordEdit.getText().toString();
            if (TextUtils.isEmpty(mAccount)) {
                YoYo.with(Techniques.Tada)
                        .duration(700)
                        .playOn(findViewById(R.id.login_image));
                T.showShort(LoginActivity.this, "用户名为空");
                return;
            }
            if (TextUtils.isEmpty(mPassword)) {
                YoYo.with(Techniques.Shake).playOn(findViewById(R.id.login_image));
                T.showShort(LoginActivity.this, "密码为空");
                return;
            }
            if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running)
                mLoginOutTimeProcess.start();

            if (mXxService != null) {
                mXxService.Login(mAccount, mPassword);
                ;
            }

            if (mLogin.getProgress() == 0) {
                simulateErrorProgress(mLogin);
            } else {
                mLogin.setProgress(0);
            }
        }
    };

    private void simulateErrorProgress(final CircularProgressButton button) {
        ValueAnimator widthAnimation = ValueAnimator.ofInt(1, 99);
        widthAnimation.setDuration(1500);
        widthAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        widthAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                button.setProgress(value);
                if (value == 99) {
                    button.setProgress(-1);
                }
            }
        });
        widthAnimation.start();
    }

    private OnClickListener registerOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(mContext, RegisterPhoneActivity.class);
            startActivityForResult(intent, GET_LOGIN_DATA);
        }
    };


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
                if (System.currentTimeMillis() - this.startTime > LOGIN_SLEEP_OUT_TIME) {
                    mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
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

    private void savePreferences() {
        boolean isAutoSavePassword = mAutoSavePasswordCK.isCheck();
        boolean isHideLogin = mHideLoginCK.isCheck();
        PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,
                mAccount);// 帐号是一直保存的
        if (isAutoSavePassword)
            PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
                    mPassword);
        else
            PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
                    "");
        PreferenceUtils.setPrefBoolean(this,
                PreferenceConstants.AUTO_SAVE_PASSWORD, isAutoSavePassword);

        if (isHideLogin)
            PreferenceUtils.setPrefString(this,
                    PreferenceConstants.STATUS_MODE, PreferenceConstants.XA);
        else
            PreferenceUtils.setPrefString(this,
                    PreferenceConstants.STATUS_MODE,
                    PreferenceConstants.AVAILABLE);
    }

    private byte[] getAvatar(){
        File tempFile = new File(Environment.getExternalStorageDirectory() + "/theOldMen",
                mAccount +"_avatar.jpg");
        Bitmap bm = null;
        if(!tempFile.exists()) {
            return null;
        }
        bm = BitmapFactory.decodeFile(tempFile.toString());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis;
        bm.compress(Bitmap.CompressFormat.PNG,100,baos);

        int options = 100;
        while ( baos.toByteArray().length / 1024>100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            bm.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
//        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中

        byte[] bbytes =  baos.toByteArray();
        return bbytes;
    }

    public void readBaseInformation(){
        //**********Dialog********//

        final VCard vCard = mXxService.getVCard();
        if(TextUtils.isEmpty(vCard.getField("isFirstLogin"))) {

            BaseLDialog.Builder builder = new BaseLDialog.Builder(this);
            LayoutInflater inflater = (LayoutInflater)this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.logininfo, null);

            final RadioGroup radioGroup = (RadioGroup)view.findViewById(R.id.myRadioGroup);
            final EditText nickText = (EditText)view.findViewById(R.id.myNickName);
            final RadioButton radioButtonMale = (RadioButton)view.findViewById(R.id.myMaleRadioButton);
            mLoginHead = (CircleImageView)view.findViewById(R.id.login_head);
            Button login_camera = (Button)view.findViewById(R.id.login_camera);
            Button login_gallery = (Button)view.findViewById(R.id.login_gallery);
            login_camera.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(PictureUtils.createOpenSystemCameraIntent("/theOldMen/" + mAccount +"_avatar.jpg"),
                            REQUEST_FROM_CAMERA);
                }
            });

            login_gallery.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(PictureUtils.createOpenSystemGalleryIntent(),
                            REQUEST_FROM_GALLERY);
                }
            });
            BaseLDialog dialog = builder.setTitle("第一次登陆\n请完善一下基本信息") //设置标题
                    .setTitleSize(20) //设置标题的字体大小
                    .setTitleColor("#434343") //设置标题的颜色
                    .setView(view) //设置dialog的视图
                    .setPositiveButtonText("完成") //设置按钮
                    .setPositiveColor("#3c78d8") //设置按钮字体颜色
                    .setMode(true) //设置模态
                    .create(); //创建dialog
            //添加监听，该方法一定要写

            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mXxService.logout();
                    T.showShort(DemoApplication.getContext(),"真的不完善信息吗..!");
                }
            });
            dialog.setListeners(new BaseLDialog.ClickListener() {
                @Override
                public void onConfirmClick() {
                    Log.d("1", "Confirm");
                    String nickName = nickText.getText().toString().trim();

                    if (TextUtils.isEmpty(nickName)) {
                        T.showShort(LoginActivity.this, "昵称不能为空!");
                        mXxService.logout();
                    }
                    else {
                        vCard.setNickName(nickName);
                        vCard.setField("Sex", radioGroup.getCheckedRadioButtonId() == radioButtonMale.getId() ? "男" : "女");

                        vCard.setAvatar(getAvatar());
                        vCard.setField("isFirstLogin", "Loginned");
                        mXxService.saveCard(vCard);

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                }

                @Override
                public void onCancelClick() {
                    Log.d("2", "Cancel");
                }
            });

            dialog.show();
        }else{
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {

        if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
            mLoginOutTimeProcess.stop();
            mLoginOutTimeProcess = null;
        }
        if (connectedState == XXService.CONNECTED) {
            savePreferences();
            readBaseInformation();

        } else if (connectedState == XXService.DISCONNECTED)
            T.showLong(LoginActivity.this, getString(R.string.request_failed)
                    + reason);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindXMPPService();
        if (mLoginOutTimeProcess != null) {
            mLoginOutTimeProcess.stop();
            mLoginOutTimeProcess = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        String fileName = "/theOldMen/" + mAccount +"_avatar.jpg";
        switch (requestCode) {
            case GET_LOGIN_DATA:
                mAccountEdit.setText(intent.getStringExtra("account"));
                mPasswordEdit.setText(intent.getStringExtra("password"));
                break;
            case REQUEST_FROM_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    Intent myIntent= PictureUtils.createCropPicIntent(Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                            fileName)), 200, 200, fileName);
                    startActivityForResult(myIntent, REQUEST_FOR_RESULT);
                }
                break;
            case REQUEST_FROM_GALLERY:
                Uri uri = intent.getData();
                Intent intent1 = PictureUtils.createCropPicIntent(uri, 200, 200, fileName);
                startActivityForResult(intent1, REQUEST_FOR_RESULT);
                break;
            case REQUEST_FOR_RESULT:
                FileInputStream fileInputStream = null;
                //打开头像文件
                File f = new File(Environment.getExternalStorageDirectory(), fileName);
                try {
                    fileInputStream = new FileInputStream(f);
                    mLoginHead.setImageBitmap(BitmapFactory.decodeStream(fileInputStream));
                    fileInputStream.close();
                } catch (IOException e) {}
                ////////////////////////////////////////////////////////////////
                break;
        }
    }
}

