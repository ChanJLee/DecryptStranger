package com.theOldMen.contactListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Administrator on 2015-4-16.
 */
public class UsersGroup {

    /*
    * m_groupName  分组的组名
    * m_users      分组的用户列表
    * m_userNumber 分组的用户数目
    * */
    private int m_userNumber;
    private String m_groupName;
    private ArrayList<User> m_users = new ArrayList<User>();

    /*用作存储的JSON文件的键名*/
    private static final String USERS = "users";
    private static final String GROUPNAME = "groupname";
    private static final String USERNUMBER = "usenumber";

    public UsersGroup(String groupName, ArrayList<User> users) {
        this.m_users      = users;
        this.m_groupName  = groupName;
        this.m_userNumber = users.size();
    }

    public UsersGroup(String groupName) {
        this.m_users      = new ArrayList<User>();
        this.m_groupName  = groupName;
        this.m_userNumber = 0;
    }

    public UsersGroup(JSONObject jsonObject) throws JSONException {
        this.m_groupName = jsonObject.getString(GROUPNAME);
        this.m_userNumber = jsonObject.getInt(USERNUMBER);
        this.m_users = new ArrayList<User>();
        if (jsonObject.has(USERS)) {
            JSONArray jsonArray = jsonObject.getJSONArray(USERS);
            for (int i = 0; i < jsonArray.length(); ++ i)
                this.m_users.add(new User(jsonArray.getJSONObject(i)));
        }
    }

    public String getGroupName() {
        return m_groupName;
    }

    public void setGroupName(String groupName) {
        this.m_groupName = groupName;
    }

    public int getUsersNumber() {
        return this.m_userNumber;
    }

    public ArrayList<User> getUsers() {
        return this.m_users;
    }

    public void setUsers(ArrayList<User> users) {
        this.m_users = users;
    }

    public void clear() {
        this.m_users.clear();
        this.m_userNumber = 0;
    }

    public void addUser(User newUser) {
        this.m_users.add(newUser);
        this.m_userNumber ++;
    }

    public User removeUser(int index) {
        this.m_userNumber --;
        return this.m_users.remove(index);
    }

    public boolean removeUser(User user) {
        this.m_userNumber --;
        return this.m_users.remove(user);
    }

    public User getUser(int index) {
        return this.m_users.get(index);
    }

    private JSONArray getJSONUsersArray() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (User user : this.m_users) {
            jsonArray.put(user.toJSON());
        }
        return jsonArray;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(GROUPNAME, m_groupName);
        jsonObject.put(USERNUMBER, m_userNumber);
        if (m_userNumber > 0)
            jsonObject.put(USERS, getJSONUsersArray());
        return jsonObject;
    }
}
