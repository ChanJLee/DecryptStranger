package com.theOldMen.boxGame;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created by 李嘉诚 on 2015/4/11.
 * 最后修改时间: 2015/4/11
 */
public class BoxGameGestureDetector implements GestureDetector.OnGestureListener {

    ////////////////////////////////////////////////////////////////////////////////////
    //回调接口 用于在获得了 用户手势方向后 调用这所需要做的工作
    public interface Callback{

        public void action(ACTION action);
    }
    private Callback m_callback;
    public void setCallback(Callback callback) { m_callback = callback; }
    ////////////////////////////////////////////////////////////////////////////////////
    //用户移动大小的阀值
    final static short s_flingMinDistance = 0x0042;
    ///////////////////////////////////////////////////////////////////////////////////
    // 运动方向的枚举
    public enum ACTION { LEFT, RIGHT, UP, DOWN }
    ///////////////////////////////////////////////////////////////////////////////////
    //这里我们都是使用的接口默认实现
    @Override
    public boolean onDown(MotionEvent event) { return false; }
    @Override
    public void onShowPress(MotionEvent event) {}
    @Override
    public boolean onSingleTapUp(MotionEvent event) { return false; }
    @Override
    public boolean onScroll(MotionEvent event, MotionEvent event2, float v, float v2) { return false; }
    @Override
    public void onLongPress(MotionEvent event) {}
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        //获得x y 的距离之差
        float x = e2.getX() - e1.getX();
        float y = e2.getY() - e1.getY();

        //如果x 方向的移动大于最小的值 并且x 方向的速度更快
        // 那么我们认为用户是像右滑动
        // 否则为向左
        if (x > s_flingMinDistance
                && Math.abs(velocityX) > Math.abs(velocityY))
        {
            m_callback.action(ACTION.RIGHT);

        } else if (x < -s_flingMinDistance
                && Math.abs(velocityX) > Math.abs(velocityY))
        {
            m_callback.action(ACTION.LEFT);

        }
        //如果y方向的移动大于最小的阀值并且y方向的移动速度更快
        // 那么我们认为用户是向下滑动的
        //否则为向上滑动
        else if (y > s_flingMinDistance
                && Math.abs(velocityX) < Math.abs(velocityY))
        {
            m_callback.action(ACTION.DOWN);

        } else if (y < -s_flingMinDistance
                && Math.abs(velocityX) < Math.abs(velocityY))
        {
            m_callback.action(ACTION.UP);
        }

        return true;
    }
}
