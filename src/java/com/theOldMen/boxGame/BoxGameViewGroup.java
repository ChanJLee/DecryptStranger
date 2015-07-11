package com.theOldMen.boxGame;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by 李嘉诚 on 2015/4/11.
 * 最后修改时间: 2015/4/11
 */
public class BoxGameViewGroup extends RelativeLayout{

    private onAddFriendListener mlistener ;
    //矩阵大小
    private static final short s_size        = 0x0003;
    //向量大小
    private static final short s_vecSize     = s_size * s_size;
    //存放向量处
    private BoxGameViewItem m_viewItemVec[]  = new BoxGameViewItem[s_vecSize];
    /////////////////////////////////////////////////////////////////////////////////
    //间隔
    private short m_margin                   = 0x000A;
    private short m_padding                  = 0x000A;
    private short m_itemWidth                = 0x0000;
    private short m_width                    = 0x0000;
    private Checker m_checker                = new Checker();
    private boolean m_once                   = false;
    private short m_blankIdx                 = 0x0000;

    //检测手势
    private GestureDetector m_gestureDetector;
    /////////////////////////////////////////////////////////////////////////////////
    //ctor
    public BoxGameViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
        mlistener = (BoxGameMainActivity)getContext();
    }
    public BoxGameViewGroup(Context context)
    {
        this(context, null);
    }
    public BoxGameViewGroup(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }
    ////////////////////////////////////////////////////////////////////////////////////

    private void init(){

        m_margin  = (short) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                m_margin, getResources().getDisplayMetrics());
        // 设置Layout的内边距，四边一致，设置为四内边距中的最小值
        m_padding = min(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                getPaddingBottom());

        //设置detector 的回调函数 使之能够获得 margin 和 padding
        //并且屏蔽布局的实现细节
        BoxGameGestureDetector detector = new BoxGameGestureDetector();
        detector.setCallback(new BoxGameGestureDetector.Callback(){

            @Override
            public void action(BoxGameGestureDetector.ACTION action) {
                move(action);
            }
        });

        m_gestureDetector = new GestureDetector(getContext(),detector);
        m_gestureDetector.setIsLongpressEnabled(true);
    }

    //找到最小的整数
    static short min(int x,int y,int m ,int n){

        return (short)Math.min(Math.min(x,y),Math.min(m,n));
    }

    private void move(BoxGameGestureDetector.ACTION action){

        //移动项
        moveItems(action);

        //check
        if(m_checker.isSuccessed()){
            Toast.makeText(getContext(),"success!",Toast.LENGTH_LONG).show();

            mlistener.addFriend();
        }
    }

    public interface onAddFriendListener {
        public void addFriend();
    }


    private void moveItems(BoxGameGestureDetector.ACTION action){

        //如果是垂直移动
        if(action == BoxGameGestureDetector.ACTION.UP ||
                action == BoxGameGestureDetector.ACTION.DOWN)
            moveItemsVertical(action);

            //如果是水平移动
        else moveItemsHorizontal(action);
    }

    /**
     * 安卓坐标系统
     * +------------ x
     * \
     * \
     * \
     * \
     * y
     * */

    private void moveItemsVertical(BoxGameGestureDetector.ACTION action){

        //获得Y 值
        int y = m_blankIdx / s_size;

        //检测是否可以移动  如果在最上面且 还要向上移动
        //或者 在最下面 且还要向下移动
        //那么我们就什么都不做
        if((y == 0 && action == BoxGameGestureDetector.ACTION.DOWN) ||
                (y == s_size - 1 && action == BoxGameGestureDetector.ACTION.UP))
            return;

        //获得新的空白位置偏移量
        //默认向下运动
        int offset = -s_size;
        //如果是向上运动
        if(action == BoxGameGestureDetector.ACTION.UP)
            offset = s_size;

        //更新新的空白位置
        m_blankIdx += offset;

        //获得新空白位置的内容 （此时虽叫新空白位置 但它显示的文字却不是空的)
        String text = m_viewItemVec[m_blankIdx].getItemText();

        //如果当前的位置数字对应到正确的下标 那么我们还是要设置一下标志位
        if(text.equals((m_blankIdx - offset + 1) + "")){
            m_checker.setPostion((short) (m_blankIdx - offset + 1));
        }

        //设置一下之前的空白位置文字为新的空白位置的内容
        m_viewItemVec[m_blankIdx - offset].
                setItemText(text);
        //设置新的空白位置为空
        m_viewItemVec[m_blankIdx].setItemText("");
        //取消其标志位
        m_checker.cancelPostion((short) (m_blankIdx + 1));
    }

    private void moveItemsHorizontal(BoxGameGestureDetector.ACTION action){

        //同上
        int x = m_blankIdx % s_size;

        if((x == 0 && action == BoxGameGestureDetector.ACTION.RIGHT) ||
                (x == s_size - 1 && action == BoxGameGestureDetector.ACTION.LEFT))
            return;

        int offset = -1;
        if(action == BoxGameGestureDetector.ACTION.LEFT)
            offset = 1;

        m_blankIdx += offset;

        String text = m_viewItemVec[m_blankIdx].getItemText();

        if(text.equals((m_blankIdx - offset + 1) + "")){

            m_checker.setPostion((short) (m_blankIdx - offset + 1));
        }

        m_viewItemVec[m_blankIdx - offset].
                setItemText(text);
        m_viewItemVec[m_blankIdx].setItemText("");

        m_checker.cancelPostion((short) (m_blankIdx + 1));
    }

    /**
     * 测量Layout的宽和高，以及设置Item的宽和高，
     * 这里忽略wrap_content 以宽、高之中的最小值绘制正方形
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 获得正方形的边长
        m_width = (short)Math.min(getMeasuredHeight(), getMeasuredWidth());
        // 获得Item的宽度
        m_itemWidth = (short) ((m_width - m_padding * 2 - m_margin * (s_size - 1))
                / s_size);

        //如果调用过那么就不调用
        //因为这里要计算出高宽 而我们只需调用一次
        //所以使用这个方法
        //如果当前的布尔设置过 就不做这个事情了
        //如果没有那么我们是要做的
        if(m_once) {
            setMeasuredDimension(m_width, m_width);
            return;
        }

        // 放置Item
        for (int i = 0; i < m_viewItemVec.length; ++i) {

            BoxGameViewItem item = new BoxGameViewItem(getContext());
            m_viewItemVec[i] = item;
            item.setId(i + 1);

            LayoutParams lp = new LayoutParams(m_itemWidth, m_itemWidth);
            // 设置横向边距,不是最后一列
            if ((i + 1) % s_size != 0) {
                lp.rightMargin = m_margin;
            }

            // 如果不是第一列
            if (i % s_size != 0) {
                lp.addRule(RelativeLayout.RIGHT_OF,
                        m_viewItemVec[i - 1].getId());
            }

            // 如果不是第一行，//设置纵向边距，非最后一行
            if ((i + 1) > s_size) {
                lp.topMargin = m_margin;
                lp.addRule(RelativeLayout.BELOW,
                        m_viewItemVec[i - s_size].getId());
            }

            //添加到视图组中
            addView(item, lp);
        }

        //设置大小
        setMeasuredDimension(m_width, m_width);

        //设置内容
        setItemsText();

        //设置已经显示过了
        m_once = true;
    }

    private void setItemsText(){

        //获得一个0 ~ length * length - 1的链表
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0;i < m_viewItemVec.length;++i){
            list.add(i);
        }

        //随机打乱容器
        Collections.shuffle(list);

        //将容器中的内容填充到向量中
        for(int i = 0; i < m_viewItemVec.length;++i){

            int x = list.get(i);

            if(x == (i + 1)) m_checker.setPostion((short)(i + 1));

            if(x == 0){
                m_blankIdx = (short)(i);
                m_viewItemVec[i].setItemText("");
            }else m_viewItemVec[i].setItemText(x + "");
        }
    }

    //根据用户手势  进行游戏决策
    @Override public boolean onTouchEvent(MotionEvent event){

        //获得手势的信息
        m_gestureDetector.onTouchEvent(event);
        return true;
    }
}
