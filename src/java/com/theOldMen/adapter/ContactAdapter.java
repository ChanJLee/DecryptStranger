package com.theOldMen.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.theOldMen.Activity.MainActivity;
import com.theOldMen.Activity.R;
import com.theOldMen.CircleImage.CircleImageView;
import com.theOldMen.RosterManagement.Group;
import com.theOldMen.RosterManagement.User;
import com.theOldMen.db.RosterProvider;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.util.PreferenceConstants;
import com.theOldMen.util.SystemHandleUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cheng on 2015/5/4.
 */
public class ContactAdapter extends BaseExpandableListAdapter {
    //查询总人数
    private static final String COUNT_MEMBERS = "SELECT COUNT() FROM "
            + RosterProvider.TABLE_ROSTER
            + " inner_query where inner_query."
            + RosterProvider.RosterConstants.GROUP
            + " = "
            + RosterProvider.QUERY_ALIAS
            + "."
            + RosterProvider.RosterConstants.GROUP
            ;

    //不在线的状态
    private static final String OFFLINE_EXCLUSION = RosterProvider.RosterConstants.STATUS_MODE +
            " != " + RosterProvider.RosterConstants.OFFLINE_NUMBER;
    //查询在线的人数
    private static final String COUNT_NOT_OFFLINE_MEMBERS = COUNT_MEMBERS
            + " AND inner_query."
            + OFFLINE_EXCLUSION;

    private static final String[] GROUP_QUERY = new String[] {
            RosterProvider.RosterConstants._ID,
            RosterProvider.RosterConstants.GROUP,
            "(" + COUNT_NOT_OFFLINE_MEMBERS + ") || '/' || (" + COUNT_MEMBERS
                    + ") AS members" //把查询到的写成a/b的形式
    };

    //联系人查询
    private static final String[] USER_QUERY = new String[] {
            RosterProvider.RosterConstants._ID,
            RosterProvider.RosterConstants.USER_ID,
            RosterProvider.RosterConstants.USER_ALIAS,
//            RosterConstants.STATUS_MODE,
            RosterProvider.RosterConstants.STATUS_MESSAGE
    };

    private ArrayList<Group> m_groups;
    private MainActivity m_context;
    private ContentResolver m_contentResolver;
    private LayoutInflater m_inflater;
    private boolean mIsShowOffline;
    private HashMap<String, String> m_hashMap; // 用户ID和图片名称的映射

    private static String TAG = "ContactAdapter";

    public ContactAdapter(Context context) {
        m_context = (MainActivity)context;
        m_groups = new ArrayList<Group>();
        m_contentResolver = context.getContentResolver();
        m_inflater = ((Activity)context).getLayoutInflater();
        m_hashMap = new HashMap<String, String>();
    }

    public void refresh() {
        if (m_groups != null && m_groups.size() > 0) {
            m_groups.clear();
        }
        mIsShowOffline = PreferenceUtils.getPrefBoolean(m_context,
                PreferenceConstants.SHOW_OFFLINE, true);
        String selectWhere = null;
        if (!mIsShowOffline)
            selectWhere = OFFLINE_EXCLUSION;
        Cursor cursor = m_contentResolver.query(RosterProvider.GROUPS_URI, GROUP_QUERY, selectWhere, null,
                RosterProvider.RosterConstants.GROUP);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Group group = new Group();
            group.setGroupName(cursor.getString(cursor.getColumnIndex(RosterProvider.RosterConstants.GROUP)));
            group.setMembers(cursor.getString(cursor.getColumnIndex(RosterProvider.RosterConstants.ONLINE_MEMBERS)));
            m_groups.add(group);
            cursor.moveToNext();
        }
        cursor.close();
        notifyDataSetChanged();

    }

    public ArrayList<Group> getGroups() {
        return m_groups;
    }

    @Override
    public int getGroupCount() {
        return m_groups.size();
    }

    protected ArrayList<User> getUsers(String groupName) {
        String selectWhere = RosterProvider.RosterConstants.GROUP + " = ?";
        if (!mIsShowOffline)
            selectWhere += " AND " + OFFLINE_EXCLUSION;

        ArrayList<User> users = new ArrayList<>();
        Cursor cursor = m_contentResolver.query(RosterProvider.CONTENT_URI, USER_QUERY, selectWhere,
                new String[] { groupName }, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            User user = new User();
            String userId = cursor.getString(cursor.getColumnIndex(RosterProvider.RosterConstants.USER_ID));
            String userAlias = cursor.getString(cursor.getColumnIndex(RosterProvider.RosterConstants.USER_ALIAS));
            user.setUserId(userId);

                user.setAlias(userAlias);
            PreferenceUtils.setPrefString(m_context, userId, userAlias);

            user.setStatusMessage(cursor.getString(cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE)));
            users.add(user);
            cursor.moveToNext();
        }
        cursor.close();
        return users;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (m_groups.size() < 1) {
            return 0;
        }
        return getUsers(m_groups.get(groupPosition).getGroupName()).size();
    }

    @Override
    public Group getGroup(int groupPosition) {
        return m_groups.get(groupPosition);
    }

    @Override
    public User getChild(int groupPosition, int childPosition) {
        return getUsers(m_groups.get(groupPosition).getGroupName()).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = m_inflater.inflate(R.layout.contact_list_group, parent, false);
        Group group = m_groups.get(groupPosition);
        TextView groupName = (TextView)convertView.findViewById(R.id.group_name);

        groupName.setText(TextUtils.isEmpty(group.getGroupName()) ? m_context
                .getString(R.string.default_group) : group.getGroupName());

        TextView online = (TextView)convertView.findViewById(R.id.online_count);
        online.setText(group.getMembers());
        ImageView indicator = (ImageView)convertView.findViewById(R.id.group_indicator);
        if (isExpanded) {
            indicator.setImageResource(R.drawable.indicator_expanded);
        } else {
            indicator.setImageResource(R.drawable.indicator_unexpanded);
        }
        convertView.setTag(R.id.position01, groupPosition);
        convertView.setTag(R.id.position02, -1);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        User user = getChild(groupPosition, childPosition);
        if (convertView == null) {
            convertView = m_inflater.inflate(R.layout.contact_list_child, parent, false);
        }
        CircleImageView headView = (CircleImageView)convertView.findViewById(R.id.head);
        //PATH是图片下载到SD卡中的地址
        //获得图片的绝对地址
        if (SystemHandleUtils.existSDCard()) {

            String mAccount = user.getUserId();

            String avatarPath = Environment.getExternalStorageDirectory() + "/theOldMen/" +
                    mAccount.split("@")[0] + "_avatar.jpg";
            File avatarFile = new File(avatarPath);
            Bitmap bitmap = null;

            if (m_context.isConnected()) {
                if (!avatarFile.exists()) {
                    byte[] b = m_context.getService().getUserVCard(mAccount).getAvatar();
                    if (b != null) {
                        try {
                            FileOutputStream out = new FileOutputStream(avatarFile);
                            out.write(b);
                            out.flush();
                            out.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
                        headView.setImageBitmap(bitmap);
                        //Log.d(TAG, "头像加载成功");
                    } else {
                        headView.setImageResource(R.drawable.avatar);
                        //Log.d(TAG, "使用默认头像");
                    }
                } else {
                    headView.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
                    //Log.d(TAG, "存在,使用成功下载的头像");
                }
            } else {
                headView.setImageResource(R.drawable.avatar);
                //Log.d(TAG, "SD卡不存在,使用默认头像");
            }
        }

        //先检索头像文件是否存在。如果不存在则使用byte[] b= m_context.getService().getVCard().getAvatar()
        // 获得头像字节码 （可以被直接set） 如果存在则使用本地文件。
        TextView textView = (TextView)convertView.findViewById(R.id.contact_list_item_name);
        textView.setText(user.getAlias());
        textView = (TextView)convertView.findViewById(R.id.contact_list_item_state);
        textView.setText(user.getStatusMessage());
        convertView.setTag(R.id.position01, groupPosition);
        convertView.setTag(R.id.position02, childPosition);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
