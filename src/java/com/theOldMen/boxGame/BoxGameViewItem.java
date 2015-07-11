package com.theOldMen.boxGame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by 李嘉诚 on 2015/4/11.
 * 最后修改时间: 2015/4/11
 */
public class BoxGameViewItem extends View{

    //小格子的颜色
    private final static String s_color      = "#ddddff";
    private final static String s_blankColor = "#bbbbdd";
    //小格子中文字的大小
    private final static float s_textSz      = 30.0f;
    ///////////////////////////////////////////////////////////////////////
    //画笔
    private Paint m_paint                    = new Paint();
    //绘制用的矩形
    private Rect m_rect                      = new Rect();
    //小格子显示的文字
    private String m_text;
    /////////////////////////////////////////////////////////////////////
    //ctor
    public BoxGameViewItem(Context context, AttributeSet attrs,int style) {

        super(context, attrs,style);

        init();
    }

    public BoxGameViewItem(Context context)
    {
        this(context, null);
    }

    public BoxGameViewItem(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    ////////////////////////////////////////////////////////////////////
    //初始化
    private void init(){

        //设置文字大小
        m_paint.setTextSize(s_textSz);
    }

    //返回text
    public String getItemText() { return m_text; }

    //设置text
    public void setItemText(String text){

        //设置当前的文字
        m_text = text;

        //获得要绘制的文字矩形大小
        m_paint.getTextBounds(text,0,text.length(),m_rect);

        //强制视图进行重绘
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas){

        //先调用父类
        super.onDraw(canvas);

        //绘制背景 背景色由s_color s_blankColor 决定
        m_paint.setColor(m_text.length() != 0 ? Color.parseColor(s_color) : Color.parseColor(s_blankColor));
        m_paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, getWidth(), getHeight(), m_paint);

        //如果当前的文字有 那么就绘制它
        if(m_text.length() != 0)
            drawItemText(canvas);
    }

    //绘制文字
    private void drawItemText(Canvas canvas)
    {

        //设置字的颜色
        m_paint.setColor(Color.BLACK);

        //使其绘制在ITEM 的中央
        float x = (getWidth() - m_rect.width()) / 2;
        float y = getHeight() / 2 + m_rect.height() / 2;

        //绘制文字
        canvas.drawText(m_text, x, y, m_paint);
    }
}
