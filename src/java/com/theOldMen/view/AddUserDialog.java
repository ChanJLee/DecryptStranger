package com.theOldMen.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import com.theOldMen.Activity.R;
import com.theOldMen.service.XXService;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.T;

import org.jivesoftware.smack.RosterEntry;

/**
 * Created by cheng on 2015/5/8.
 */
public class AddUserDialog extends AlertDialog implements TextWatcher{
    private static String TAG = "AddUserDialog";

    private EditText m_accountNumberEdit;
    private EditText m_nickNameEdit;
    private BetterSpinner m_betterSpinner;
    private Context m_context;
    private Button m_okButton;
    private int m_textColor;
    private boolean m_isAddNewGroup = false;
    final private View m_editView;

    private XXService m_service;
    public AddUserDialog(Context context, final XXService service, final ArrayAdapter<String> adapter) {
        super(context);
        m_context = context;
        m_service = service;
        LayoutInflater layoutInflater = (LayoutInflater)
                m_context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        m_editView = layoutInflater.inflate(R.layout.adduserdialog, null);
        m_accountNumberEdit = (EditText)m_editView.findViewById(R.id.account_number);
        m_nickNameEdit = (EditText)m_editView.findViewById(R.id.nickname);
        m_betterSpinner = (BetterSpinner)m_editView.findViewById(R.id.better_spinner);
        setTitle("添加好友");
        this.setView(m_editView);
        m_betterSpinner.setAdapter(adapter);
        m_betterSpinner.setText(m_context.getString(R.string.default_group));

        setButton(BUTTON_POSITIVE, m_context.getString(android.R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TextView errorField = (TextView) m_editView.findViewById(R.id.errorFieldView);
                //在这里只检查备注名和分组的输入情况
                String groupName = m_betterSpinner.getText().toString();
                //后台需要groupName，m_isAddNewGroup，备注名三个参数进行检查
                //这里只做简单检查
                if (m_nickNameEdit.getText().toString().equals("")) {
                    try {
                        Field field = dialog.getClass()
                                .getSuperclass()
                                .getSuperclass()
                                .getDeclaredField("mShowing");
                        field.setAccessible(true);
                        field.set(dialog, false);
                    } catch (Exception e) {
                        Log.d(TAG, "异常");
                    }
                    Log.d(TAG, "非法输入");
                    errorField.setVisibility(View.VISIBLE);
                    errorField.setText("备注为空!");
                } else {
                    if(isInGroup(m_accountNumberEdit.getText().toString()+"@"+ PreferenceConstants.DOMAIN)){
                        try {
                            Field field = dialog.getClass()
                                    .getSuperclass()
                                    .getSuperclass()
                                    .getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        errorField.setVisibility(View.VISIBLE);
                        errorField.setText("该用户已经是你的好友!");
                    }
                    /////////////////////////////////////////////////
                    //后台添加友，并刷新界面
                    else if (!m_service.addRosterItem(m_accountNumberEdit.getText().toString()
                            , m_nickNameEdit.getText().toString()
                            , (groupName.equals(m_context.getString(R.string.default_group)) ? "" : groupName))) {
                        try {
                            Field field = dialog.getClass()
                                    .getSuperclass()
                                    .getSuperclass()
                                    .getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        errorField.setVisibility(View.VISIBLE);
                        errorField.setText("要添加的用户不存在!");
                    }else {
                        try {
                            Field field = dialog.getClass()
                                    .getSuperclass()
                                    .getSuperclass()
                                    .getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, true);
                        } catch (Exception e) {
                        }
                        errorField.setVisibility(View.GONE);
                        Log.d(TAG, "合法输入");
                    }
                    dialog.dismiss();
                }
            }
        });
        setButton(BUTTON_NEGATIVE, m_context.getString(android.R.string.cancel),
                new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Field field = dialog.getClass()
                                    .getSuperclass()
                                    .getSuperclass()
                                    .getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, true);
                        } catch (Exception e) {
                        }
                    }
                });
    }

    private boolean isInGroup(String user){
        ArrayList<String>arrayList = m_service.getRosterEnteries();
        return arrayList.contains(user);
    }
//
//    public AddUserDialog(Context context, XXService service, ArrayAdapter<String> adapter,
//                         String account_number) {
//        this(context, service, adapter);
//        m_accountNumberEdit.setText(account_number);
//    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        m_okButton = getButton(BUTTON_POSITIVE);
        m_textColor = m_accountNumberEdit.getCurrentTextColor();
        afterTextChanged(m_accountNumberEdit.getText());
        m_accountNumberEdit.addTextChangedListener(this);
    }

    public void afterTextChanged(Editable e) {
        /////////////////////////////////
        try {
            ///////////////////
            /*在这里后台服务对e的text进行检查，以判断输入的账号是否符合格式*/
            check(e);
            m_okButton.setEnabled(true);
            m_accountNumberEdit.setTextColor(m_textColor);
        } catch(Exception ex) {
            m_okButton.setEnabled(false);
            m_accountNumberEdit.setTextColor(Color.RED);
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {}

    public void onTextChanged(CharSequence s, int start, int before, int count) {}


    private void check(Editable s) throws Exception {
        if (s.toString().equals(""))
            throw new Exception();
        return;
    }

}
