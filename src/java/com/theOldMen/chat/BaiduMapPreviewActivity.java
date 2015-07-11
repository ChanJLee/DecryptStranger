package com.theOldMen.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.theOldMen.Activity.R;

/**
 * Created by 李嘉诚 on 2015/5/10.
 * 最后修改时间: 2015/5/10
 */
public class BaiduMapPreviewActivity extends Activity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private BaiduMap m_baiduMap             = null;
    private MapView m_mapView              = null;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        setContentView(R.layout.baidu_map_preview_layout);

        init();
    }

    private void init(){
        m_mapView = (MapView) findViewById(R.id.m_mapView);
        m_baiduMap = m_mapView.getMap();

        initMap();
    }

    private void initMap(){

        Intent x            = getIntent();

        String latitude     = x.getStringExtra(TheOldMenChatMainActivity.s_latitude);
        String longitude    = x.getStringExtra(TheOldMenChatMainActivity.s_longitude);

        double d_latitude   = Double.parseDouble(latitude);
        double d_longitude  = Double.parseDouble(longitude);

        //设定中心点坐标
        LatLng cenpt        = new LatLng(d_latitude,d_longitude);

        //定义地图状态
        MapStatus mapStatus = new MapStatus.Builder()
                .target(cenpt)
                .zoom(18)
                .build();

        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);

        //改变地图状态
        m_baiduMap.setMapStatus(mapStatusUpdate);

        mark(d_latitude,d_longitude);
    }

    private void mark(double latitude, double longitude) {

        // 定义Maker坐标点
        LatLng point            = new LatLng(latitude, longitude);

        // 构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);

        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option   = new MarkerOptions().position(point)
                .icon(bitmap);

        // 在地图上添加Marker，并显示
        m_baiduMap.addOverlay(option);
    }
}
