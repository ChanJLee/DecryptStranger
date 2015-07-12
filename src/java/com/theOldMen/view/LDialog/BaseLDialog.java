package com.theOldMen.view.LDialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.theOldMen.Activity.R;
import com.theOldMen.tools.L;
import com.theOldMen.view.RadioButton;

/**
 * Created by cheng on 2015/5/13.
 */
public class BaseLDialog extends Dialog {
    //默认的标题内容按钮的字体颜色
    private static int DEFAULT_TITLECOLOR = Color.parseColor("#CCCCCC");
    private static int DEFAULT_CONTENTCOLOR = Color.parseColor("#999999");
    private static int DEFAULT_BUTTONCOLOR = Color.parseColor("#CCCCCC");

    //默认的标题内容按钮的字符串
    private static String DEFAULT_TITLE = "TITLE";
    private static String DEFAULT_CONTENT = "this is content";
    private static String DEFAULT_POSITIVETEXT = "CONFIRM";
    private static String DEFAULT_NEGATIVETEXT = "CONCEL";

    //默认的标题内容按钮的字体大小
    private static int DEFAULT_TITLESIZE = 15;
    private static int DEFAULT_CONTENTSIZE = 12;
    private static int DEFAULT_BUTTONSIZE = 15;

    private ClickListener m_clickListener; //按钮事件的监听器
    private Button m_positiveButton, m_negativeButton;
    private boolean m_isMode = false;
    //按钮事件的监听器接口
    public interface ClickListener {
        public void onConfirmClick();
        public void onCancelClick();
    }

    private void setMode() {
        if (m_isMode) {
            Window window = getWindow();
            WindowManager.LayoutParams wl = window.getAttributes();
            wl.flags = wl.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            wl.flags = wl.flags | WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(wl);
        }
    }

    private BaseLDialog(Builder builder) {
        this(builder.m_context, R.style.LDialogs_Light);
//        this.m_clickListener = builder.m_clickListener;
        this.m_positiveButton = builder.m_positiveButton;
        this.m_negativeButton = builder.m_negativeButton;
        this.m_isMode = builder.m_isMotai;
        setMode();
    }

    public BaseLDialog(Context context, int theme) {
        super(context, theme);
    }

    //设置监听器
    public void setListeners(ClickListener clickListener) {
        m_clickListener = clickListener;
//        m_positiveButton = (Button)getWindow().getDecorView().findViewById(R.id.dialog_positiveButton);
        if (m_positiveButton != null) {
            m_positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (m_clickListener != null)
                        m_clickListener.onConfirmClick();
                    dismiss(); //dialog消失
                }
            });
        }

        if (m_negativeButton != null) {
            m_negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (m_clickListener != null)
                        m_clickListener.onCancelClick();
                    dismiss();
                }
            });
        }
    }

    //帮助构建dialog的辅助类
    public static class Builder {
        private final Context m_context;
     //   Typeface m_typeface; //字体，默认为Roboto-Medium.ttf
        private View m_behindView;
        private String m_dialogTitle;
        private String m_contentText;
        private String m_positiveClickText, m_negativeClickText;
        private int m_titleColor, m_contentColor, m_positiveButtonColor, m_negativeButtonColor;
        private int m_titleSize, m_contentSize, m_buttonSize;
        private Button m_positiveButton, m_negativeButton;
        private boolean m_isMotai = false; //是否用模态

        public Builder(Context context) {
            m_context = context;

    //        m_typeface = Typeface.createFromAsset(this.m_context.getResources()
    //                .getAssets(), "Roboto-Medium.ttf");
        }

        //设置dialog是否为模态
        public Builder setMode(boolean isMode) {
            m_isMotai = isMode;
            return this;
        }

        //设置自定义的dialog的视图
        public Builder setView(View contentView) {
            m_behindView = contentView;
            return this;
        }

        //视图不存在是默认的dialog的内容
        public Builder setContent(String content) {
            m_contentText = content;
            return this;
        }

        //设置dialog的标题
        public Builder setTitle(String title) {
            m_dialogTitle = title;
            return this;
        }

        //设置dialog标题的字体颜色
        public Builder setTitleColor(String color) {
            m_titleColor = Color.parseColor(color);
            return this;
        }

        public Builder setTitleColor(int color) {
            m_titleColor = color;
            return this;
        }

        //设置默认内容的字体颜色
        public Builder setContentColor(int color) {
            m_contentColor = color;
            return this;
        }

        public Builder setContentColor(String color) {
            m_contentColor = Color.parseColor(color);
            return this;
        }

        //设置positiveButton的字体的颜色
        public Builder setPositiveColor(int color) {
            m_positiveButtonColor = color;
            return this;
        }

        public Builder setPositiveColor(String color) {
            m_positiveButtonColor = Color.parseColor(color);
            return this;
        }

        //设置negetiveButton的字体的颜色
        public Builder setNegativeColor(String color) {
            m_negativeButtonColor = Color.parseColor(color);
            return this;
        }

        public Builder setNegativeColor(int color) {
            m_negativeButtonColor = color;
            return this;
        }

        //设置positiveButton的text
        public Builder setPositiveButtonText(int positiveButtonTextId) {
            m_positiveClickText = m_context.getString(positiveButtonTextId);
            return this;
        }

        public Builder setPositiveButtonText(String positiveButtonText) {
            m_positiveClickText = positiveButtonText;
            return this;
        }

        //设置negativeButton的text
        public Builder setNegativeButtonText(int negativeButtonTextId) {
            m_negativeClickText = m_context.getString(negativeButtonTextId);
            return this;
        }

        public Builder setNegativeButtonText(String negativeButtonText) {
            m_negativeClickText = negativeButtonText;
            return this;
        }

        public Builder setTitleSize(int size) {
            m_titleSize = size;
            return this;
        }

        public Builder setContentSize(int size) {
            m_contentSize = size;
            return this;
        }

        public Builder setButtonSize(int size) {
            m_buttonSize = size;
            return this;
        }

        //初始化dialog的样式
        private void initBehindDialog(View layout) {
            TextView text = null;
            if (m_dialogTitle != null) {
                text = (TextView)layout.findViewById(R.id.dialog_behind_title);
                text.setVisibility(View.VISIBLE);
                text.setText(m_dialogTitle == null ? DEFAULT_TITLE : m_dialogTitle );
                text.setTextColor(m_titleColor == 0 ? DEFAULT_TITLECOLOR : m_titleColor);
                text.setTextSize(m_titleSize == 0 ? DEFAULT_TITLESIZE : m_titleSize);
            }
            if (m_contentText != null) {
                text = (TextView)layout.findViewById(R.id.dialog_behind_content);
                text.setVisibility(View.VISIBLE);
                text.setText(m_contentText == null ? DEFAULT_CONTENT : m_contentText);
                text.setTextColor(m_contentColor == 0 ? DEFAULT_CONTENTCOLOR : m_contentColor);
                text.setTextSize(m_contentSize == 0 ? DEFAULT_CONTENTSIZE : m_contentSize);
            }

            if (m_positiveClickText != null) {
                m_positiveButton = (Button)layout.findViewById(R.id.dialog_positiveButton);
                m_positiveButton.setVisibility(View.VISIBLE);
                m_positiveButton.setText(m_positiveClickText == null ? DEFAULT_POSITIVETEXT : m_positiveClickText);
                m_positiveButton.setTextColor(m_positiveButtonColor == 0 ? DEFAULT_BUTTONCOLOR : m_positiveButtonColor);
                m_positiveButton.setTextSize(m_buttonSize == 0 ? DEFAULT_BUTTONSIZE : m_buttonSize);
            }
            if (m_negativeClickText != null) {
                m_negativeButton = (Button)layout.findViewById(R.id.dialog_negativeButton);
                m_negativeButton.setVisibility(View.VISIBLE);
                m_negativeButton.setText(m_negativeClickText == null ? DEFAULT_NEGATIVETEXT : m_negativeClickText);
                m_negativeButton.setTextColor(m_negativeButtonColor == 0 ? DEFAULT_BUTTONCOLOR : m_negativeButtonColor);
                m_negativeButton.setTextSize(m_buttonSize == 0 ? DEFAULT_BUTTONSIZE : m_buttonSize);
            }

        }

        @SuppressWarnings("deprecated")
        public BaseLDialog create() {
            LayoutInflater inflater = (LayoutInflater)
                    m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.dialog_behind, null);
            initBehindDialog(layout);

            final BaseLDialog dialog = new BaseLDialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(layout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (m_behindView != null) {
                ((LinearLayout)layout.findViewById(R.id.content_behind_view))
                        .addView(m_behindView, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            return dialog;
        }
    }
}
