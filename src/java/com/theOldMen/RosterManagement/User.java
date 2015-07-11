package com.theOldMen.RosterManagement;

/**
 * Created by cheng on 2015/5/4.
 */
public class User {
    private String m_userId;
    private String m_alias;
    private String m_statusMode;
    private String m_statusMessage;
    private String m_usedName;

    public String getUserId() {
        return m_userId;
    }

    public void setUserId(String userId) {
        this.m_userId = userId;
    }

    public String getAlias() {
        return m_alias;
    }

    public void setAlias(String alias) {
        this.m_alias = alias;
    }


    public String getStatusMode() {
        return m_statusMode;
    }

    public void setStatusMode(String statusMode) {
        this.m_statusMode = statusMode;
    }

    public String getStatusMessage() {
        return m_statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.m_statusMessage = statusMessage;
    }
}
