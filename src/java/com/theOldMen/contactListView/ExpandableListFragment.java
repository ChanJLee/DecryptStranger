//package com.theOldMen.contactListView;
//
//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.support.v4.app.ListFragment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.BaseAdapter;
//import android.widget.ExpandableListView;
//
//import com.theOldMen.Activity.R;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
///**
// * Created by Administrator on 2015-4-16.
// */
//public class ExpandableListFragment extends ListFragment {
//    private ArrayList<UsersGroup> m_usersGroups;
//    private ExpandableListView m_expandableListView;
//    private MyExpandableAdapter m_expandableAdapter;
//
//    @Override
//    public void onCreate(Bundle onSavedInstanceState) {
//        super.onCreate(onSavedInstanceState);
////        UsersLab.getUserLab(getActivity()).loadUsers();
//        m_usersGroups = UsersLab.getUserLab(getActivity()).getUsersGroup();
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater layoutInflater, ViewGroup parent,
//                             Bundle onSavedInstanceState) {
//        View view = layoutInflater.inflate(R.layout.list_empty, parent, false);
//        return view;
//    }
//
//    @Override
//    public void onViewCreated(View view, Bundle onSavedInstanceState) {
//
//        m_expandableListView =
//                (ExpandableListView)view.findViewById(R.id.expandable_container);
//        m_expandableAdapter = new MyExpandableAdapter(getActivity(), m_usersGroups);
//
//        m_expandableListView.setAdapter(m_expandableAdapter);
//
//        m_expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                if (id >= (1L << 31)) {
//                    HashMap<String, Integer> hashMap = Position2IdUtils.Id2Position(id);
//                    final int groupPosition = hashMap.get(Position2IdUtils.groupPosition);
//                    final int childPosition = hashMap.get(Position2IdUtils.childPosition);
//                    Log.d("listen", "Child: " + "position :" + groupPosition + " id :" + childPosition);
//
//                    // 如果需要自定义，请重写这里的代码
//                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                    builder.setTitle("提示");
//                    builder.setMessage("确定删除该好友?");
//                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            m_usersGroups.get(groupPosition).removeUser(childPosition);
//                            m_expandableListView.setAdapter(m_expandableAdapter);//必须加这一句，否则异常
//                            ((BaseAdapter)m_expandableListView.getAdapter()).notifyDataSetChanged();
//                            m_expandableListView.expandGroup(groupPosition);
//                            dialog.dismiss();
//                        }
//                    });
//
//
//                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                        }
//                    });
//                    builder.create().show();
//                } else {
//                    Log.d("listen", "Group: " + "Position :" + id);
//                }
//                return true;
//            }
//        });
//    }
//
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        UsersLab.getUserLab(getActivity()).saveUsers();
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//    }
////
////    @Override
////    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
////        super.onCreateOptionsMenu(menu, menuInflater);
////        menuInflater.inflate(R.menu.fargment_list, menu);
////    }
//
////    @Override
////    public boolean onOptionsItemSelected(MenuItem menuItem) {
////        switch (menuItem.getItemId()) {
////            case R.id.menu_list_add_new_group:
////                UsersGroup usersGroup = new UsersGroup();
////                UsersLab.getUserLab(getActivity()).getUsersGroup().add(usersGroup);
////                ((BaseAdapter)m_expandableListView.getAdapter()).notifyDataSetChanged();
////                return true;
////            default:
////                return super.onOptionsItemSelected(menuItem);
////        }
////    }
//
////    @Override
////    public void onCreateContextMenu(ContextMenu menu, View v,
////                                    ContextMenu.ContextMenuInfo menuInfo) {
////        getActivity().getMenuInflater().inflate(R.menu.group_list_item_context, menu);
////    }
//}
