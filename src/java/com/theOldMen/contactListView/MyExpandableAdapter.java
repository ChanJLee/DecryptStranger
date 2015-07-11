//package com.theOldMen.contactListView;
//
//import android.app.Activity;
//import android.content.Context;
//import android.database.DataSetObserver;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseExpandableListAdapter;
//import android.widget.TextView;
//
//import com.theOldMen.Activity.R;
//
//import java.util.ArrayList;
//
///**
// * Created by Administrator on 2015-4-16.
// */
//public class MyExpandableAdapter extends BaseExpandableListAdapter {
//
//    private ArrayList<UsersGroup> m_usersGroups;
//    private LayoutInflater m_layoutInflater;
//
//    public MyExpandableAdapter(Context context, ArrayList<UsersGroup> usersGroups) {
//        super();
//        m_usersGroups    = usersGroups;
//        m_layoutInflater = ((Activity)context).getLayoutInflater();
//    }
//
//    @Override
//    public Object getChild(int groupPosition, int childPosition) {
//        return m_usersGroups.get(groupPosition).getUser(childPosition);
//    }
//
//    @Override
//    public long getChildId(int groupPosition, int childPosition) {
//        return Position2IdUtils.position2Id(groupPosition, childPosition);
//    };
//
//    @Override
//    public int getChildrenCount(int groupPosition) {
//        return m_usersGroups.get(groupPosition).getUsersNumber();
//    }
//
//    @Override
//    public Object getGroup(int groupPosition) {
//        return m_usersGroups.get(groupPosition);
//    }
//
//    @Override
//    public long getGroupId(int groupPosition) {
//        return groupPosition;
//    }
//
//    @Override
//    public int getGroupCount() {
//        return m_usersGroups.size();
//    }
//
//    @Override
//    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
//                                View convertView, ViewGroup parent) {
//        if(convertView == null) {
//            convertView = m_layoutInflater.inflate(R.layout.contact_list_child, parent, false);
//        }
//        User user = m_usersGroups.get(groupPosition).getUser(childPosition);
//        TextView name = (TextView)convertView.findViewById(R.id.contact_list_item_name);
//        name.setText(user.getUeserName());
//        TextView sign = (TextView)convertView.findViewById(R.id.user_sign);
//        sign.setText(user.getSign());
//
//
////        ImageView imageView = (ImageView)convertView.findViewById(R.id.user_image);
////        imageView.setImageResource(R.drawable.qqhead1);
//        return convertView;
//    }
//
//    @Override
//    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
//                             ViewGroup parent) {
//        if(convertView == null) {
//            convertView = m_layoutInflater.inflate(R.layout.layout_group_view, parent, false);
//        }
//        UsersGroup usersGroup = m_usersGroups.get(groupPosition);
//        TextView name = (TextView)convertView.findViewById(R.id.group_name);
//        name.setText(usersGroup.getGroupName());
//        TextView number = (TextView)convertView.findViewById(R.id.group_members_number);
//        number.setText("" + usersGroup.getUsersNumber());
//        return convertView;
//    }
//
//    @Override
//    public boolean hasStableIds() {
//        return true;
//    }
//
//    @Override
//    public boolean isChildSelectable(int groupPosition, int childPosition) {
//        return true;
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return m_usersGroups.size() == 0;
//    }
//
//    @Override
//    public void onGroupCollapsed(int groupPosition) {
//        //
//    }
//
//    @Override
//    public void onGroupExpanded(int groupPosition) {
//        //
//    }
//
//    @Override
//    public long getCombinedGroupId(long groupId) {
//        return groupId;
//    }
//
//    @Override
//    public long getCombinedChildId(long groupId, long childId) {
//        return childId;
//    }
//
//    @Override
//    public void registerDataSetObserver(DataSetObserver observer) {
//        //
//    }
//
//    @Override
//    public void unregisterDataSetObserver(DataSetObserver observer) {
//        //
//    }
//
//    @Override
//    public boolean areAllItemsEnabled() {
//        return true;
//    }
//
//}
