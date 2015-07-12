package com.theOldMen.adapter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.LruCache;
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

import org.jivesoftware.smackx.packet.VCard;

import java.io.File;
import java.io.FileOutputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by cheng on 2015/5/4.
 */
public class ContactAdapter extends BaseExpandableListAdapter {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //by chan
    private LruCache<String,Bitmap> m_lruCache;
    private static final short s_destWidthAndHeight = 100;
    ////////////////////////////////////////////////////////////////////////////////////////////////


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
            RosterProvider.RosterConstants.STATUS_MESSAGE
    };

    private ArrayList<Group> m_groups;
    private MainActivity m_context;
    private ContentResolver m_contentResolver;
    private LayoutInflater m_inflater;
    private boolean mIsShowOffline;

    public ContactAdapter(Context context) {
        m_context = (MainActivity)context;
        m_groups = new ArrayList<Group>();
        m_contentResolver = context.getContentResolver();
        m_inflater = ((Activity)context).getLayoutInflater();


        //by chan
        int memSize = (int) (Runtime.getRuntime().maxMemory() / 8);
        m_lruCache = new LruCache<String,Bitmap>(memSize){

            @Override
            protected int sizeOf(String key,Bitmap bitmap){
                return bitmap.getByteCount();
            }
        };
    }

    public void refresh() {

        if (m_groups != null) {
            m_groups.clear();
        }

        mIsShowOffline = PreferenceUtils.getPrefBoolean(m_context,
                PreferenceConstants.SHOW_OFFLINE, true);

        String selectWhere = null;

        if (!mIsShowOffline)
            selectWhere = OFFLINE_EXCLUSION;

        Cursor cursor = m_contentResolver.query(
                RosterProvider.GROUPS_URI,
                GROUP_QUERY,
                selectWhere,
                null,
                RosterProvider.RosterConstants.GROUP
        );
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            Group group = new Group();

            group.setGroupName(cursor.getString(
                    cursor.getColumnIndex(RosterProvider.RosterConstants.GROUP)
                )
            );
            group.setMembers(cursor.getString(
                    cursor.getColumnIndex(RosterProvider.RosterConstants.ONLINE_MEMBERS)
                )
            );

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
        Cursor cursor = m_contentResolver.query(
                RosterProvider.CONTENT_URI,
                USER_QUERY,
                selectWhere,
                new String[] { groupName },
                null
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {

            User user = new User();

            String userId = cursor.getString(
                    cursor.getColumnIndex(RosterProvider.RosterConstants.USER_ID
                    )
            );
            String userAlias = cursor.getString(
                    cursor.getColumnIndex(RosterProvider.RosterConstants.USER_ALIAS
                    )
            );

            user.setUserId(userId);
            user.setAlias(userAlias);

            PreferenceUtils.setPrefString(m_context, userId, userAlias);
            user.setStatusMessage(cursor.getString(
                    cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE)
                )
            );
            users.add(user);
            cursor.moveToNext();
        }

        cursor.close();
        return users;
    }

    @Override
    public int getChildrenCount(int groupPosition) {

        if(m_groups.isEmpty()) return 0;

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
        //7/12 chan
        Group group = m_groups.get(groupPosition);
        GroupViewHolder viewHolder = null;

        if(convertView == null){
            convertView = m_inflater.inflate(R.layout.contact_list_group,null);
            viewHolder = new GroupViewHolder();

            viewHolder.m_groupName = (TextView) convertView.findViewById(R.id.group_name);
            viewHolder.m_online = (TextView)convertView.findViewById(R.id.online_count);
            viewHolder.m_indicator = (ImageView)convertView.findViewById(R.id.group_indicator);

            convertView.setTag(viewHolder);
        } viewHolder = (GroupViewHolder) convertView.getTag();

        viewHolder.m_groupName.setText(TextUtils.isEmpty(group.getGroupName()) ? m_context
                .getString(R.string.default_group) : group.getGroupName());
        viewHolder.m_online.setText(group.getMembers());
        if (isExpanded) {
            viewHolder.m_indicator.setImageResource(R.drawable.indicator_expanded);
        } else {
            viewHolder.m_indicator.setImageResource(R.drawable.indicator_unexpanded);
        }

        convertView.setTag(R.id.position01, groupPosition);
        convertView.setTag(R.id.position02, -1);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        User user = getChild(groupPosition, childPosition);
        ChildViewHolder viewHolder = null;

        if(convertView == null) {
            convertView = m_inflater.inflate(R.layout.contact_list_child, null);
            viewHolder = new ChildViewHolder();

            viewHolder.m_headView = (CircleImageView) convertView.findViewById(R.id.head);
            viewHolder.m_itemName = (TextView) convertView.findViewById(R.id.contact_list_item_name);
            viewHolder.m_itemState = (TextView) convertView.findViewById(R.id.contact_list_item_state);
            convertView.setTag(viewHolder);
        }else viewHolder = (ChildViewHolder) convertView.getTag();

        //先都使用默认的头像
        viewHolder.m_headView.setImageBitmap(
                decodeSampledBitmapFromResource(m_context.getResources(),
                        R.drawable.avatar,
                        s_destWidthAndHeight,
                        s_destWidthAndHeight));
       // viewHolder.m_headView.setImageResource(R.drawable.avatar);

        Bitmap bitmap = getBitmapCache(user.getUserId());
        if (bitmap == null)
            loadImage(user,viewHolder);
        else viewHolder.m_headView.setImageBitmap(bitmap);

        viewHolder.m_itemName.setText(user.getAlias());
        viewHolder.m_itemState.setText(user.getStatusMessage());

        ///////这里没有修改过
        convertView.setTag(R.id.position01, groupPosition);
        convertView.setTag(R.id.position02, childPosition);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }

    private Bitmap getBitmapCache(String key){
        return m_lruCache.get(key);
    }

    private void addBitmapCache(String key,Bitmap bitmap) {

        if (getBitmapCache(key) == null) {
            m_lruCache.put(key, bitmap);
        }
    }

    private void loadImage(User user,ChildViewHolder viewHolder){

        //PATH是图片下载到SD卡中的地址
        //获得图片的绝对地址
        if (SystemHandleUtils.existSDCard()) {

            String mAccount = user.getUserId();
            String avatarPath = Environment.getExternalStorageDirectory() + "/theOldMen/" +
                    mAccount.split("@")[0] + "_avatar.jpg";

            File avatarFile = new File(avatarPath);
            Bitmap bitmap = null;

            //判断是否与服务器保持链接
            if (m_context.isConnected()) {

                //头像文件是不存在的
                if (!avatarFile.exists()) {

                    VCard vCard = m_context.getService().getUserVCard(mAccount);
                    byte[] bytes = null;

                    if (vCard != null) bytes = vCard.getAvatar();

                    if (bytes != null) {
                        FileOutputStream out = null;
                        try {
                            out = new FileOutputStream(avatarFile);
                            out.write(bytes);

                        } catch (Exception e) {
                        } finally {
                            try {
                                if (out != null) {
                                    out.flush();
                                    out.close();
                                }
                            } catch (Exception e) {
                            }
                        }

                        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        Bitmap tmp = Bitmap.createScaledBitmap(
                                bitmap,
                                s_destWidthAndHeight,
                                s_destWidthAndHeight,
                                false);
                        bitmap.recycle();
                        bitmap = tmp;
                        viewHolder.m_headView.setImageBitmap(bitmap);
                        addBitmapCache(mAccount, bitmap);
                    }
                } else {

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.outHeight = s_destWidthAndHeight;
                    options.outWidth = s_destWidthAndHeight;
                    options.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeFile(avatarPath,options);
                    viewHolder.m_headView.setImageBitmap(bitmap);
                    addBitmapCache(mAccount, bitmap);
                }
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    class GroupViewHolder{
        public TextView m_groupName;
        public ImageView m_indicator;
        public TextView m_online;
    }

    class ChildViewHolder{
        public CircleImageView m_headView;
        public TextView m_itemName;
        public TextView m_itemState;
    }
}
