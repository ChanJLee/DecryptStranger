package com.theOldMen.contactListView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2015-4-16.
 */
public class User {
    private String m_userName;
    private String m_sign;
    private String m_imagefileName;

    private static final String USERNAME = "username";
    private static final String SIGN = "sign";
    private static final String IMAGEFILENAME = "imagefilename";

    public User(String userName) {
        this.m_userName = userName;
    }

    public User(String userName, String sign) {
        this(userName);
        this.m_sign = sign;
    }

    public User(JSONObject jsonObject) throws JSONException {
        if (jsonObject.has(IMAGEFILENAME)) {
            this.m_imagefileName = jsonObject.getString(IMAGEFILENAME);
        }
        if (jsonObject.has(SIGN)) {
            this.m_sign = jsonObject.getString(SIGN);
        }
        this.m_userName = jsonObject.getString(USERNAME);
    }

    public String getUeserName() {
        return m_userName;
    }

    public void setUserName(String name) {
        m_userName = name;
    }

    public String getSign() {
        return m_sign;
    }

    public void setSign(String sign) {
        m_sign = sign;
    }

    public void setImage(String imageName) {
        this.m_imagefileName = imageName;
    }

    public String getImage() {
        return m_imagefileName;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(USERNAME, m_userName);
        if (m_sign != null)
            jsonObject.put(SIGN, m_sign);
        if (m_imagefileName != null)
            jsonObject.put(IMAGEFILENAME, m_imagefileName);
        return jsonObject;
    }
}