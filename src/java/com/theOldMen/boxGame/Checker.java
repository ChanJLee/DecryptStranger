package com.theOldMen.boxGame;

/**
 * Created by 李嘉诚 on 2015/4/11.
 * 最后修改时间: 2015/4/11
 */
public class Checker {

    //标志位
    private short m_code                       = 0x0000;
    //检验码
    final static private short s_successedCode = (short) 0x01FE;

    public void setPostion(short postion){

        m_code |= (1 << postion);
    }

    public void cancelPostion(short postion){

        int x = (1 << postion);

        if((m_code & x) != 0)
          m_code ^= x;
    }

    public boolean isSuccessed() { return m_code == s_successedCode; }
}
