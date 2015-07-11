package com.theOldMen.chat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 4李嘉诚 on 2015/5/6.
 * 最后修改时间: 2015/5/6
 */
public class Message {

    //消息的类型
    //////////////////////////////////////////////////////////
    public static final String s_video = "video";
    public static final String s_voice  = "voice";
    public static final String s_text   = "text";
    public static final String s_location = "location";
    public static final String s_file   = "file";
    public static final String s_pic    = "picture";
    //////////////////////////////////////////////////////////

    ///转为json键值对
    /////////////////////////////////////////////////////////
    public static final String USERID ="userid";
    public static final String MSG_CONTENT ="msg";//消息内容
    public static final String DATE ="date";
    public static final String FROM ="from";
    public static final String MSG_TYPE ="type";
    public static final String RECEIVE_STAUTS="receive";// 接收状态
    public static final String TIME_REDIO="time";
    public static final String FIL_PAHT="filePath";

    ///////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////

    //日期
    public String m_date;
    //语音时间
    public String m_time;
    //类型
    public String m_type;
    //接受的消息 还是发送出去的消息
    public boolean m_isOut;
    //资源存放sd卡中的路径
    public String m_path;
    //存放数据
    public Object m_data;
    public Object m_extraData;

    //用户id
    public String userid;
    //from方式
    public String from;
    //text内容
    public String msg;
    //接收
    public String receive;
    //文件路径
    public String filePath;


    @Override
    public String toString() {
        return "Msg [userid=" + userid + ", msg=" + msg + ", date=" + m_date
                + ", type=" + m_type + ", receive=" + receive
                + ", time=" + m_time + ", filePath=" + filePath + "]";
    }


    public Message(){
    }

    public Message(String userid, String msg) {
        this.userid = userid;
        this.msg = msg;
    }
    public Message(String userid, String msg,String type,
                   String time, String filePath , boolean from) {
        super();
        this.userid = userid;
        this.msg = msg;
        this.m_type = type;
        this.m_time = time;
        this.filePath = filePath;
        this.m_isOut = from;
    }

    public Message(String userid, String msg,String type,
                   String time, String filePath , String videoPath) {
        super();
        this.userid = userid;
        this.msg = msg;
        this.m_type = type;
        this.m_time = time;
        this.filePath = filePath;
        this.receive = videoPath;
    }
    public static Message analyseMsgBody(String jsonStr) {
        //System.out.println(jsonStr+"jsonstr");
        Message msg = new Message();
        // 获取用户、消息、时间、IN
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            msg.userid = jsonObject.getString(Message.USERID);
            msg.msg = jsonObject.getString(Message.MSG_CONTENT);
            msg.m_type = jsonObject.getString(Message.MSG_TYPE);
            msg.receive = jsonObject.getString(Message.RECEIVE_STAUTS);
            msg.m_isOut = jsonObject.getBoolean(Message.FROM);
            //System.out.println(jsonStr+"解析字符串");
            String type=jsonObject.getString(Message.MSG_TYPE);

            if(!type.equals(s_text)){
                //	System.out.println(jsonObject.getString(Msg.TIME_REDIO)+"解析"+jsonObject.getString(Msg.FIL_PAHT));
                msg.m_time = jsonObject.getString(Message.TIME_REDIO);
                msg.filePath = jsonObject.getString(Message.FIL_PAHT);
            }

        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }finally{
            return msg;
        }


    }

    /**
     * 传json
     */
    public static  String  toJson(Message msg){
        JSONObject jsonObject=new JSONObject();
        String jsonStr="";
        try {
            jsonObject.put(Message.USERID, msg.userid+"");
            jsonObject.put(Message.MSG_CONTENT, msg.msg+"");
            jsonObject.put(Message.DATE, msg.m_date+"");
            jsonObject.put(Message.MSG_TYPE, msg.m_type+"");
            jsonObject.put(Message.FROM, msg.m_isOut+"");
            jsonObject.put(Message.RECEIVE_STAUTS, msg.receive+"");
            jsonObject.put(Message.TIME_REDIO, msg.m_time);
            jsonObject.put(Message.FIL_PAHT, msg.filePath);
            jsonStr= jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }finally{
            return jsonStr;
        }
    }


}
