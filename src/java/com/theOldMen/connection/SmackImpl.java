package com.theOldMen.connection;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.theOldMen.Activity.R;
import com.theOldMen.db.ChatProvider;
import com.theOldMen.db.RosterProvider;
import com.theOldMen.exception.MyException;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.tools.StatusWay;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class SmackImpl {
    //向服务器登记标识名与类型
    public static final String IDENTITY_NAME = "jz-pc";

    //注册账号返回的信息
    public static final int REGIST_NETWORKFAIL = 0;
    public static final int REGIST_SUCCESS = 1;
    public static final int REGIST_EXIST = 2;
    public static final int REGIST_FAIL = 3;

    //试用ip（试用阶段为了方便）
//    public static final String TEST_IP = "192.168.155.1";

    public static final String TEST_IP = "120.25.125.232";

    private static final int PACKET_TIMEOUT = 30*1000;

    // 发送离线消息的字段
    final static private String[] SEND_OFFLINE_PROJECTION = new String[] {
            ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
            ChatProvider.ChatConstants.DATE, ChatProvider.ChatConstants.PACKET_ID };
    // 发送离线消息的搜索数据库条件，自己发出去的OUTGOING，并且状态为DS_NEW
    final static private String SEND_OFFLINE_SELECTION = ChatProvider.ChatConstants.DIRECTION
            + " = "
            + ChatProvider.ChatConstants.OUTGOING
            + " AND "
            + ChatProvider.ChatConstants.DELIVERY_STATUS + " = " + ChatProvider.ChatConstants.DS_NEW;

    //发送文件与录音的路径
    public static String FILE_ROOT_PATH = Environment
            .getExternalStorageDirectory().getPath() + "/chat/file";
    public static String RECORD_ROOT_PATH = Environment
            .getExternalStorageDirectory().getPath() + "/chat/record";

    //静态块注册信息
    static {
        registerSmackProviders();
    }

    //设置xmpp命名空间以及扩展协议
    static void registerSmackProviders() {
        ProviderManager pm = ProviderManager.getInstance();
        // add IQ handling

        pm.addIQProvider("query", "jabber:iq:private",new PrivateDataManager.PrivateDataIQProvider());
        // XHTML
        pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",new XHTMLExtensionProvider());
        // Data Forms
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());


        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
                new DiscoverInfoProvider());
        // add delayed delivery notifications
        pm.addExtensionProvider("delay", "urn:xmpp:delay",
                new DelayInfoProvider());
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
        // add carbons and forwarding
        pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE,
                new Forwarded.Provider());
        pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
        pm.addExtensionProvider("received", Carbon.NAMESPACE,
                new Carbon.Provider());
        // add delivery receipts
        pm.addExtensionProvider(DeliveryReceipt.ELEMENT,
                DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
        pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT,
                DeliveryReceipt.NAMESPACE,
                new DeliveryReceiptRequest.Provider());
        // add XMPP Ping (XEP-0199)
        pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());

        // FileTransfer
        pm.addIQProvider("si", "http://jabber.org/protocol/si",
                new StreamInitiationProvider());

        pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
                new BytestreamsProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",new DiscoverItemsProvider());

        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",new DiscoverInfoProvider());

        //VCard
        pm.addIQProvider("vCard",  "vcard-temp", new VCardProvider());
        //Over

//        ServiceDiscoveryManager.setDefaultIdentity();
//        ServiceDiscoveryManager.setIdentityName(IDENTITY_NAME);
//        ServiceDiscoveryManager.setIdentityType(IDENTITY_TYPE);
    }

    private ConnectionConfiguration mXMPPConfig;//连接配置
    private XMPPConnection mXMPPConnection;//连接对象
    private XXService mService;//主服务
    private Roster mRoster;//联系人对象
    private final ContentResolver mContentResolver;//数据库操作者

    private RosterListener mRosterListener;//联系人监听
    private PacketListener mPacketListener;//消息监听
    private PacketListener mSendFailureListener;//消息发送失败监听
    private PacketListener mPongListener;//服务器连接监听

    private PacketListener mAddFriendListener;

    private FileTransferListener mFileTransferListener;//文件传输监听

    //----- ping-pong服务器----
    private String mPingID;//ping服务器的id
    private long mPingTimestamp;//时间戳
    private PendingIntent mPingAlarmPendIntent;//使用闹钟ping服务器
    private PendingIntent mPongTimeoutAlarmPendIntent;//判断超时的闹钟
    private static final String PING_ALARM = "com.theOldMen.PING_ALARM";// ping服务器闹钟广播的Action
    private static final String PONG_TIMEOUT_ALARM = "com.theOldMen.PONG_TIMEOUT_ALARM";// 判断超时的闹钟广播的Action

    private Intent mPingAlarmIntent = new Intent(PING_ALARM);
    private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
    private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
    private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

    private static int REFUSE_ADD_FRIEND = 0;
    private static int AGREE_ADD_FRIEND = 1;

    private int addFriendResult = -1;

    //-----------------------

    public SmackImpl(XXService service) {
        /*
            getPrefString(context , key , defaultValue)
            如果value存在 获取key-value键值对
            如不存在 获取key-defaultValue
        */
        String customServer = PreferenceUtils.getPrefString(service,
                PreferenceConstants.CUSTOM_SERVER, TEST_IP);//获取ip
        int port = PreferenceUtils.getPrefInt(service,
                PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);//获取端口
        String server = PreferenceUtils.getPrefString(service,
                PreferenceConstants.Server, PreferenceConstants.DOMAIN);//获取默认服务器domain

        boolean smackdebug = PreferenceUtils.getPrefBoolean(service,
                PreferenceConstants.SMACKDEBUG, false);//不需要smack debug --调试使用

        //---------配置连接------

        this.mXMPPConfig = new ConnectionConfiguration(customServer, port,
                    server);
        this.mXMPPConfig.setReconnectionAllowed(false);//关闭默认的重新尝试连接 以下自行实现
        this.mXMPPConfig.setSendPresence(false);//设置离线状态
        this.mXMPPConfig.setCompressionEnabled(false); // 是否可压缩
        this.mXMPPConfig.setDebuggerEnabled(smackdebug); //不开启smackdebug

        //----------------------

        this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
        this.mService = service;
        mContentResolver = service.getContentResolver();
    }

    //注册使用
    public SmackImpl(Context context){

        mXMPPConfig = new ConnectionConfiguration(PreferenceUtils.getPrefString(context,
                PreferenceConstants.CUSTOM_SERVER, TEST_IP),PreferenceUtils.getPrefInt(context,
                PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT));
        this.mXMPPConfig.setReconnectionAllowed(false);
        this.mXMPPConfig.setSendPresence(false);
        this.mXMPPConfig.setCompressionEnabled(false);

        this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
        this.mContentResolver = null;
        }


    public void saveVcard(VCard vcard) throws XMPPException {
        if (!mXMPPConnection.isConnected())
            return;
        vcard.save(mXMPPConnection);
    }

    public VCard getVcard(){
        if (!mXMPPConnection.isConnected())
            return null;
        VCard vcard = new VCard();
        try {
            vcard.load(mXMPPConnection);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        return vcard;
    }

    public VCard getUserVcard(String userId){
        if (!mXMPPConnection.isConnected())
            return null;
        VCard vcard = new VCard();
        try {
            vcard.load(mXMPPConnection,userId);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        return vcard;
    }

    /**
     * 注册
     *
     * @param account
     *            注册帐号
     * @param password
     *            注册密码
     * @return 1、注册成功 0、服务器没有返回结果2、这个账号已经存在3、注册失败
     */
    public int regist(String account, String password) {
        try {
            if (mXMPPConnection.isConnected()) {// 首先判断是否还连接着服务器，需要先断开
                try {
                    mXMPPConnection.disconnect();
                } catch (Exception e) {
                    //L.d("conn.disconnect() failed: " + e);
                }
            }
            try {
                mXMPPConnection.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!mXMPPConnection.isConnected()) {
                throw new XMPPException("SMACK connect failed without exception!");
            }
            //添加注册信息
            Registration reg = new Registration();
            reg.setType(IQ.Type.SET);
            reg.setTo(mXMPPConnection.getServiceName()); // 这个sendTo决定了域。这里是jz-pc

            reg.setUsername(account);
            reg.setPassword(password);
            reg.addAttribute("android", "theOldMen_createUser_android"); // 这边addAttribute不能为空，记作标识

            //过滤注册信息
            PacketFilter filter = new AndFilter(new PacketIDFilter(
                    reg.getPacketID()), new PacketTypeFilter(IQ.class));
            PacketCollector collector = mXMPPConnection.createPacketCollector(
                    filter);
            mXMPPConnection.sendPacket(reg);
            IQ result = (IQ) collector.nextResult(SmackConfiguration
                    .getPacketReplyTimeout());

            collector.cancel();// Stop queuing results停止请求results（是否成功的结果）

            //根据返回值判断注册结果
            if (result == null) {
                return REGIST_NETWORKFAIL;
            } else if (result.getType() == IQ.Type.RESULT) {
                return REGIST_SUCCESS;
            } else { // if (result.getType() == IQ.Type.ERROR)
                if (result.getError().toString().equalsIgnoreCase("conflict(409)")) {
                    return REGIST_EXIST;
                } else {
                    return REGIST_FAIL;
                }
            }

        } catch (XMPPException e) {
            e.printStackTrace();
        }
        return REGIST_FAIL;
    }

    /**
     * 登陆
     *
     * @param account
     *            登陆帐号
     * @param password
     *            登陆密码
     */
    public boolean login(String account, String password) throws MyException {
        try {
            if (mXMPPConnection.isConnected()) {// 首先判断是否还连接着服务器，需要先断开
                try {
                    mXMPPConnection.disconnect();
                } catch (Exception e) {
                    //L.d("conn.disconnect() failed: " + e);
                }
            }
            PreferenceUtils.setPrefBoolean(mService,
                    PreferenceConstants.isLogin, true);

            SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);//超时时间
//            SmackConfiguration.setKeepAliveInterval(-1);
            SmackConfiguration.setDefaultPingInterval(0);

            registerRosterListener();// 监听联系人动态变化
            mXMPPConnection.connect();

            if (!mXMPPConnection.isConnected()) {
                throw new MyException("SMACK connect failed without exception!");
            }

            mXMPPConnection.addConnectionListener(new ConnectionListener() {
                public void connectionClosedOnError(Exception e) {
                    mService.postConnectionFailed(e.getMessage());// 连接关闭时，动态反馈给服务
                }
                public void connectionClosed() {
                }

                public void reconnectingIn(int seconds) {
                }

                public void reconnectionFailed(Exception e) {
                }

                public void reconnectionSuccessful() {
                }
            });

            // 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
            initServiceDiscovery();

            // 如果登陆过了 则自动登陆
            if (!mXMPPConnection.isAuthenticated()) {
                String ressource = PreferenceUtils.getPrefString(mService,
                        PreferenceConstants.RESSOURCE, IDENTITY_NAME);
                mXMPPConnection.login(account, password, ressource);
            }
            setStatusFromConfig();// 更新在线状态

        } catch (Exception e) {}

        registerAllListener();// 注册监听其他的事件，比如新消息
        return mXMPPConnection.isAuthenticated();
    }

    public void sendFile(String user, File filePath) {

             // 创建文件传输管理器
            FileTransferManager mFileTransferManager = new FileTransferManager(mXMPPConnection);

            // 创建输出的文件传输
            OutgoingFileTransfer transfer = mFileTransferManager
                    .createOutgoingFileTransfer(user);

            // 发送文件
            try {
//                InputStream inputStream;
//                try {
//                    inputStream = new FileInputStream(filePath);
//                    transfer.sendStream(inputStream, filePath.getName(), filePath.length(), filePath.getName());
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
                transfer.sendFile(filePath, "send file!");
                
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    private void registerAllListener() {
        // actually, authenticated must be true now, or an exception must have
        // been thrown.
        if (isAuthenticated()) {
            registerMessageListener();// 注册新消息监听
            registerMessageSendFailureListener();// 注册消息发送失败监听
            registerPongListener();// 注册服务器回应ping消息监听
            sendOfflineMessages();// 发送离线消息

            registerAddFrientListener();//注册添加好友监听

            receivedFileListener();//添加文件传输监听


            if (mService == null) {
                mXMPPConnection.disconnect();
                return;
            }
            // 简单说就是为保证长连接 所以定时ping-pong服务器
            mService.rosterChanged();
        }
    }

    /************ start 接收文件处理 ********************/
    public void receivedFileListener() {
        // Create the file transfer manager

        /////////////////////////////////////////test//////////////////////////////
//        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection);
//        if (sdm == null)
//            sdm = new ServiceDiscoveryManager(mXMPPConnection);
//        sdm.addFeature("http://jabber.org/protocol/disco#info");
//        sdm.addFeature("jabber:iq:privacy");
        // Create the file transfer manager
//        sdm.setIdentityName(IDENTITY_NAME);
//        sdm.setIdentityType(IDENTITY_TYPE);
//        FileTransferNegotiator.setServiceEnabled(mXMPPConnection, true);
/////////////////////////////////////////////////////////////////////////

        final FileTransferManager manager = new FileTransferManager(mXMPPConnection);

		/*System.out.println("接收语音文件开始");
	    if (mfiletransfransferlistener != null){

			//如果监听存在就删除监听
			manager.removeFileTransferListener(mfiletransfransferlistener);
		}*/


        mFileTransferListener=new FileTransferListener() {
            public void fileTransferRequest(FileTransferRequest request) {
                // Check to see if the request should be accepted

//                System.out.println("接收语音文件");
                // Accept it
                IncomingFileTransfer transfer = request.accept();
                try {

                    //此处是聊天窗口
                    File file = new File(FILE_ROOT_PATH
                            +"/"+ request.getFileName());

                    //L.i("//////////////////****",request.getFileName()+"接收路径"+file.getPath()+"接收文件名称"+file.exists());

                    transfer.recieveFile(file);

                } catch (XMPPException e) {
                    e.printStackTrace();
                }

            }
        };


        manager.addFileTransferListener(mFileTransferListener);

    }
    /************ end 接收文件处理 ********************/


    /************ start 添加好友处理 ********************/
    public void registerAddFrientListener(){
        if (mAddFriendListener != null)
            mXMPPConnection.removePacketListener(mAddFriendListener);

        PacketTypeFilter filter = new PacketTypeFilter(Presence.class);

        mAddFriendListener = new PacketListener() {
            @Override
            public void processPacket(Packet packet) {
                if (packet instanceof Presence) {
                    Log.i("Presence", packet.toXML());
                    Presence presence = (Presence) packet;
                    String from = presence.getFrom();//发送方
                    String to = presence.getTo();//接收方


//                    showBox(mService ,"listener1");

                    //L.i("1111111111111*****",from);
                    //L.i("2222222222222*****",to);

                    if (presence.getType().equals(Presence.Type.subscribe)) {//好友申请
                        //L.i("3333333333333*****","addFriend");


                        Presence response = new Presence(Presence.Type.subscribe);
                        response.setTo(from);
                        mXMPPConnection.sendPacket(response);

                        mService.updateServiceNotification(from + "申请加您为好友");

                    } else if (presence.getType().equals(
                            Presence.Type.subscribed)) {//同意添加好友
                        //L.i("4444444444444*****","ok");
//                        Presence response = new Presence(Presence.Type.subscribe);
//                        response.setTo(from);
//                        mXMPPConnection.sendPacket(response);
                    } else if (presence.getType().equals(
                            Presence.Type.unsubscribe)) {//删除好友
                        //L.i("55555555555555*****","delete");
                    } else if (presence.getType().equals(
                            Presence.Type.unsubscribed)){//拒绝添加好友
                        //L.i("66666666666666*****","refuse");
                    }

                }
            }
        };
        mXMPPConnection.addPacketListener(mAddFriendListener,filter);
    }
    /************ end 添加好友处理 ********************/


    /************ start 新消息处理 ********************/
    private void registerMessageListener() {
        // do not register multiple packet listeners
        if (mPacketListener != null)
            mXMPPConnection.removePacketListener(mPacketListener);

        PacketTypeFilter filter = new PacketTypeFilter(Message.class);

        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                try {
                    if (packet instanceof Message) {// 如果是消息类型
                        Message msg = (Message) packet;
                        String chatMessage = msg.getBody();

                        // try to extract a carbon
                        Carbon cc = CarbonManager.getCarbon(msg);
                        if (cc != null
                                && cc.getDirection() == Carbon.Direction.received) {// 收到的消息
                            //L.d("carbon: " + cc.toXML());
                            msg = (Message) cc.getForwarded()
                                    .getForwardedPacket();
                            chatMessage = msg.getBody();
                            // fall through
                        } else if (cc != null
                                && cc.getDirection() == Carbon.Direction.sent) {// 如果是自己发送的消息，则添加到数据库后直接返回
                            //L.d("carbon: " + cc.toXML());
                            msg = (Message) cc.getForwarded()
                                    .getForwardedPacket();
                            chatMessage = msg.getBody();
                            if (chatMessage == null)
                                return;
                            String fromJID = getJabberID(msg.getTo());

                            addChatMessageToDB(ChatProvider.ChatConstants.OUTGOING, fromJID,
                                    chatMessage, ChatProvider.ChatConstants.DS_SENT_OR_READ,
                                    System.currentTimeMillis(),
                                    msg.getPacketID());
                            // always return after adding
                            return;
                        }

                        if (chatMessage == null) {
                            return;
                        }

                        if (msg.getType() == Message.Type.error) {
                            chatMessage = "<Error> " + chatMessage;
                        }

                        long ts;
                        DelayInfo timestamp = (DelayInfo) msg.getExtension(
                                "delay", "urn:xmpp:delay");
                        if (timestamp == null)
                            timestamp = (DelayInfo) msg.getExtension("x",
                                    "jabber:x:delay");
                        if (timestamp != null)
                            ts = timestamp.getStamp().getTime();
                        else
                            ts = System.currentTimeMillis();

                        String fromJID = getJabberID(msg.getFrom());// 消息来自对象

                        addChatMessageToDB(ChatProvider.ChatConstants.INCOMING, fromJID,
                                chatMessage, ChatProvider.ChatConstants.DS_NEW, ts,
                                msg.getPacketID());// 存入数据库，并标记为新消息DS_NEW
                        mService.newMessage(fromJID, chatMessage);// 通知service，处理是否需要显示通知栏
                    }
                } catch (Exception e) {
                    // SMACK silently discards exceptions dropped from
                    // processPacket :(
                    //L.e("failed to process packet:");
                    e.printStackTrace();
                }
            }
        };

        mXMPPConnection.addPacketListener(mPacketListener, filter);
    }

    /**
     * 将消息添加到数据库
     *
     * @param direction
     *            是否为收到的消息INCOMING为收到，OUTGOING为自己发出
     * @param JID
     *            此消息对应的jid
     * @param message
     *            消息内容
     * @param delivery_status
     *            消息状态 DS_NEW为新消息，DS_SENT_OR_READ为自己发出或者已读的消息
     * @param ts
     *            消息时间戳
     * @param packetID
     *            服务器为了区分每一条消息生成的消息包的id
     */
    private void addChatMessageToDB(int direction, String JID, String message,
                                    int delivery_status, long ts, String packetID) {
        ContentValues values = new ContentValues();

        values.put(ChatProvider.ChatConstants.DIRECTION, direction);
        values.put(ChatProvider.ChatConstants.JID, JID);
        values.put(ChatProvider.ChatConstants.MESSAGE, message);
        values.put(ChatProvider.ChatConstants.DELIVERY_STATUS, delivery_status);
        values.put(ChatProvider.ChatConstants.DATE, ts);
        values.put(ChatProvider.ChatConstants.PACKET_ID, packetID);

        mContentResolver.insert(ChatProvider.CONTENT_URI, values);
    }

    /************ end 新消息处理 ********************/

    /***************** start 处理消息发送失败状态 ***********************/
    private void registerMessageSendFailureListener() {
        // do not register multiple packet listeners
//        if (mSendFailureListener != null)
//            mXMPPConnection
//                    .removePacketSendFailureListener(mSendFailureListener);
//
//        PacketTypeFilter filter = new PacketTypeFilter(Message.class);
//
//        mSendFailureListener = new PacketListener() {
//            public void processPacket(Packet packet) {
//                try {
//                    if (packet instanceof Message) {
//                        Message msg = (Message) packet;
//                        String chatMessage = msg.getBody();
//
//                        Log.d("SmackableImp",
//                                "message "
//                                        + chatMessage
//                                        + " could not be sent (ID:"
//                                        + (msg.getPacketID() == null ? "null"
//                                        : msg.getPacketID()) + ")");
//                        changeMessageDeliveryStatus(msg.getPacketID(),
//                                ChatProvider.ChatConstants.DS_NEW);// 当消息发送失败时，将此消息标记为新消息，下次再发送
//                    }
//                } catch (Exception e) {
//                    // SMACK silently discards exceptions dropped from
//                    // processPacket :(
//                    //L.e("failed to process packet:");
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        mXMPPConnection.addPacketSendFailureListener(mSendFailureListener,
//                filter);
    }

    /**
     * 改变消息状态
     *
     * @param packetID
     *            消息的id
     * @param new_status
     *            新状态类型
     */
    public void changeMessageDeliveryStatus(String packetID, int new_status) {
        ContentValues cv = new ContentValues();
        cv.put(ChatProvider.ChatConstants.DELIVERY_STATUS, new_status);
        Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                + ChatProvider.TABLE_NAME);
        mContentResolver.update(rowuri, cv, ChatProvider.ChatConstants.PACKET_ID
                + " = ? AND " + ChatProvider.ChatConstants.DIRECTION + " = "
                + ChatProvider.ChatConstants.OUTGOING, new String[] { packetID });
    }

    /***************** end 处理消息发送失败状态 ***********************/
    /***************** start 处理ping服务器消息 ***********************/
    private void registerPongListener() {
        // reset ping expectation on new connection
        mPingID = null;

        if (mPongListener != null)
            mXMPPConnection.removePacketListener(mPongListener);

        mPongListener = new PacketListener() {

            @Override
            public void processPacket(Packet packet) {
                if (packet == null)
                    return;

                if (packet.getPacketID().equals(mPingID)) {// 如果服务器返回的消息为ping服务器时的消息，说明没有掉线

                    mPingID = null;
                    ((AlarmManager) mService
                            .getSystemService(Context.ALARM_SERVICE))
                            .cancel(mPongTimeoutAlarmPendIntent);
                }
            }

        };

        mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(
                IQ.class));// 正式开始监听
        mPingAlarmPendIntent = PendingIntent.getBroadcast(
                mService.getApplicationContext(), 0, mPingAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);// 定时ping服务器，以此来确定是否掉线
        mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(
                mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);// 超时闹钟
        mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(
                PING_ALARM));// 注册定时ping服务器广播接收者
        mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(
                PONG_TIMEOUT_ALARM));// 注册连接超时广播接收者
        ((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
                .setInexactRepeating(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis()
                                + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                        AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                        mPingAlarmPendIntent);//15分钟ping一次服务器
    }

    /**
     * BroadcastReceiver to trigger reconnect on pong timeout.
     */
    private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
        public void onReceive(Context ctx, Intent i) {
            //L.d("Ping: timeout for " + mPingID);
            mService.postConnectionFailed(XXService.PONG_TIMEOUT);
            logout();// 超时就断开连接
        }
    }

    /**
     * BroadcastReceiver to trigger sending pings to the server
     */
    private class PingAlarmReceiver extends BroadcastReceiver {
        public void onReceive(Context ctx, Intent i) {

            if (mXMPPConnection.isAuthenticated()) {
                sendServerPing();// 收到ping服务器的闹钟，即ping一下服务器
            }
        }
    }

    /***************** end 处理ping服务器消息 ***********************/

    /***************** start 发送离线消息 ***********************/

    public void sendOfflineMessages() {
        Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
                SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);// 查询数据库获取离线消息游标

        final int _ID_COL = cursor.getColumnIndexOrThrow(ChatProvider.ChatConstants._ID);
        final int JID_COL = cursor.getColumnIndexOrThrow(ChatProvider.ChatConstants.JID);
        final int MSG_COL = cursor.getColumnIndexOrThrow(ChatProvider.ChatConstants.MESSAGE);
        final int TS_COL = cursor.getColumnIndexOrThrow(ChatProvider.ChatConstants.DATE);
        final int PACKETID_COL = cursor
                .getColumnIndexOrThrow(ChatProvider.ChatConstants.PACKET_ID);

        ContentValues mark_sent = new ContentValues();
        mark_sent.put(ChatProvider.ChatConstants.DELIVERY_STATUS,
                ChatProvider.ChatConstants.DS_SENT_OR_READ);//将消息状态设置为已发送

        while (cursor.moveToNext()) {// 遍历之后将离线消息发出
            int _id = cursor.getInt(_ID_COL);
            String toJID = cursor.getString(JID_COL);
            String message = cursor.getString(MSG_COL);
            String packetID = cursor.getString(PACKETID_COL);
            long ts = cursor.getLong(TS_COL);
            //L.d("sendOfflineMessages: " + toJID + " > " + message);
            final Message newMessage = new Message(toJID, Message.Type.chat);
            newMessage.setBody(message);
            DelayInformation delay = new DelayInformation(new Date(ts));
            newMessage.addExtension(delay);
            newMessage.addExtension(new DelayInfo(delay));
            newMessage.addExtension(new DeliveryReceiptRequest());
            if ((packetID != null) && (packetID.length() > 0)) {
                newMessage.setPacketID(packetID);
            } else {
                packetID = newMessage.getPacketID();
                mark_sent.put(ChatProvider.ChatConstants.PACKET_ID, packetID);
            }
            Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
                    + ChatProvider.TABLE_NAME + "/" + _id);

            // 将消息标记为已发送再调用发送，因为假设此消息又未发送成功，有SendFailListener重新标记消息
            mContentResolver.update(rowuri, mark_sent, null, null);
            mXMPPConnection.sendPacket(newMessage); // must be after marking
            // delivered, otherwise it
            // may override the
            // SendFailListener
        }
        cursor.close();
    }

    /**
     * 作为离线消息存储起来，当自己掉线时调用
     *
     * @param cr
     * @param toJID
     * @param message
     */
    public static void sendOfflineMessage(ContentResolver cr, String toJID,
                                          String message) {
        ContentValues values = new ContentValues();
        values.put(ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.OUTGOING);
        values.put(ChatProvider.ChatConstants.JID, toJID);
        values.put(ChatProvider.ChatConstants.MESSAGE, message);
        values.put(ChatProvider.ChatConstants.DELIVERY_STATUS, ChatProvider.ChatConstants.DS_NEW);
        values.put(ChatProvider.ChatConstants.DATE, System.currentTimeMillis());

        cr.insert(ChatProvider.CONTENT_URI, values);
    }

    /***************** end 发送离线消息 ***********************/


    /****************** start 好友请求对话框**************************/
    private void showBox(final Context context ,final String user)
    {
        AlertDialog.Builder dialog=new AlertDialog.Builder(context);
        dialog.setTitle("添加好友请求");
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.setMessage("来自"+user+"的好友申请");
        dialog.setPositiveButton("接受", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addFriendResult = 1;

            }
        });
        dialog.setNegativeButton("拒绝",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addFriendResult = 0;
            }
        });
        AlertDialog mDialog=dialog.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);//设定为系统级警告，关键
        mDialog.show();
    }
    /******************************* end 好友请求对话框 ****************************/


    /******************************* start 联系人数据库事件处理 **********************************/
    private void registerRosterListener() {
        mRoster = mXMPPConnection.getRoster();
        mRosterListener = new RosterListener() {
            private boolean isFristRoter;

            @Override
            public void presenceChanged(Presence presence) {// 联系人状态改变，比如在线或离开、隐身之类
                //L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
                String jabberID = getJabberID(presence.getFrom());
                RosterEntry rosterEntry = mRoster.getEntry(jabberID);
                updateRosterEntryInDB(rosterEntry);// 更新联系人数据库
                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
            }

            @Override
            public void entriesUpdated(Collection<String> entries) {// 更新数据库，第一次登陆
                //L.i("entriesUpdated(" + entries + ")");
                for (String entry : entries) {
                    RosterEntry rosterEntry = mRoster.getEntry(entry);
                    updateRosterEntryInDB(rosterEntry);
                }
                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
            }

            @Override
            public void entriesDeleted(Collection<String> entries) {// 有好友删除时
                //L.i("entriesDeleted(" + entries + ")");

                for (String entry : entries) {
                    deleteRosterEntryFromDB(entry);
                }
                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
            }

            @Override
            public void entriesAdded(Collection<String> entries) {// 有人添加好友时，弹出对话框确认
                //L.i("entriesAdded(" + entries + ")");
                ContentValues[] cvs = new ContentValues[entries.size()];
                int i = 0;
                for (String entry : entries) {
                    RosterEntry rosterEntry = mRoster.getEntry(entry);
                    cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
                    //L.i("iiiiiiiiiiiiii",""+addFriendResult);
                }
                mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
                if (isFristRoter) {
                    isFristRoter = false;
                    mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
                }
            }
        };
        mRoster.addRosterListener(mRosterListener);
    }

    private String getJabberID(String from) {
        String[] res = from.split("/");
        return res[0].toLowerCase();
    }

    /**
     * 更新联系人数据库
     *
     * @param entry
     *            联系人RosterEntry对象
     */
    private void updateRosterEntryInDB(final RosterEntry entry) {
        final ContentValues values = getContentValuesForRosterEntry(entry);

        if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
                RosterProvider.RosterConstants.USER_ID + " = ?", new String[] { entry.getUser() }) == 0)// 如果数据库无此好友
            addRosterEntryToDB(entry);// 则添加到数据库
    }

    /**
     * 添加到数据库
     *
     * @param entry
     *            联系人RosterEntry对象
     */
    private void addRosterEntryToDB(final RosterEntry entry) {
        ContentValues values = getContentValuesForRosterEntry(entry);
        Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
        //L.i("addRosterEntryToDB: Inserted " + uri);
    }

    /**
     * 将联系人从数据库中删除
     *
     * @param jabberID
     */
    private void deleteRosterEntryFromDB(final String jabberID) {
        int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
                RosterProvider.RosterConstants.USER_ID + " = ?", new String[] { jabberID });
        //L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
    }

    /**
     * 将联系人RosterEntry转化成ContentValues，方便存储数据库，存储的个人信息
     *
     * @param entry
     * @return
     */
    private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
        final ContentValues values = new ContentValues();

        values.put(RosterProvider.RosterConstants.USER_ID, entry.getUser());
        values.put(RosterProvider.RosterConstants.USER_ALIAS, getName(entry));

//        VCard vCard = getVcard();
//        values.put(RosterProvider.RosterConstants.ALIAS, vCard.getNickName());

        Presence presence = mRoster.getPresence(entry.getUser());
        values.put(RosterProvider.RosterConstants.STATUS_MODE, getStatusInt(presence));
        values.put(RosterProvider.RosterConstants.STATUS_MESSAGE, presence.getStatus());
        values.put(RosterProvider.RosterConstants.GROUP, getGroup(entry.getGroups()));

        return values;
    }

    /**
     * 遍历获取组名
     *
     * @param groups
     * @return
     */
    private String getGroup(Collection<RosterGroup> groups) {
        for (RosterGroup group : groups) {
            return group.getName();
        }
        return "";
    }

    /**
     * 获取联系人名称
     *
     * @param rosterEntry
     * @return
     */
    private String getName(RosterEntry rosterEntry) {
        String name = rosterEntry.getName();
        if (name != null && name.length() > 0) {
            return name;
        }
        name = StringUtils.parseName(rosterEntry.getUser());
        if (name.length() > 0) {
            return name;
        }
        return rosterEntry.getUser();
    }

    /**
     * 获取状态
     *
     * @param presence
     * @return
     */
    private StatusWay getStatus(Presence presence) {
        if (presence.getType() == Presence.Type.available) {
            if (presence.getMode() != null) {
                return StatusWay.valueOf(presence.getMode().name());
            }
            return StatusWay.available;
        }
        return StatusWay.offline;
    }

    /******************************* end 联系人数据库事件处理 **********************************/

    /**
     * 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
     */
    private void initServiceDiscovery() {
        // register connection features
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager
                .getInstanceFor(mXMPPConnection);
        if (sdm == null)
            sdm = new ServiceDiscoveryManager(mXMPPConnection);

        sdm.addFeature("http://jabber.org/protocol/disco#info");

        // reference PingManager, set ping flood protection to 10s
        PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(
                10 * 1000);
        // reference DeliveryReceiptManager, add listener

        DeliveryReceiptManager dm = DeliveryReceiptManager
                .getInstanceFor(mXMPPConnection);
        dm.enableAutoReceipts();
//        dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener() {
//            public void onReceiptReceived(String fromJid, String toJid,
//                                          String receiptId) {
//                //L.d(SmackImpl.class, "got delivery receipt for " + receiptId);
//                changeMessageDeliveryStatus(receiptId, ChatProvider.ChatConstants.DS_ACKED);// 标记为对方已读
//            }
//        });
    }

    private int getStatusInt(final Presence presence) {
        return getStatus(presence).ordinal();
    }

    public void setStatusFromConfig() {
        boolean messageCarbons = PreferenceUtils.getPrefBoolean(mService,
                PreferenceConstants.MESSAGE_CARBONS, true);
        String statusMode = PreferenceUtils.getPrefString(mService,
                PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
        String statusMessage = PreferenceUtils.getPrefString(mService,
                PreferenceConstants.STATUS_MESSAGE,
                mService.getString(R.string.status_online));
        int priority = PreferenceUtils.getPrefInt(mService,
                PreferenceConstants.PRIORITY, 0);
        if (messageCarbons)
            CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(
                    true);

        Presence presence = new Presence(Presence.Type.available);
        Mode mode = Mode.valueOf(statusMode);
        presence.setMode(mode);
        presence.setStatus(statusMessage);
        presence.setPriority(priority);
        mXMPPConnection.sendPacket(presence);
    }


    public boolean isAuthenticated() {// 是否与服务器连接上，供本类和外部服务调用
        if (mXMPPConnection != null) {
            return (mXMPPConnection.isConnected() && mXMPPConnection
                    .isAuthenticated());
        }
        return false;
    }

    public ArrayList<String> getRosterEnteries(){
        mRoster = mXMPPConnection.getRoster();
        Collection<RosterEntry>re =mRoster.getEntries();
        ArrayList<String>al = new ArrayList<>();
        for(RosterEntry r : re){
            al.add(r.getUser());
        }
        return al;
    }

    // 添加联系人，供外部服务调用主
    public boolean addRosterItem(String user, String alias, String group)
            throws MyException {
        return addRosterEntry(user, alias, group);
    }

    // 添加联系人，供外部服务调用
    private boolean addRosterEntry(String user, String alias, String group)
            throws MyException {
//        //获取xmpp联系人对象
        mRoster = mXMPPConnection.getRoster();
        try {
            //添加好友到xmpp服务器好友数据库中

//            if (mRoster.getEntry(user) != null) {
            user = user+"@"+PreferenceConstants.DOMAIN;

            VCard vCard = getUserVcard(user);
            if(!TextUtils.isEmpty(vCard.getField("isFirstLogin"))){
                requestAuthorizationForRosterItem(user);
                return true;
            } else {
                //L.i("要添加的用户不存在");
                return false;
                }
        }
        catch (Exception e) {
            throw new MyException(e.getLocalizedMessage());
        }
    }


    // 删除联系人，供外部服务调用主
    public void removeRosterItem(String user) throws MyException {
        //L.d("removeRosterItem(" + user + ")");

        removeRosterEntry(user);
        mService.rosterChanged();
    }

    // 删除联系人，供外部服务调用
    private void removeRosterEntry(String user) throws MyException {
        mRoster = mXMPPConnection.getRoster();
        try {
            RosterEntry rosterEntry = mRoster.getEntry(user);

            if (rosterEntry != null) {
                mRoster.removeEntry(rosterEntry);
            }
        } catch (XMPPException e) {
            throw new MyException(e.getLocalizedMessage());
        }
    }

    // 重命名联系人，供外部服务调用
    public void renameRosterItem(String user, String newName)
            throws MyException {
        mRoster = mXMPPConnection.getRoster();
        RosterEntry rosterEntry = mRoster.getEntry(user);

        if (!(newName.length() > 0) || (rosterEntry == null)) {
            throw new MyException("JabberID to rename is invalid!");
        }
        rosterEntry.setName(newName);
    }

    // 移动好友到其他分组，供外部服务调用
   public void moveRosterItemToGroup(String user, String group)
            throws MyException {
        tryToMoveRosterEntryToGroup(user, group);
    }

    //移动好友到其他分组，供外部服务调用
    private void tryToMoveRosterEntryToGroup(String userName, String groupName)
            throws MyException {

        mRoster = mXMPPConnection.getRoster();
        RosterGroup rosterGroup = getRosterGroup(groupName);
        RosterEntry rosterEntry = mRoster.getEntry(userName);

        removeRosterEntryFromGroups(rosterEntry);

        if (groupName.length() == 0)
            return;
        else {
            try {
                rosterGroup.addEntry(rosterEntry);
            } catch (XMPPException e) {
                throw new MyException(e.getLocalizedMessage());
            }
        }
    }

    // 从对应组中删除联系人，供外部服务调用
    private void removeRosterEntryFromGroups(RosterEntry rosterEntry)
            throws MyException {
        Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

        for (RosterGroup group : oldGroups) {
            tryToRemoveUserFromGroup(group, rosterEntry);
        }
    }

    //删除组
    private void tryToRemoveUserFromGroup(RosterGroup group,
                                          RosterEntry rosterEntry) throws MyException {
        try {
            group.removeEntry(rosterEntry);
        } catch (XMPPException e) {
            throw new MyException(e.getLocalizedMessage());
        }
    }

    private RosterGroup getRosterGroup(String groupName) {// 获取联系人分组
        RosterGroup rosterGroup = mRoster.getGroup(groupName);

        // create group if unknown
        if ((groupName.length() > 0) && rosterGroup == null) {
            rosterGroup = mRoster.createGroup(groupName);
        }
        return rosterGroup;

    }

    public void renameRosterGroup(String group, String newGroup) {// 重命名分组
        //L.i("oldgroup=" + group + ", newgroup=" + newGroup);
        mRoster = mXMPPConnection.getRoster();
        RosterGroup groupToRename = mRoster.getGroup(group);
        if (groupToRename == null){
            return;
        }
        groupToRename.setName(newGroup);
    }

   public void requestAuthorizationForRosterItem(String user) {// 重新向对方发出添加好友申请
        Presence response = new Presence(Presence.Type.subscribe);
        response.setTo(user);
        mXMPPConnection.sendPacket(response);
   /*	available: 表示处于在线状态
		unavailable: 表示处于离线状态
		subscribe: 表示发出添加好友的申请
		unsubscribe: 表示发出删除好友的申请
		unsubscribed: 表示拒绝添加对方为好友
		error: 表示presence信息报中包含了一个错误消息。*/
   }

    public void addRosterGroup(String group) {// 增加联系人组
        //通过Roster类您可以找到所有Roster登陆、他们所属的组以及每个登陆当前的存在状态。
        mRoster = mXMPPConnection.getRoster();
        mRoster.createGroup(group);
    }

    public void sendMessage(String toJID, String message) {// 发送消息
        final Message newMessage = new Message(toJID, Message.Type.chat);
        newMessage.setBody(message);
        newMessage.addExtension(new DeliveryReceiptRequest());

        //处理可能的乱码
        String alterMessage = null;
        try {
            alterMessage= new String(message.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (isAuthenticated()) {

            //连上服务器
            //自己发出，对方id，消息内容，为自己发出或者已读，消息时间戳，每个消息自动id
            addChatMessageToDB(ChatProvider.ChatConstants.OUTGOING, toJID, alterMessage,
                    ChatProvider.ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
                    newMessage.getPacketID());
            mXMPPConnection.sendPacket(newMessage);
        } else {
            // send offline -> store to DB
            //没连上服务器；当消息发送失败时，将此消息标记为新消息，下次再发送
            addChatMessageToDB(ChatProvider.ChatConstants.OUTGOING, toJID, alterMessage,
                    ChatProvider.ChatConstants.DS_NEW, System.currentTimeMillis(),
                    newMessage.getPacketID());
        }
    }

    public void sendServerPing() {
        if (mPingID != null) {// 此时说明上一次ping服务器还未回应，直接返回，直到连接超时
            //L.d("Ping: requested, but still waiting for " + mPingID);
            return; // a ping is still on its way
        }
        Ping ping = new Ping();
        ping.setType(Type.GET);
        ping.setTo(PreferenceUtils.getPrefString(mService,
                PreferenceConstants.Server, PreferenceConstants.DOMAIN));
        mPingID = ping.getPacketID();// 此id其实是随机生成，但是唯一的
        mPingTimestamp = System.currentTimeMillis();
        //L.d("Ping: sending ping " + mPingID);
        mXMPPConnection.sendPacket(ping);// 发送ping消息

        // register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
        ((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).set(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                        + PACKET_TIMEOUT + 3000, mPongTimeoutAlarmPendIntent);// 此时需要启动超时判断的闹钟了，时间间隔为30+3秒
    }

    public String getNameForJID(String jid) {
        if (null != this.mRoster.getEntry(jid)
                && null != this.mRoster.getEntry(jid).getName()
                && this.mRoster.getEntry(jid).getName().length() > 0) {
            return this.mRoster.getEntry(jid).getName();
        } else {
            return jid;
        }
    }

    public boolean logout() {// 注销登录
        //L.d("unRegisterCallback()");
        // remove callbacks _before_ tossing old connection
        try {

            PreferenceUtils.setPrefBoolean(mService,
                    PreferenceConstants.isLogin,false);
            PreferenceUtils.setPrefString(mService,
                    PreferenceConstants.LASTLOGIN,
                    PreferenceUtils.getPrefString(mService, PreferenceConstants.ACCOUNT, null));
            RosterProvider.clearDatabase();

            mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
            mXMPPConnection.removePacketListener(mPacketListener);
//            mXMPPConnection
//                    .removePacketSendFailureListener(mSendFailureListener);
            mXMPPConnection.removePacketListener(mPongListener);
            ((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPingAlarmPendIntent);
            ((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
                    .cancel(mPongTimeoutAlarmPendIntent);
            mService.unregisterReceiver(mPingAlarmReceiver);
            mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
        } catch (Exception e) {
            // ignore it!
            return false;
        }
        if (mXMPPConnection.isConnected()) {
            // work around SMACK's #%&%# blocking disconnect()
            new Thread() {
                public void run() {
                    //L.d("shutDown thread started");
                    mXMPPConnection.disconnect();
                    //L.d("shutDown thread finished");
                }
            }.start();
        }
        setStatusOffline();
        this.mService = null;
        return true;
    }

    private void setStatusOffline() {//设置离线状态
        ContentValues values = new ContentValues();
        values.put(RosterProvider.RosterConstants.STATUS_MODE, StatusWay.offline.ordinal());
        mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
    }
}