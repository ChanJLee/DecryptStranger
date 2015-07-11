package com.theOldMen.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import com.theOldMen.chat.TheOldMenAlterDialog;
import com.theOldMen.chat.TheOldMenChatMainActivity;

/**
 * Created by 李嘉诚 on 2015/5/4.
 * 最后修改时间: 2015/5/4
 */
public class TheOldMenTextEdit extends EditText {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private Context m_context;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //ctor
    public TheOldMenTextEdit(Context context) {
        super(context);

        m_context = context;
    }
    public TheOldMenTextEdit(Context context, AttributeSet attrs) {
        super(context, attrs);

        m_context = context;
    }
    public TheOldMenTextEdit(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        m_context = context;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressLint("NewApi")
    @Override
    public boolean onTextContextMenuItem(int id) {

        //如果是复制
        if(id == android.R.id.paste){

            //获得剪切板中的内容
            @SuppressWarnings("deprecation")
            ClipboardManager clip = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            String text           = clip.getText().toString();

            if(text.startsWith(TheOldMenChatMainActivity.s_copyImage)){

                //将头部的标识文字删掉
                text = text.replace(TheOldMenChatMainActivity.s_copyImage, "");

                //设置标题
                //转发的图片
                Intent intent = new Intent(m_context,TheOldMenAlterDialog.class);
                intent.putExtra("title", "发送以下图片?");
                intent.putExtra("forwardImage", text);
                intent.putExtra("cancel", true);

                //启动新的activity
                ((Activity)m_context).startActivityForResult(
                        intent,
                        TheOldMenChatMainActivity.s_copyAndPaste
                );
            }
        }

        //响应父动作
        return super.onTextContextMenuItem(id);
    }



    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {

        //如果内容变化了
        //内容分控
        //并且是发送图片
        if(!TextUtils.isEmpty(text) &&
                text.toString().startsWith(
                        TheOldMenChatMainActivity.s_copyImage)){

            //那么清空内容
            setText("");
        }

        //调用父方法
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

}
