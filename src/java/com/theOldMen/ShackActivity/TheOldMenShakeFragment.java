package com.theOldMen.ShackActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.theOldMen.Activity.R;
import com.theOldMen.boxGame.BoxGameMainActivity;
import com.theOldMen.connection.SmackImpl;
import com.theOldMen.view.LDialog.BaseLDialog;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


/**
 * Created by 李嘉诚 on 2015/4/13.
 * 最后修改时间: 2015/4/13
 */
public class TheOldMenShakeFragment extends Fragment {

    //////////////////////////////////////////////////////////////////////////////////
    public static final String          s_idTag                         = "the_old_men_shake_id";
    public static final String          s_nameTag                       = "the_old_men_shake_name";
    public static final short           s_upAndDownPicDuration          = 1000;
    public static final short           s_locTag                        = 0525;
    //get id
    private static final String s_idEle                                 = "id";
    //get name
    private static final String s_nameEle                               = "name";
    //get longitude
    private static final String s_longitudeEle                          = "longitude";
    //get latitude
    private static final String s_latitudeEle                           = "latitude";
    private static final String s_url                                   =
            "http://"+ SmackImpl.TEST_IP + ":8080/TheOldMenPeopleInTheVicinity";
    ////////////////////// /////////////////////////////////////////////////////////////
    TheOldMenShakeListener m_shakeListener                 = null;
    Vibrator                            m_vibrator                      = null;
    private RelativeLayout              m_imageUp                       = null;
    private RelativeLayout              m_imageDown                     = null;
    private RelativeLayout              m_title                         = null;
    private SlidingDrawer               m_drawer                        = null;
    private Button                      m_drawerButton                  = null;
    private BaiduMap m_baiduMap                      = null;
    private MapView m_baiduMapView                  = null;
    private LocationClient m_locationClient                = null;
    boolean                             m_isFirstLoc                    = true;// 是否首次定位
    private SDKReceiver                 m_receiver                      = null;
    private BDLocation m_location                      = null;
    private String                      m_name                          = null;
    private String                      m_id                            = null;

    ///////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // 注册 SDK 广播监听者 建立过滤器
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);

        //生成监听
        m_receiver          = new SDKReceiver();

        Intent x            = getActivity().getIntent();
        m_name              = (String) x.getSerializableExtra(s_nameTag);
        m_id                = (String) x.getSerializableExtra(s_idTag);

        //注册监听器
        getActivity().registerReceiver(m_receiver, iFilter);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup root,Bundle bundle) {

        View view = inflater.inflate(R.layout.the_old_men_shake_fragment,root,false);

        init(view);
        
        return view;
    }


    @SuppressWarnings("deprecated")
    private void init(View view){

        m_baiduMapView  = (MapView)view.findViewById(R.id.m_baiduMapView);

        //获得振动器
        m_vibrator      = (Vibrator) getActivity().getApplication()
                .getSystemService(Context.VIBRATOR_SERVICE);

        m_imageUp       = (RelativeLayout) view.findViewById(R.id.shakeImgUp);
        m_imageDown     = (RelativeLayout) view.findViewById(R.id.shakeImgDown);
        m_title         = (RelativeLayout) view.findViewById(R.id.shake_title_bar);

        m_drawer        = (SlidingDrawer) view.findViewById(R.id.m_slidingDrawer);
        m_drawerButton  = (Button) view.findViewById(R.id.handle);

        //设置抽屉的打开事件
        m_drawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener()
        {	public void onDrawerOpened()
            {

                //设置按钮显示的内容
                m_drawerButton.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.shake_report_dragger_down));

                //向上移动的动画
                TranslateAnimation up = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF,
                        0f,
                        Animation.RELATIVE_TO_SELF,
                        0f,Animation.RELATIVE_TO_SELF,
                        0f,Animation.RELATIVE_TO_SELF,
                        -1.0f);

                //设置持续时间
                up.setDuration(200);
                //设置停留在最后的位置
                up.setFillAfter(true);

                //开始播放动画
                m_title.startAnimation(up);

                //将震动监听器暂停
                m_shakeListener.stop();
            }
        });

		 /* 设定SlidingDrawer被关闭的事件处理 */
        m_drawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener()
        {	public void onDrawerClosed()
            {

                //设置按钮的外观
                m_drawerButton.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.shake_report_dragger_up));

                //向下移动的动画
                TranslateAnimation down = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF,
                        0f,
                        Animation.RELATIVE_TO_SELF,
                        0f,
                        Animation.RELATIVE_TO_SELF,
                        -1.0f,
                        Animation.RELATIVE_TO_SELF,
                        0f);

                //设置持续的时间
                down.setDuration(200);
                down.setFillAfter(false);
                m_title.startAnimation(down);

                m_shakeListener.start();
            }
        });

        //对返回按钮注册监听器
        ((Button)view.findViewById(R.id.m_mapBackButton))
                .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shakeActivityBack(v);
            }
        });

        m_shakeListener = new TheOldMenShakeListener(getActivity());
        m_shakeListener.setOnShakeListener(new TheOldMenShakeListener.OnShakeListener() {

            public void onShake() {

                //开始 摇一摇手掌动画
                startAnimation();
                //开始 震动
                startVibrato();

                startLoc();
            }
        });

        //初始化地图
        initMap();
    }

    private void startLoc(){

        //如果链接服务器的必须条件没有满足 那么就不连接了
        if(m_location == null ||
                m_id == null  ||
                m_name == null) return;

        //url 的后缀
        final String suffix  = "?" + s_nameEle + "=" + m_name + "&" +
                    s_idEle + "=" + m_id + "&" +
                    s_latitudeEle + "=" + m_location.getLatitude() + "&" +
                    s_longitudeEle + "=" + m_location.getLongitude();

        new Thread(

            new Runnable() {

                @Override
                public void run() {

                    //存放从服务器取得的xml文件
                    String xml = "";

                    try {

                        URL url               = new URL(s_url + suffix);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setConnectTimeout(3000);

                        //使用GET方式发送
                        con.setRequestMethod("GET");
                        con.setDoOutput(false);
                        con.setDoInput(true);
                        con.setUseCaches(false);

                        //设置请求头
                        con.setRequestProperty("Accept", "text/xml; charset=utf-8");
                        con.setRequestProperty("Content-Length", String.valueOf(0));
                        con.setRequestProperty("Accept-Charset", "utf-8");

                        //获得输入流
                        DataInputStream in        = new DataInputStream(con.getInputStream());

                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        byte[] bytes              = new byte[256];
                        int length                = -1;

                        while ((length = in.read(bytes)) > -1) {

                            out.write(bytes, 0, length);
                            out.flush();
                        }

                        xml                      = new String(out.toByteArray());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Message msg = m_handler.obtainMessage();

                    msg.arg1    = s_locTag;
                    msg.obj     = xml;

                    //发送报文
                    m_handler.sendMessage(msg);

                }
            }
        ).start();
    }

    private void initMap(){

        // 获取BaiduMap对象
        m_baiduMap       = m_baiduMapView.getMap();
        m_baiduMapView.removeViewAt(1); // 去掉百度logo

        //不显示我的位置，样覆盖物代替
        m_baiduMap.setMyLocationEnabled(true);

        //地图在tabhost中，请传入getApplicationContext()
        m_locationClient = new LocationClient(getActivity());

        //注册定位的监听器
        m_locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {

                // map view 销毁后不在处理新接收的位置
                if (bdLocation == null) return;

                //记录我现在的位置
                m_location = bdLocation;

                //获得我的坐标数据
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                                // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(100).latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude()).build();

                //谁知我的坐标数据
                m_baiduMap.setMyLocationData(locData);

                //如果是第一次定位
                if (m_isFirstLoc) {

                    //将标志位取消
                    m_isFirstLoc = false;

                    LatLng ll = new LatLng(bdLocation.getLatitude(),
                            bdLocation.getLongitude());
                    MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                    m_baiduMap.animateMapStatus(u);
                }
            }
        }); //绑定定位监听

        //配置参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        m_locationClient.setLocOption(option);
        m_locationClient.start();  //开始定位

        //设置地图的监听器
        setMapListener();
    }

    private void setMapListener() {

        //点击覆盖物事件
        m_baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {


            @Override
            public boolean onMarkerClick(Marker arg0) {

                final Intent x = new Intent(getActivity(), BoxGameMainActivity.class);
                x.putExtra(s_idTag,arg0.getExtraInfo().getSerializable(s_idTag));
                x.putExtra(s_nameTag,arg0.getExtraInfo().getSerializable(s_nameTag));

                BaseLDialog.Builder builder = new BaseLDialog.Builder(getActivity());
                BaseLDialog dialog =
                        builder.setContent("为申请添加" + arg0.getExtraInfo().getSerializable(s_idTag)
                                + "(" + arg0.getExtraInfo().getSerializable(s_nameTag) + ")而进入BoxGame?")
                                .setContentSize(20)
                                .setContentColor("#434343")
                                .setMode(false)
                                .setPositiveButtonText("确定")
                                .setPositiveColor("#3c78d8")
                                .setNegativeButtonText("取消")
                                .setNegativeColor("#cccccc")
                                .create();
                dialog.setListeners(new BaseLDialog.ClickListener() {
                    @Override
                    public void onConfirmClick() {

                        startActivity(x);
                        getActivity().finish();
                    }

                    @Override
                    public void onCancelClick() {
                    }
                });
                dialog.show();

                return false;
            }
        });
    }

    private void mark(double latitude, double longitude, String name,String id) {

        // 定义Maker坐标点
        LatLng point = new LatLng(latitude, longitude);

        // 构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.mk_friend);

        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option   = new MarkerOptions().position(point)
                .icon(bitmap);

        // 在地图上添加Marker，并显示
        Marker marker           = (Marker) m_baiduMap.addOverlay(option);
        marker.setTitle(name);

        //设置mark 的额外信息
        Bundle bundle           = new Bundle();

        //这里可以添加从服务器传回的信息
        bundle.putSerializable(s_idTag, id);
        bundle.putSerializable(s_nameTag, name);

        marker.setExtraInfo(bundle);
    }

    public void startAnimation() {

        //定义摇一摇动画动画
        //摇一摇动画有上下两个图片组成
        //最后形成一张整的图片

        //定义动画的集合
        AnimationSet set                   = new AnimationSet(true);

        //上部动画的第一个运动轨迹
        TranslateAnimation up1 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                -0.5f);
        //设置持续的时间
        up1.setDuration(s_upAndDownPicDuration);

        //上面部分动画运动的第二个轨迹
        TranslateAnimation up2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                +0.5f);
        //设置持续时间
        up2.setDuration(s_upAndDownPicDuration);
        //设置延迟时间 因为它是跟着第一个动画后面的
        up2.setStartOffset(s_upAndDownPicDuration);

        //讲动画放到动画结合中
        set.addAnimation(up1);
        set.addAnimation(up2);

        //开始播放动画
        m_imageUp.startAnimation(set);

        //同理上面的内容 两者是相反的
        AnimationSet        set2 = new AnimationSet(true);
        TranslateAnimation down1 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                +0.5f);
        down1.setDuration(s_upAndDownPicDuration);

        TranslateAnimation down2 = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                0f,
                Animation.RELATIVE_TO_SELF,
                -0.5f);
        down2.setDuration(s_upAndDownPicDuration);
        down2.setStartOffset(s_upAndDownPicDuration);

        set2.addAnimation(down1);
        set2.addAnimation(down2);

        m_imageDown.startAnimation(set2);
    }

    public void startVibrato(){

        //定义震动
        // 第一个｛｝里面是节奏数组，
        // 第二个参数是重复次数，
        // -1为不重复，
        // 非-1俄日从pattern的指定下标开始重复
        m_vibrator.vibrate( new long[]{500,200,500,200}, -1);

    }
    
    //标题栏 返回按钮
    public void shakeActivityBack(View v) {

        //添加代码处
        getActivity().finish();
    }
    
    
    @Override
    public void onDestroy() {

        super.onDestroy();
        
        if (m_shakeListener != null) {
            
            m_shakeListener.destroy();
        }

        getActivity().unregisterReceiver(m_receiver);
        // 退出时销毁定位
        m_locationClient.stop();
        // 关闭定位图层
        m_baiduMap.setMyLocationEnabled(false);
        m_baiduMapView.onDestroy();
        m_baiduMap = null;
    }

    @Override
    public void onPause() {

        m_baiduMapView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {

        m_baiduMapView.onResume();
        super.onResume();
    }

    /**
     * 构造广播监听类，监听 SDK key 验证以及网络异常广播
     */
    public class SDKReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            String s = intent.getAction();

            if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {

                //提示用户出错
                Toast.makeText(
                        getActivity(),
                        "key 验证出错! 请在 AndroidManifest.xml 文件中检查 key 设置",
                        Toast.LENGTH_SHORT).show();
            } else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
                Toast.makeText(getActivity(),"网络出错",Toast.LENGTH_SHORT).show();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private Handler m_handler = new Handler(){

      @Override
      public void handleMessage(Message msg){

          if(msg.arg1 == s_locTag){

              String xml = (String)msg.obj;

              if(xml.isEmpty())
              {
                  Toast.makeText(getActivity(),"未获得信息 请重试",Toast.LENGTH_SHORT).show();
              }
              else
              {
                  showInMap(xml);

                  m_drawer.open();
              }
          }

          else super.handleMessage(msg);
      }
    };

    private void showInMap(String xml){

        StringReader reader = new StringReader(xml);
        InputSource src = new InputSource(reader);

        SAXBuilder builder = new SAXBuilder();
        try {

            Document doc = builder.build(src);
            Element root = doc.getRootElement();

            List<Element> children = root.getChildren();

            for(Element item : children) {

                mark(
                        Double.parseDouble(item.getChild(s_latitudeEle).getValue()),
                        Double.parseDouble(item.getChild(s_longitudeEle).getValue()),
                        item.getChild(s_nameEle).getValue(),
                        item.getChild(s_idEle).getValue()
                );

            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
}
