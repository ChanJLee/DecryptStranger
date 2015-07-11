package com.theOldMen.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.theOldMen.Activity.R;
import com.theOldMen.CircleImage.CircleImageView;
import com.theOldMen.ResideMenu.ResideMenu;
import com.theOldMen.chat.Message;
import com.theOldMen.db.ChatProvider;
import com.theOldMen.db.ChatProvider.ChatConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.util.MyUtil;
import com.theOldMen.view.LDialog.BaseLDialog;

/**
 * Created by cheng on 2015/5/21.
 */
public class RecentChatAdapter extends SimpleCursorAdapter {

    // 查询合并重复jid字段的所有聊天对象
    private static final String SELECT = ChatConstants.DATE
            + " in (select max(" + ChatConstants.DATE + ") from "
            + ChatProvider.TABLE_NAME + " group by " + ChatConstants.JID
            + " having count(*)>0)";

    //需要查询的字段
    ///////////////////////////////////////////////////////////////////////////
    private static final String[] FROM = new String[] {
            ChatProvider.ChatConstants._ID,
            ChatProvider.ChatConstants.DIRECTION,
            ChatProvider.ChatConstants.JID,
            ChatProvider.ChatConstants.MESSAGE,
            ChatProvider.ChatConstants.DELIVERY_STATUS };

    //按照日期从近到远的顺序排序
    private static final String SORT_ORDER = ChatConstants.DATE + " DESC";

    private Activity m_context;
    private LayoutInflater m_inflater;
    private ContentResolver m_contentResolver;
    private ResideMenu m_resideMenu;
    public RecentChatAdapter(Activity activity, ResideMenu resideMenu) {
        super(activity, 0, null, FROM, null);
        m_context = activity;
        m_inflater = LayoutInflater.from(activity);
        m_contentResolver = activity.getContentResolver();
        m_resideMenu = resideMenu;
    }


    public void requery() {
        Cursor cursor = m_contentResolver.query(ChatProvider.CONTENT_URI, FROM, SELECT, null,
                SORT_ORDER);
        Cursor oldCursor = getCursor();
        changeCursor(cursor); //更换新的游标
        m_context.stopManagingCursor(oldCursor); //停止管理老游标
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);

        String message = cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
        String jid = cursor.getString(cursor
                .getColumnIndex(ChatProvider.ChatConstants.JID));

        String userAlias = PreferenceUtils.getPrefString(m_context, jid, "");

        //新消息数量的字段
        String selection = ChatConstants.JID + " = '" + jid + "' AND "
                + ChatConstants.DIRECTION + " = " + ChatConstants.INCOMING
                + " AND " + ChatConstants.DELIVERY_STATUS + " = "
                + ChatConstants.DS_NEW;

        Cursor msgNumbercursor = m_contentResolver.query(ChatProvider.CONTENT_URI,
                new String[] { "count(" + ChatConstants.PACKET_ID + ")",
                        ChatConstants.MESSAGE }, selection,
                null, SORT_ORDER);
        msgNumbercursor.moveToFirst();
        int count = msgNumbercursor.getInt(0); // 获得新消息的数目
        ViewHolder viewHolder;
        if (convertView == null || convertView.getTag(R.drawable.ic_launcher) == null) {
            convertView = m_inflater.inflate(R.layout.recent_listview_item, parent, false);
            viewHolder = buildHolder(convertView, jid);
            convertView.setTag(R.drawable.ic_launcher, viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag(R.drawable.ic_launcher);
        }
        m_resideMenu.addIgnoredView(convertView);

        viewHolder.jidView.setText(userAlias+"("+MyUtil.splitJidAndServer(jid)+")");
        ///////////////////////////////////////////////////////////////////
        Message msg = Message.analyseMsgBody(message);
        viewHolder.msgView.setText(msg.msg);

        if (count > 0) {
            viewHolder.msgView.setText(Message.analyseMsgBody(msgNumbercursor.getString(msgNumbercursor
                    .getColumnIndex(ChatConstants.MESSAGE))).msg);
            //设置未读提示
//            viewHolder.unReadView.setText(msgNumbercursor.getString(0));
        }

//        viewHolder.unReadView.setVisibility(count > 0 ? View.VISIBLE
//                : View.GONE);
        viewHolder.unReadView.setVisibility(View.GONE);
        viewHolder.unReadView.bringToFront();
        msgNumbercursor.close();
        return convertView;
    }

    ////////////////////////////////////////////////////////////////
    //删除聊天记录
    void removeChatHistory(final String JID) {
        m_contentResolver.delete(ChatProvider.CONTENT_URI,
                ChatProvider.ChatConstants.JID + " = ?", new String[] { JID });
    }

    private void removeChatHistoryDialog(final String jid) {
        BaseLDialog.Builder builder = new BaseLDialog.Builder(m_context);
        BaseLDialog dialog =
                builder.setTitle("提示")
                        .setTitleColor("#434343")
                        .setTitleSize(20)
                        .setMode(false)
                        .setContent("确定要清空与该好友的聊天记录吗?")
                        .setPositiveButtonText("确定")
                        .setNegativeButtonText("取消")
                        .setPositiveColor("#3c78d8")
                        .setNegativeColor("#cccccc")
                        .create();
        dialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                removeChatHistory(jid);
            }

            @Override
            public void onCancelClick() {

            }
        });

        dialog.show();

    }

    private ViewHolder buildHolder(View convertView, final String jid) {
        ViewHolder holder = new ViewHolder();
        holder.jidView = (TextView) convertView
                .findViewById(R.id.recent_list_item_name);
        holder.msgView = (TextView) convertView
                .findViewById(R.id.recent_list_item_msg);
        holder.headView = (CircleImageView)convertView
                .findViewById(R.id.recent_chat_head);
        holder.unReadView = (TextView) convertView
                .findViewById(R.id.unreadmsg);
        holder.deleteBtn = (Button) convertView
                .findViewById(R.id.recent_del_btn);
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                removeChatHistoryDialog(jid);
            }
        });
        String avatarPath = MyUtil.getUserAvatarPath(jid);
        Bitmap avatarBitmap = BitmapFactory.decodeFile(avatarPath);
        if(avatarBitmap == null)
            holder.headView.setImageResource(R.drawable.avatar);
        else
            holder.headView.setImageBitmap(avatarBitmap);
        return holder;
    }

    public static class ViewHolder {
        TextView jidView;
        TextView msgView;
        TextView unReadView;
        CircleImageView headView;
        Button deleteBtn;

    }

}
