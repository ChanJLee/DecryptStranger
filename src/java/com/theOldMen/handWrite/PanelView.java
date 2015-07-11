package com.theOldMen.handWrite;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by 李嘉诚 on 2015/6/6.
 * 最后修改时间: 2015/6/6
 */
public class PanelView extends View {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Canvas m_canvas                 = null;
    private Bitmap  m_bitmap                = null;
    private float m_clickX                  = 0;
    private float m_clickY                  = 0;
    private float m_startX                  = 0;
    private float m_startY                  = 0;
    private Paint m_paint                   = null;
    private int m_color                     = 0;
    private float m_strokeWidth             = 10.f;
    private boolean m_isMove                = false;
    private boolean m_isStart               = true;
    private boolean m_isModified            = false;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (m_isStart) {
            m_isStart = false;

            int width = getMeasuredWidth();
            int height = getMeasuredHeight();

            boolean isClear = true;

            if (m_bitmap == null) {
                m_isModified = true;
                m_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } else {

                isClear = false;
                //m_bitmap = Bitmap.createBitmap(m_bitmap, 0, 0, width, height);

                m_xRatio = m_bitmap.getWidth() / (float)width;
                m_yRatio = m_bitmap.getHeight() / (float)height;
            }

            m_dest = new Rect(0, 0, width, height);

            init(m_bitmap, isClear);
        }

        handWriting();
        canvas.drawBitmap(m_bitmap, null, m_dest, null);
    }

    private Rect m_dest;
    private float m_xRatio = 1;
    private float m_yRatio = 1;


    public void setBackGround(Bitmap bitmap){

        m_bitmap = bitmap;
    }

    public Bitmap getBackGround(){

        if(m_isModified){
            //保存全部图层
            m_canvas.save(Canvas.ALL_SAVE_FLAG);
            m_canvas.restore();
        }

        return m_bitmap;
    }

    private void handWriting() {

        if (!m_isMove) return;


        m_isModified = true;
        m_canvas.drawLine(m_startX, m_startY, m_clickX, m_clickY, m_paint);

        m_startX = m_clickX;
        m_startY = m_clickY;
    }

    private void init(Bitmap originBitmap,boolean isClear){
        m_canvas = new Canvas(originBitmap);

        if(isClear)
          m_canvas.drawColor(Color.WHITE);

        m_color = Color.GREEN;

        m_paint = new Paint();
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setColor(m_color);
        m_paint.setAntiAlias(true);
        m_paint.setStrokeWidth(m_strokeWidth * m_xRatio);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(event.getAction() == MotionEvent.ACTION_DOWN){

            m_isMove = false;

            m_startX = m_clickX = event.getX() * m_xRatio;
            m_startY = m_clickY = event.getY() * m_yRatio;

            //强制重绘
            invalidate();
            return true;
        }

        else if(event.getAction() == MotionEvent.ACTION_MOVE){

            m_isMove = true;

            m_clickX = event.getX() * m_xRatio;
            m_clickY = event.getY() * m_yRatio;

            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }
}
