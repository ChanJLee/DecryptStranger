package com.theOldMen.maininterface;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.theOldMen.Activity.LoginActivity;
import com.theOldMen.Activity.MainActivity;
import com.theOldMen.Activity.R;
import com.theOldMen.CircleImage.CircleImageView;
import com.theOldMen.tools.PreferenceConstants;
import com.theOldMen.tools.PreferenceUtils;
import com.theOldMen.util.DialogUtil;
import com.theOldMen.view.BetterSpinner;
import com.theOldMen.view.LDialog.BaseLDialog;
import com.theOldMen.util.PictureUtils;
import com.theOldMen.view.Switch;

import org.jivesoftware.smackx.packet.VCard;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SettingsFragment extends Fragment implements View.OnClickListener, Switch.OnCheckedChangeListener {

    private static final int REQUEST_FROM_GALLERY = 0;
    private static final int REQUEST_FROM_CAMERA = 1;
    private static final int REQUEST_TO_SOLVED = 2;
    private static final String TAG = "SettingFragment";

    private String avatarPath;
    private CircleImageView m_headView; //设置界面的头像
    private TextView m_accountName; //设置界面的用户名
    private RelativeLayout m_modifyName; //更改用户的用户名
    private RelativeLayout m_modifySex; //更改用户的性别
    private RelativeLayout m_aboutButton; //关于的按钮
    private RelativeLayout m_logoutButton; //反馈的按钮
    private FragmentCallback m_callback;

    private Switch m_showOfflineSwitch;
    private Switch m_muteSwitch;
    private Switch m_vibratorSwitch;

    private VCard vCard ;

    private void handleCommentField(LinearLayout linearLayout) {
        if (linearLayout == null) return;
        ((ImageView) linearLayout.findViewById(R.id.titleStatus)).setVisibility(View.GONE);
        ((ProgressBar) linearLayout.findViewById(R.id.titleProgress)).setVisibility(View.GONE);
        TextView title = (TextView) linearLayout.findViewById(R.id.comment_title);
        title.setText(R.string.icon_menu_setting);
//        m_title.setOnClickListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            m_callback = (FragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Something Wrong..!");
        }
    }

    private boolean isConnected() {
        //检测是否连接的方法
        return m_callback.isConnected();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout = ((MainActivity) getActivity()).getCommentField();//获得标题栏区域
        handleCommentField(linearLayout);

        vCard = (isConnected())?m_callback.getService().getVCard():null;
        avatarPath = "theOldMen/" + PreferenceUtils.getPrefString(getActivity(),
                com.theOldMen.tools.PreferenceConstants.ACCOUNT, "") + "_avatar.jpg";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        m_headView = (CircleImageView) view.findViewById(R.id.setting_head);
        m_accountName = (TextView) view.findViewById(R.id.account_name);
        m_modifyName = (RelativeLayout) view.findViewById(R.id.modify_name);
        m_modifySex = (RelativeLayout) view.findViewById(R.id.modify_sex);

        m_aboutButton = (RelativeLayout) view.findViewById(R.id.about);
        m_logoutButton = (RelativeLayout) view.findViewById(R.id.logout);

        //设置监听
        m_headView.setOnClickListener(this);
        m_accountName.setOnClickListener(this);
        m_modifyName.setOnClickListener(this);
        m_modifySex.setOnClickListener(this);
        m_aboutButton.setOnClickListener(this);
        m_logoutButton.setOnClickListener(this);

        m_showOfflineSwitch = (Switch)view.findViewById(R.id.showOfflineSwitch);
        m_muteSwitch = (Switch)view.findViewById(R.id.muteSwitch);
        m_vibratorSwitch = (Switch)view.findViewById(R.id.vibratorSwitch);

        m_showOfflineSwitch.setOnCheckedChangeListener(this);
        m_muteSwitch.setOnCheckedChangeListener(this);
        m_vibratorSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(Switch view, boolean checked) {
        switch (view.getId()) {
            case R.id.showOfflineSwitch:
                PreferenceUtils.setPrefBoolean(getActivity(), PreferenceConstants.SHOW_OFFLINE, checked);
                break;
            case R.id.muteSwitch:
                PreferenceUtils.setPrefBoolean(getActivity(), PreferenceConstants.SCLIENTNOTIFY, checked);
                break;
            case R.id.vibratorSwitch:
                PreferenceUtils.setPrefBoolean(getActivity(), PreferenceConstants.VIBRATIONNOTIFY, checked);
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 设置用户头像
        File file = new File(Environment.getExternalStorageDirectory(), avatarPath);
        if (file.exists())
            m_headView.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        else m_headView.setImageResource(R.drawable.avatar);

        if(isConnected()) {

            vCard = m_callback.getService().getVCard();

            m_accountName.setText((isConnected()) ? vCard.getNickName() : PreferenceUtils.getPrefString(getActivity(), PreferenceConstants.ACCOUNT, ""));
            m_showOfflineSwitch.setChecked(PreferenceUtils.getPrefBoolean(getActivity(),
                    PreferenceConstants.SHOW_OFFLINE, true));
            m_muteSwitch.setChecked(PreferenceUtils.getPrefBoolean(getActivity(),
                    PreferenceConstants.SCLIENTNOTIFY, false));
            m_vibratorSwitch.setChecked(PreferenceUtils.getPrefBoolean(getActivity(),
                    PreferenceConstants.VIBRATIONNOTIFY, false));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isConnected()) {

            m_callback.getService().saveCard(vCard);

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting_head:
                showHeadModifyDialog();
                break;
            case R.id.modify_name:
                showModifyNameDialog();
                break;
            case R.id.modify_sex:
                showModifySexDialog();
                break;
            case R.id.about:
                showAboutDialog();
                break;
            case R.id.logout:
                showLogoutDialog();
                break;
        }
    }

    public void showHeadModifyDialog() {
        BaseLDialog.Builder builder = new BaseLDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_modify_head, null);
        final BaseLDialog dialog = builder.setTitle("更改头像")
                .setTitleColor("#434343")
                .setTitleSize(20)
                .setMode(false)
                .setView(view)
                .setPositiveButtonText("取消")
                .setButtonSize(18)
                .setPositiveColor("#cccccc")
                .create();
        Button camera = (Button) view.findViewById(R.id.open_camera);
        Button gallery = (Button) view.findViewById(R.id.open_gallery);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(PictureUtils.createOpenSystemCameraIntent(avatarPath),
                        REQUEST_FROM_CAMERA);
                dialog.dismiss();
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(PictureUtils.createOpenSystemGalleryIntent(),
                        REQUEST_FROM_GALLERY);
                //打开相册，dialog消失
                dialog.dismiss();
            }
        });

        dialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
            }

            @Override
            public void onCancelClick() {
            }
        });
        DialogUtil.setPopupDialog(getActivity(), dialog);
        dialog.show();
    }

    public void showModifyNameDialog() {
        BaseLDialog.Builder builder = new BaseLDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.layout_modify_name, null);
        final EditText editText = (EditText) view.findViewById(R.id.modify_name_editText);
        BaseLDialog dialog =
                builder.setTitle("更改用户名")
                        .setTitleColor("#434343")
                        .setMode(false)
                        .setView(view)
                        .setPositiveButtonText("确定")
                        .setNegativeButtonText("取消")
                        .setPositiveColor("#3c78d8")
                        .setNegativeColor("#cccccc")
                        .create();
        dialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                if(!isConnected()) {
                    Toast.makeText(getActivity(), "网络未连接，请检查连接情况", Toast.LENGTH_SHORT).show();
                } else {
                    String newNickName = editText.getText().toString();
                    if (isConnected()) {
                        if (vCard == null)
                            vCard = m_callback.getService().getVCard();
                        vCard.setNickName(newNickName);
                        m_callback.getService().saveCard(vCard);
                        m_accountName.setText(newNickName);
                    }
                }
            }

            @Override
            public void onCancelClick() {
            }
        });

        dialog.show();
    }

    public void showModifySexDialog() {
        BaseLDialog.Builder builder = new BaseLDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater)
                getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.layout_modify_sex, null);
        /////////////////////////////////////////////////////////////////////////
        //
        final BetterSpinner betterSpinner = (BetterSpinner) view.findViewById(R.id.sex_choose_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"男", "女"});
        betterSpinner.setAdapter(adapter);
        betterSpinner.setText((isConnected())?vCard.getField("Sex"):"男"); //从服务器端获取用户性别信息
        BaseLDialog dialog =
                builder.setTitle("更改用户名性别")
                        .setTitleColor("#434343")
                        .setMode(false)
                        .setView(view)
                        .setPositiveButtonText("确定")
                        .setNegativeButtonText("取消")
                        .setPositiveColor("#3c78d8")
                        .setNegativeColor("#cccccc")
                        .create();
        dialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                if(!isConnected()) {
                    Toast.makeText(getActivity(), "网络未连接，请检查连接情况", Toast.LENGTH_SHORT).show();
                } else {
                   vCard.setField("Sex",betterSpinner.getText().toString());
                   m_callback.getService().saveCard(vCard);
//                   vCard = m_callback.getService().getVCard();
                }
            }

            @Override
            public void onCancelClick() {

            }
        });

        dialog.show();
    }

    public void showAboutDialog() {
        BaseLDialog.Builder builder = new BaseLDialog.Builder(getActivity());
        BaseLDialog dialog =
                builder.setTitle("关于")
                        .setTitleColor("#434343")
                        .setContent("解密陌生人")
                        .setContentColor("#bcb8b7")
                        .setMode(false)
                        .setPositiveButtonText("确定")
                        .setPositiveColor("#3c78d8")
                        .create();
        dialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
            }

            @Override
            public void onCancelClick() {
            }
        });
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            case REQUEST_FROM_CAMERA:
                if (resultCode == Activity.RESULT_OK) {
                    Intent myIntent= PictureUtils.createCropPicIntent(Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                            avatarPath)), 200, 200, avatarPath);
                    startActivityForResult(myIntent, REQUEST_TO_SOLVED);
                }
                break;
            case REQUEST_FROM_GALLERY:
                Uri uri = intent.getData();
                Intent intent1 = PictureUtils.createCropPicIntent(uri, 200, 200, avatarPath);
                startActivityForResult(intent1, REQUEST_TO_SOLVED);
                break;
            case REQUEST_TO_SOLVED:
                FileInputStream fileInputStream = null;
                //打开头像文件
                File f = new File(Environment.getExternalStorageDirectory(), avatarPath);
                try {
                    fileInputStream = new FileInputStream(f);
                    m_headView.setImageBitmap(BitmapFactory.decodeStream(fileInputStream));
                    fileInputStream.close();

                    vCard.setAvatar(PictureUtils.FileToByte(f));

                } catch (IOException e) {
                    Log.e(TAG, "error handle the file", e);
                    Log.e(TAG, "file : " + f.getAbsolutePath());
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, intent);
        }
    }


    public void showLogoutDialog() {
        BaseLDialog.Builder builder = new BaseLDialog.Builder(getActivity());
        BaseLDialog baseLDialog =
                builder.setTitle("提示")
               .setTitleColor("#434343")
               .setContent("注销本账号, 并使用其他账号登陆?")
               .setContentColor(Color.parseColor("red"))
               .setContentSize(20)
               .setPositiveButtonText("确定")
               .setPositiveColor("#3c78d8")
               .setNegativeButtonText("取消")
               .setNegativeColor("#cccccc")
               .create();
        baseLDialog.setListeners(new BaseLDialog.ClickListener() {
            @Override
            public void onConfirmClick() {
                //////////////////////////////////////////

                if(isConnected())
                    m_callback.getService().logout();
                startActivity(new Intent(getActivity(),
                        LoginActivity.class));
                getActivity().finish();
            }

            @Override
            public void onCancelClick() {
            }
        });
        DialogUtil.setPopupDialog(getActivity(), baseLDialog);
        baseLDialog.show();
    }
}
