package com.theOldMen.chat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.theOldMen.Activity.R;


public class BaiduMapActivity extends Activity {

	////////////////////////////////////////////////////////////////////////////////////////////////
	public static final String s_latitude 			= "latitude";
	public static final String s_longitude			= "longitude";
	public static final String s_address			= "address";
	////////////////////////////////////////////////////////////////////////////////////////////////
	// 定位相关
	private LocationClient m_locClient			= null;
	//发送按钮
	private Button 				m_sendButton 		= null;
	//正在处理的窗口
	private ProgressDialog 		m_progressDialog	= null;
	//百度地图的组件
	private BaiduMap m_baiduMap			= null;
	//监听器 用于监听百度地图的状态
	private BaiduSDKReceiver 	m_baiduReceiver		= null;
	//是否为第一次定位
	private boolean				m_isFirstLoc		= true;
	//显示百度地图的内容
	private MapView m_mapView 			= null;
	//坐标
	private BDLocation m_lastLocation  	= null;
	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 构造广播监听类，监听 SDK key 验证以及网络异常广播
	 */
	public class BaiduSDKReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String s = intent.getAction();
			if (s.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)) {
				Toast.makeText(BaiduMapActivity.this, "key 验证出错! 请在 AndroidManifest.xml 文件中检查 key 设置", Toast.LENGTH_SHORT).show();
			} else if (s.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
				Toast.makeText(BaiduMapActivity.this, "网络出错", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		init();
	}

	private void init(){

		setContentView(R.layout.activity_baidumap);
		m_mapView 		= (MapView) findViewById(R.id.bmapView);
		m_sendButton 	= (Button) findViewById(R.id.btn_location_send);

		//发送按钮 一开始设置为无效
		//当百度地图能够获得 到坐标信息后
		//使之为有效 用户按下按钮 即发送坐标信息
		m_sendButton.setEnabled(false);

		//发送按钮的监听事件
		m_sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendLocation();
			}
		});

		//获得百度地图
		m_baiduMap 		= m_mapView.getMap();

		//显示处理的对话框
		showProgressDialog();

		//初始化地图
		initMapView();

		// 注册 SDK 广播监听者
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
		iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
		m_baiduReceiver = new BaiduSDKReceiver();
		registerReceiver(m_baiduReceiver, iFilter);

		findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	private void showProgressDialog() {

		//产生一个显示正在确定你的位置的处理对话框
		//设置点击组件外部范围为无效
		//设置风格为SPINNER
		m_progressDialog = new ProgressDialog(this);
		m_progressDialog.setCanceledOnTouchOutside(false);
		m_progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		m_progressDialog.setMessage("正在确定你的位置...");

		//设置用户选择取消后的动作
		//即退出定位窗口 并且不发送定位消息
		m_progressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {

				if (m_progressDialog.isShowing()) {
					m_progressDialog.dismiss();
				}

				finish();
			}
		});

		m_progressDialog.show();
	}

	//重载三个函数 释放百度地图占用的资源
	@Override
	protected void onPause() {

		m_mapView.onPause();

		if (m_locClient != null) {
			m_locClient.stop();
		}

		super.onPause();
		m_lastLocation = null;
	}

	@Override
	protected void onResume() {
		m_mapView.onResume();

		if (m_locClient != null) {
			m_locClient.start();
		}

		super.onResume();
	}

	@Override
	protected void onDestroy() {
		if (m_locClient != null)
			m_locClient.stop();
		m_mapView.onDestroy();
		unregisterReceiver(m_baiduReceiver);
		super.onDestroy();
	}


	//初始化百度地图
	private void initMapView() {

		// 去掉百度logo
		m_mapView.removeViewAt(1);

		//不显示我的位置，样覆盖物代替
		m_baiduMap.setMyLocationEnabled(false);

		//地图在tabhost中，请传入getApplicationContext()
		m_locClient = new LocationClient(this);

		//注册定位的监听器
		m_locClient.registerLocationListener(new BDLocationListener() {
			@Override
			public void onReceiveLocation(BDLocation bdLocation) {

				// map view 销毁后不在处理新接收的位置
				if (bdLocation == null) return;

				//记录我现在的位置
				m_lastLocation = bdLocation;

				if(m_progressDialog.isShowing())
					m_progressDialog.dismiss();

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

					m_sendButton.setEnabled(true);
				}

				mark(bdLocation.getLatitude(),bdLocation.getLongitude());
			}
		}); //绑定定位监听

		//配置参数
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setCoorType("bd09ll");
		option.setScanSpan(1000);
		m_locClient.setLocOption(option);
		m_locClient.start();  //开始定位
	}

	private void mark(double latitude, double longitude) {

		// 定义Maker坐标点
		LatLng point = new LatLng(latitude, longitude);

		// 构建Marker图标
		BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);

		// 构建MarkerOption，用于在地图上添加Marker
		OverlayOptions option   = new MarkerOptions().position(point)
				.icon(bitmap);

		 m_baiduMap.addOverlay(option);
	}

	//发送我的位置信息  其中内容为
	//经度 纬度 地址
	public void sendLocation() {

		Intent intent = new Intent();
		intent.putExtra(s_latitude, m_lastLocation.getLatitude());
		intent.putExtra(s_longitude, m_lastLocation.getLongitude());
		intent.putExtra(s_address, m_lastLocation.getAddrStr());

		//设置返回值
		this.setResult(RESULT_OK, intent);

		//结束定位的界面
		finish();

		//设置动画
		overridePendingTransition(
				R.anim.slide_in_from_left,
				R.anim.slide_out_to_right
		);
	}
}
