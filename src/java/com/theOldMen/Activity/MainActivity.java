package com.theOldMen.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.theOldMen.ResideMenu.ResideMenu;
import com.theOldMen.ResideMenu.ResideMenuItem;
import com.theOldMen.RosterManagement.Group;
import com.theOldMen.ShackActivity.TheOldMenShakeActivity;
import com.theOldMen.ShackActivity.TheOldMenShakeFragment;
import com.theOldMen.app.DemoApplication;
import com.theOldMen.maininterface.ChatFragment;
import com.theOldMen.maininterface.ContactFragment;
import com.theOldMen.maininterface.FragmentCallback;
import com.theOldMen.maininterface.SettingsFragment;
import com.theOldMen.service.IConnectionStatusCallback;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.tools.T;
import com.theOldMen.util.DialogUtil;
import com.theOldMen.view.AddUserDialog;
import com.theOldMen.view.LDialog.BaseLDialog;
import com.theOldMen.zone.TheOldMenUserZoneActivity;

import java.util.ArrayList;

import at.markushi.ui.ActionView;
import at.markushi.ui.action.BackAction;
import at.markushi.ui.action.DrawerAction;


public class MainActivity
        extends FragmentActivity
        implements View.OnClickListener,
        IConnectionStatusCallback,
        FragmentCallback {


    ////////////////////////////////////////////////////////////////////////////////////////////////
    private XXService mXxService;
    private ResideMenu resideMenu;//滑动菜单控件
    private MainActivity mContext;//即当前的MainActivity对象
    private ResideMenuItem itemHome;//联系人界面菜单项(主界面)
    private ResideMenuItem itemChat;//聊天界面的菜单项
    private ResideMenuItem itemSettings;//设置界面的菜单项
    private ResideMenuItem itemSearch;//设置搜索好友的菜单项
    private ResideMenuItem itemShack;//设置摇一摇的菜单项
    private ResideMenuItem itemLove;//设置缘分墙的菜单项
    private ResideMenuItem itemExit;//设置退出的菜单项
    private LinearLayout mCommentField;//用于显示标题栏的区域
    private ActionView mLeftMenu; //标题栏左侧按钮

    private onConnectionChangedListener mChangedListener;
    private toAdapterGroup mAdapterGroup;
    private statusListener mStatusListener;

    private boolean isFirstLogin = true;

    ServiceConnection mServiceConnection = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        startService(new Intent(MainActivity.this, XXService.class));
        mContext = this;
        mCommentField = (LinearLayout)findViewById(R.id.commentField);//获得标题栏区域的引用
        setUpMenu();
        Fragment mFragment = new ContactFragment();

        mChangedListener =(onConnectionChangedListener)mFragment;
        mAdapterGroup = (toAdapterGroup)mFragment;
        mStatusListener = (statusListener)mFragment;

        changeFragment(mFragment);
        mServiceConnection =  new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mXxService = ((XXService.XXBinder) service).getService();
                mXxService.registerConnectionStatusCallback(MainActivity.this);
                // 开始连接xmpp服务器
                if (!mXxService.isAuthenticated()) {
                    String usr = PreferenceUtils.getPrefString(MainActivity.this,
                            PreferenceConstants.ACCOUNT, "");
                    String password = PreferenceUtils.getPrefString(
                            MainActivity.this, PreferenceConstants.PASSWORD, "");
                    mXxService.Login(usr, password);
                }else{
                    if(isConnected() && isFirstLogin) {
                        mStatusListener.refreshStatus(true);
                        isFirstLogin = false;
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mXxService.unRegisterConnectionStatusCallback();
                mXxService = null;
            }

        };
     }

     /**
     * 方法名: getCommentField()
     * 功能: 返回标题栏区域以便各个fragment做相应的修改和设置
     * 参数: 无
     * 返回值: LinearLayout - 标题栏区域
     */
    public LinearLayout getCommentField() {
        if (mCommentField == null) return null;
        return mCommentField;
    }

    ResideMenu.OnMenuListener mMenuListener = new ResideMenu.OnMenuListener() {
        @Override
        public void openMenu() {
            mLeftMenu.setAction(new BackAction(), ActionView.ROTATE_COUNTER_CLOCKWISE);
        }

        @Override
        public void closeMenu() {
            mLeftMenu.setAction(new DrawerAction(), ActionView.ROTATE_COUNTER_CLOCKWISE);
        }
    };

    /**
     * 方法名: setUpMenu()
     * 功能: 设置菜单，添加菜单项
     * 参数: 无
     * 返回值: 无
     */
    private void setUpMenu() {
        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.menu_background);//设置滑动菜单的背景
        resideMenu.attachToActivity(this);//将菜单绑定到当前的activity
        resideMenu.setScaleValue(0.7f);//设置滑动缩放比例
        // 创建菜单项
        itemHome = new ResideMenuItem(this, R.drawable.icon_home,
                getString(R.string.icon_menu_home));
        itemChat = new ResideMenuItem(this, R.drawable.icon_profile,
                getString(R.string.icon_menu_chat));
        itemSettings = new ResideMenuItem(this, R.drawable.icon_settings,
                getString(R.string.icon_menu_setting));
        itemSearch = new ResideMenuItem(this, R.drawable.ic_action_search, R.string.icon_menu_search);
        itemLove = new ResideMenuItem(this, R.drawable.icon_love, R.string.icon_menu_love);
        itemShack = new ResideMenuItem(this, R.drawable.icon_shack, R.string.icon_menu_shack);
        itemExit = new ResideMenuItem(this, R.drawable.icon_exit, R.string.icon_menu_exit);
        //添加点击监听
        itemHome.setOnClickListener(this);
        itemChat.setOnClickListener(this);
        itemSettings.setOnClickListener(this);
        itemSearch.setOnClickListener(this);
        itemLove.setOnClickListener(this);
        itemShack.setOnClickListener(this);
        itemExit.setOnClickListener(this);

        //把菜单项添加到菜单上
        resideMenu.addMenuItem(itemHome, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemChat, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemSettings, ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemLove,ResideMenu.DIRECTION_LEFT);
        resideMenu.addMenuItem(itemExit, ResideMenu.DIRECTION_LEFT);

        resideMenu.addMenuItem(itemShack, ResideMenu.DIRECTION_RIGHT);
        resideMenu.addMenuItem(itemSearch,ResideMenu.DIRECTION_RIGHT);

        mLeftMenu = (ActionView)findViewById(R.id.title_bar_left_menu);
        //设置点击标题栏区域左边的按钮就将当前界面移动到菜单栏的右侧
        mLeftMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeftMenu.setAction(new BackAction(), ActionView.ROTATE_COUNTER_CLOCKWISE);
                resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
            }
        });
//        设置点击标题栏区域右边的按钮就将当前界面移动到菜单栏的左侧
        findViewById(R.id.title_bar_right_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resideMenu.openMenu(ResideMenu.DIRECTION_RIGHT);
            }
        });

        resideMenu.setMenuListener(mMenuListener);
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
            //L.i(LoginActivity.class, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            //L.e(LoginActivity.class, "Service wasn't bound!");
        }
    }


    private void bindXMPPService() {
        //L.i(LoginActivity.class, "[SERVICE] Unbind");
        bindService(new Intent(MainActivity.this, XXService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }

    @Override
    public boolean isConnected(){
        return mXxService!= null && mXxService.isAuthenticated();
    }

    @Override
    public XXService getService(){
        return mXxService;
    }

    public static interface onConnectionChangedListener{
        public void connectedChanged();
        public void connectingChanged();
        public void disconnectedChanged();
    }

    public static interface toAdapterGroup{
        public ArrayList<Group> getGroups();
    }

    public static interface statusListener{
        public void refreshStatus(boolean isConnected);
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        switch (connectedState) {
            case XXService.CONNECTED:
                mChangedListener.connectedChanged();
                break;
            case XXService.CONNECTING:
                mChangedListener.connectingChanged();

                break;
            case XXService.DISCONNECTED:
                mChangedListener.disconnectedChanged();

                Handler mHandler = new Handler(mContext.getMainLooper()){
                    @Override
                    public void handleMessage(android.os.Message msg) {

                        super.handleMessage(msg);
                    };
                };
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        T.showLong(mContext.getApplicationContext(), "DISCONNECTED");

                    }
                });

                break;

            default:
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return resideMenu.dispatchTouchEvent(ev);
    }

    @Override
    public void onClick(View view) {
        //点击相应的菜单项，切换到相应的Fragment
        if (view == itemHome){
            changeFragment(new ContactFragment());
        }else if (view == itemChat){
            changeFragment(ChatFragment.getChatFragmentInstance(resideMenu));
        }else if (view == itemSettings){
            changeFragment(new SettingsFragment());
        }else if(view == itemLove) {
            if(isConnected()) {
                Intent x = TheOldMenUserZoneActivity.getIntent(MainActivity.this, PreferenceUtils.getPrefString(MainActivity.this,
                        PreferenceConstants.ACCOUNT, ""), mXxService.getVCard().getNickName());
                startActivity(x);
            }else T.showShort(this,"请检查网络连接..");
        }else if(view == itemShack){
            Intent x = new Intent(MainActivity.this,TheOldMenShakeActivity.class);

            Bundle bundle = new Bundle();
            bundle.putSerializable(TheOldMenShakeFragment.s_idTag,PreferenceUtils.getPrefString(MainActivity.this,
                    PreferenceConstants.ACCOUNT, ""));
            bundle.putSerializable(TheOldMenShakeFragment.s_nameTag,mXxService.getVCard().getNickName());
            x.putExtras(bundle);

            startActivity(x);
        }else if(view == itemSearch){
            showAddFriendDialog();
        } else if(view == itemExit) {
            BaseLDialog.Builder builder = new BaseLDialog.Builder(this);
            BaseLDialog baseLDialog =
                    builder.setTitle("提示")
                            .setTitleColor("#434343")
                            .setContent("你确定要退出?")
                            .setContentColor(Color.parseColor("red"))
                            .setContentSize(20)
                            .setPositiveButtonText("确定")
                            .setPositiveColor("#3c78d8")
                            .setNegativeButtonText("取消")
                            .setNegativeColor("#cccccc")
                            .create();
            baseLDialog.setListeners(new BaseLDialog.ClickListener() {
                @Override
                public void onConfirmClick() {
                    //退出
                    if (mXxService != null) {
                        mXxService.logout();// 注销
                        mXxService.stopSelf();
                        finish();
                    }
                }

                @Override
                public void onCancelClick() {
                }
            });
            DialogUtil.setPopupDialog(this, baseLDialog);
            baseLDialog.show();
        }
        resideMenu.closeMenu(); //关闭菜单的界面
    }



    //切换相应的Fragment
    private void changeFragment(Fragment targetFragment){

        resideMenu.clearIgnoredViewList();//清空resideMenu忽略的视图列表
        // 启动Fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, targetFragment, "fragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public ResideMenu getResideMenu(){
        return resideMenu;
    }

    private long firstTime;
    /**
     * 连续点击两次退出
     */
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - firstTime < 3000) {
            finish();
        } else {
            firstTime = System.currentTimeMillis();
            T.showShort(this, R.string.press_again_backrun);
        }
    }

    private void showAddFriendDialog() {
        if (!isConnected())
            return;

        ArrayList<Group> arrayList = mAdapterGroup.getGroups();

        String[] groupNames = new String[arrayList.size()];
        for (int i = 0; i < groupNames.length; ++ i) {
            if(TextUtils.isEmpty(arrayList.get(i).getGroupName()))
                groupNames[i] = getString(R.string.default_group);
            else
                groupNames[i] = arrayList.get(i).getGroupName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,
                groupNames);
        //默认好友添加到的分组为分组列表的第一个分组
        new AddUserDialog(this, mXxService, adapter).show();
    }
}