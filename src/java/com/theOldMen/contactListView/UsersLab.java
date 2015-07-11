package com.theOldMen.contactListView;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015-4-16.
 */
public class UsersLab {
//    private UUID m_id;
    private Context m_context;
    private ArrayList<UsersGroup> m_usersGroups;
    private UsersInfoJSONSerializer m_usersInfoJSONSerializer;

    private static UsersLab s_usersLab;
    private static final String TAG = "UsersLab";
    private void init() {
        this.m_usersGroups = new ArrayList<UsersGroup>();
        UsersGroup userGroup1 = new UsersGroup("同学");
        userGroup1.addUser(new User("张三", "我是张三"));
        userGroup1.addUser(new User("李四", "我是李四"));
        userGroup1.addUser(new User("王五", "我是王五"));
        this.m_usersGroups.add(userGroup1);
        UsersGroup userGroup2 = new UsersGroup("家庭");
        userGroup2.addUser(new User("爸爸"));
        userGroup2.addUser(new User("妈妈"));
        this.m_usersGroups.add(userGroup2);
    }

    private UsersLab(Context context) {
//        this.m_id = UUID.randomUUID();
        this.m_context = context;
        this.m_usersInfoJSONSerializer = new UsersInfoJSONSerializer(context, TAG + ".json");
        init();
//        this.m_usersGroups = new ArrayList<UsersGroup>();
//        this.m_usersGroups.add(new UsersGroup(this.m_context.getString(R.string.default_group_name)));
    }

    public static UsersLab getUserLab(Context context) {
        if (s_usersLab == null) {
            s_usersLab = new UsersLab(context);
        }
        return s_usersLab;
    }

    public ArrayList<UsersGroup> getUsersGroup() {
        return this.m_usersGroups;
    }

//    public UUID getID() {
//        return m_id;
//    }

    public boolean saveUsers() {
        try {
            m_usersInfoJSONSerializer.saveUsers(m_usersGroups);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error save Users info", e);
            return false;
        }
    }

    public boolean loadUsers() {
        try {
            m_usersGroups = m_usersInfoJSONSerializer.loadUsers();
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error load Users info", e);
            return false;
        }
    }

    public void deleteGroup(UsersGroup usersGroup) {
        m_usersGroups.remove(usersGroup);
    }

    /**
     * 将一个用户从当前分组移动到另一个分组
     * fromIndex 当前分组在m_userGroups里面的index
     * 另一个分组在m_userGroups里面的index
     * */
    public void moveUserToAnotherGroup(User user, int fromIndex, int toIndex) {
        m_usersGroups.get(toIndex).addUser(user);
        m_usersGroups.get(fromIndex).removeUser(user);
    }
}