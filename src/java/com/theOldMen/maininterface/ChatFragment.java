package com.theOldMen.maininterface;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.theOldMen.Activity.MainActivity;
import com.theOldMen.Activity.R;
import com.theOldMen.ResideMenu.ResideMenu;
import com.theOldMen.adapter.RecentChatAdapter;
import com.theOldMen.chat.TheOldMenChatMainActivity;
import com.theOldMen.db.ChatProvider;
import com.theOldMen.swipelistview.BaseSwipeListViewListener;
import com.theOldMen.swipelistview.SwipeListView;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.util.MyUtil;

public class ChatFragment extends Fragment {
    private Handler m_handle = new Handler();
    private ChatObserver m_chatObserver;
    private SwipeListView m_swipeListView;
    private RecentChatAdapter m_recentChatAdapter;
    private ContentResolver m_contentResolver;
    private BaseSwipeListViewListener m_baseSwipeListViewListener =
            new BaseSwipeListViewListener() {
                @Override
                public void onClickFrontView(int position) {
                    //////////////////////////////////////////////////
                    // 打开聊天activity

                    Cursor cursor = m_recentChatAdapter.getCursor();
                    cursor.moveToPosition(position);
                    // 获得聊天对方的账号
                    String jid = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.JID));

                    String userAlias = PreferenceUtils.getPrefString(getActivity(), jid, "");
                    /////////////////////////////////////////////////////////
                    //启动对话activity
                    String fromId = MyUtil.splitJidAndServer(PreferenceUtils
                            .getPrefString(getActivity(),
                                    com.theOldMen.tools.PreferenceConstants.ACCOUNT, ""));
                    Intent i = TheOldMenChatMainActivity.getChatIntent(getActivity(), jid, fromId, userAlias, fromId,
                            MyUtil.getUserAvatarPath(jid), MyUtil.getUserAvatarPath(fromId));
                    startActivity(i);
                }

                @Override
                public void onClickBackView(int position) {
                    m_swipeListView.closeOpenedItems(); //关闭打开项
                }
            };

    private ResideMenu m_resideMenu;
    private void setResideMenu(ResideMenu resideMenu) {
        m_resideMenu = resideMenu;
    }

    public static ChatFragment getChatFragmentInstance(ResideMenu resideMenu) {
        ChatFragment chatFragment = new ChatFragment();
        chatFragment.setResideMenu(resideMenu);
        return chatFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout = ((MainActivity) getActivity()).getCommentField();//获得标题栏区域
        m_recentChatAdapter = new RecentChatAdapter(getActivity(), m_resideMenu);
        m_contentResolver = getActivity().getContentResolver();
        m_chatObserver = new ChatObserver();
        handleCommentField(linearLayout);

    }

    //将标题栏设置为“最近会话”
    private void handleCommentField(LinearLayout linearLayout) {
        if (linearLayout == null) return;
        (linearLayout.findViewById(R.id.titleStatus)).setVisibility(View.GONE);
        (linearLayout.findViewById(R.id.titleProgress)).setVisibility(View.GONE);
        TextView title = (TextView) linearLayout.findViewById(R.id.comment_title);
        title.setText(R.string.icon_menu_chat);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        m_swipeListView = (SwipeListView)view.findViewById(R.id.recent_listview);
        m_swipeListView.setEmptyView(view.findViewById(R.id.recent_empty));
        m_swipeListView.setAdapter(m_recentChatAdapter);
        m_swipeListView.setSwipeListViewListener(m_baseSwipeListViewListener);
        m_swipeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        m_recentChatAdapter.requery(); // 刷新adapter
        // 注册观察者
        m_contentResolver.registerContentObserver(ChatProvider.CONTENT_URI, true, m_chatObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        //注销观察者
        m_contentResolver.unregisterContentObserver(m_chatObserver);
    }


    private class ChatObserver extends ContentObserver {
        public ChatObserver() {
            super(m_handle);
        }

        @Override
        public void onChange(boolean change) {
            updateRoster();
        }
    }

    public void updateRoster() {
        m_recentChatAdapter.requery();
        m_recentChatAdapter.notifyDataSetChanged();//我感觉要加这一句
    }
}
