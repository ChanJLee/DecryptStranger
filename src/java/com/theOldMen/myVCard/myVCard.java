package com.theOldMen.myVCard;

import org.jivesoftware.smackx.packet.VCard;

/**
 * Created by jz on 2015/5/7.
 */
public class myVCard extends VCard {
    private String mSex;
    private String mKind;

    public static final String KIND_OWN_VCARD = "OWN";
    public static final String KIND_OTHER_VCARD = "OTHER";
    public static final String KIND_SAVE = "SAVE";


    public void setSex(String sex){
        mSex=sex;
    }

    public void setKind(String kind){
        mKind=kind;
    }

    public String getSex(){
        return mSex;
    }




}
