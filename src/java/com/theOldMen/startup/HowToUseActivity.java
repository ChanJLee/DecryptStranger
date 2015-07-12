package com.theOldMen.startup;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.theOldMen.Activity.R;


/**
 * Created by chan on 15-7-10.
 */
public class HowToUseActivity extends Activity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static int s_touchSlop;

    private static final String s_messages[] = {
            "丰富的交互体验 让你体会到前所未有的酸爽",
            "朋友圈 拉近人们的距离",
            "摇一摇 结识陌生的你",
            "不光是聊天哦 趣味更强"
    };

    private final short s_duration = 1000;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView m_messageTextView;
    private ImageView m_imageView;
    private ImageView m_pointsImageView[];
    private short m_index = 0;
    private float m_startX;
    private float m_endX;
    private View m_showContainer;
    private ObjectAnimator m_goneAnimator;
    private ObjectAnimator m_visibleAnimator;
    private boolean m_isLeft;
    private Button m_startButton;
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.how_to_use_layout);

        init();
    }

    //初始化函数
    private void init(){

        m_messageTextView = (TextView) findViewById(R.id.m_htuMessageTextView);
        m_imageView = (ImageView) findViewById(R.id.m_htuImageView);

        //获得认为是最小滑动的距离
        s_touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.m_htuPointsContainer);
        m_showContainer = findViewById(R.id.m_htuShowRelativeLayout);

        m_startButton = (Button) findViewById(R.id.m_htuStartButton);
        m_startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });

        int size = viewGroup.getChildCount();
        m_pointsImageView = new ImageView[size];

        for(int i = 0;i < size;++i){
            m_pointsImageView[i] = (ImageView) viewGroup.getChildAt(i);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        switch (event.getAction()) {
            ////////////////////////////////////////////////////////////////////////////////////
            case MotionEvent.ACTION_DOWN:
                m_startX = event.getRawX();
                break;
            ////////////////////////////////////////////////////////////////////////////////////
            case MotionEvent.ACTION_UP:

                m_endX = event.getRawX();

                if (s_touchSlop < Math.abs(m_endX - m_startX))
                    startAnimation(m_startX, m_endX);
                break;
            ////////////////////////////////////////////////////////////////////////////////////
            default: break;
        }

        return super.onTouchEvent(event);
    }

    public static Intent getIntent(Context context) {

        Intent intent = new Intent(context, HowToUseActivity.class);
        return intent;
    }


    private void startAnimation(final float startX,final float endX){

        //判断滑动的操作
        m_isLeft = (startX > endX);

        //如果当前在最后一个元素或者在第一个元素
        // 不合法的动作就会被屏蔽
        if((m_isLeft && m_index == 0) ||
                (!m_isLeft && m_index == (m_pointsImageView.length - 1)))
            return;

        //采用懒惰初始化  在必要的时候才分配内存
        //这里生成两个动画  一个是把旧的内容隐藏 一个是显示新的内容
        if(m_goneAnimator == null){

            m_goneAnimator = ObjectAnimator.ofFloat(m_showContainer,"alpha",1f,0f);
            m_visibleAnimator = ObjectAnimator.ofFloat(m_showContainer,"alpha",0f,1f);

            m_goneAnimator.setDuration(s_duration);
            m_visibleAnimator.setDuration(s_duration);

            //当旧的内容隐藏后就显示新的内容
            //新的内容在显示之前需要设置一下新的内容
            //通过nextMessage实现
            m_goneAnimator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);

                    nextMessage();
                    m_visibleAnimator.start();
                }
            });
        }

        m_goneAnimator.start();
    }

    @SuppressWarnings("deprecated")
    synchronized private void nextMessage(){

        //先把当前的小点点转换为正常颜色
        //后把索引下标指向下一个元素
        m_pointsImageView[m_index].setImageDrawable(
                getResources().getDrawable(R.drawable.page_indicator_unfocused)
        );

        if(m_isLeft && m_index != 0) --m_index;
        else if(!m_isLeft && m_index != m_pointsImageView.length - 1) ++m_index;

        //这次你们再说不会用我们的软件我就认为你们有内幕
        //这里我们强制导航  你必须阅读完我们的导航 才能使用我们的软件
        if(m_index == m_pointsImageView.length - 1)
            m_startButton.setVisibility(View.VISIBLE);

        m_messageTextView.setText(s_messages[m_index]);
        m_pointsImageView[m_index].setImageDrawable(
                getResources().getDrawable(R.drawable.page_indicator_focused));
    }
}
