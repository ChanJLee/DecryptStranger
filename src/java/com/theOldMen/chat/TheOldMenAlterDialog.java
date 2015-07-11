package com.theOldMen.chat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.gc.materialdesign.views.Button;
import com.theOldMen.Activity.R;

/**
 * Created by 李嘉诚 on 2015/5/4.
 * 最后修改时间: 2015/5/4
 */
public class TheOldMenAlterDialog extends Activity {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static final String s_message = "message";
    public static final String s_bitmap = "bitmap";
    public static final String s_postion = "postion";
    public static final String s_title = "title";
    public static final String s_cancelTitle  = "cancel_title";
    public static final String s_cancelShow = "cancel_show";
    public static final String s_isTextEditShow = "text_edit_show";
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView        m_textView      = null;
    private Button m_button                 = null;
    private int             m_position      = 0;
    private ImageView       m_imageView     = null;
    private EditText        m_editText      = null;
    private boolean         m_isShow        = true;
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        setContentView(R.layout.the_old_men_alter_dialog);

        init();
    }

    public void init(){

        //初始化部件
        m_textView               = (TextView) findViewById(R.id.m_alterDialogTitleTextView);
        m_button                 = (Button) findViewById(R.id.m_alterDialogCancel);
        m_imageView              = (ImageView) findViewById(R.id.m_alterDialogImageView);
        m_editText               = (EditText) findViewById(R.id.m_alterDialogEditText);

        //获得发送的额外信息
        //提示内容
        String msg               = (String) getIntent().getSerializableExtra(s_message);
        //提示标题
        String title             = (String) getIntent().getSerializableExtra(s_title);
        m_position               = (int) getIntent().getSerializableExtra(s_postion);


        //是否显示取消标题
        boolean isCancelTitle    = (boolean) getIntent().getSerializableExtra(s_cancelTitle);
        //是否显示取消按钮
        boolean isCancelShow     = (boolean) getIntent().getSerializableExtra(s_cancelShow);
        //是否显示文本编辑框
        m_isShow                 = (boolean) getIntent().getSerializableExtra(s_isTextEditShow);
        //转发复制的图片的path
      //  Bitmap  bitmap           = getIntent().getSerializableExtra(s_bitmap)
        //要文本内容
        String editText          = getIntent().getStringExtra("edit_text");

        if(msg != null) ((TextView)findViewById(R.id.m_alterDialogTitleTextView)).setText(msg);
        if(title != null) m_textView.setText(title);
        if(isCancelTitle) m_textView.setVisibility(View.GONE);
        if(isCancelShow) m_button.setVisibility(View.VISIBLE);

  //      m_imageView.setImageBitmap(bitmap);
        if(m_isShow){
            m_editText.setVisibility(View.VISIBLE);
            m_editText.setText(editText);
        }
    }
}
