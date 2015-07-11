package com.theOldMen.boxGame;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.theOldMen.Activity.LoginActivity;
import com.theOldMen.Activity.R;
import com.theOldMen.ShackActivity.TheOldMenShakeFragment;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.L;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.view.LDialog.BaseLDialog;


public class BoxGameMainActivity extends ActionBarActivity implements BoxGameViewGroup.onAddFriendListener{

    private String userName;
    private String userId;
    private static XXService mXxService;
    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            // 开始连接xmpp服务器
            if (!mXxService.isAuthenticated()) {
                String usr = PreferenceUtils.getPrefString(BoxGameMainActivity.this,
                        PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        BoxGameMainActivity.this, PreferenceConstants.PASSWORD, "");
                mXxService.Login(usr, password);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_box_game_main);

        startService(new Intent(BoxGameMainActivity.this, XXService.class));
        init();
    }

    private void init(){
        Intent x = getIntent();
        userName = x.getStringExtra(TheOldMenShakeFragment.s_nameTag);
        userId = x.getStringExtra(TheOldMenShakeFragment.s_idTag);
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
        bindService(new Intent(BoxGameMainActivity.this, XXService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void addFriend() {
        makeFriend(userId,userName);
    };


    private void makeFriend(final String friendId ,final String friendName) {
        mXxService.addRosterItem(friendId, "", "");

        BaseLDialog.Builder builder = new BaseLDialog.Builder(BoxGameMainActivity.this);
        BaseLDialog dialog =
                builder.setContent("申请加" + friendId + "(" + friendName + ")为好友请求发送成功!")
                        .setContentSize(20)
                        .setContentColor("#434343")
                        .setMode(true)
                        .setPositiveButtonText("确定")
                        .setPositiveColor("#3c78d8")
                        .create();
        dialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                finish();
            }

            @Override
            public void onCancelClick() {
            }
        });
        dialog.show();
    }

}
