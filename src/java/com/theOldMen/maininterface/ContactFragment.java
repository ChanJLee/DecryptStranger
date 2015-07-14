package com.theOldMen.maininterface;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.theOldMen.Activity.MainActivity;
import com.theOldMen.Activity.R;
import com.theOldMen.RosterManagement.Group;
import com.theOldMen.RosterManagement.User;
import com.theOldMen.adapter.ContactAdapter;
import com.theOldMen.app.DemoApplication;
import com.theOldMen.broadcast.XXBroadcastReceiver;
import com.theOldMen.chat.TheOldMenChatMainActivity;
import com.theOldMen.db.RosterProvider;
import com.theOldMen.pulltorefresh.PullToRefreshBase;
import com.theOldMen.pulltorefresh.PullToRefreshScrollView;
import com.theOldMen.quickaction.ActionItem;
import com.theOldMen.quickaction.QuickAction;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.L;
import com.theOldMen.tools.NetUtil;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.tools.T;
import com.theOldMen.util.MyUtil;
import com.theOldMen.util.PreferenceConstants;
import com.theOldMen.util.SettingPreferenceUtils;
import com.theOldMen.view.BetterSpinner;
import com.theOldMen.view.CustomDialog;
import com.theOldMen.view.ScrollExpandableListView;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ContactFragment
        extends Fragment
        implements XXBroadcastReceiver.EventHandler,
        MainActivity.onConnectionChangedListener,
        MainActivity.toAdapterGroup,
        MainActivity.statusListener {

    private ImageView m_titleStatus; //用户状态栏
    private PullToRefreshScrollView m_pullToRefreshScrollView;//下拉刷新列表视图
    private ScrollExpandableListView m_expandableListView;//可拓展列表视图
    private ContactAdapter m_contactAdapter;
    private ContentObserver mRosterObserver = new RosterObserver();
    private View m_errorNetTip;
    private TextView m_title;
    private FragmentCallback m_callback;
    private Handler myHandler = new Handler();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    //注册adapter
    private void registerAdapter() {
        m_contactAdapter = new ContactAdapter(getActivity());
        m_expandableListView.setAdapter(m_contactAdapter);
        m_contactAdapter.refresh();//刷新用户好友分组列表
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            m_callback = (FragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Something Wrong..!");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().getContentResolver().registerContentObserver(
                RosterProvider.CONTENT_URI, true, mRosterObserver);
        m_contactAdapter.refresh();
        setStatusImage(isConnected());

        XXBroadcastReceiver.mListeners.add(this);
        if (NetUtil.getNetworkState(getActivity()) == NetUtil.NETWORN_NONE)
            m_errorNetTip.setVisibility(View.VISIBLE);
        else
            m_errorNetTip.setVisibility(View.GONE);

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mRosterObserver);
        XXBroadcastReceiver.mListeners.remove(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        LinearLayout commentField = ((MainActivity) getActivity()).getCommentField();//获得标题栏区域
        handleCommentField(commentField);

        View v = inflater.inflate(R.layout.main_center_layout, container, false);

        m_errorNetTip = v.findViewById(R.id.net_status_bar_top);

        m_pullToRefreshScrollView = (PullToRefreshScrollView) v.findViewById(R.id.pull_refresh_scrollview);

        m_pullToRefreshScrollView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ScrollView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ScrollView> refreshView) {
                new GetDataTask().execute();
            }
        });

        m_expandableListView = (ScrollExpandableListView) v.findViewById(R.id.expandable_list_view);
        m_expandableListView.setEmptyView(v.findViewById(R.id.empty));
        m_expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int groupPosition = (Integer) view.getTag(R.id.position01);
                int childPosition = (Integer) view.getTag(R.id.position02);
                if (childPosition == -1) {
                    setGroupQuickActionBar(view, groupPosition);
                } else {
                    setChildQuickActionBar(view, groupPosition, childPosition);
                }
                return true;
            }
        });

        m_expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                //////////////////////点击进入聊天///////////////////////////
                int groupIndex = (Integer) v.getTag(R.id.position01);
                int childIndex = (Integer) v.getTag(R.id.position02);
                User user = m_contactAdapter.getChild(groupIndex, childIndex);
                String fromId = MyUtil.splitJidAndServer(PreferenceUtils
                        .getPrefString(getActivity(),
                                com.theOldMen.tools.PreferenceConstants.ACCOUNT, ""));
                L.i(user.getUserId());
                Intent i = TheOldMenChatMainActivity.getChatIntent(getActivity(), user.getUserId(), fromId, user.getAlias(), fromId,
                        MyUtil.getUserAvatarPath(user.getUserId()), MyUtil.getUserAvatarPath(fromId));
                startActivity(i);
                return true;
            }
        });
        registerAdapter();
        return v;
    }

    @Override
    public void refreshStatus(boolean isConnected) {
        setStatusImage(isConnected);
    }

    @Override
    public void connectedChanged() {
//        mTitleProgressBar.setVisibility(View.GONE);
//        mTitleStatusView.setVisibility(View.GONE);
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                setStatusImage(true);
            }
        });
    }

    @Override
    public void connectingChanged() {

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                m_title.setText("正在连接...");

            }
        });
    }

    @Override
    public void disconnectedChanged() {

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                m_titleStatus.setVisibility(View.GONE);
                m_title.setText(R.string.net_error_tip);

            }
        });
    }

    @Override
    public ArrayList<Group> getGroups() {
        return m_contactAdapter.getGroups();
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    //连接状态响应的图标和设置
    private void setStatusImage(boolean isConnected) {
        if (!isConnected) {
            m_titleStatus.setVisibility(View.GONE); //状态标识隐藏
            m_title.setText(R.string.net_error_tip); //把标题上的昵称名改为未连接
            return;
        }
        m_titleStatus.setVisibility(View.VISIBLE);
        m_titleStatus.setImageResource(R.drawable.status_online);
        m_title.setText(MyUtil.splitJidAndServer(PreferenceUtils
                .getPrefString(DemoApplication.getContext(),
                        com.theOldMen.tools.PreferenceConstants.ACCOUNT, ""))); //显示当前用户的昵称
    }

    private void handleCommentField(LinearLayout linearLayout) {

        if (linearLayout == null) return;

        m_title = (TextView) linearLayout.findViewById(R.id.comment_title);
        m_titleStatus = (ImageView) linearLayout.findViewById(R.id.titleStatus);
    }


    private boolean isConnected() {

        //检测是否连接的方法
        return m_callback.isConnected();
    }

    private void setGroupQuickActionBar(View v, final int groupPosition) {
        QuickAction quickAction = new QuickAction(getActivity(), QuickAction.VERTICAL);
        quickAction.addActionItem(new ActionItem(0, getString(R.string.rename)));

        quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (!isConnected()) {
                    Toast.makeText(getActivity(), getString(R.string.net_error),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (actionId) {
                    case 0:
                        String groupName = m_contactAdapter.getGroup(groupPosition).getGroupName();
                        if (TextUtils.isEmpty(groupName)) {
                            Toast.makeText(getActivity(), getString(R.string.group_rename_failed),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        renameGroupDialog(groupName);
                        break;
                    default:
                        break;
                }
            }
        });
        quickAction.show(v);
    }

    private void notifyNotConnected() {
        T.showShort(getActivity(),
                getString(R.string.net_error));
    }

    private void setChildQuickActionBar(View v, final int groupPosition, final int childPosition) {
        QuickAction quickAction = new QuickAction(getActivity(), QuickAction.VERTICAL);
        quickAction.addActionItem(new ActionItem(0, getString(R.string.open)));
        quickAction.addActionItem(new ActionItem(1, getString(R.string.rename)));
        quickAction.addActionItem(new ActionItem(2, getString(R.string.move)));
        quickAction.addActionItem(new ActionItem(3, getString(R.string.delete)));

        quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                User child = m_contactAdapter.getChild(groupPosition, childPosition);
                String userId = child.getUserId();
                String userNickName = child.getAlias();
                switch (actionId) {
                    case 0:
                        //启动聊天activity
                        if (!isConnected()) {
                            notifyNotConnected();
                            return;
                        }
                        String fromId = MyUtil.splitJidAndServer(PreferenceUtils
                                .getPrefString(getActivity(),
                                        com.theOldMen.tools.PreferenceConstants.ACCOUNT, ""));
                        Intent i = TheOldMenChatMainActivity.getChatIntent(getActivity(), userId, fromId, userNickName, fromId,
                                MyUtil.getUserAvatarPath(userId), MyUtil.getUserAvatarPath(fromId));
                        startActivity(i);
                        break;
                    case 1:
                        if (!isConnected()) {
                            notifyNotConnected();
                            return;
                        }
                        renameUserDialog(userId, userNickName);
                        break;
                    case 2:
                        if (!isConnected()) {
                            notifyNotConnected();
                            return;
                        }
                        ArrayList<Group> arrayList = m_contactAdapter.getGroups();
                        moveUserToAnotherGroup(userId, groupPosition, arrayList);
                        break;
                    case 3:
                        if (!isConnected()) {
                            notifyNotConnected();
                            return;
                        }
                        removeUserDialog(userId, userNickName);
                        break;
                    default:
                        break;
                }
            }
        });
        quickAction.show(v);

    }

    private void removeUserDialog(final String userId, String nickName) {
        new CustomDialog.Builder(getActivity()).setTitle(R.string.deleteUser_title)
                .setMessage(getString(R.string.deleteUser_message, nickName, userId))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //删除好友
                        m_callback.getService().removeRosterItem(userId);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();
    }

    private void setEditTextDialog(String defaultText,
                                   final OnEditSuccess editSuccess) {
        LayoutInflater inflater = (LayoutInflater) getActivity().
                getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.edit_dialog, null);
        final EditText editText = (EditText) view.findViewById(R.id.editText);
        editText.setTransformationMethod(android.text.method.SingleLineTransformationMethod.getInstance());
        editText.setText(defaultText);
        final TextView errorView = (TextView) view.findViewById(R.id.errorTextView);
        new CustomDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newName = editText.getText().toString();
                                ///////////////////////////////////
                                //后台的检查并提示用户，可以根据需求重写OnEditSuccess
                                if (newName.equals("")) {
                                    //阻止关闭dialog
                                    try {
                                        Field field = dialog.getClass()
                                                .getSuperclass().getDeclaredField("mShowing");
                                        field.setAccessible(true);
                                        field.set(dialog, false);
                                    } catch (Exception e) {
                                    }
                                    //将editText的字符串清空
                                    editText.setText("");
                                    errorView.setVisibility(View.VISIBLE);
                                    errorView.setText(R.string.error_input);
                                } else {
                                    try {
                                        Field field = dialog.getClass()
                                                .getSuperclass().getDeclaredField("mShowing");
                                        field.setAccessible(true);
                                        field.set(dialog, true);
                                    } catch (Exception e) {
                                    }
                                    /////////////////////////////////////////////
                                    editSuccess.editOk(newName);
                                }
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Field field = dialog.getClass()
                                            .getSuperclass().getDeclaredField("mShowing");
                                    field.setAccessible(true);
                                    field.set(dialog, true);
                                } catch (Exception e) {
                                }
                                dialog.dismiss();
                            }
                        })
                .create()
                .show();
    }

    private void moveUserToAnotherGroup(final String userId, int groupPosition,
                                        ArrayList<Group> groups) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final View editView = layoutInflater.inflate(R.layout.movetoanothergroup, null);
        final EditText newGroup = (EditText) editView.findViewById(R.id.moveToNewGroup);
        final String[] groupNames = new String[groups.size() + 1];
        for (int i = 0; i < groups.size(); ++i) {
            if (TextUtils.isEmpty(groups.get(i).getGroupName()))
                groupNames[i] = getString(R.string.default_group);
            else
                groupNames[i] = groups.get(i).getGroupName();
        }
        groupNames[groups.size()] = getString(R.string.new_group_name);
        final BetterSpinner betterSpinner = (BetterSpinner)
                editView.findViewById(R.id.moveUserSpinner);
        betterSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, groupNames));

//        String myGroupName = m_contactAdapter.getGroup(groupPosition).getGroupName();
//        if(TextUtils.isEmpty(myGroupName))
        betterSpinner.setText(getString(R.string.default_group));
//        else
//            betterSpinner.setText(m_contactAdapter.getGroup(groupPosition).getGroupName());

        betterSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == groupNames.length - 1) {
                    editView.findViewById(R.id.moveToNewGroup).setVisibility(View.VISIBLE);
                } else {
                    editView.findViewById(R.id.moveToNewGroup).setVisibility(View.GONE);
                }
            }
        });

        final TextView errorView = (TextView) editView.findViewById(R.id.errorTextView);
        new CustomDialog.Builder(getActivity())
                .setView(editView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!isConnected()) return;
                        ///////////////////////////////////////////////////////////////////
                        //在这里对新建的分组的组名进行检查
                        if (newGroup.getVisibility() == View.VISIBLE &&
                                TextUtils.isEmpty(newGroup.getText().toString())) {
                          //  Log.d(TAG, "数据不合法");
                            try {
                                Field field = dialog.getClass()
                                        .getSuperclass().getDeclaredField("mShowing");
                                field.setAccessible(true);
                                field.set(dialog, false);
                            } catch (Exception e) {
                             //   Log.d(TAG, "异常");
                            }
                            newGroup.setText("");
                            errorView.setVisibility(View.VISIBLE);
                            errorView.setText("非法输入");
                        } else {
                           // Log.d(TAG, "合法数据");
                            /////////////////////////////////////////////////
                            //在这里后台服务移动好友至目标分组
                            String spinnerItem = betterSpinner.getText().toString();
                            String result = "";
                            if (spinnerItem.equals(getString(R.string.default_group)))
                                result = "";
                            else if (spinnerItem.equals(getString(R.string.new_group_name)))
                                result = newGroup.getText().toString();
                            else
                                result = spinnerItem;
                         //   L.i("RESULT", result);
                            m_callback.getService().moveRosterItemToGroup(userId,
                                    result);
                            try {
                                Field field = dialog.getClass()
                                        .getSuperclass().getDeclaredField("mShowing");
                                field.setAccessible(true);
                                field.set(dialog, true);
                            } catch (Exception e) {
                             //   Log.d(TAG, "异常");
                            }
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Field field = dialog.getClass()
                                    .getSuperclass().getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, true);
                        } catch (Exception e) {
                         //   Log.d(TAG, "异常");
                        }

                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    private void renameGroupDialog(final String groupName) {
        setEditTextDialog(groupName,
                new OnEditSuccess() {
                    @Override
                    public void editOk(String value) {
                        ///////////////////////////////////////////////////////////////////
                        //后台修改分组名
                        XXService xxService = m_callback.getService();
                        if (xxService != null)
                            xxService.renameRosterGroup(groupName, value);
                    }

                    @Override
                    public void checkValue(String value) {
                        ///////////////////////////////////////////////////////////////
                        //检查数据的代码
                    }
                });
    }

    private void renameUserDialog(final String UserId, String nickName) {
        setEditTextDialog(nickName, new OnEditSuccess() {
            @Override
            public void editOk(String value) {
                ////////////////////////////////////////////////////
                //后台修改好友昵称名
                XXService xxService = m_callback.getService();
                if (xxService != null)
                    xxService.renameRosterItem(UserId, value);
            }

            @Override
            public void checkValue(String value) {
                ////////////////////////////////////////////
                //重写这里的数据检查代码
            }
        });
    }

    private void setStatusChoiceActionBar(View v, final ImageView imageView) {
        QuickAction quickAction = new QuickAction(getActivity(), QuickAction.VERTICAL);
        quickAction.addActionItem(new ActionItem(0, getString(R.string.status_qme),
                getResources().getDrawable(R.drawable.status_qme)));
        quickAction.addActionItem(new ActionItem(1, getString(R.string.status_available),
                getResources().getDrawable(R.drawable.status_online)));
        quickAction.addActionItem(new ActionItem(2, getString(R.string.status_ys),
                getResources().getDrawable(R.drawable.status_invisible)));
        quickAction.addActionItem(new ActionItem(3, getString(R.string.status_dnd),
                getResources().getDrawable(R.drawable.status_dnd)));
        quickAction.addActionItem(new ActionItem(4, getString(R.string.status_away),
                getResources().getDrawable(R.drawable.status_leave)));
        quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (!isConnected()) {
                    Toast.makeText(getActivity(), R.string.net_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (actionId) {
                    case 0:
                        imageView.setImageResource(R.drawable.status_qme);
                        storeStatusMode(PreferenceConstants.QME, R.string.status_qme);
                        break;
                    case 1:
                        imageView.setImageResource(R.drawable.status_online);
                        storeStatusMode(PreferenceConstants.AVAILABLE, R.string.status_available);
                        break;
                    case 2:
                        imageView.setImageResource(R.drawable.status_invisible);
                        storeStatusMode(PreferenceConstants.YS, R.string.status_ys);
                        break;
                    case 3:
                        imageView.setImageResource(R.drawable.status_dnd);
                        storeStatusMode(PreferenceConstants.DND, R.string.status_dnd);
                        break;
                    case 4:
                        imageView.setImageResource(R.drawable.status_leave);
                        storeStatusMode(PreferenceConstants.AWAY, R.string.status_away);
                        break;
                    default:
                        break;
                }
                //调整用户模式
                m_callback.getService().setStatusFromConfig();
            }
        });
        quickAction.show(v);
    }

    private void storeStatusMode(String statusMode, int statusId) {
        SettingPreferenceUtils.setPreferenceString(getActivity(), PreferenceConstants.STATUS_MODE,
                statusMode);
        SettingPreferenceUtils.setPreferenceString(getActivity(), PreferenceConstants.STATUS_STRING,
                getString(statusId));
    }

    public void updateRoster() {
        m_contactAdapter.refresh();
    }

    private class RosterObserver extends ContentObserver {
        public RosterObserver() {
            super(myHandler);
        }

        public void onChange(boolean selfChange) {
            L.d(MainActivity.class, "RosterObserver.onChange: " + selfChange);
            if (m_contactAdapter != null)
                myHandler.postDelayed(new Runnable() {
                    public void run() {
                        updateRoster();
                    }
                }, 100);
        }
    }

    @Override
    public void onNetChange() {
        if (NetUtil.getNetworkState(getActivity()) == NetUtil.NETWORN_NONE) {
            T.showShort(getActivity(), R.string.net_error_tip);
            m_errorNetTip.setVisibility(View.VISIBLE);
        } else {
            m_errorNetTip.setVisibility(View.GONE);
        }
    }

    private class GetDataTask extends AsyncTask<Void, Void, String[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // if (mPullRefreshScrollView.getState() != State.REFRESHING)
            // mPullRefreshScrollView.setState(State.REFRESHING, true);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            // Simulates a background job.
            if (!isConnected()) {// 如果没有连接重新连接
                String usr = PreferenceUtils.getPrefString(getActivity(),
                        com.theOldMen.tools.PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        getActivity(), com.theOldMen.tools.PreferenceConstants.PASSWORD, "");
                m_callback.getService().Login(usr, password);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            // Do some stuff here
            // Call onRefreshComplete when the list has been refreshed.
            if (isConnected()) {
                m_contactAdapter.refresh();// 重新查询一下数据库
                m_pullToRefreshScrollView.onRefreshComplete();
                // mPullRefreshScrollView.getLoadingLayoutProxy().setLastUpdatedLabel(
                // "最近更新：刚刚");
                T.showShort(getActivity(), "刷新成功!");
            } else {
                T.showShort(getActivity(), "网络异常!请检查网络连接");
            }
            super.onPostExecute(result);
        }
    }


    ////////////////////////////////////////////////////
    //修改用户分组信息后的回调接口
    public abstract class OnEditSuccess {
        //处理后台修改用户信息的服务，然后更新列表
        abstract public void editOk(String value);

        abstract public void checkValue(String value);
    }
}