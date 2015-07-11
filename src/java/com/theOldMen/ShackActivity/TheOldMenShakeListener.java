package com.theOldMen.ShackActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by 李嘉诚 on 2015/4/13.
 * 最后修改时间: 2015/4/13
 */
public class TheOldMenShakeListener implements SensorEventListener {

    // 速度阈值，当摇晃速度达到这值后产生作用
    private static final int    s_speedShreshold     = 300;
    // 两次检测的时间间隔
    private static final int    s_updateIntervalTime = 70;
    //////////////////////////////////////////////////////////////////
    // 传感器管理器
    private SensorManager       m_sensorManager;
    // 传感器
    private Sensor              m_sensor;
    // 重力感应监听器
    private OnShakeListener     m_onShakeListener;

    // 上下文
    private Context             m_context;

    // 手机上一个位置时重力感应坐标
    private float               m_lastX;
    private float               m_lastY;
    private float               m_lastZ;

    // 上次检测时间
    private long                m_lastUpdateTime;
    ///////////////////////////////////////////////////////

    // ctor
    public TheOldMenShakeListener(Context context) {
        // 获得监听对象
        m_context = context;
        init();
    }
    ///////////////////////////////////////////////////////

    // 初始化
    public void init() {

        // 获得传感器管理器
        m_sensorManager = (SensorManager) m_context
                .getSystemService(Context.SENSOR_SERVICE);

        //如果获得了传感器管理器 那么我们就获得传感器
        // 获得重力传感器
        m_sensor = m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // 为传感器注册监听器
        m_sensorManager.registerListener(this, m_sensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    //取消注册
    public void destroy() {

        m_sensorManager.unregisterListener(this);
    }

    //停止
    public void stop(){
        destroy();
    }

    //开始
    public void start(){
        init();
    }

    // 设置重力感应监听器
    public void setOnShakeListener(OnShakeListener listener) {
        m_onShakeListener = listener;
    }

    // 重力感应器感应获得变化数据
    public void onSensorChanged(SensorEvent event) {

        // 现在检测时间
        long currentUpdateTime = System.currentTimeMillis();

        //两次检测的时间间隔
        long timeInterval = currentUpdateTime - m_lastUpdateTime;

        // 判断是否达到了检测时间间隔
        if (timeInterval < s_updateIntervalTime)
            return;

        // 现在的时间变成last时间
        m_lastUpdateTime = currentUpdateTime;

        // 获得x,y,z坐标
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // 获得x,y,z的变化值
        float deltaX = x - m_lastX;
        float deltaY = y - m_lastY;
        float deltaZ = z - m_lastZ;

        // 将现在的坐标变成last坐标
        m_lastX = x;
        m_lastY = y;
        m_lastZ = z;

        //sqrt 返回最近的双近似的平方根
        double speed2 = (deltaX * deltaX + deltaY * deltaY + deltaZ
                * deltaZ)/ timeInterval * 10000;

        // 达到速度阀值，发出提示
        if (speed2 >= (s_speedShreshold * s_speedShreshold)) {
             m_onShakeListener.onShake();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // 摇晃监听接口
    public interface OnShakeListener {
        public void onShake();
    }

}
